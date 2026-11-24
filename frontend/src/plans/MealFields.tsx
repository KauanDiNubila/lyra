import { Button } from '@astryxdesign/core/Button'
import { Heading } from '@astryxdesign/core/Heading'
import { HStack } from '@astryxdesign/core/HStack'
import { NumberInput } from '@astryxdesign/core/NumberInput'
import { Text } from '@astryxdesign/core/Text'
import { TextInput } from '@astryxdesign/core/TextInput'
import { TimeInput } from '@astryxdesign/core/TimeInput'
import { VStack } from '@astryxdesign/core/VStack'
import { quantityLabel } from '../foods/formatNutrients.ts'
import type { MealDraft } from './planDraft.ts'

type Props = {
  meal: MealDraft
  index: number
  onChange: (meal: MealDraft) => void
  onRemove: () => void
  onAddFood: () => void
}

export function MealFields({ meal, index, onChange, onRemove, onAddFood }: Props) {
  function changeAmount(itemKey: number, amount: number | null) {
    onChange({ ...meal, items: meal.items.map((item) => (item.key === itemKey ? { ...item, amount } : item)) })
  }

  function removeItem(itemKey: number) {
    onChange({ ...meal, items: meal.items.filter((item) => item.key !== itemKey) })
  }

  return (
    <VStack gap={2} role="group" aria-label={`Refeição ${index + 1}`}>
      <Heading level={2}>{`Refeição ${index + 1}`}</Heading>
      <TextInput
        label="Nome da refeição"
        value={meal.name}
        onChange={(name) => onChange({ ...meal, name })}
        isRequired
      />
      <TimeInput
        label="Horário"
        value={meal.time ?? undefined}
        onChange={(time) => onChange({ ...meal, time: time ?? null })}
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
              onChange={(amount) => changeAmount(item.key, amount)}
              min={0}
              isRequired
            />
            <Button label="Remover item" variant="ghost" onClick={() => removeItem(item.key)} />
          </HStack>
        </VStack>
      ))}
      <HStack gap={2} wrap="wrap">
        <Button label="Adicionar alimento" variant="secondary" onClick={onAddFood} />
        <Button label="Remover refeição" variant="ghost" onClick={onRemove} />
      </HStack>
    </VStack>
  )
}
