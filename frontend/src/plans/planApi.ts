import { readJson, send } from '../http.ts'
import type { Quantity } from '../foods/foodApi.ts'

export type Targets = {
  kcal: number
  protein: number
  carbohydrate: number
  fat: number
}

export type PlanSummary = {
  id: string
  name: string
  active: boolean
}

export type PlanItem = {
  foodId: string
  foodName: string
  quantity: Quantity
}

export type PlanMeal = {
  name: string
  time: string | null
  items: PlanItem[]
}

export type Plan = PlanSummary & {
  targets: Targets
  meals: PlanMeal[]
}

export type PlanRequest = {
  name: string
  targets: Targets
  meals: {
    name: string
    time: string | null
    items: { foodId: string; quantity: Quantity }[]
  }[]
}

export async function fetchPlans(signal: AbortSignal): Promise<PlanSummary[]> {
  const response = await send('/api/plans', { signal })
  return readJson<PlanSummary[]>(response)
}

export async function fetchPlan(id: string, signal: AbortSignal): Promise<Plan> {
  const response = await send(`/api/plans/${id}`, { signal })
  return readJson<Plan>(response)
}

export async function createPlan(request: PlanRequest): Promise<Plan> {
  const response = await send('/api/plans', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  return readJson<Plan>(response)
}

export async function replacePlan(id: string, request: PlanRequest): Promise<Plan> {
  const response = await send(`/api/plans/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  return readJson<Plan>(response)
}

export async function activatePlan(id: string): Promise<Plan> {
  const response = await send(`/api/plans/${id}/activation`, { method: 'POST' })
  return readJson<Plan>(response)
}
