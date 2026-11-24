import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DiaryPage } from './DiaryPage.tsx'
import { emptyDay, jsonResponse, planDay } from './testSupport.ts'

const fetchMock = vi.fn<typeof fetch>()
const NO_PLAN = { detail: 'There is no active plan to fill the day' }

type Server = {
  days?: Record<string, unknown>
  fill?: (date: string) => Promise<Response>
}

function serve({ days = {}, fill }: Server = {}) {
  fetchMock.mockImplementation(async (url) => {
    const path = String(url)
    const date = path.split('/')[3]
    if (path.endsWith('/plan')) return fill ? fill(date) : jsonResponse(planDay)
    return jsonResponse(days[date] ?? emptyDay(date))
  })
}

function fills() {
  return fetchMock.mock.calls.filter(([url]) => String(url).endsWith('/plan'))
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

describe('filling the day from the plan', () => {
  it('fills an empty day of today by itself and shows what came from the plan', async () => {
    serve()
    render(<DiaryPage />)

    expect(await screen.findByRole('heading', { name: 'Almoço' })).toBeInTheDocument()
    expect(fills()).toHaveLength(1)
    expect(fills()[0][0]).toBe('/api/diary/2026-11-19/plan')
    expect(fills()[0][1]?.method).toBe('POST')
  })

  it('does not fill an empty day of the future by itself, but offers to', async () => {
    serve()
    render(<DiaryPage />)
    await screen.findByRole('heading', { name: 'Almoço' })

    fireEvent.click(screen.getByRole('button', { name: 'Próximo dia' }))

    const button = await screen.findByRole('button', { name: 'Preencher com o plano' })
    expect(fills()).toHaveLength(1)
    fireEvent.click(button)
    await waitFor(() => expect(fills().map(([url]) => url)).toContain('/api/diary/2026-11-20/plan'))
  })

  it('does not fill an empty day of the past by itself, but offers to', async () => {
    serve({ fill: () => Promise.resolve(jsonResponse(NO_PLAN, 404)) })
    render(<DiaryPage />)
    await screen.findByText('Nada registrado neste dia')

    fireEvent.click(screen.getByRole('button', { name: 'Dia anterior' }))

    const button = await screen.findByRole('button', { name: 'Preencher com o plano' })
    expect(fills().map(([url]) => url)).not.toContain('/api/diary/2026-11-18/plan')
    fireEvent.click(button)
    await waitFor(() => expect(fills().map(([url]) => url)).toContain('/api/diary/2026-11-18/plan'))
  })

  it('shows the day filled from the plan when the user asks for it', async () => {
    let filled = false
    fetchMock.mockImplementation(async (url) => {
      if (String(url).endsWith('/plan')) {
        filled = true
        return jsonResponse(planDay)
      }
      return jsonResponse(filled ? planDay : emptyDay(String(url).split('/')[3]))
    })
    render(<DiaryPage />)
    await screen.findByRole('heading', { name: 'Almoço' })
    fireEvent.click(screen.getByRole('button', { name: 'Dia anterior' }))
    filled = false

    fireEvent.click(await screen.findByRole('button', { name: 'Preencher com o plano' }))

    expect(await screen.findByRole('heading', { name: 'Almoço' })).toBeInTheDocument()
  })

  it('shows the loading spinner, and not the empty message, while it fills', async () => {
    serve({ fill: () => new Promise(() => {}) })
    render(<DiaryPage />)

    expect(await screen.findByLabelText('Carregando o dia')).toBeInTheDocument()
    await waitFor(() => expect(fills()).toHaveLength(1))
    expect(screen.queryByText('Nada registrado neste dia')).not.toBeInTheDocument()
  })

  it('tells the user to activate a plan when there is none and lets them try again', async () => {
    serve({ fill: () => Promise.resolve(jsonResponse(NO_PLAN, 404)) })
    render(<DiaryPage />)

    expect(await screen.findByText('Ative um plano na aba Plano para o diário abrir preenchido.')).toBeInTheDocument()
    expect(screen.queryByText('Não foi possível preencher o dia')).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Preencher com o plano' }))

    await waitFor(() => expect(fills()).toHaveLength(2))
  })

  it('shows the problem and offers another try when the fill fails', async () => {
    serve({ fill: () => Promise.reject(new TypeError('Failed to fetch')) })
    render(<DiaryPage />)

    expect(await screen.findByText('Não foi possível preencher o dia')).toBeInTheDocument()
    expect(screen.getByText('Sem conexão com o servidor. Verifique a internet e tente de novo.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Preencher com o plano' })).toBeInTheDocument()
  })

  it('does not fill a day that already has entries', async () => {
    serve({ days: { '2026-11-19': planDay } })
    render(<DiaryPage />)

    await screen.findByRole('heading', { name: 'Almoço' })

    expect(fills()).toHaveLength(0)
  })
})

describe('the day compared with the plan', () => {
  beforeEach(() => serve({ days: { '2026-11-19': planDay } }))

  it('says what came from the plan and what changed in each entry', async () => {
    render(<DiaryPage />)

    expect(await screen.findByText('Ajustado (plano: 150g)')).toBeInTheDocument()
    expect(screen.getByText('Pulado (plano: 100g)')).toBeInTheDocument()
    expect(screen.getByText('Trocado (plano: Peixe, 100g)')).toBeInTheDocument()
    expect(screen.getByText('250g · 320 kcal · P 32 g · C 64 g · G 16 g')).toBeInTheDocument()
    expect(screen.getByText('0g · 0 kcal · P 0 g · C 0 g · G 0 g')).toBeInTheDocument()
  })

  it('shows the extras without any note about the plan', async () => {
    render(<DiaryPage />)

    await screen.findByText('Maçã')

    expect(screen.getByText('100g · 60 kcal · P 6 g · C 12 g · G 3 g')).toBeInTheDocument()
    expect(screen.queryByText('Planejado')).not.toBeInTheDocument()
  })

  it('shows the plan and the targets of the day with the differences', async () => {
    render(<DiaryPage />)

    await screen.findByRole('heading', { name: 'Almoço' })

    expect(screen.getByText('388 kcal · P 38,8 g · C 77,6 g · G 19,4 g')).toBeInTheDocument()
    expect(screen.getByText('+182 kcal · P +18,2 g · C +36,4 g · G +9,1 g')).toBeInTheDocument()
    expect(screen.getByText('2.000 kcal · P 140 g · C 200 g · G 60 g')).toBeInTheDocument()
    expect(screen.getByText('-1.430 kcal · P -83 g · C -86 g · G -31,5 g')).toBeInTheDocument()
  })

  it('shows what each meal planned and the difference in kcal', async () => {
    render(<DiaryPage />)

    await screen.findByRole('heading', { name: 'Almoço' })

    expect(screen.getByText('Plano 192 kcal · +128 kcal')).toBeInTheDocument()
    expect(screen.getByText('Plano 196 kcal · -6 kcal')).toBeInTheDocument()
    expect(screen.queryByText(/Plano 0 kcal/)).not.toBeInTheDocument()
  })

  it('does not show the comparison for a day without plan and without targets', async () => {
    serve({ days: { '2026-11-19': { ...planDay, meals: [{ ...planDay.meals[2] }], targets: null, differenceFromTargets: null } } })
    render(<DiaryPage />)

    await screen.findByRole('heading', { name: 'Lanche' })

    expect(screen.queryByText('Diferença para o plano')).not.toBeInTheDocument()
    expect(screen.queryByText('Metas')).not.toBeInTheDocument()
  })
})
