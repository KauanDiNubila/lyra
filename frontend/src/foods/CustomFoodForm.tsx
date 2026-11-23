import { useState, type FormEvent } from 'react'
import { Banner } from '@astryxdesign/core/Banner'
import { Button } from '@astryxdesign/core/Button'
import { NumberInput } from '@astryxdesign/core/NumberInput'
import { Selector } from '@astryxdesign/core/Selector'
import { TextInput } from '@astryxdesign/core/TextInput'
import { VStack } from '@astryxdesign/core/VStack'
import { createFood, MEASUREMENT_UNITS, type Food, type MeasurementUnit, type NewFood } from './foodApi.ts'
import { unitLabels } from './formatNutrients.ts'

type Props = {
  initialName: string
  onCreated: (food: Food) => void
}

type Fields = {
  name: string
  amount: number | null
  unit: MeasurementUnit
  kcal: number | null
  protein: number | null
  carbohydrate: number | null
  fat: number | null
  fiber: number | null
  sodium: number | null
}

const unitOptions = MEASUREMENT_UNITS.map((unit) => ({ value: unit, label: unitLabels[unit] }))

export function CustomFoodForm({ initialName, onCreated }: Props) {
  const [fields, setFields] = useState<Fields>({
    name: initialName,
    amount: 100,
    unit: 'GRAM',
    kcal: null,
    protein: null,
    carbohydrate: null,
    fat: null,
    fiber: null,
    sodium: null,
  })
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function change<Key extends keyof Fields>(key: Key) {
    return (value: Fields[Key]) => setFields((current) => ({ ...current, [key]: value }))
  }

  const { name, amount, unit, kcal, protein, carbohydrate, fat, fiber, sodium } = fields
  const draft: NewFood | null =
    name.trim().length >= 2 && amount !== null && amount > 0
      && kcal !== null && protein !== null && carbohydrate !== null && fat !== null
      ? {
          name: name.trim(),
          referenceAmount: { amount, unit },
          nutrients: { kcal, protein, carbohydrate, fat, fiber, sodium },
        }
      : null

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!draft || isSaving) return
    setIsSaving(true)
    setError(null)
    try {
      onCreated(await createFood(draft))
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause))
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <form onSubmit={submit} aria-label="Novo alimento">
      <VStack gap={3}>
        {error && <Banner status="error" title="Não foi possível salvar" description={error} />}
        <TextInput label="Nome" value={name} onChange={change('name')} isRequired />
        <NumberInput label="Quantidade de referência" value={amount} onChange={change('amount')} min={0} isRequired />
        <Selector
          label="Unidade"
          options={unitOptions}
          value={unit}
          onChange={(value) => value && change('unit')(value as MeasurementUnit)}
        />
        <NumberInput label="Calorias (kcal)" value={kcal} onChange={change('kcal')} min={0} isRequired />
        <NumberInput label="Proteína (g)" value={protein} onChange={change('protein')} min={0} isRequired />
        <NumberInput label="Carboidrato (g)" value={carbohydrate} onChange={change('carbohydrate')} min={0} isRequired />
        <NumberInput label="Gordura (g)" value={fat} onChange={change('fat')} min={0} isRequired />
        <NumberInput label="Fibra (g)" value={fiber} onChange={change('fiber')} min={0} isOptional />
        <NumberInput label="Sódio (mg)" value={sodium} onChange={change('sodium')} min={0} isOptional />
        <Button type="submit" label="Salvar alimento" variant="primary" isDisabled={!draft} isLoading={isSaving} />
      </VStack>
    </form>
  )
}
