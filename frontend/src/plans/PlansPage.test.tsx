import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { jsonResponse, milkshake, rice } from '../diary/testSupport.ts'
import type { PlanSummary } from './planApi.ts'
import { PlansPage } from './PlansPage.tsx'
import { bulking, cutting, summary } from './testSupport.ts'

const fetchMock = vi.fn<typeof fetch>()

type Server = {
  plans: PlanSummary[]
  write?: (init: RequestInit) => Promise<Response>
  activate?: () => Promise<Response>
}

function serve(server: Server) {
  fetchMock.mockImplementation(async (url, init) => {
    const path = String(url)
    const method = init?.method ?? 'GET'
    if (path.startsWith('/api/foods')) return jsonResponse([rice, milkshake])
    if (path.endsWith('/activation')) return server.activate ? server.activate() : jsonResponse(cutting)
    if (method === 'POST' || method === 'PUT') {
      return server.write ? server.write(init ?? {}) : jsonResponse(cutting, method === 'POST' ? 201 : 200)
    }
    if (path === '/api/plans') return jsonResponse(server.plans)
    return jsonResponse(path.endsWith(bulking.id) ? bulking : cutting)
  })
}

function writes() {
  return fetchMock.mock.calls.filter(([, init]) => init?.method === 'POST' || init?.method === 'PUT')
}

function planListRequests() {
  return fetchMock.mock.calls.filter(([url, init]) => String(url) === '/api/plans' && !init?.method)
}

function setNumber(scope: HTMLElement, label: string, value: string) {
  const field = within(scope).getByLabelText(label, { exact: false })
  fireEvent.change(field, { target: { value } })
  fireEvent.blur(field)
}

function setText(scope: HTMLElement, label: string, value: string) {
  fireEvent.change(within(scope).getByLabelText(label, { exact: false }), { target: { value } })
}

async function openNewPlan() {
  fireEvent.click(await screen.findByRole('button', { name: 'Novo plano' }))
  return screen.findByRole('form', { name: 'Plano alimentar' })
}

async function openPlan(name: string) {
  fireEvent.click(await screen.findByText(name))
  return screen.findByRole('form', { name: 'Plano alimentar' })
}

async function addFood(form: HTMLElement, mealName: string, foodName: string) {
  const meal = within(form).getByRole('group', { name: mealName })
  fireEvent.click(within(meal).getByRole('button', { name: 'Adicionar alimento' }))
  fireEvent.change(await screen.findByLabelText('Buscar alimento'), { target: { value: 'arroz' } })
  fireEvent.click(await screen.findByText(foodName))
  return screen.findByRole('form', { name: 'Plano alimentar' })
}

async function fillNewPlan() {
  const form = await openNewPlan()
  setText(form, 'Nome do plano', 'Cutting')
  setNumber(form, 'Calorias', '2000')
  setNumber(form, 'Proteína', '140')
  setNumber(form, 'Carboidrato', '200')
  setNumber(form, 'Gordura', '60')
  setText(form, 'Nome da refeição', 'Almoço')
  const withFood = await addFood(form, 'Refeição 1', 'Arroz, tipo 1, cozido')
  return withFood
}

function saveButton(form: HTMLElement) {
  return within(form).getByRole('button', { name: 'Salvar plano' })
}

