import { Button } from '@astryxdesign/core/Button'
import { HStack } from '@astryxdesign/core/HStack'
import { TextInput } from '@astryxdesign/core/TextInput'
import { suggestMeals } from './meals.ts'

type Props = {
  value: string
  onChange: (mealName: string) => void
  mealNames: string[]
}

export function MealField({ value, onChange, mealNames }: Props) {
  return (
    <>
      <TextInput label="Refeição" value={value} onChange={onChange} isRequired />
      <HStack gap={2} wrap="wrap">
        {suggestMeals(mealNames).map((name) => (
          <Button key={name} label={name} size="sm" variant="secondary" onClick={() => onChange(name)} />
        ))}
      </HStack>
    </>
  )
}
