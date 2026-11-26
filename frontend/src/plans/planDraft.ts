export type ISOTimeString = `${number}:${number}`
import type { MeasurementUnit } from '../foods/foodApi.ts'
import type { Plan, PlanRequest } from './planApi.ts'

export type ItemDraft = {
  key: number
  foodId: string
  foodName: string
  unit: MeasurementUnit
  amount: number | null
}

export type MealDraft = {
  key: number
  name: string
  time: ISOTimeString | null
  items: ItemDraft[]
}

export type PlanFormState = {
  name: string
  kcal: number | null
  protein: number | null
  carbohydrate: number | null
  fat: number | null
  meals: MealDraft[]
}

let lastKey = 0

export function newKey(): number {
  lastKey += 1
  return lastKey
}

export function emptyMeal(): MealDraft {
  return { key: newKey(), name: '', time: null, items: [] }
}

export function emptyForm(): PlanFormState {
  return { name: '', kcal: null, protein: null, carbohydrate: null, fat: null, meals: [emptyMeal()] }
}

export function formFromPlan(plan: Plan): PlanFormState {
  return {
    name: plan.name,
    ...plan.targets,
    meals: plan.meals.map((meal) => ({
      key: newKey(),
      name: meal.name,
      time: meal.time as ISOTimeString | null,
      items: meal.items.map((item) => ({
        key: newKey(),
        foodId: item.foodId,
        foodName: item.foodName,
        unit: item.quantity.unit,
        amount: item.quantity.amount,
      })),
    })),
  }
}

export function toRequest(form: PlanFormState): PlanRequest | null {
  const name = form.name.trim()
  const { kcal, protein, carbohydrate, fat } = form
  if (name === '' || kcal === null || protein === null || carbohydrate === null || fat === null) return null
  if (form.meals.length === 0) return null

  const meals: PlanRequest['meals'] = []
  for (const meal of form.meals) {
    const mealName = meal.name.trim()
    if (mealName === '' || meal.items.length === 0) return null
    const items: PlanRequest['meals'][number]['items'] = []
    for (const item of meal.items) {
      if (item.amount === null || item.amount <= 0) return null
      items.push({ foodId: item.foodId, quantity: { amount: item.amount, unit: item.unit } })
    }
    meals.push({ name: mealName, time: meal.time, items })
  }
  return { name, targets: { kcal, protein, carbohydrate, fat }, meals }
}
