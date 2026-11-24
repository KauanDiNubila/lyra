import type { Plan, PlanSummary } from './planApi.ts'

export const cutting: Plan = {
  id: 'plan-1',
  name: 'Cutting',
  active: true,
  targets: { kcal: 2000, protein: 140, carbohydrate: 200, fat: 60 },
  meals: [
    {
      name: 'Almoço',
      time: '12:30:00',
      items: [{ foodId: 'rice', foodName: 'Arroz, tipo 1, cozido', quantity: { amount: 150, unit: 'GRAM' } }],
    },
    {
      name: 'Jantar',
      time: null,
      items: [{ foodId: 'milkshake', foodName: 'Milkshake de morango', quantity: { amount: 1, unit: 'PORTION' } }],
    },
  ],
}

export const bulking: Plan = { ...cutting, id: 'plan-2', name: 'Bulking', active: false }

export function summary({ id, name, active }: Plan): PlanSummary {
  return { id, name, active }
}
