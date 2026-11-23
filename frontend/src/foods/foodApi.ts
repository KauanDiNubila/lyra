import { readJson, send } from '../http.ts'

export const MEASUREMENT_UNITS = ['GRAM', 'MILLILITER', 'PORTION'] as const

export type MeasurementUnit = (typeof MEASUREMENT_UNITS)[number]

export type Quantity = {
  amount: number
  unit: MeasurementUnit
}

export type Nutrients = {
  kcal: number
  protein: number
  carbohydrate: number
  fat: number
  fiber: number | null
  sodium: number | null
}

export type Food = {
  id: string
  name: string
  source: 'TACO' | 'CUSTOM'
  referenceAmount: Quantity
  nutrients: Nutrients
}

export type NewFood = Omit<Food, 'id' | 'source'>

export async function searchFoods(query: string, signal: AbortSignal): Promise<Food[]> {
  const response = await send(`/api/foods?query=${encodeURIComponent(query)}`, { signal })
  return readJson<Food[]>(response)
}

export async function createFood(food: NewFood): Promise<Food> {
  const response = await send('/api/foods', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(food),
  })
  return readJson<Food>(response)
}
