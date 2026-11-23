import { fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import App from './App.tsx'

const fetchMock = vi.fn<typeof fetch>()

beforeEach(() => {
  fetchMock.mockReset()
  fetchMock.mockResolvedValue(
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
