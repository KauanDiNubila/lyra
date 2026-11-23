import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DiaryPage } from './DiaryPage.tsx'
import { day, emptyDay, jsonResponse } from './testSupport.ts'

const fetchMock = vi.fn<typeof fetch>()

function requestedUrls() {
  return fetchMock.mock.calls.map(([url]) => url)
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

describe('DiaryPage', () => {
  it('opens on today and shows the meals with their totals rounded to one decimal', async () => {
    fetchMock.mockResolvedValue(jsonResponse(day))
    render(<DiaryPage />)

    expect(await screen.findByRole('heading', { name: 'Almoço' })).toBeInTheDocument()
    expect(requestedUrls()).toEqual(['/api/diary/2026-11-19'])
    expect(screen.getByText('782,4 kcal · P 16,8 g · C 140,1 g · G 16,3 g')).toBeInTheDocument()
    expect(screen.getByText('192,4 kcal · P 3,8 g · C 42,1 g · G 0,3 g')).toBeInTheDocument()
    expect(screen.getByText('150g · 192,4 kcal · P 3,8 g · C 42,1 g · G 0,3 g')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Jantar' })).toBeInTheDocument()
    expect(screen.getByText('1 porção · 590 kcal · P 13 g · C 98 g · G 16 g')).toBeInTheDocument()
  })

  it('treats today as the local date even when it is already tomorrow in UTC', async () => {
    vi.setSystemTime(new Date('2026-11-20T01:30:00Z'))
    fetchMock.mockResolvedValue(jsonResponse(emptyDay('2026-11-19')))
    render(<DiaryPage />)

    await screen.findByText('Nada registrado neste dia')

    expect(requestedUrls()).toEqual(['/api/diary/2026-11-19'])
  })

  it('moves to the previous and to the next day', async () => {
    fetchMock.mockImplementation(async (url) => jsonResponse(emptyDay(String(url).split('/').pop() ?? '')))
    render(<DiaryPage />)
    await screen.findByText('Nada registrado neste dia')

    fireEvent.click(screen.getByRole('button', { name: 'Dia anterior' }))
    await waitFor(() => expect(requestedUrls()).toContain('/api/diary/2026-11-18'))
    fireEvent.click(screen.getByRole('button', { name: 'Próximo dia' }))
    fireEvent.click(screen.getByRole('button', { name: 'Próximo dia' }))

    await waitFor(() => expect(requestedUrls()).toContain('/api/diary/2026-11-20'))
  })

  it('shows an empty day with a message', async () => {
    fetchMock.mockResolvedValue(jsonResponse(emptyDay('2026-11-19')))
    render(<DiaryPage />)

    expect(await screen.findByText('Nada registrado neste dia')).toBeInTheDocument()
    expect(screen.queryByText('Total do dia')).not.toBeInTheDocument()
  })

  it('does not show the previous day while another day is loading', async () => {
    let answerNextDay: (response: Response) => void = () => {}
    fetchMock
      .mockResolvedValueOnce(jsonResponse(day))
      .mockImplementationOnce(() => new Promise((resolve) => { answerNextDay = resolve }))
    render(<DiaryPage />)
    await screen.findByRole('heading', { name: 'Almoço' })

    fireEvent.click(screen.getByRole('button', { name: 'Próximo dia' }))

    expect(await screen.findByLabelText('Carregando o dia')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Almoço' })).not.toBeInTheDocument()
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))
    await act(async () => answerNextDay(jsonResponse(emptyDay('2026-11-20'))))
    expect(await screen.findByText('Nada registrado neste dia')).toBeInTheDocument()
  })

  it('ignores a slow answer that arrives after the user changed the day', async () => {
    let answerFirst: (response: Response) => void = () => {}
    fetchMock
      .mockImplementationOnce(() => new Promise((resolve) => { answerFirst = resolve }))
      .mockResolvedValueOnce(jsonResponse(emptyDay('2026-11-20')))
    render(<DiaryPage />)
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))

    fireEvent.click(screen.getByRole('button', { name: 'Próximo dia' }))
    await screen.findByText('Nada registrado neste dia')
    await act(async () => answerFirst(jsonResponse(day)))

    expect(screen.queryByRole('heading', { name: 'Almoço' })).not.toBeInTheDocument()
    expect(screen.getByText('Nada registrado neste dia')).toBeInTheDocument()
  })

  it('explains the failure and loads the day again when the user asks', async () => {
    fetchMock
      .mockRejectedValueOnce(new TypeError('Failed to fetch'))
      .mockResolvedValueOnce(jsonResponse(day))
    render(<DiaryPage />)
    expect(await screen.findByText('Não foi possível carregar o dia')).toBeInTheDocument()
    expect(screen.getByText('Sem conexão com o servidor. Verifique a internet e tente de novo.')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Tentar de novo' }))

    expect(await screen.findByRole('heading', { name: 'Almoço' })).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })
})
