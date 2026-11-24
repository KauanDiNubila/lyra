import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { DiaryPage } from './DiaryPage.tsx'
import { adjustedRice, jsonResponse, milkshake, planDay, rice } from './testSupport.ts'

const fetchMock = vi.fn<typeof fetch>()

function serve() {
  fetchMock.mockImplementation(async (url, init) => {
    if (init?.method === 'PATCH') return jsonResponse(adjustedRice)
    if (String(url).startsWith('/api/foods')) return jsonResponse([rice, milkshake])
    return jsonResponse(planDay)
  })
}

function patches() {
  return fetchMock.mock.calls.filter(([, init]) => init?.method === 'PATCH')
}

function lastPatchBody() {
  return JSON.parse(String(patches()[0][1]?.body))
}

async function openEntry(name: string) {
  fireEvent.click(await screen.findByText(name))
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

beforeEach(() => {
  fetchMock.mockReset()
  vi.stubGlobal('fetch', fetchMock)
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 10, 19, 10, 0))
  serve()
})

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

describe('editing an entry of the plan', () => {
  it('says what the plan asked for and offers to skip, but not to remove', async () => {
    render(<DiaryPage />)

    const form = await openEntry('Arroz, tipo 1, cozido')

    expect(within(form).getByText('Ajustado (plano: 150g)')).toBeInTheDocument()
    expect(within(form).getByRole('button', { name: 'Pular' })).toBeInTheDocument()
    expect(within(form).queryByRole('button', { name: 'Remover' })).not.toBeInTheDocument()
  })

  it('skips the entry with a quantity of zero and loads the day again', async () => {
    render(<DiaryPage />)
    const form = await openEntry('Arroz, tipo 1, cozido')

    fireEvent.click(within(form).getByRole('button', { name: 'Pular' }))

    await waitFor(() => expect(patches()).toHaveLength(1))
    expect(patches()[0][0]).toBe('/api/diary/entries/adjusted-rice')
    expect(lastPatchBody()).toEqual({ quantity: { amount: 0, unit: 'GRAM' } })
    await waitFor(() => expect(screen.queryByRole('form', { name: 'Editar entrada' })).not.toBeInTheDocument())
  })

  it('accepts a quantity of zero as a way to skip', async () => {
    render(<DiaryPage />)
    const form = await openEntry('Arroz, tipo 1, cozido')

    setAmount(form, '0')
    fireEvent.click(saveButton(form))

    await waitFor(() => expect(patches()).toHaveLength(1))
    expect(lastPatchBody()).toEqual({ quantity: { amount: 0, unit: 'GRAM' } })
  })

  it('does not offer to skip an entry that was already skipped, and lets the user eat it again', async () => {
    render(<DiaryPage />)
    const form = await openEntry('Feijão')

    expect(within(form).queryByRole('button', { name: 'Pular' })).not.toBeInTheDocument()
    expect(within(form).getByText('Pulado (plano: 100g)')).toBeInTheDocument()
    setAmount(form, '80')
    fireEvent.click(saveButton(form))

    await waitFor(() => expect(patches()).toHaveLength(1))
    expect(lastPatchBody()).toEqual({ quantity: { amount: 80, unit: 'GRAM' } })
  })

  it('keeps the save button disabled until something changes', async () => {
    render(<DiaryPage />)
    const form = await openEntry('Arroz, tipo 1, cozido')

    expect(saveButton(form)).toHaveAttribute('aria-disabled', 'true')
    setAmount(form, '260')
    expect(saveButton(form)).not.toHaveAttribute('aria-disabled', 'true')
    setAmount(form, '250')
    expect(saveButton(form)).toHaveAttribute('aria-disabled', 'true')
  })
})

