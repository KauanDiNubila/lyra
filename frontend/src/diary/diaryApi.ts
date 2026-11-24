import { ApiError, ensureOk, readJson, send } from '../http.ts'
import type { Quantity } from '../foods/foodApi.ts'

export type Macros = {
  kcal: number
  protein: number
  carbohydrate: number
  fat: number
}

export type Origin = 'EXTRA' | 'PLANNED' | 'ADJUSTED' | 'SKIPPED' | 'SWAPPED'

export type Planned = {
  foodId: string
  foodName: string
  quantity: Quantity
  macros: Macros
}

export type DiaryEntry = {
  id: string
  date: string
  mealName: string
  foodId: string
  foodName: string
  quantity: Quantity
  macros: Macros
  origin: Origin
  planned: Planned | null
}

export type Meal = {
  name: string
  entries: DiaryEntry[]
  total: Macros
  plannedTotal: Macros
  differenceFromPlan: Macros
}

export type DiaryDay = {
  date: string
  meals: Meal[]
  total: Macros
  plannedTotal: Macros
  differenceFromPlan: Macros
  targets: Macros | null
  differenceFromTargets: Macros | null
}

export type NewEntry = {
  foodId: string
  mealName: string
  quantity: Quantity
}

export type EntryChange = {
  foodId?: string
  quantity?: Quantity
  mealName?: string
}

export async function fetchDay(date: string, signal: AbortSignal): Promise<DiaryDay> {
  const response = await send(`/api/diary/${date}`, { signal })
  return readJson<DiaryDay>(response)
}

export async function fillDay(date: string): Promise<DiaryDay | null> {
  const response = await send(`/api/diary/${date}/plan`, { method: 'POST' })
  try {
    return await readJson<DiaryDay>(response)
  } catch (cause) {
    if (cause instanceof ApiError && cause.status === 404) return null
    throw cause
  }
}

export async function addEntry(date: string, entry: NewEntry): Promise<DiaryEntry> {
  const response = await send(`/api/diary/${date}/entries`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(entry),
  })
  return readJson<DiaryEntry>(response)
}

export async function changeEntry(id: string, change: EntryChange): Promise<DiaryEntry> {
  const response = await send(`/api/diary/entries/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(change),
  })
  return readJson<DiaryEntry>(response)
}

export async function removeEntry(id: string): Promise<void> {
  const response = await send(`/api/diary/entries/${id}`, { method: 'DELETE' })
  await ensureOk(response)
}
