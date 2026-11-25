import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { Weight } from './weightApi.ts'
import { WeightPage } from './WeightPage.tsx'

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': status >= 400 ? 'application/problem+json' : 'application/json' },
  })
}

const fetchMock = vi.fn<typeof fetch>()

function callsWith(method: string) {
  return fetchMock.mock.calls.filter(([, init]) => init?.method === method)
}

function typeWeight(value: string) {
  const field = screen.getByLabelText(/^Peso/)
  fireEvent.change(field, { target: { value } })
  fireEvent.blur(field)
}

function saveButton() {
  return screen.getByRole('button', { name: 'Salvar peso' })
}

function serveWeights(weights: Weight[]) {
  fetchMock.mockImplementation(async (url, init) => {
    if (init?.method === 'PUT') {
      const kilograms = JSON.parse(String(init.body)).kilograms
      const date = String(url).split('/').pop() ?? ''
      weights = [...weights.filter((weight) => weight.date !== date), { date, kilograms }]
        .sort((a, b) => a.date.localeCompare(b.date))
      return jsonResponse({ date, kilograms })
    }
    if (init?.method === 'DELETE') {
      const date = String(url).split('/').pop()
      weights = weights.filter((weight) => weight.date !== date)
      return new Response(null, { status: 204 })
    }
    return jsonResponse(weights)
  })
}

beforeEach(() => {
  fetchMock.mockReset()
  vi.stubGlobal('fetch', fetchMock)
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 10, 25, 9, 0))
})

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

