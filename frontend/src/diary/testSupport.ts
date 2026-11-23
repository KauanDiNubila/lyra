import type { Food } from '../foods/foodApi.ts'
import type { DiaryDay, DiaryEntry } from './diaryApi.ts'

export const lunchRice: DiaryEntry = {
  id: 'rice-entry',
  date: '2026-11-19',
  mealName: 'Almoço',
  foodId: 'rice',
  foodName: 'Arroz, tipo 1, cozido',
  quantity: { amount: 150, unit: 'GRAM' },
  macros: { kcal: 192.38775, protein: 3.7812, carbohydrate: 42.0897, fat: 0.3405 },
}

export const dinnerMilkshake: DiaryEntry = {
  id: 'milkshake-entry',
  date: '2026-11-19',
  mealName: 'Jantar',
  foodId: 'milkshake',
  foodName: 'Milkshake de morango',
  quantity: { amount: 1, unit: 'PORTION' },
  macros: { kcal: 590, protein: 13, carbohydrate: 98, fat: 16 },
}

export const day: DiaryDay = {
  date: '2026-11-19',
  meals: [
    { name: 'Almoço', entries: [lunchRice], total: lunchRice.macros },
    { name: 'Jantar', entries: [dinnerMilkshake], total: dinnerMilkshake.macros },
  ],
  total: { kcal: 782.38775, protein: 16.7812, carbohydrate: 140.0897, fat: 16.3405 },
}

export const emptyDay = (date: string): DiaryDay => ({
  date,
  meals: [],
  total: { kcal: 0, protein: 0, carbohydrate: 0, fat: 0 },
})

export const rice: Food = {
  id: 'rice',
  name: 'Arroz, tipo 1, cozido',
  source: 'TACO',
  referenceAmount: { amount: 100, unit: 'GRAM' },
  nutrients: { kcal: 128.2585, protein: 2.5208, carbohydrate: 28.0598, fat: 0.227, fiber: 1.561, sodium: 1.2007 },
}

export const milkshake: Food = {
  id: 'milkshake',
  name: 'Milkshake de morango',
  source: 'CUSTOM',
  referenceAmount: { amount: 1, unit: 'PORTION' },
  nutrients: { kcal: 590, protein: 13, carbohydrate: 98, fat: 16, fiber: null, sodium: null },
}

export function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': status >= 400 ? 'application/problem+json' : 'application/json' },
  })
}
