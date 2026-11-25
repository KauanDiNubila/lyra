import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import App from './App.tsx'

const fetchMock = vi.fn<typeof fetch>()

beforeEach(() => {
  fetchMock.mockReset()
  fetchMock.mockImplementation(async (url) => {
    if (String(url) === '/api/me') {
      return new Response(JSON.stringify({ id: 'user-1', email: 'ana@example.com', name: 'Ana Souza' }), {
        headers: { 'Content-Type': 'application/json' },
      })
    }
    if (String(url) === '/api/weights') {
      return new Response(JSON.stringify([]), { headers: { 'Content-Type': 'application/json' } })
    }
    if (String(url).endsWith('/plan')) {
      return new Response(JSON.stringify({ detail: 'There is no active plan to fill the day' }), { status: 404 })
    }
    return new Response(JSON.stringify({ date: '2026-11-19', meals: [], total: { kcal: 0, protein: 0, carbohydrate: 0, fat: 0 } }), {
      headers: { 'Content-Type': 'application/json' },
    })
  })
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

it('opens on the diary and switches to the foods', async () => {
  render(<App />)

  expect(await screen.findByRole('heading', { name: 'Diário' })).toBeInTheDocument()
  fireEvent.click(screen.getByRole('tab', { name: 'Alimentos' }))

  expect(await screen.findByRole('heading', { name: 'Alimentos' })).toBeInTheDocument()
  expect(screen.queryByRole('heading', { name: 'Diário' })).not.toBeInTheDocument()
})

it('keeps the diary day when the user goes to the foods and comes back', async () => {
  render(<App />)
  await screen.findByRole('heading', { name: 'Diário' })
  fireEvent.click(screen.getByRole('button', { name: 'Próximo dia' }))
  await waitFor(() => expect(diaryRequests()).toHaveLength(2))

  fireEvent.click(screen.getByRole('tab', { name: 'Alimentos' }))
  await screen.findByRole('heading', { name: 'Alimentos' })
  fireEvent.click(screen.getByRole('tab', { name: 'Diário' }))

  expect(await screen.findByRole('heading', { name: 'Diário' })).toBeInTheDocument()
  expect(diaryRequests()).toHaveLength(2)
})

it('switches to the plans', async () => {
  render(<App />)

  fireEvent.click(await screen.findByRole('tab', { name: 'Plano' }))

  expect(await screen.findByRole('heading', { name: 'Plano' })).toBeInTheDocument()
  expect(screen.queryByRole('heading', { name: 'Diário' })).not.toBeInTheDocument()
})

it('switches to the weight', async () => {
  render(<App />)

  fireEvent.click(await screen.findByRole('tab', { name: 'Peso' }))

  expect(await screen.findByRole('heading', { name: 'Peso' })).toBeInTheDocument()
  expect(screen.queryByRole('heading', { name: 'Diário' })).not.toBeInTheDocument()
})

it('keeps a weight typed and not saved when the user leaves the tab and comes back', async () => {
  render(<App />)
  fireEvent.click(await screen.findByRole('tab', { name: 'Peso' }))
  await screen.findByText('Nenhum peso registrado')
  const field = screen.getByLabelText(/^Peso/)
  fireEvent.change(field, { target: { value: '82.4' } })
  fireEvent.blur(field)

  fireEvent.click(screen.getByRole('tab', { name: 'Alimentos' }))
  fireEvent.click(screen.getByRole('tab', { name: 'Peso' }))

  expect(screen.getByLabelText(/^Peso/)).toHaveDisplayValue(/82/)
  expect(weightRequests()).toHaveLength(1)
})

function weightRequests() {
  return fetchMock.mock.calls.filter(([url]) => String(url) === '/api/weights')
}

function diaryRequests() {
  return fetchMock.mock.calls.filter(([url, init]) => String(url).startsWith('/api/diary') && !init?.method)
}
