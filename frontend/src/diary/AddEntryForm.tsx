import { useState, type FormEvent } from 'react'
import { Banner } from '@astryxdesign/core/Banner'
import { Button } from '@astryxdesign/core/Button'
import { Heading } from '@astryxdesign/core/Heading'
import { HStack } from '@astryxdesign/core/HStack'
import { NumberInput } from '@astryxdesign/core/NumberInput'
import { TextInput } from '@astryxdesign/core/TextInput'
import { VStack } from '@astryxdesign/core/VStack'
import type { Food } from '../foods/foodApi.ts'
import { FoodSearchPanel } from '../foods/FoodSearchPanel.tsx'
import { quantityLabel } from '../foods/formatNutrients.ts'
import { useFoodSearch } from '../foods/useFoodSearch.ts'
import { useSubmit } from '../useSubmit.ts'
import { addEntry, type NewEntry } from './diaryApi.ts'
import { suggestMeals } from './meals.ts'

type Props = {
  date: string
  mealNames: string[]
  onAdded: () => void
}

export function AddEntryForm({ date, mealNames, onAdded }: Props) {
  const [query, setQuery] = useState('')
  const [food, setFood] = useState<Food | null>(null)
  const { search, reload } = useFoodSearch(query)

  if (food === null) {
    return (
      <VStack gap={4}>
        <FoodSearchPanel
          query={query}
          onQueryChange={setQuery}
          search={search}
          onRetry={reload}
          onSelect={setFood}
          emptyDescription="Cadastre o alimento na aba Alimentos e volte aqui para registrá-lo."
        />
      </VStack>
    )
  }
  return (
    <EntryDetails
      food={food}
      date={date}
      mealNames={mealNames}
      onChangeFood={() => setFood(null)}
      onAdded={onAdded}
    />
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
        <TextInput label="Refeição" value={mealName} onChange={setMealName} isRequired />
        <HStack gap={2} wrap="wrap">
          {suggestMeals(mealNames).map((name) => (
            <Button key={name} label={name} size="sm" variant="secondary" onClick={() => setMealName(name)} />
          ))}
        </HStack>
        <Button type="submit" label="Adicionar ao dia" variant="primary" isDisabled={!entry} isLoading={isRunning} />
      </VStack>
    </form>
  )
}
