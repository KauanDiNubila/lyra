import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { Food } from '../foods/foodApi.ts'
import type { DiaryDay } from './diaryApi.ts'
import { DiaryPage } from './DiaryPage.tsx'
import { day, emptyDay, jsonResponse, lunchRice, milkshake, nutella, rice } from './testSupport.ts'

const fetchMock = vi.fn<typeof fetch>()

function serve(options: { day?: DiaryDay; foods?: Food[]; post?: (init: RequestInit) => Promise<Response> }) {
  fetchMock.mockImplementation(async (url, init) => {
    const path = String(url)
    if (path.endsWith('/plan')) return jsonResponse({ detail: 'There is no active plan to fill the day' }, 404)
    if (init?.method === 'POST') {
      return options.post ? options.post(init) : jsonResponse(lunchRice, 201)
    }
    if (path === '/api/foods/barcode/7891000100103') return jsonResponse(nutella)
    if (path.startsWith('/api/foods/barcode/')) return jsonResponse({ detail: 'Product not found' }, 404)
    if (path.startsWith('/api/foods')) return jsonResponse(options.foods ?? [rice, milkshake])
    return jsonResponse(options.day ?? emptyDay('2026-11-19'))
  })
}

function posts() {
  return fetchMock.mock.calls.filter(([url, init]) => init?.method === 'POST' && !String(url).endsWith('/plan'))
}

function dayRequests() {
  return fetchMock.mock.calls.filter(([url]) => String(url) === '/api/diary/2026-11-19')
}

async function pickFood(name: string) {
  fireEvent.click(await screen.findByRole('button', { name: 'Adicionar' }))
  fireEvent.change(await screen.findByLabelText('Buscar alimento'), { target: { value: 'arroz' } })
  const sheet = await screen.findByRole('dialog')
  fireEvent.click(await within(sheet).findByText(name))
  return screen.findByRole('form', { name: 'Adicionar ao diário' })
}

function setAmount(form: HTMLElement, label: string, value: string) {
  const field = within(form).getByLabelText(label, { exact: false })
  fireEvent.change(field, { target: { value } })
  fireEvent.blur(field)
}

function addButton(form: HTMLElement) {
  return within(form).getByRole('button', { name: 'Adicionar ao dia' })
}

beforeEach(() => {
  fetchMock.mockReset()
  vi.stubGlobal('fetch', fetchMock)
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 10, 19, 10, 0))
})

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