beforeEach(() => {
  fetchMock.mockReset()
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('listing the plans', () => {
  it('invites the user to create the first plan', async () => {
    serve({ plans: [] })
    render(<PlansPage />)

    expect(await screen.findByText('Nenhum plano cadastrado')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Criar plano' }))

    expect(await screen.findByRole('form', { name: 'Plano alimentar' })).toBeInTheDocument()
  })

  it('shows which plan is active and offers to activate the others', async () => {
    serve({ plans: [summary(cutting), summary(bulking)] })
    render(<PlansPage />)

    expect(await screen.findByText('Cutting')).toBeInTheDocument()
    expect(screen.getByText('Bulking')).toBeInTheDocument()
    expect(screen.getByText('Ativo')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Ativar Cutting' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ativar Bulking' })).toBeInTheDocument()
  })

  it('explains the failure and tries again', async () => {
    serve({ plans: [summary(cutting)] })
    fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'))
    render(<PlansPage />)

    expect(await screen.findByText('Não foi possível carregar os planos')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Tentar de novo' }))

    expect(await screen.findByText('Cutting')).toBeInTheDocument()
  })
})

describe('creating a plan', () => {
  it('sends the whole plan with the quantity in the unit of the food', async () => {
    serve({ plans: [] })
    render(<PlansPage />)
    const form = await fillNewPlan()

    const meal = within(form).getByRole('group', { name: 'Arroz, tipo 1, cozido' })
    setNumber(meal, 'Quantidade', '150')
    fireEvent.click(saveButton(form))

    await waitFor(() => expect(writes()).toHaveLength(1))
    expect(writes()[0][0]).toBe('/api/plans')
    expect(writes()[0][1]?.method).toBe('POST')
    expect(JSON.parse(String(writes()[0][1]?.body))).toEqual({
      name: 'Cutting',
      targets: { kcal: 2000, protein: 140, carbohydrate: 200, fat: 60 },
      meals: [{ name: 'Almoço', time: null, items: [{ foodId: 'rice', quantity: { amount: 150, unit: 'GRAM' } }] }],
    })
    await waitFor(() => expect(screen.queryByRole('form', { name: 'Plano alimentar' })).not.toBeInTheDocument())
    expect(planListRequests()).toHaveLength(2)
  })

  it('starts the quantity from the reference amount of the food', async () => {
    serve({ plans: [] })
    render(<PlansPage />)
    const form = await fillNewPlan()

    const item = within(form).getByRole('group', { name: 'Arroz, tipo 1, cozido' })

    expect(within(item).getByLabelText('Quantidade (g)', { exact: false })).toHaveDisplayValue('100')
  })

  it('keeps the save button disabled until the plan is complete', async () => {
    serve({ plans: [] })
    render(<PlansPage />)
    const form = await openNewPlan()

    expect(saveButton(form)).toHaveAttribute('aria-disabled', 'true')

    setText(form, 'Nome do plano', 'Cutting')
    setNumber(form, 'Calorias', '2000')
    setNumber(form, 'Proteína', '140')
    setNumber(form, 'Carboidrato', '200')
    setNumber(form, 'Gordura', '60')
    setText(form, 'Nome da refeição', 'Almoço')
    expect(saveButton(form)).toHaveAttribute('aria-disabled', 'true')

    const complete = await addFood(form, 'Refeição 1', 'Arroz, tipo 1, cozido')
    expect(saveButton(complete)).not.toHaveAttribute('aria-disabled', 'true')
  })

  it('goes back to the plan without a food when the user gives up on the search', async () => {
    serve({ plans: [] })
    render(<PlansPage />)
    const form = await openNewPlan()
    setText(form, 'Nome do plano', 'Cutting')
    fireEvent.click(within(form).getByRole('button', { name: 'Adicionar alimento' }))

    fireEvent.click(await screen.findByRole('button', { name: 'Voltar ao plano' }))

    const back = await screen.findByRole('form', { name: 'Plano alimentar' })
    expect(within(back).getByLabelText('Nome do plano', { exact: false })).toHaveValue('Cutting')
    expect(within(back).queryByRole('group', { name: 'Arroz, tipo 1, cozido' })).not.toBeInTheDocument()
  })

  it('adds and removes meals and items', async () => {
    serve({ plans: [] })
    render(<PlansPage />)
    const form = await fillNewPlan()

    fireEvent.click(within(form).getByRole('button', { name: 'Adicionar refeição' }))
    expect(within(form).getByRole('group', { name: 'Refeição 2' })).toBeInTheDocument()

    fireEvent.click(within(form).getByRole('button', { name: 'Remover item' }))
    expect(within(form).queryByRole('group', { name: 'Arroz, tipo 1, cozido' })).not.toBeInTheDocument()

    fireEvent.click(within(within(form).getByRole('group', { name: 'Refeição 2' })).getByRole('button', { name: 'Remover refeição' }))
    expect(within(form).queryByRole('group', { name: 'Refeição 2' })).not.toBeInTheDocument()
    expect(within(form).getByRole('group', { name: 'Refeição 1' })).toBeInTheDocument()
  })

  it('sends the plan once when the user saves twice', async () => {
    let answer: (response: Response) => void = () => {}
    serve({ plans: [], write: () => new Promise((resolve) => { answer = resolve }) })
    render(<PlansPage />)
    const form = await fillNewPlan()

    fireEvent.click(saveButton(form))
    fireEvent.click(saveButton(form))
    fireEvent.submit(form)

    expect(writes()).toHaveLength(1)
    await act(async () => answer(jsonResponse(cutting, 201)))
    await waitFor(() => expect(screen.queryByRole('form', { name: 'Plano alimentar' })).not.toBeInTheDocument())
    expect(writes()).toHaveLength(1)
  })

  it.each([
    ['the server rejects the plan', () => Promise.resolve(jsonResponse({ detail: 'Meal names must be different inside a plan' }, 400)), 'Meal names must be different inside a plan'],
    ['the server cannot be reached', () => Promise.reject(new TypeError('Failed to fetch')), 'Sem conexão com o servidor. Verifique a internet e tente de novo.'],
  ])('keeps the sheet open and ready for another try when %s', async (_case, answer, message) => {
    serve({ plans: [], write: answer })
    render(<PlansPage />)
    const form = await fillNewPlan()

    fireEvent.click(saveButton(form))

    expect(await screen.findByText(message)).toBeInTheDocument()
    expect(screen.getByText('Não foi possível salvar o plano')).toBeInTheDocument()
    expect(screen.getByRole('form', { name: 'Plano alimentar' })).toBeInTheDocument()
    await waitFor(() => expect(saveButton(form)).not.toHaveAttribute('aria-disabled', 'true'))
  })
})

describe('editing a plan', () => {
  it('opens the saved plan and replaces it as a whole, keeping the time it had', async () => {
    serve({ plans: [summary(cutting), summary(bulking)] })
    render(<PlansPage />)
    const form = await openPlan('Cutting')

    expect(within(form).getByLabelText('Nome do plano', { exact: false })).toHaveValue('Cutting')
    expect(within(form).getByLabelText('Calorias', { exact: false })).toHaveDisplayValue('2000')
    const lunch = within(form).getByRole('group', { name: 'Refeição 1' })
    expect(within(lunch).getByLabelText('Nome da refeição', { exact: false })).toHaveValue('Almoço')
    expect(within(lunch).getByRole('group', { name: 'Arroz, tipo 1, cozido' })).toBeInTheDocument()

    setText(form, 'Nome do plano', 'Cutting 2')
    fireEvent.click(saveButton(form))

    await waitFor(() => expect(writes()).toHaveLength(1))
    expect(writes()[0][0]).toBe('/api/plans/plan-1')
    expect(writes()[0][1]?.method).toBe('PUT')
    expect(JSON.parse(String(writes()[0][1]?.body))).toEqual({
      name: 'Cutting 2',
      targets: cutting.targets,
      meals: [
        { name: 'Almoço', time: '12:30:00', items: [{ foodId: 'rice', quantity: { amount: 150, unit: 'GRAM' } }] },
        { name: 'Jantar', time: null, items: [{ foodId: 'milkshake', quantity: { amount: 1, unit: 'PORTION' } }] },
      ],
    })
    await waitFor(() => expect(screen.queryByRole('form', { name: 'Plano alimentar' })).not.toBeInTheDocument())
  })

  it('explains when the plan cannot be loaded and tries again', async () => {
    serve({ plans: [summary(cutting)] })
    render(<PlansPage />)
    await screen.findByText('Cutting')
    fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'))

    fireEvent.click(screen.getByText('Cutting'))
    expect(await screen.findByText('Não foi possível carregar o plano')).toBeInTheDocument()
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Tentar de novo' }))

    expect(await screen.findByRole('form', { name: 'Plano alimentar' })).toBeInTheDocument()
  })
})

describe('activating a plan', () => {
  it('activates the plan without opening it and reloads the list', async () => {
    serve({ plans: [summary(cutting), summary(bulking)] })
    render(<PlansPage />)

    fireEvent.click(await screen.findByRole('button', { name: 'Ativar Bulking' }))

    await waitFor(() => expect(writes()).toHaveLength(1))
    expect(writes()[0][0]).toBe('/api/plans/plan-2/activation')
    await waitFor(() => expect(planListRequests()).toHaveLength(2))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(fetchMock.mock.calls.filter(([url]) => String(url) === '/api/plans/plan-2')).toHaveLength(0)
  })

  it('activates once when the user clicks twice', async () => {
    let answer: (response: Response) => void = () => {}
    serve({ plans: [summary(cutting), summary(bulking)], activate: () => new Promise((resolve) => { answer = resolve }) })
    render(<PlansPage />)
    const button = await screen.findByRole('button', { name: 'Ativar Bulking' })

    fireEvent.click(button)
    fireEvent.click(button)

    expect(writes()).toHaveLength(1)
    await act(async () => answer(jsonResponse(bulking)))
    await waitFor(() => expect(planListRequests()).toHaveLength(2))
    expect(writes()).toHaveLength(1)
  })

  it('tells the user when the plan could not be activated', async () => {
    serve({
      plans: [summary(cutting), summary(bulking)],
      activate: () => Promise.resolve(jsonResponse({ detail: 'Plan not found: plan-2' }, 404)),
    })
    render(<PlansPage />)

    fireEvent.click(await screen.findByRole('button', { name: 'Ativar Bulking' }))

    expect(await screen.findByText('Plan not found: plan-2')).toBeInTheDocument()
    expect(screen.getByText('Não foi possível ativar o plano')).toBeInTheDocument()
  })
})