describe('changing the meal of an entry', () => {
  it('sends only the meal, trimmed', async () => {
    render(<DiaryPage />)
    const form = await openEntry('Arroz, tipo 1, cozido')

    fireEvent.change(within(form).getByLabelText('Refeição', { exact: false }), { target: { value: '  Pós-treino ' } })
    fireEvent.click(saveButton(form))

    await waitFor(() => expect(patches()).toHaveLength(1))
    expect(lastPatchBody()).toEqual({ mealName: 'Pós-treino' })
  })

  it('does not allow a blank meal', async () => {
    render(<DiaryPage />)
    const form = await openEntry('Arroz, tipo 1, cozido')

    fireEvent.change(within(form).getByLabelText('Refeição', { exact: false }), { target: { value: '   ' } })

    expect(saveButton(form)).toHaveAttribute('aria-disabled', 'true')
  })

  it('sends the quantity and the meal together', async () => {
    render(<DiaryPage />)
    const form = await openEntry('Arroz, tipo 1, cozido')

    setAmount(form, '300')
    fireEvent.change(within(form).getByLabelText('Refeição', { exact: false }), { target: { value: 'Jantar' } })
    fireEvent.click(saveButton(form))

    await waitFor(() => expect(patches()).toHaveLength(1))
    expect(lastPatchBody()).toEqual({ quantity: { amount: 300, unit: 'GRAM' }, mealName: 'Jantar' })
  })
})

describe('swapping the food of an entry', () => {
  async function pickMilkshake(form: HTMLElement) {
    fireEvent.click(within(form).getByRole('button', { name: 'Trocar alimento' }))
    fireEvent.change(await screen.findByLabelText('Buscar alimento'), { target: { value: 'milk' } })
    fireEvent.click(await screen.findByText('Milkshake de morango'))
    return screen.findByRole('form', { name: 'Editar entrada' })
  }

  it('takes the reference amount and the unit of the new food and sends both with its id', async () => {
    render(<DiaryPage />)
    const form = await openEntry('Arroz, tipo 1, cozido')

    const swapped = await pickMilkshake(form)

    expect(within(swapped).getByRole('heading', { name: 'Milkshake de morango' })).toBeInTheDocument()
    expect(within(swapped).getByLabelText('Quantidade (porção)', { exact: false })).toHaveDisplayValue('1')
    fireEvent.click(saveButton(swapped))
    await waitFor(() => expect(patches()).toHaveLength(1))
    expect(lastPatchBody()).toEqual({ foodId: 'milkshake', quantity: { amount: 1, unit: 'PORTION' } })
  })

  it('goes back to the entry without a food when the user gives up on the search', async () => {
    render(<DiaryPage />)
    const form = await openEntry('Arroz, tipo 1, cozido')
    fireEvent.click(within(form).getByRole('button', { name: 'Trocar alimento' }))

    fireEvent.click(await screen.findByRole('button', { name: 'Voltar à entrada' }))

    const back = await screen.findByRole('form', { name: 'Editar entrada' })
    expect(within(back).getByRole('heading', { name: 'Arroz, tipo 1, cozido' })).toBeInTheDocument()
    expect(saveButton(back)).toHaveAttribute('aria-disabled', 'true')
  })

  it('undoes the swap and brings the old quantity back', async () => {
    render(<DiaryPage />)
    const form = await openEntry('Arroz, tipo 1, cozido')
    const swapped = await pickMilkshake(form)

    fireEvent.click(within(swapped).getByRole('button', { name: 'Desfazer a troca' }))

    expect(within(swapped).getByRole('heading', { name: 'Arroz, tipo 1, cozido' })).toBeInTheDocument()
    expect(within(swapped).getByLabelText('Quantidade (g)', { exact: false })).toHaveDisplayValue('250')
    expect(saveButton(swapped)).toHaveAttribute('aria-disabled', 'true')
  })

  it('does not offer to skip while a swap is waiting to be saved', async () => {
    render(<DiaryPage />)
    const form = await openEntry('Arroz, tipo 1, cozido')

    const swapped = await pickMilkshake(form)

    expect(within(swapped).queryByRole('button', { name: 'Pular' })).not.toBeInTheDocument()
  })

  it('also lets an extra entry swap its food', async () => {
    render(<DiaryPage />)
    const form = await openEntry('Maçã')

    expect(within(form).getByRole('button', { name: 'Remover' })).toBeInTheDocument()
    expect(within(form).queryByRole('button', { name: 'Pular' })).not.toBeInTheDocument()
    const swapped = await pickMilkshake(form)
    fireEvent.click(saveButton(swapped))

    await waitFor(() => expect(patches()).toHaveLength(1))
    expect(patches()[0][0]).toBe('/api/diary/entries/snack-apple')
  })
})
