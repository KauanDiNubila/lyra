import { describe, expect, it } from 'vitest'
import { emptyForm, formFromPlan, toRequest, type PlanFormState } from './planDraft.ts'
import { cutting } from './testSupport.ts'

function validForm(): PlanFormState {
  return formFromPlan(cutting)
}

describe('turning the form into a request', () => {
  it('sends the plan the way it was loaded, with the trimmed names', () => {
    const form = validForm()
    form.name = '  Cutting  '
    form.meals[0].name = ' Almoço '

    const request = toRequest(form)

    expect(request).toEqual({
      name: 'Cutting',
      targets: cutting.targets,
      meals: [
        { name: 'Almoço', time: '12:30:00', items: [{ foodId: 'rice', quantity: { amount: 150, unit: 'GRAM' } }] },
        { name: 'Jantar', time: null, items: [{ foodId: 'milkshake', quantity: { amount: 1, unit: 'PORTION' } }] },
      ],
    })
  })

  it('has nothing to send for a new empty form', () => {
    expect(toRequest(emptyForm())).toBeNull()
  })

  it.each([
    ['a blank plan name', (form: PlanFormState) => { form.name = '   ' }],
    ['a missing target', (form: PlanFormState) => { form.fat = null }],
    ['no meals', (form: PlanFormState) => { form.meals = [] }],
    ['a blank meal name', (form: PlanFormState) => { form.meals[1].name = ' ' }],
    ['a meal without items', (form: PlanFormState) => { form.meals[1].items = [] }],
    ['a missing quantity', (form: PlanFormState) => { form.meals[0].items[0].amount = null }],
    ['a quantity of zero', (form: PlanFormState) => { form.meals[0].items[0].amount = 0 }],
  ])('has nothing to send with %s', (_case, spoil) => {
    const form = validForm()
    spoil(form)

    expect(toRequest(form)).toBeNull()
  })

  it('accepts targets of zero', () => {
    const form = validForm()
    form.kcal = 0

    expect(toRequest(form)?.targets.kcal).toBe(0)
  })
})
