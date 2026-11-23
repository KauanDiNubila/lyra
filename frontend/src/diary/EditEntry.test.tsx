import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DiaryPage } from './DiaryPage.tsx'
import { day, jsonResponse, lunchRice } from './testSupport.ts'

const fetchMock = vi.fn<typeof fetch>()

type Handlers = {
  patch?: () => Promise<Response>
  remove?: () => Promise<Response>
}

function serve({ patch, remove }: Handlers = {}) {
  fetchMock.mockImplementation(async (_url, init) => {
    if (init?.method === 'PATCH') return patch ? patch() : jsonResponse(lunchRice)
    if (init?.method === 'DELETE') return remove ? remove() : new Response(null, { status: 204 })
    return jsonResponse(day)
  })
}

function callsWith(method: string) {
  return fetchMock.mock.calls.filter(([, init]) => init?.method === method)
}

function dayRequests() {
  return fetchMock.mock.calls.filter(([url]) => String(url) === '/api/diary/2026-11-19')
}

async function openEntry() {
  fireEvent.click(await screen.findByText('Arroz, tipo 1, cozido'))
  return screen.findByRole('form', { name: 'Editar entrada' })
}

function setAmount(form: HTMLElement, value: string) {
  const field = within(form).getByLabelText('Quantidade', { exact: false })
  fireEvent.change(field, { target: { value } })
  fireEvent.blur(field)
}

function saveButton(form: HTMLElement) {
  return within(form).getByRole('button', { name: 'Salvar' })
}

