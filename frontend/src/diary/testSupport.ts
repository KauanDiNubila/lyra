import type { Food } from '../foods/foodApi.ts'
import type { DiaryDay, DiaryEntry, Macros } from './diaryApi.ts'

export const zeroMacros: Macros = { kcal: 0, protein: 0, carbohydrate: 0, fat: 0 }

export function macros(kcal: number): Macros {
  return { kcal, protein: kcal / 10, carbohydrate: kcal / 5, fat: kcal / 20 }
}

export const lunchRice: DiaryEntry = {
  id: 'rice-entry',
  date: '2026-11-19',
  mealName: 'Almoço',
  foodId: 'rice',
  foodName: 'Arroz, tipo 1, cozido',
  quantity: { amount: 150, unit: 'GRAM' },
  macros: { kcal: 192.38775, protein: 3.7812, carbohydrate: 42.0897, fat: 0.3405 },
  origin: 'EXTRA',
  planned: null,
}

export const dinnerMilkshake: DiaryEntry = {
  id: 'milkshake-entry',
  date: '2026-11-19',
  mealName: 'Jantar',
  foodId: 'milkshake',
  foodName: 'Milkshake de morango',
  quantity: { amount: 1, unit: 'PORTION' },
  macros: { kcal: 590, protein: 13, carbohydrate: 98, fat: 16 },
  origin: 'EXTRA',
  planned: null,
}

export const day: DiaryDay = {
  date: '2026-11-19',
  meals: [
    { name: 'Almoço', entries: [lunchRice], total: lunchRice.macros, plannedTotal: zeroMacros, differenceFromPlan: lunchRice.macros },
    {
      name: 'Jantar',
      entries: [dinnerMilkshake],
      total: dinnerMilkshake.macros,
      plannedTotal: zeroMacros,
      differenceFromPlan: dinnerMilkshake.macros,
    },
  ],
  total: { kcal: 782.38775, protein: 16.7812, carbohydrate: 140.0897, fat: 16.3405 },
  plannedTotal: zeroMacros,
  differenceFromPlan: { kcal: 782.38775, protein: 16.7812, carbohydrate: 140.0897, fat: 16.3405 },
  targets: null,
  differenceFromTargets: null,
}

export const emptyDay = (date: string): DiaryDay => ({
  date,
  meals: [],
  total: zeroMacros,
  plannedTotal: zeroMacros,
  differenceFromPlan: zeroMacros,
  targets: null,
  differenceFromTargets: null,
})

export const adjustedRice: DiaryEntry = {
  id: 'adjusted-rice',
  date: '2026-11-19',
  mealName: 'Almoço',
  foodId: 'rice',
  foodName: 'Arroz, tipo 1, cozido',
  quantity: { amount: 250, unit: 'GRAM' },
  macros: macros(320),
  origin: 'ADJUSTED',
  planned: {
    foodId: 'rice',
    foodName: 'Arroz, tipo 1, cozido',
    quantity: { amount: 150, unit: 'GRAM' },
    macros: macros(192),
  },
}

export const skippedBeans: DiaryEntry = {
  id: 'skipped-beans',
  date: '2026-11-19',
  mealName: 'Jantar',
  foodId: 'beans',
  foodName: 'Feijão',
  quantity: { amount: 0, unit: 'GRAM' },
  macros: zeroMacros,
  origin: 'SKIPPED',
  planned: { foodId: 'beans', foodName: 'Feijão', quantity: { amount: 100, unit: 'GRAM' }, macros: macros(76) },
}

export const swappedChicken: DiaryEntry = {
  id: 'swapped-chicken',
  date: '2026-11-19',
  mealName: 'Jantar',
  foodId: 'chicken',
  foodName: 'Frango',
  quantity: { amount: 120, unit: 'GRAM' },
  macros: macros(190),
  origin: 'SWAPPED',
  planned: { foodId: 'fish', foodName: 'Peixe', quantity: { amount: 100, unit: 'GRAM' }, macros: macros(120) },
}

export const snackApple: DiaryEntry = {
  id: 'snack-apple',
  date: '2026-11-19',
  mealName: 'Lanche',
  foodId: 'apple',
  foodName: 'Maçã',
  quantity: { amount: 100, unit: 'GRAM' },
  macros: macros(60),
  origin: 'EXTRA',
  planned: null,
}

export const planDay: DiaryDay = {
  date: '2026-11-19',
  meals: [
    { name: 'Almoço', entries: [adjustedRice], total: macros(320), plannedTotal: macros(192), differenceFromPlan: macros(128) },
    {
      name: 'Jantar',
      entries: [skippedBeans, swappedChicken],
      total: macros(190),
      plannedTotal: macros(196),
      differenceFromPlan: macros(-6),
    },
    { name: 'Lanche', entries: [snackApple], total: macros(60), plannedTotal: zeroMacros, differenceFromPlan: macros(60) },
  ],
  total: macros(570),
  plannedTotal: macros(388),
  differenceFromPlan: macros(182),
  targets: { kcal: 2000, protein: 140, carbohydrate: 200, fat: 60 },
  differenceFromTargets: { kcal: -1430, protein: -83, carbohydrate: -86, fat: -31.5 },
}

export const rice: Food = {
  id: 'rice',
  name: 'Arroz, tipo 1, cozido',
  source: 'TACO',
  referenceAmount: { amount: 100, unit: 'GRAM' },
  nutrients: { kcal: 128.2585, protein: 2.5208, carbohydrate: 28.0598, fat: 0.227, fiber: 1.561, sodium: 1.2007 },
}

export const nutella: Food = {
  id: 'nutella',
  name: 'Nutella (Ferrero)',
  source: 'OPEN_FOOD_FACTS',
  referenceAmount: { amount: 100, unit: 'GRAM' },
  nutrients: { kcal: 539, protein: 6.3, carbohydrate: 57.5, fat: 30.9, fiber: null, sodium: 41 },
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
