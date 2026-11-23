import { readJson, send } from '../http.ts'
import type { Quantity } from '../foods/foodApi.ts'

export type Macros = {
  kcal: number
  protein: number
  carbohydrate: number
  fat: number
}

export type DiaryEntry = {
  id: string
  date: string
  mealName: string
  foodId: string
  foodName: string
  quantity: Quantity
  macros: Macros
}

export type Meal = {
  name: string
  entries: DiaryEntry[]
  total: Macros
}

export type DiaryDay = {
  date: string
  meals: Meal[]
  total: Macros
}

export async function fetchDay(date: string, signal: AbortSignal): Promise<DiaryDay> {
  const response = await send(`/api/diary/${date}`, { signal })
  return readJson<DiaryDay>(response)
}