async function askToRemove(form: HTMLElement) {
  fireEvent.click(within(form).getByRole('button', { name: 'Remover' }))
  const dialog = await screen.findByRole('alertdialog')
  return { dialog, confirm: () => fireEvent.click(within(dialog).getByRole('button', { name: 'Remover' })) }
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

describe('editing a diary entry', () => {
  it('opens the entry with its quantity and the unit of the food', async () => {
    serve()
    render(<DiaryPage />)

    const form = await openEntry()

    expect(within(form).getByLabelText('Quantidade (g)', { exact: false })).toHaveDisplayValue('150')
    expect(within(form).getByRole('heading', { name: 'Arroz, tipo 1, cozido' })).toBeInTheDocument()
  })

  it('sends the new quantity and loads the day again', async () => {
    serve()
    render(<DiaryPage />)
    const form = await openEntry()

    setAmount(form, '200')
    fireEvent.click(saveButton(form))

    await waitFor(() => expect(callsWith('PATCH')).toHaveLength(1))
    expect(callsWith('PATCH')[0][0]).toBe('/api/diary/entries/rice-entry')
    expect(JSON.parse(String(callsWith('PATCH')[0][1]?.body))).toEqual({ quantity: { amount: 200, unit: 'GRAM' } })
    await waitFor(() => expect(screen.queryByRole('form', { name: 'Editar entrada' })).not.toBeInTheDocument())
    expect(dayRequests()).toHaveLength(2)
  })

  it('keeps the day on screen while it loads again after a change', async () => {
    let answerReload: (response: Response) => void = () => {}
    let dayRequestCount = 0
    fetchMock.mockImplementation((_url, init) => {
      if (init?.method === 'PATCH') return Promise.resolve(jsonResponse(lunchRice))
      dayRequestCount += 1
      return dayRequestCount === 1
        ? Promise.resolve(jsonResponse(day))
        : new Promise((resolve) => { answerReload = resolve })
    })
    render(<DiaryPage />)
    const form = await openEntry()
    setAmount(form, '200')

    fireEvent.click(saveButton(form))

    await waitFor(() => expect(dayRequestCount).toBe(2))
    await waitFor(() => expect(screen.queryByRole('form', { name: 'Editar entrada' })).not.toBeInTheDocument())
    expect(screen.getByRole('heading', { name: 'Almoço' })).toBeInTheDocument()
    expect(screen.queryByLabelText('Carregando o dia')).not.toBeInTheDocument()
    await act(async () => answerReload(jsonResponse({ ...day, meals: [day.meals[0]] })))
    await waitFor(() => expect(screen.queryByRole('heading', { name: 'Jantar' })).not.toBeInTheDocument())
    expect(screen.getByRole('heading', { name: 'Almoço' })).toBeInTheDocument()
  })

  it('does not allow a quantity of zero', async () => {
    serve()
    render(<DiaryPage />)
    const form = await openEntry()

    setAmount(form, '0')

    expect(saveButton(form)).toHaveAttribute('aria-disabled', 'true')
  })

  it('sends the change once when the user saves twice', async () => {
    let answerPatch: (response: Response) => void = () => {}
    serve({ patch: () => new Promise((resolve) => { answerPatch = resolve }) })
    render(<DiaryPage />)
    const form = await openEntry()
    setAmount(form, '200')

    fireEvent.click(saveButton(form))
    fireEvent.click(saveButton(form))
    fireEvent.submit(form)

    expect(callsWith('PATCH')).toHaveLength(1)
    await act(async () => answerPatch(jsonResponse(lunchRice)))
    await waitFor(() => expect(screen.queryByRole('form', { name: 'Editar entrada' })).not.toBeInTheDocument())
    expect(callsWith('PATCH')).toHaveLength(1)
  })

  it('asks before removing and does nothing when the user cancels', async () => {
    serve()
    render(<DiaryPage />)
    const form = await openEntry()

    const { dialog } = await askToRemove(form)
    expect(within(dialog).getByText('Remover do diário?')).toBeInTheDocument()
    expect(within(dialog).getByText(/Arroz, tipo 1, cozido \(150g\) será removido de Almoço/)).toBeInTheDocument()
    fireEvent.click(within(dialog).getByRole('button', { name: 'Cancelar' }))

    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument())
    expect(callsWith('DELETE')).toHaveLength(0)
    expect(screen.getByRole('form', { name: 'Editar entrada' })).toBeInTheDocument()
  })

  it('removes the entry after the confirmation and loads the day again', async () => {
    serve()
    render(<DiaryPage />)
    const form = await openEntry()

    const { confirm } = await askToRemove(form)
    confirm()

    await waitFor(() => expect(callsWith('DELETE')).toHaveLength(1))
    expect(callsWith('DELETE')[0][0]).toBe('/api/diary/entries/rice-entry')
    await waitFor(() => expect(screen.queryByRole('form', { name: 'Editar entrada' })).not.toBeInTheDocument())
    expect(dayRequests()).toHaveLength(2)
  })

  it('removes once when the user confirms twice', async () => {
    let answerDelete: (response: Response) => void = () => {}
    serve({ remove: () => new Promise((resolve) => { answerDelete = resolve }) })
    render(<DiaryPage />)
    const form = await openEntry()
    const { confirm } = await askToRemove(form)

    confirm()
    confirm()

    expect(callsWith('DELETE')).toHaveLength(1)
    await act(async () => answerDelete(new Response(null, { status: 204 })))
    await waitFor(() => expect(screen.queryByRole('form', { name: 'Editar entrada' })).not.toBeInTheDocument())
    expect(callsWith('DELETE')).toHaveLength(1)
  })

  it.each([
    [
      'the server cannot find the entry',
      { remove: () => Promise.resolve(jsonResponse({ detail: 'Diary entry not found: rice-entry' }, 404)) },
      'Diary entry not found: rice-entry',
    ],
    [
      'the server cannot be reached',
      { remove: () => Promise.reject(new TypeError('Failed to fetch')) },
      'Sem conexão com o servidor. Verifique a internet e tente de novo.',
    ],
  ])('keeps the sheet open and shows the problem when %s while removing', async (_case, handlers, message) => {
    serve(handlers)
    render(<DiaryPage />)
    const form = await openEntry()
    const { confirm } = await askToRemove(form)

    confirm()

    expect(await screen.findByText(message)).toBeInTheDocument()
    expect(screen.getByRole('form', { name: 'Editar entrada' })).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument())
  })

  it('keeps the sheet open and shows the problem when the change is rejected', async () => {
    serve({ patch: () => Promise.resolve(jsonResponse({ detail: 'Quantity must be positive' }, 400)) })
    render(<DiaryPage />)
    const form = await openEntry()
    setAmount(form, '200')

    fireEvent.click(saveButton(form))

    expect(await screen.findByText('Quantity must be positive')).toBeInTheDocument()
    expect(screen.getByRole('form', { name: 'Editar entrada' })).toBeInTheDocument()
    await waitFor(() => expect(saveButton(form)).not.toHaveAttribute('aria-disabled', 'true'))
  })
})
