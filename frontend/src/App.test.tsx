import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import App from './App.tsx'

const fetchMock = vi.fn<typeof fetch>()

beforeEach(() => {
  fetchMock.mockReset()
  fetchMock.mockImplementation(async () =>
    new Response(JSON.stringify({ date: '2026-11-19', meals: [], total: { kcal: 0, protein: 0, carbohydrate: 0, fat: 0 } }), {
      headers: { 'Content-Type': 'application/json' },
    }),
  )
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
  await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))

  fireEvent.click(screen.getByRole('tab', { name: 'Alimentos' }))
  await screen.findByRole('heading', { name: 'Alimentos' })
  fireEvent.click(screen.getByRole('tab', { name: 'Diário' }))

  expect(await screen.findByRole('heading', { name: 'Diário' })).toBeInTheDocument()
  expect(fetchMock).toHaveBeenCalledTimes(2)
})
