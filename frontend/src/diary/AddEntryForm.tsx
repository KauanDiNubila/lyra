import { useState, type FormEvent } from 'react'
import { Banner } from '@astryxdesign/core/Banner'
import { Button } from '@astryxdesign/core/Button'
import { Heading } from '@astryxdesign/core/Heading'
import { HStack } from '@astryxdesign/core/HStack'
import { NumberInput } from '@astryxdesign/core/NumberInput'
import { VStack } from '@astryxdesign/core/VStack'
import type { Food } from '../foods/foodApi.ts'
import { FoodPicker } from '../foods/FoodPicker.tsx'
import { quantityLabel } from '../foods/formatNutrients.ts'
import { useSubmit } from '../useSubmit.ts'
import { addEntry, type NewEntry } from './diaryApi.ts'
import { MealField } from './MealField.tsx'
import { QuickAddForm } from './QuickAddForm.tsx'

type Props = {
  date: string
  mealNames: string[]
  onAdded: () => void
}

type Step = { name: 'search' } | { name: 'details'; food: Food } | { name: 'quick-add' }

const SEARCH: Step = { name: 'search' }

export function AddEntryForm({ date, mealNames, onAdded }: Props) {
  const [query, setQuery] = useState('')
  const [step, setStep] = useState<Step>(SEARCH)

  if (step.name === 'quick-add') {
    return (
      <QuickAddForm
        date={date}
        mealNames={mealNames}
        initialName={query}
        onAdded={onAdded}
        onBack={() => setStep(SEARCH)}
      />
    )
  }
  if (step.name === 'details') {
    return (
      <EntryDetails
        food={step.food}
        date={date}
        mealNames={mealNames}
        onChangeFood={() => setStep(SEARCH)}
        onAdded={onAdded}
      />
    )
  }
  return (
    <VStack gap={4}>
      <FoodPicker
        query={query}
        onQueryChange={setQuery}
        onSelect={(food) => setStep({ name: 'details', food })}
        emptyDescription="Não achou? Use o botão abaixo para adicionar com os valores de uma porção."
      />
      <Button label="Não achou? Adicionar rápido" variant="secondary" onClick={() => setStep({ name: 'quick-add' })} />
    </VStack>
  )
}

type DetailsProps = Props & {
  food: Food
  onChangeFood: () => void
}

function EntryDetails({ food, date, mealNames, onChangeFood, onAdded }: DetailsProps) {
  const { unit } = food.referenceAmount
  const [amount, setAmount] = useState<number | null>(food.referenceAmount.amount)
  const [mealName, setMealName] = useState('')
  const { run, isRunning, error } = useSubmit()

  const entry: NewEntry | null =
    amount !== null && amount > 0 && mealName.trim() !== ''
      ? { foodId: food.id, mealName: mealName.trim(), quantity: { amount, unit } }
      : null

  function submit(event: FormEvent) {
    event.preventDefault()
    if (entry) {
      void run(async () => {
        await addEntry(date, entry)
        onAdded()
      })
    }
  }

  return (
    <form onSubmit={submit} aria-label="Adicionar ao diário">
      <VStack gap={3}>
        {error && <Banner status="error" title="Não foi possível adicionar" description={error} />}
        <HStack justify="between" align="center">
          <Heading level={2}>{food.name}</Heading>
          <Button label="Trocar alimento" variant="ghost" onClick={onChangeFood} />
        </HStack>
        <NumberInput label={quantityLabel(unit)} value={amount} onChange={setAmount} min={0} isRequired />
        <MealField value={mealName} onChange={setMealName} mealNames={mealNames} />
        <Button type="submit" label="Adicionar ao dia" variant="primary" isDisabled={!entry} isLoading={isRunning} />
      </VStack>
    </form>
  )
}