describe('adding a food to the diary', () => {
  it('searches a food, picks the meal and sends the quantity in the unit of the food', async () => {
    serve({ day })
    render(<DiaryPage />)

    const form = await pickFood('Arroz, tipo 1, cozido')
    setAmount(form, 'Quantidade', '150')
    fireEvent.click(within(form).getByRole('button', { name: 'Almoço' }))
    fireEvent.click(addButton(form))

    await waitFor(() => expect(posts()).toHaveLength(1))
    expect(posts()[0][0]).toBe('/api/diary/2026-11-19/entries')
    expect(JSON.parse(String(posts()[0][1]?.body))).toEqual({
      foodId: 'rice',
      mealName: 'Almoço',
      quantity: { amount: 150, unit: 'GRAM' },
    })
    await waitFor(() => expect(screen.queryByRole('form', { name: 'Adicionar ao diário' })).not.toBeInTheDocument())
    expect(dayRequests()).toHaveLength(2)
  })

  it('picks a packaged food by its barcode and logs it', async () => {
    serve({ day })
    render(<DiaryPage />)
    fireEvent.click(await screen.findByRole('button', { name: 'Adicionar' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Código de barras' }))
    const barcode = await screen.findByRole('form', { name: 'Buscar por código de barras' })
    expect(within(barcode).getByText(/Open Food Facts/)).toBeInTheDocument()

    fireEvent.change(within(barcode).getByLabelText('Código de barras', { exact: false }), { target: { value: '7891000100103' } })
    fireEvent.click(within(barcode).getByRole('button', { name: 'Buscar produto' }))
    const form = await screen.findByRole('form', { name: 'Adicionar ao diário' })
    fireEvent.click(within(form).getByRole('button', { name: 'Almoço' }))
    fireEvent.click(addButton(form))

    await waitFor(() => expect(posts()).toHaveLength(1))
    expect(JSON.parse(String(posts()[0][1]?.body))).toEqual({
      foodId: 'nutella',
      mealName: 'Almoço',
      quantity: { amount: 100, unit: 'GRAM' },
    })
  })

  it('tells when the barcode is unknown and lets the user search by name again', async () => {
    serve({ day })
    render(<DiaryPage />)
    fireEvent.click(await screen.findByRole('button', { name: 'Adicionar' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Código de barras' }))
    const barcode = await screen.findByRole('form', { name: 'Buscar por código de barras' })

    fireEvent.change(within(barcode).getByLabelText('Código de barras', { exact: false }), { target: { value: '12345678' } })
    fireEvent.click(within(barcode).getByRole('button', { name: 'Buscar produto' }))

    expect(await screen.findByText('Produto não encontrado')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cadastrar alimento' })).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Buscar por nome' }))
    expect(await screen.findByLabelText('Buscar alimento')).toBeInTheDocument()
  })

  it('starts from the reference amount of the food and follows its unit', async () => {
    serve({ day })
    render(<DiaryPage />)

    const form = await pickFood('Milkshake de morango')

    expect(within(form).getByLabelText('Quantidade (porção)', { exact: false })).toHaveDisplayValue('1')
  })

  it('offers the meals of the day first and then the usual ones, without repeating', async () => {
    serve({ day })
    render(<DiaryPage />)

    const form = await pickFood('Arroz, tipo 1, cozido')

    const names = within(form).getAllByRole('button').map((button) => button.textContent)
    expect(names).toEqual(expect.arrayContaining(['Almoço', 'Jantar', 'Café da manhã', 'Lanche', 'Ceia']))
    expect(names.filter((name) => name === 'Almoço')).toHaveLength(1)
    expect(names.indexOf('Jantar')).toBeLessThan(names.indexOf('Café da manhã'))
  })

  it('accepts a meal typed by the user', async () => {
    serve({})
    render(<DiaryPage />)
    const form = await pickFood('Arroz, tipo 1, cozido')

    fireEvent.change(within(form).getByLabelText('Refeição', { exact: false }), { target: { value: '  Pós-treino ' } })
    fireEvent.click(addButton(form))

    await waitFor(() => expect(posts()).toHaveLength(1))
    expect(JSON.parse(String(posts()[0][1]?.body)).mealName).toBe('Pós-treino')
  })

  it('keeps the add button disabled until the meal is chosen', async () => {
    serve({})
    render(<DiaryPage />)
    const form = await pickFood('Arroz, tipo 1, cozido')

    expect(addButton(form)).toHaveAttribute('aria-disabled', 'true')
    fireEvent.click(within(form).getByRole('button', { name: 'Lanche' }))

    expect(addButton(form)).not.toHaveAttribute('aria-disabled', 'true')
  })

  it('goes back to the search with the same text when the user changes the food', async () => {
    serve({})
    render(<DiaryPage />)
    const form = await pickFood('Arroz, tipo 1, cozido')

    fireEvent.click(within(form).getByRole('button', { name: 'Trocar alimento' }))

    expect(await screen.findByLabelText('Buscar alimento')).toHaveValue('arroz')
    expect(await screen.findByText('Milkshake de morango')).toBeInTheDocument()
  })

  it('sends the entry once when the user adds twice', async () => {
    let answerPost: (response: Response) => void = () => {}
    serve({ post: () => new Promise((resolve) => { answerPost = resolve }) })
    render(<DiaryPage />)
    const form = await pickFood('Arroz, tipo 1, cozido')
    fireEvent.click(within(form).getByRole('button', { name: 'Almoço' }))

    fireEvent.click(addButton(form))
    fireEvent.click(addButton(form))
    fireEvent.submit(form)

    expect(posts()).toHaveLength(1)
    await act(async () => answerPost(jsonResponse(lunchRice, 201)))
    await waitFor(() => expect(screen.queryByRole('form', { name: 'Adicionar ao diário' })).not.toBeInTheDocument())
    expect(posts()).toHaveLength(1)
  })

  it.each([
    ['the server rejects the entry', () => Promise.resolve(jsonResponse({ detail: 'Quantity must be in GRAM' }, 400)), 'Quantity must be in GRAM'],
    ['the server cannot be reached', () => Promise.reject(new TypeError('Failed to fetch')), 'Sem conexão com o servidor. Verifique a internet e tente de novo.'],
  ])('keeps the sheet open and ready for another try when %s', async (_case, answerPost, message) => {
    serve({ post: answerPost })
    render(<DiaryPage />)
    const form = await pickFood('Arroz, tipo 1, cozido')
    fireEvent.click(within(form).getByRole('button', { name: 'Almoço' }))

    fireEvent.click(addButton(form))

    expect(await screen.findByText(message)).toBeInTheDocument()
    expect(screen.getByText('Não foi possível adicionar')).toBeInTheDocument()
    expect(screen.getByRole('form', { name: 'Adicionar ao diário' })).toBeInTheDocument()
    await waitFor(() => expect(addButton(form)).not.toHaveAttribute('aria-disabled', 'true'))
  })

  it('points to the quick add when the search finds nothing', async () => {
    serve({ foods: [] })
    render(<DiaryPage />)
    fireEvent.click(await screen.findByRole('button', { name: 'Adicionar' }))

    fireEvent.change(await screen.findByLabelText('Buscar alimento'), { target: { value: 'xyzzy' } })

    expect(await screen.findByText('Não achou? Use o botão abaixo para adicionar com os valores de uma porção.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Não achou? Adicionar rápido' })).toBeInTheDocument()
  })

  it('opens the same sheet from the empty day', async () => {
    serve({})
    render(<DiaryPage />)

    fireEvent.click(await screen.findByRole('button', { name: 'Adicionar alimento' }))

    expect(await screen.findByLabelText('Buscar alimento')).toBeInTheDocument()
  })
})

async function openQuickAdd(query: string) {
  fireEvent.click(await screen.findByRole('button', { name: 'Adicionar' }))
  fireEvent.change(await screen.findByLabelText('Buscar alimento'), { target: { value: query } })
  fireEvent.click(await screen.findByRole('button', { name: 'Não achou? Adicionar rápido' }))
  return screen.findByRole('form', { name: 'Adicionar rápido' })
}

function fillQuickAdd(form: HTMLElement) {
  setAmount(form, 'Porções', '2')
  setAmount(form, 'Calorias', '550')
  setAmount(form, 'Proteína', '25')
  setAmount(form, 'Carboidrato', '45')
  setAmount(form, 'Gordura', '30')
  fireEvent.click(within(form).getByRole('button', { name: 'Almoço' }))
}

describe('the quick add', () => {
  it('is offered next to the results too, with the typed text as the name', async () => {
    serve({})
    render(<DiaryPage />)

    const form = await openQuickAdd('Big Mac')

    expect(within(form).getByLabelText('Nome', { exact: false })).toHaveValue('Big Mac')
  })

  it('puts the focus on the name when the form opens', async () => {
    serve({})
    render(<DiaryPage />)

    const form = await openQuickAdd('Big Mac')

    await waitFor(() => expect(within(form).getByLabelText('Nome', { exact: false })).toHaveFocus())
  })

  it('only allows adding when the name, the portions, the four values and the meal are filled', async () => {
    serve({})
    render(<DiaryPage />)
    const form = await openQuickAdd('Big Mac')
    expect(addButton(form)).toHaveAttribute('aria-disabled', 'true')

    setAmount(form, 'Calorias', '550')
    setAmount(form, 'Proteína', '25')
    setAmount(form, 'Carboidrato', '45')
    fireEvent.click(within(form).getByRole('button', { name: 'Almoço' }))
    expect(addButton(form)).toHaveAttribute('aria-disabled', 'true')

    setAmount(form, 'Gordura', '30')
    await waitFor(() => expect(addButton(form)).not.toHaveAttribute('aria-disabled', 'true'))
  })

  it('sends the values of one portion and the number of portions, then shows the day again', async () => {
    serve({})
    render(<DiaryPage />)
    const form = await openQuickAdd('Big Mac')
    fillQuickAdd(form)
    await waitFor(() => expect(addButton(form)).not.toHaveAttribute('aria-disabled', 'true'))
    const requestsBefore = dayRequests().length

    fireEvent.click(addButton(form))

    await waitFor(() => expect(posts()).toHaveLength(1))
    const [url, init] = posts()[0]
    expect(String(url)).toBe('/api/diary/2026-11-19/quick-entries')
    expect(JSON.parse(String(init?.body))).toEqual({
      name: 'Big Mac',
      mealName: 'Almoço',
      portions: 2,
      macros: { kcal: 550, protein: 25, carbohydrate: 45, fat: 30 },
    })
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(dayRequests().length).toBeGreaterThan(requestsBefore)
  })

  it('sends once when the user clicks twice', async () => {
    let answer: (response: Response) => void = () => {}
    serve({ post: () => new Promise((resolve) => { answer = resolve }) })
    render(<DiaryPage />)
    const form = await openQuickAdd('Big Mac')
    fillQuickAdd(form)
    await waitFor(() => expect(addButton(form)).not.toHaveAttribute('aria-disabled', 'true'))

    fireEvent.click(addButton(form))
    fireEvent.click(addButton(form))

    expect(posts()).toHaveLength(1)
    await act(async () => answer(jsonResponse(lunchRice, 201)))
    expect(posts()).toHaveLength(1)
  })

  it('says why the server refused and keeps what the user typed', async () => {
    serve({ post: () => Promise.resolve(jsonResponse({ detail: 'Food name must have between 2 and 200 characters' }, 400)) })
    render(<DiaryPage />)
    const form = await openQuickAdd('Big Mac')
    fillQuickAdd(form)
    await waitFor(() => expect(addButton(form)).not.toHaveAttribute('aria-disabled', 'true'))

    fireEvent.click(addButton(form))

    const message = await screen.findByText('Food name must have between 2 and 200 characters')
    const fat = within(form).getByLabelText('Gordura', { exact: false })
    expect(fat.compareDocumentPosition(message) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(message.compareDocumentPosition(addButton(form)) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(screen.getByText('Não foi possível adicionar')).toBeInTheDocument()
    expect(within(screen.getByRole('form', { name: 'Adicionar rápido' })).getByLabelText('Nome', { exact: false })).toHaveValue('Big Mac')
    await waitFor(() => expect(addButton(form)).not.toHaveAttribute('aria-disabled', 'true'))
  })

  it('goes back to the search with the text the user had typed', async () => {
    serve({})
    render(<DiaryPage />)
    const form = await openQuickAdd('arroz')

    fireEvent.click(within(form).getByRole('button', { name: 'Voltar à busca' }))

    expect(await screen.findByLabelText('Buscar alimento')).toHaveValue('arroz')
    expect(screen.queryByRole('form', { name: 'Adicionar rápido' })).not.toBeInTheDocument()
  })
})
