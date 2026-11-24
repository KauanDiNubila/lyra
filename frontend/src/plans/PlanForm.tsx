import { useState, type FormEvent } from 'react'
import { Banner } from '@astryxdesign/core/Banner'
import { Button } from '@astryxdesign/core/Button'
import { Heading } from '@astryxdesign/core/Heading'
import { NumberInput } from '@astryxdesign/core/NumberInput'
import { TextInput } from '@astryxdesign/core/TextInput'
import { VStack } from '@astryxdesign/core/VStack'
import type { Food } from '../foods/foodApi.ts'
import { FoodPicker } from '../foods/FoodPicker.tsx'
import { useSubmit } from '../useSubmit.ts'
import { MealFields } from './MealFields.tsx'
import type { PlanRequest } from './planApi.ts'
import { emptyMeal, newKey, toRequest, type MealDraft, type PlanFormState } from './planDraft.ts'

type Props = {
  initial: PlanFormState
  save: (request: PlanRequest) => Promise<unknown>
  onSaved: () => void
}

type TargetName = 'kcal' | 'protein' | 'carbohydrate' | 'fat'

const targetFields: { name: TargetName; label: string }[] = [
  { name: 'kcal', label: 'Calorias (kcal)' },
  { name: 'protein', label: 'Proteína (g)' },
  { name: 'carbohydrate', label: 'Carboidrato (g)' },
  { name: 'fat', label: 'Gordura (g)' },
]

export function PlanForm({ initial, save, onSaved }: Props) {
  const [form, setForm] = useState(initial)
  const [pickingForMeal, setPickingForMeal] = useState<number | null>(null)
  const [query, setQuery] = useState('')
  const { run, isRunning, error } = useSubmit()
  const request = toRequest(form)

  function replaceMeal(changed: MealDraft) {
    setForm((current) => ({
      ...current,
      meals: current.meals.map((meal) => (meal.key === changed.key ? changed : meal)),
    }))
  }

  function removeMeal(key: number) {
    setForm((current) => ({ ...current, meals: current.meals.filter((meal) => meal.key !== key) }))
  }

  function addItem(mealKey: number, food: Food) {
    const item = {
      key: newKey(),
      foodId: food.id,
      foodName: food.name,
      unit: food.referenceAmount.unit,
      amount: food.referenceAmount.amount,
    }
    setForm((current) => ({
      ...current,
      meals: current.meals.map((meal) => (meal.key === mealKey ? { ...meal, items: [...meal.items, item] } : meal)),
    }))
    setPickingForMeal(null)
    setQuery('')
  }

  function submit(event: FormEvent) {
    event.preventDefault()
    if (request) {
      void run(async () => {
        await save(request)
        onSaved()
      })
    }
  }

  if (pickingForMeal !== null) {
    return (
      <VStack gap={4}>
        <Button label="Voltar ao plano" variant="ghost" onClick={() => setPickingForMeal(null)} />
        <FoodPicker
          query={query}
          onQueryChange={setQuery}
          onSelect={(food) => addItem(pickingForMeal, food)}
          emptyDescription="Cadastre o alimento na aba Alimentos e volte aqui para usá-lo no plano."
        />
      </VStack>
    )
  }

  return (
    <form onSubmit={submit} aria-label="Plano alimentar">
      <VStack gap={4}>
        {error && <Banner status="error" title="Não foi possível salvar o plano" description={error} />}
        <TextInput
          label="Nome do plano"
          value={form.name}
          onChange={(name) => setForm((current) => ({ ...current, name }))}
          isRequired
        />
        <VStack gap={2} role="group" aria-label="Metas diárias">
          <Heading level={2}>Metas diárias</Heading>
          {targetFields.map(({ name, label }) => (
            <NumberInput
              key={name}
              label={label}
              value={form[name]}
              onChange={(value) => setForm((current) => ({ ...current, [name]: value }))}
              min={0}
              isRequired
            />
          ))}
        </VStack>
        {form.meals.map((meal, index) => (
          <MealFields
            key={meal.key}
            meal={meal}
            index={index}
            onChange={replaceMeal}
            onRemove={() => removeMeal(meal.key)}
            onAddFood={() => setPickingForMeal(meal.key)}
          />
        ))}
        <Button
          label="Adicionar refeição"
          variant="secondary"
          onClick={() => setForm((current) => ({ ...current, meals: [...current.meals, emptyMeal()] }))}
        />
        <Button type="submit" label="Salvar plano" variant="primary" isDisabled={!request} isLoading={isRunning} />
      </VStack>
    </form>
  )
}
