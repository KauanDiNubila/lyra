import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { Food } from './foodApi.ts'
import { FoodsPage } from './FoodsPage.tsx'

const rice: Food = {
  id: 'rice',
  name: 'Arroz, tipo 1, cozido',
  source: 'TACO',
  referenceAmount: { amount: 100, unit: 'GRAM' },
  nutrients: { kcal: 128.2585, protein: 2.5208, carbohydrate: 28.0598, fat: 0.227, fiber: 1.561, sodium: 1.2007 },
}

const milkshake: Food = {
  id: 'milkshake',
  name: 'Milkshake de morango',
  source: 'CUSTOM',
  referenceAmount: { amount: 1, unit: 'PORTION' },
  nutrients: { kcal: 590, protein: 13, carbohydrate: 98, fat: 16, fiber: null, sodium: null },
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': status >= 400 ? 'application/problem+json' : 'application/json' },
  })
}

const fetchMock = vi.fn<typeof fetch>()

function search(text: string) {
  fireEvent.change(screen.getByLabelText('Buscar alimento'), { target: { value: text } })
}

function openForm() {
  fireEvent.click(screen.getByRole('button', { name: 'Novo alimento' }))
  return screen.findByRole('form', { name: 'Novo alimento' })
}

function fillForm(form: HTMLElement) {
  const field = (label: string) => within(form).getByLabelText(label, { exact: false })
  fireEvent.change(field('Nome'), { target: { value: 'Milkshake de morango' } })
  for (const [label, value] of [['Calorias', '590'], ['Proteína', '13'], ['Carboidrato', '98'], ['Gordura', '16']]) {
    fireEvent.change(field(label), { target: { value } })
    fireEvent.blur(field(label))
  }
}

function saveButton(form: HTMLElement) {
  return within(form).getByRole('button', { name: 'Salvar alimento' })
}

function postCalls() {
  return fetchMock.mock.calls.filter(([, init]) => init?.method === 'POST')
}

