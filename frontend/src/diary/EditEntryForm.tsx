import { useState, type FormEvent } from 'react'
import { AlertDialog } from '@astryxdesign/core/AlertDialog'
import { Banner } from '@astryxdesign/core/Banner'
import { Button } from '@astryxdesign/core/Button'
import { Heading } from '@astryxdesign/core/Heading'
import { NumberInput } from '@astryxdesign/core/NumberInput'
import { Text } from '@astryxdesign/core/Text'
import { TextInput } from '@astryxdesign/core/TextInput'
import { VStack } from '@astryxdesign/core/VStack'
import type { Food, Quantity } from '../foods/foodApi.ts'
import { FoodPicker } from '../foods/FoodPicker.tsx'
import { formatReference, quantityLabel } from '../foods/formatNutrients.ts'
import { useSubmit } from '../useSubmit.ts'
import { changeEntry, removeEntry, type DiaryEntry, type EntryChange } from './diaryApi.ts'
import { planNote } from './diaryFormat.ts'

type Props = {
  entry: DiaryEntry
  onChanged: () => void
}

function buildChange(entry: DiaryEntry, food: Food | null, quantity: Quantity | null, mealName: string): EntryChange | null {
  if (quantity === null || mealName === '') return null
  const change: EntryChange = {}
  if (food) {
    change.foodId = food.id
    change.quantity = quantity
  } else if (quantity.amount !== entry.quantity.amount) {
    change.quantity = quantity
  }
  if (mealName !== entry.mealName) change.mealName = mealName
  return Object.keys(change).length === 0 ? null : change
}

export function EditEntryForm({ entry, onChanged }: Props) {
  const isFromPlan = entry.planned !== null
  const [food, setFood] = useState<Food | null>(null)
  const [isPicking, setIsPicking] = useState(false)
  const [query, setQuery] = useState('')
  const [amount, setAmount] = useState<number | null>(entry.quantity.amount)
  const [mealName, setMealName] = useState(entry.mealName)
  const [isConfirmingRemoval, setIsConfirmingRemoval] = useState(false)
  const { run, isRunning, error } = useSubmit()

  const unit = food?.referenceAmount.unit ?? entry.quantity.unit
  const quantity = amount !== null && (amount > 0 || isFromPlan) ? { amount, unit } : null
  const change = buildChange(entry, food, quantity, mealName.trim())
  const note = planNote(entry)

  function pick(picked: Food) {
    setFood(picked)
    setAmount(picked.referenceAmount.amount)
    setIsPicking(false)
    setQuery('')
  }

  function undoSwap() {
    setFood(null)
    setAmount(entry.quantity.amount)
  }

  function save(event: FormEvent) {
    event.preventDefault()
    if (change) {
      void run(async () => {
        await changeEntry(entry.id, change)
        onChanged()
      })
    }
  }

  function skip() {
    void run(async () => {
      await changeEntry(entry.id, { quantity: { amount: 0, unit: entry.quantity.unit } })
      onChanged()
    })
  }

  async function remove() {
    const isRemoved = await run(async () => {
      await removeEntry(entry.id)
      onChanged()
    })
    if (!isRemoved) setIsConfirmingRemoval(false)
  }

  if (isPicking) {
    return (
      <VStack gap={4}>
        <Button label="Voltar à entrada" variant="ghost" onClick={() => setIsPicking(false)} />
        <FoodPicker
          query={query}
          onQueryChange={setQuery}
          onSelect={pick}
          emptyDescription="Cadastre o alimento na aba Alimentos e volte aqui para trocá-lo."
        />
      </VStack>
    )
  }

  return (
    <form onSubmit={save} aria-label="Editar entrada">
      <VStack gap={3}>
        {error && <Banner status="error" title="Não foi possível salvar" description={error} />}
        <VStack gap={1}>
          <Heading level={2}>{food?.name ?? entry.foodName}</Heading>
          {note && <Text type="supporting">{note}</Text>}
        </VStack>
        <NumberInput label={quantityLabel(unit)} value={amount} onChange={setAmount} min={0} isRequired />
        <TextInput label="Refeição" value={mealName} onChange={setMealName} isRequired />
        {food ? (
          <Button label="Desfazer a troca" variant="ghost" onClick={undoSwap} />
        ) : (
          <Button label="Trocar alimento" variant="secondary" onClick={() => setIsPicking(true)} />
        )}
        <Button type="submit" label="Salvar" variant="primary" isDisabled={!change} isLoading={isRunning} />
        {isFromPlan && entry.quantity.amount > 0 && !food && (
          <Button label="Pular" variant="secondary" isDisabled={isRunning} onClick={skip} />
        )}
        {!isFromPlan && <Button label="Remover" variant="destructive" onClick={() => setIsConfirmingRemoval(true)} />}
      </VStack>
      <AlertDialog
        isOpen={isConfirmingRemoval}
        onOpenChange={setIsConfirmingRemoval}
        title="Remover do diário?"
        description={`${entry.foodName} (${formatReference(entry.quantity)}) será removido de ${entry.mealName}. Isso não pode ser desfeito.`}
        actionLabel="Remover"
        cancelLabel="Cancelar"
        isActionLoading={isRunning}
        onAction={remove}
      />
    </form>
  )
}
