import { useState, type FormEvent } from 'react'
import { Banner } from '@astryxdesign/core/Banner'
import { Button } from '@astryxdesign/core/Button'
import { Heading } from '@astryxdesign/core/Heading'
import { HStack } from '@astryxdesign/core/HStack'
import { NumberInput } from '@astryxdesign/core/NumberInput'
import { TextInput } from '@astryxdesign/core/TextInput'
import { VStack } from '@astryxdesign/core/VStack'
import { useSubmit } from '../useSubmit.ts'
import { quickAddEntry, type NewQuickEntry } from './diaryApi.ts'
import { MealField } from './MealField.tsx'

type Props = {
  date: string
  mealNames: string[]
  initialName: string
  onAdded: () => void
  onBack: () => void
}

type Fields = {
  name: string
  mealName: string
  portions: number | null
  kcal: number | null
  protein: number | null
  carbohydrate: number | null
  fat: number | null
}

export function QuickAddForm({ date, mealNames, initialName, onAdded, onBack }: Props) {
  const [fields, setFields] = useState<Fields>({
    name: initialName,
    mealName: '',
    portions: 1,
    kcal: null,
    protein: null,
    carbohydrate: null,
    fat: null,
  })
  const { run, isRunning, error } = useSubmit()

  function change<Key extends keyof Fields>(key: Key) {
    return (value: Fields[Key]) => setFields((current) => ({ ...current, [key]: value }))
  }

  const { name, mealName, portions, kcal, protein, carbohydrate, fat } = fields
  const entry: NewQuickEntry | null =
    name.trim().length >= 2 && mealName.trim() !== '' && portions !== null && portions > 0
      && kcal !== null && protein !== null && carbohydrate !== null && fat !== null
      ? { name: name.trim(), mealName: mealName.trim(), portions, macros: { kcal, protein, carbohydrate, fat } }
      : null

  function submit(event: FormEvent) {
    event.preventDefault()
    if (entry) {
      void run(async () => {
        await quickAddEntry(date, entry)
        onAdded()
      })
    }
  }

  return (
    <form onSubmit={submit} aria-label="Adicionar rápido">
      <VStack gap={3}>
        <HStack justify="between" align="center">
          <Heading level={2}>Adicionar rápido</Heading>
          <Button label="Voltar à busca" variant="ghost" onClick={onBack} />
        </HStack>
        <TextInput label="Nome" value={name} onChange={change('name')} isRequired hasAutoFocus />
        <NumberInput label="Porções" value={portions} onChange={change('portions')} min={0} isRequired />
        <NumberInput label="Calorias de uma porção (kcal)" value={kcal} onChange={change('kcal')} min={0} isRequired />
        <NumberInput label="Proteína de uma porção (g)" value={protein} onChange={change('protein')} min={0} isRequired />
        <NumberInput
          label="Carboidrato de uma porção (g)"
          value={carbohydrate}
          onChange={change('carbohydrate')}
          min={0}
          isRequired
        />
        <NumberInput label="Gordura de uma porção (g)" value={fat} onChange={change('fat')} min={0} isRequired />
        <MealField value={mealName} onChange={change('mealName')} mealNames={mealNames} />
        {error && <Banner status="error" title="Não foi possível adicionar" description={error} />}
        <Button type="submit" label="Adicionar ao dia" variant="primary" isDisabled={!entry} isLoading={isRunning} />
      </VStack>
    </form>
  )
}
