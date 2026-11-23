import { useState, type FormEvent } from 'react'
import { AlertDialog } from '@astryxdesign/core/AlertDialog'
import { Banner } from '@astryxdesign/core/Banner'
import { Button } from '@astryxdesign/core/Button'
import { Heading } from '@astryxdesign/core/Heading'
import { NumberInput } from '@astryxdesign/core/NumberInput'
import { Text } from '@astryxdesign/core/Text'
import { VStack } from '@astryxdesign/core/VStack'
import { formatReference, quantityLabel } from '../foods/formatNutrients.ts'
import { useSubmit } from '../useSubmit.ts'
import { changeQuantity, removeEntry, type DiaryEntry } from './diaryApi.ts'

type Props = {
  entry: DiaryEntry
  onChanged: () => void
}

export function EditEntryForm({ entry, onChanged }: Props) {
  const { unit } = entry.quantity
  const [amount, setAmount] = useState<number | null>(entry.quantity.amount)
  const [isConfirmingRemoval, setIsConfirmingRemoval] = useState(false)
  const { run, isRunning, error } = useSubmit()

  const quantity = amount !== null && amount > 0 ? { amount, unit } : null

  function save(event: FormEvent) {
    event.preventDefault()
    if (quantity) {
      void run(async () => {
        await changeQuantity(entry.id, quantity)
        onChanged()
      })
    }
  }

  async function remove() {
    const isRemoved = await run(async () => {
      await removeEntry(entry.id)
      onChanged()
    })
    if (!isRemoved) setIsConfirmingRemoval(false)
  }

  return (
    <form onSubmit={save} aria-label="Editar entrada">
      <VStack gap={3}>
        {error && <Banner status="error" title="Não foi possível salvar" description={error} />}
        <VStack gap={1}>
          <Heading level={2}>{entry.foodName}</Heading>
          <Text type="supporting">{entry.mealName}</Text>
        </VStack>
        <NumberInput label={quantityLabel(unit)} value={amount} onChange={setAmount} min={0} isRequired />
        <Button type="submit" label="Salvar" variant="primary" isDisabled={!quantity} isLoading={isRunning} />
        <Button label="Remover" variant="destructive" onClick={() => setIsConfirmingRemoval(true)} />
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