beforeEach(() => {
  fetchMock.mockReset()
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('FoodsPage', () => {
  it('asks for at least two letters and does not call the API before that', async () => {
    render(<FoodsPage />)

    search('a')

    expect(screen.getByText('Digite pelo menos 2 letras para buscar.')).toBeInTheDocument()
    await act(() => new Promise((resolve) => setTimeout(resolve, 400)))
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('shows matching foods with macros rounded to one decimal', async () => {
    fetchMock.mockResolvedValue(jsonResponse([rice, milkshake]))
    render(<FoodsPage />)

    search('arroz')

    expect(await screen.findByText('Arroz, tipo 1, cozido')).toBeInTheDocument()
    expect(screen.getByText('128,3 kcal · P 2,5 g · C 28,1 g · G 0,2 g em 100g')).toBeInTheDocument()
    expect(screen.getByText('590 kcal · P 13 g · C 98 g · G 16 g em 1 porção')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith('/api/foods?query=arroz', expect.anything())
  })

  it('names the TACO as the source only when a TACO food is listed', async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse([milkshake]))
      .mockResolvedValueOnce(jsonResponse([rice]))
    render(<FoodsPage />)

    search('milkshake')
    await screen.findByText('Milkshake de morango')
    expect(screen.queryByText(/Fonte dos alimentos da TACO/)).not.toBeInTheDocument()

    search('arroz')
    expect(await screen.findByText(/Fonte dos alimentos da TACO/)).toBeInTheDocument()
  })

  it('waits for the user to stop typing before searching', async () => {
    fetchMock.mockResolvedValue(jsonResponse([rice]))
    render(<FoodsPage />)

    search('ar')
    search('arr')
    search('arroz')

    await screen.findByText('Arroz, tipo 1, cozido')
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('ignores a slow answer that arrives after a newer search', async () => {
    let answerFirst: (response: Response) => void = () => {}
    fetchMock
      .mockImplementationOnce(() => new Promise((resolve) => { answerFirst = resolve }))
      .mockResolvedValueOnce(jsonResponse([milkshake]))
    render(<FoodsPage />)

    search('arroz')
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))
    search('milkshake')
    await screen.findByText('Milkshake de morango')
    await act(async () => answerFirst(jsonResponse([rice])))

    expect(screen.queryByText('Arroz, tipo 1, cozido')).not.toBeInTheDocument()
    expect(screen.getByText('Milkshake de morango')).toBeInTheDocument()
  })

  it('does not show the previous results while a new search is loading', async () => {
    let answerSecond: (response: Response) => void = () => {}
    fetchMock
      .mockResolvedValueOnce(jsonResponse([rice]))
      .mockImplementationOnce(() => new Promise((resolve) => { answerSecond = resolve }))
    render(<FoodsPage />)

    search('arroz')
    await screen.findByText('Arroz, tipo 1, cozido')
    search('milkshake')

    expect(await screen.findByLabelText('Buscando alimentos')).toBeInTheDocument()
    expect(screen.queryByText('Arroz, tipo 1, cozido')).not.toBeInTheDocument()
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))
    await act(async () => answerSecond(jsonResponse([milkshake])))
    expect(await screen.findByText('Milkshake de morango')).toBeInTheDocument()
  })

  it('offers to create the food when nothing is found', async () => {
    fetchMock.mockResolvedValue(jsonResponse([]))
    render(<FoodsPage />)

    search('xyzzy')

    expect(await screen.findByText('Nenhum alimento encontrado')).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Novo alimento' })).toHaveLength(2)
  })

  it('shows the API problem detail when the search fails', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ title: 'Invalid food', detail: 'Search query must have at least 2 characters' }, 400))
    render(<FoodsPage />)

    search('a,')

    expect(await screen.findByText('Search query must have at least 2 characters')).toBeInTheDocument()
  })

  it('shows the problem when the search cannot reach the server', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))
    render(<FoodsPage />)

    search('arroz')

    expect(await screen.findByText('Não foi possível buscar')).toBeInTheDocument()
    expect(screen.getByText('Sem conexão com o servidor. Verifique a internet e tente de novo.')).toBeInTheDocument()
  })

  it('explains a server failure without a body in Portuguese', async () => {
    fetchMock.mockResolvedValue(new Response('', { status: 502 }))
    render(<FoodsPage />)

    search('arroz')

    expect(await screen.findByText('O servidor não conseguiu responder (erro 502). Tente de novo.')).toBeInTheDocument()
  })

  it('searches again when the user asks to try again after a failure', async () => {
    fetchMock
      .mockRejectedValueOnce(new TypeError('Failed to fetch'))
      .mockResolvedValueOnce(jsonResponse([rice]))
    render(<FoodsPage />)
    search('arroz')
    await screen.findByText('Não foi possível buscar')

    fireEvent.click(screen.getByRole('button', { name: 'Tentar de novo' }))

    expect(await screen.findByText('Arroz, tipo 1, cozido')).toBeInTheDocument()
    expect(screen.queryByText('Não foi possível buscar')).not.toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('keeps the save button disabled until the required fields are filled', async () => {
    render(<FoodsPage />)
    const form = await openForm()

    expect(saveButton(form)).toHaveAttribute('aria-disabled', 'true')
    fillForm(form)

    expect(saveButton(form)).not.toHaveAttribute('aria-disabled', 'true')
  })

  it('creates a custom food and shows it in the results', async () => {
    fetchMock.mockImplementation(async (_url, init) =>
      init?.method === 'POST' ? jsonResponse(milkshake, 201) : jsonResponse([milkshake]))
    render(<FoodsPage />)

    const form = await openForm()
    fillForm(form)
    fireEvent.click(saveButton(form))

    expect(await screen.findByText('Milkshake de morango')).toBeInTheDocument()
    expect(JSON.parse(String(postCalls()[0]?.[1]?.body))).toEqual({
      name: 'Milkshake de morango',
      referenceAmount: { amount: 100, unit: 'GRAM' },
      nutrients: { kcal: 590, protein: 13, carbohydrate: 98, fat: 16, fiber: null, sodium: null },
    })
    await waitFor(() => expect(screen.queryByRole('form', { name: 'Novo alimento' })).not.toBeInTheDocument())
  })

  it('sends the food once when the user saves twice', async () => {
    let answerPost: (response: Response) => void = () => {}
    fetchMock.mockImplementation((_url, init) =>
      init?.method === 'POST'
        ? new Promise((resolve) => { answerPost = resolve })
        : Promise.resolve(jsonResponse([milkshake])))
    render(<FoodsPage />)
    const form = await openForm()
    fillForm(form)

    fireEvent.click(saveButton(form))
    fireEvent.click(saveButton(form))
    fireEvent.submit(form)

    expect(postCalls()).toHaveLength(1)
    await act(async () => answerPost(jsonResponse(milkshake, 201)))
    await waitFor(() => expect(screen.queryByRole('form', { name: 'Novo alimento' })).not.toBeInTheDocument())
    expect(postCalls()).toHaveLength(1)
  })

  it.each([
    ['the server rejects the food', () => Promise.resolve(jsonResponse({ detail: 'Food name must have between 2 and 200 characters' }, 400)), 'Food name must have between 2 and 200 characters'],
    ['the server cannot be reached', () => Promise.reject(new TypeError('Failed to fetch')), 'Sem conexão com o servidor. Verifique a internet e tente de novo.'],
  ])('keeps the form open and ready for another try when %s', async (_case, answerPost, message) => {
    fetchMock.mockImplementation((_url, init) => (init?.method === 'POST' ? answerPost() : Promise.resolve(jsonResponse([]))))
    render(<FoodsPage />)
    const form = await openForm()
    fillForm(form)

    fireEvent.click(saveButton(form))

    expect(await screen.findByText(message)).toBeInTheDocument()
    expect(screen.getByText('Não foi possível salvar')).toBeInTheDocument()
    expect(screen.getByRole('form', { name: 'Novo alimento' })).toBeInTheDocument()
    await waitFor(() => expect(saveButton(form)).not.toHaveAttribute('aria-disabled', 'true'))
  })

  describe('barcode', () => {
    const nutella: Food = {
      id: 'nutella',
      name: 'Nutella (Ferrero)',
      source: 'OPEN_FOOD_FACTS',
      referenceAmount: { amount: 100, unit: 'GRAM' },
      nutrients: { kcal: 539, protein: 6.3, carbohydrate: 57.5, fat: 30.9, fiber: null, sodium: 41 },
    }

    async function openBarcode() {
      fireEvent.click(screen.getByRole('button', { name: 'Código de barras' }))
      return screen.findByRole('form', { name: 'Buscar por código de barras' })
    }

    function typeCode(form: HTMLElement, code: string) {
      fireEvent.change(within(form).getByLabelText('Código de barras', { exact: false }), { target: { value: code } })
    }

    function findButton(form: HTMLElement) {
      return within(form).getByRole('button', { name: 'Buscar produto' })
    }

    it('accepts only a code of 8 to 14 digits', async () => {
      render(<FoodsPage />)
      const form = await openBarcode()

      typeCode(form, '1234567')
      expect(findButton(form)).toHaveAttribute('aria-disabled', 'true')
      typeCode(form, '78910001001A3')
      expect(findButton(form)).toHaveAttribute('aria-disabled', 'true')
      typeCode(form, ' 7891000100103 ')
      expect(findButton(form)).not.toHaveAttribute('aria-disabled', 'true')
    })

    it('shows the product found in the results, with the credit of the Open Food Facts', async () => {
      fetchMock.mockImplementation(async (url) =>
        String(url).startsWith('/api/foods/barcode/') ? jsonResponse(nutella) : jsonResponse([nutella]))
      render(<FoodsPage />)
      const form = await openBarcode()

      typeCode(form, ' 7891000100103 ')
      fireEvent.click(findButton(form))

      expect(await screen.findByText('Nutella (Ferrero)')).toBeInTheDocument()
      expect(screen.getByText('Embalado')).toBeInTheDocument()
      expect(screen.getByText(/Fonte dos produtos embalados: Open Food Facts/)).toBeInTheDocument()
      expect(fetchMock).toHaveBeenCalledWith('/api/foods/barcode/7891000100103', expect.anything())
      await waitFor(() =>
        expect(screen.queryByRole('form', { name: 'Buscar por código de barras' })).not.toBeInTheDocument())
    })

    it('offers to create the food when the product is not found', async () => {
      fetchMock.mockResolvedValue(jsonResponse({ detail: 'Product not found: 7891000100103' }, 404))
      render(<FoodsPage />)
      const form = await openBarcode()
      typeCode(form, '7891000100103')
      fireEvent.click(findButton(form))

      expect(await screen.findByText('Produto não encontrado')).toBeInTheDocument()
      expect(screen.queryByText('Não foi possível buscar o produto')).not.toBeInTheDocument()
      fireEvent.click(screen.getByRole('button', { name: 'Cadastrar alimento' }))

      expect(await screen.findByRole('form', { name: 'Novo alimento' })).toBeInTheDocument()
      await waitFor(() =>
        expect(screen.queryByRole('form', { name: 'Buscar por código de barras' })).not.toBeInTheDocument())
    })

    it('keeps the form open with the problem when the product source is down', async () => {
      fetchMock.mockResolvedValue(jsonResponse({ detail: 'The product source is unavailable, try again later' }, 503))
      render(<FoodsPage />)
      const form = await openBarcode()
      typeCode(form, '7891000100103')
      fireEvent.click(findButton(form))

      expect(await screen.findByText('Não foi possível buscar o produto')).toBeInTheDocument()
      expect(screen.getByText('The product source is unavailable, try again later')).toBeInTheDocument()
      expect(screen.queryByText('Produto não encontrado')).not.toBeInTheDocument()
      await waitFor(() => expect(findButton(form)).not.toHaveAttribute('aria-disabled', 'true'))
    })

    it('forgets the failure of the last code when the code changes', async () => {
      fetchMock.mockResolvedValue(jsonResponse({ detail: 'The product source is unavailable, try again later' }, 503))
      render(<FoodsPage />)
      const form = await openBarcode()
      typeCode(form, '7891000100103')
      fireEvent.click(findButton(form))
      await screen.findByText('Não foi possível buscar o produto')

      typeCode(form, '7891000100104')

      expect(screen.queryByText('Não foi possível buscar o produto')).not.toBeInTheDocument()
    })

    it('forgets that the last code was not found when the code changes', async () => {
      fetchMock.mockResolvedValue(jsonResponse({ detail: 'Product not found: 7891000100103' }, 404))
      render(<FoodsPage />)
      const form = await openBarcode()
      typeCode(form, '7891000100103')
      fireEvent.click(findButton(form))
      await screen.findByText('Produto não encontrado')

      typeCode(form, '7891000100104')

      expect(screen.queryByText('Produto não encontrado')).not.toBeInTheDocument()
    })
  })

  it('opens an empty form for the next food, starting from the current search', async () => {
    fetchMock.mockImplementation(async (_url, init) =>
      init?.method === 'POST' ? jsonResponse(milkshake, 201) : jsonResponse([milkshake]))
    render(<FoodsPage />)
    const form = await openForm()
    fillForm(form)
    fireEvent.click(saveButton(form))
    await waitFor(() => expect(screen.queryByRole('form', { name: 'Novo alimento' })).not.toBeInTheDocument())

    const reopened = await openForm()

    expect(within(reopened).getByLabelText('Nome', { exact: false })).toHaveValue('Milkshake de morango')
    expect(within(reopened).getByLabelText('Calorias', { exact: false })).toHaveDisplayValue('')
  })
})
