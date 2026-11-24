import { useState, type FormEvent } from 'react'
import { Banner } from '@astryxdesign/core/Banner'
import { Button } from '@astryxdesign/core/Button'
import { Heading } from '@astryxdesign/core/Heading'
import { HStack } from '@astryxdesign/core/HStack'
import { NumberInput } from '@astryxdesign/core/NumberInput'
import { Text } from '@astryxdesign/core/Text'
import { TextInput } from '@astryxdesign/core/TextInput'
import { TimeInput } from '@astryxdesign/core/TimeInput'
import { VStack } from '@astryxdesign/core/VStack'
import type { Food } from '../foods/foodApi.ts'
import { FoodSearchPanel } from '../foods/FoodSearchPanel.tsx'
import { quantityLabel } from '../foods/formatNutrients.ts'
import { useFoodSearch } from '../foods/useFoodSearch.ts'
import { useSubmit } from '../useSubmit.ts'
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
  const { search, reload } = useFoodSearch(query)
  const { run, isRunning, error } = useSubmit()
  const request = toRequest(form)

  function changeMeal(key: number, change: (meal: MealDraft) => MealDraft) {
    setForm((current) => ({
      ...current,
      meals: current.meals.map((meal) => (meal.key === key ? change(meal) : meal)),
    }))
  }

  function addItem(mealKey: number, food: Food) {
    const item = {
      key: newKey(),
      foodId: food.id,
      foodName: food.name,
      unit: food.referenceAmount.unit,
      amount: food.referenceAmount.amount,
    }
    changeMeal(mealKey, (meal) => ({ ...meal, items: [...meal.items, item] }))
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
        <FoodSearchPanel
          query={query}
          onQueryChange={setQuery}
          search={search}
          onRetry={reload}
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
          <VStack key={meal.key} gap={2} role="group" aria-label={`Refeição ${index + 1}`}>
            <Heading level={2}>{`Refeição ${index + 1}`}</Heading>
            <TextInput
              label="Nome da refeição"
              value={meal.name}
              onChange={(name) => changeMeal(meal.key, (current) => ({ ...current, name }))}
              isRequired
            />
            <TimeInput
              label="Horário"
              value={meal.time ?? undefined}
              onChange={(time) => changeMeal(meal.key, (current) => ({ ...current, time: time ?? null }))}
              isOptional
              hasClear
            />
            {meal.items.map((item) => (
              <VStack key={item.key} gap={1} role="group" aria-label={item.foodName}>
                <Text type="label">{item.foodName}</Text>
                <HStack gap={2} align="end">
                  <NumberInput
                    label={quantityLabel(item.unit)}
                    value={item.amount}
                    onChange={(amount) =>
                      changeMeal(meal.key, (current) => ({
                        ...current,
                        items: current.items.map((other) => (other.key === item.key ? { ...other, amount } : other)),
                      }))
                    }
                    min={0}
                    isRequired
                  />
                  <Button
                    label="Remover item"
                    variant="ghost"
                    onClick={() =>
                      changeMeal(meal.key, (current) => ({
                        ...current,
                        items: current.items.filter((other) => other.key !== item.key),
                      }))
                    }
                  />
                </HStack>
              </VStack>
            ))}
            <HStack gap={2} wrap="wrap">
              <Button label="Adicionar alimento" variant="secondary" onClick={() => setPickingForMeal(meal.key)} />
              <Button
                label="Remover refeição"
                variant="ghost"
                onClick={() =>
                  setForm((current) => ({ ...current, meals: current.meals.filter((other) => other.key !== meal.key) }))
                }
              />
            </HStack>
          </VStack>
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
