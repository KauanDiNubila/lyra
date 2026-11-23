import { ensureOk, readJson, send } from '../http.ts'
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

export type NewEntry = {
  foodId: string
  mealName: string
  quantity: Quantity
}

export async function fetchDay(date: string, signal: AbortSignal): Promise<DiaryDay> {
  const response = await send(`/api/diary/${date}`, { signal })
  return readJson<DiaryDay>(response)
}

export async function addEntry(date: string, entry: NewEntry): Promise<DiaryEntry> {
  const response = await send(`/api/diary/${date}/entries`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(entry),
  })
  return readJson<DiaryEntry>(response)
}

export async function changeQuantity(id: string, quantity: Quantity): Promise<DiaryEntry> {
  const response = await send(`/api/diary/entries/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ quantity }),
  })
  return readJson<DiaryEntry>(response)
}

export async function removeEntry(id: string): Promise<void> {
  const response = await send(`/api/diary/entries/${id}`, { method: 'DELETE' })
  await ensureOk(response)
}