describe('WeightPage', () => {
  it('invites to record the first weight when there is none', async () => {
    serveWeights([])
    render(<WeightPage />)

    expect(await screen.findByText('Nenhum peso registrado')).toBeInTheDocument()
  })

  it('lists the weights from the newest, with the change since the previous one', async () => {
    serveWeights([
      { date: '2026-11-23', kilograms: 82.45 },
      { date: '2026-11-24', kilograms: 82.1 },
      { date: '2026-11-25', kilograms: 82.1 },
    ])
    render(<WeightPage />)

    const items = within(await screen.findByRole('list')).getAllByRole('listitem')
    expect(items.map((item) => item.textContent)).toEqual([
      expect.stringContaining('82,1 kg'),
      expect.stringContaining('82,1 kg'),
      expect.stringContaining('82,45 kg'),
    ])
    expect(items[0]).toHaveTextContent('qua., 25 de nov. de 2026')
    expect(items[0]).not.toHaveTextContent('-0')
    expect(items[1]).toHaveTextContent('-0,35 kg')
    expect(items[2]).not.toHaveTextContent(' · ')
  })

  it('records the weight of today and shows it in the history', async () => {
    serveWeights([])
    render(<WeightPage />)
    await screen.findByText('Nenhum peso registrado')

    typeWeight('82.4')
    fireEvent.click(saveButton())

    expect(await screen.findByText('82,4 kg')).toBeInTheDocument()
    expect(callsWith('PUT')[0]?.[0]).toBe('/api/weights/2026-11-25')
    expect(JSON.parse(String(callsWith('PUT')[0]?.[1]?.body))).toEqual({ kilograms: 82.4 })
    expect(screen.getByLabelText(/^Peso/)).toHaveDisplayValue('')
  })

  it('warns that saving replaces the weight the chosen day already has', async () => {
    serveWeights([{ date: '2026-11-25', kilograms: 82.1 }])
    render(<WeightPage />)

    expect(await screen.findByText('Já existe 82,1 kg neste dia. Salvar troca o valor.')).toBeInTheDocument()
  })

  it('does not warn about a day without weight', async () => {
    serveWeights([{ date: '2026-11-24', kilograms: 82.1 }])
    render(<WeightPage />)
    await screen.findByText('82,1 kg')

    expect(screen.queryByText(/Já existe/)).not.toBeInTheDocument()
  })

  it.each([
    ['too low', '19.9', 'O peso precisa estar entre 20 e 500 kg.'],
    ['too high', '8240', 'O peso precisa estar entre 20 e 500 kg.'],
    ['with three decimals', '82.455', 'Use no máximo duas casas decimais.'],
  ])('explains a weight %s and does not send it', async (_case, value, message) => {
    serveWeights([])
    render(<WeightPage />)
    await screen.findByText('Nenhum peso registrado')

    typeWeight(value)

    expect(await screen.findByText(message)).toBeInTheDocument()
    expect(saveButton()).toHaveAttribute('aria-disabled', 'true')
    fireEvent.submit(screen.getByRole('form', { name: 'Registrar peso' }))
    expect(callsWith('PUT')).toHaveLength(0)
  })

  it('keeps the value and shows the problem when the server refuses the weight', async () => {
    fetchMock.mockImplementation(async (_url, init) =>
      init?.method === 'PUT'
        ? jsonResponse({ detail: 'Weight must be between 20 and 500 kilograms' }, 400)
        : jsonResponse([]))
    render(<WeightPage />)
    await screen.findByText('Nenhum peso registrado')

    typeWeight('82.4')
    fireEvent.click(saveButton())

    expect(await screen.findByText('Não foi possível salvar o peso')).toBeInTheDocument()
    expect(screen.getByText('Weight must be between 20 and 500 kilograms')).toBeInTheDocument()
    expect(screen.getByLabelText(/^Peso/)).toHaveDisplayValue(/82/)
  })

  it('sends the weight once when the user saves twice', async () => {
    let answerPut: (response: Response) => void = () => {}
    fetchMock.mockImplementation((_url, init) =>
      init?.method === 'PUT'
        ? new Promise((resolve) => { answerPut = resolve })
        : Promise.resolve(jsonResponse([])))
    render(<WeightPage />)
    await screen.findByText('Nenhum peso registrado')
    typeWeight('82.4')

    fireEvent.click(saveButton())
    fireEvent.click(saveButton())
    fireEvent.submit(screen.getByRole('form', { name: 'Registrar peso' }))

    expect(callsWith('PUT')).toHaveLength(1)
    await act(async () => answerPut(jsonResponse({ date: '2026-11-25', kilograms: 82.4 })))
    expect(callsWith('PUT')).toHaveLength(1)
  })

  it('deletes a weight and removes it from the history', async () => {
    serveWeights([
      { date: '2026-11-24', kilograms: 82.1 },
      { date: '2026-11-25', kilograms: 81.9 },
    ])
    render(<WeightPage />)
    const items = within(await screen.findByRole('list')).getAllByRole('listitem')

    fireEvent.click(within(items[0]).getByRole('button', { name: 'Apagar o peso de qua., 25 de nov. de 2026' }))

    await waitFor(() => expect(screen.queryByText('81,9 kg')).not.toBeInTheDocument())
    expect(callsWith('DELETE')[0]?.[0]).toBe('/api/weights/2026-11-25')
    expect(screen.getByText('82,1 kg')).toBeInTheDocument()
  })

  it('shows the problem when a weight cannot be deleted', async () => {
    fetchMock.mockImplementation(async (_url, init) =>
      init?.method === 'DELETE'
        ? jsonResponse({ detail: 'Weight not found for 2026-11-25' }, 404)
        : jsonResponse([{ date: '2026-11-25', kilograms: 81.9 }]))
    render(<WeightPage />)
    const item = within(await screen.findByRole('list')).getByRole('listitem')

    fireEvent.click(within(item).getByRole('button', { name: 'Apagar o peso de qua., 25 de nov. de 2026' }))

    expect(await screen.findByText('Não foi possível apagar o peso')).toBeInTheDocument()
    expect(screen.getByText('Weight not found for 2026-11-25')).toBeInTheDocument()
  })

  it('offers to try again when the history cannot be loaded', async () => {
    fetchMock
      .mockRejectedValueOnce(new TypeError('Failed to fetch'))
      .mockResolvedValueOnce(jsonResponse([{ date: '2026-11-25', kilograms: 81.9 }]))
    render(<WeightPage />)

    expect(await screen.findByText('Não foi possível carregar os pesos')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Tentar de novo' }))

    expect(await screen.findByText('81,9 kg')).toBeInTheDocument()
  })
})
