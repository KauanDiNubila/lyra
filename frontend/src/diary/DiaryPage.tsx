import { useState } from 'react'
import { Banner } from '@astryxdesign/core/Banner'
import { Button } from '@astryxdesign/core/Button'
import { DateInput } from '@astryxdesign/core/DateInput'
import { EmptyState } from '@astryxdesign/core/EmptyState'
import { Heading } from '@astryxdesign/core/Heading'
import { HStack } from '@astryxdesign/core/HStack'
import { List, ListItem } from '@astryxdesign/core/List'
import { Spinner } from '@astryxdesign/core/Spinner'
import { Text } from '@astryxdesign/core/Text'
import { VStack } from '@astryxdesign/core/VStack'
import { formatMacroValues, formatReference } from '../foods/formatNutrients.ts'
import { shiftDay, todayIso } from './dates.ts'
import { useDiaryDay } from './useDiaryDay.ts'

export function DiaryPage() {
  const [date, setDate] = useState(() => todayIso())
  const { day, reload } = useDiaryDay(date)

  return (
    <VStack gap={4} padding={4}>
      <Heading level={1}>Diário</Heading>
      <DateInput label="Data" value={date} onChange={(value) => value && setDate(value)} />
      <HStack justify="between">
        <Button label="Dia anterior" variant="secondary" onClick={() => setDate(shiftDay(date, -1))} />
        <Button label="Próximo dia" variant="secondary" onClick={() => setDate(shiftDay(date, 1))} />
      </HStack>
      {day.status === 'loading' && <Spinner label="Carregando o dia" />}
      {day.status === 'error' && (
        <>
          <Banner status="error" title="Não foi possível carregar o dia" description={day.message} />
          <Button label="Tentar de novo" variant="secondary" onClick={reload} />
        </>
      )}
      {day.status === 'success' && day.data.meals.length === 0 && (
        <EmptyState
          title="Nada registrado neste dia"
          description="Quando você registrar o que comeu, as refeições e os totais aparecem aqui."
        />
      )}
      {day.status === 'success' && day.data.meals.length > 0 && (
        <>
          <VStack gap={1}>
            <Text type="label">Total do dia</Text>
            <Text type="large">{formatMacroValues(day.data.total)}</Text>
          </VStack>
          {day.data.meals.map((meal) => (
            <VStack key={meal.name} gap={1}>
              <Heading level={2}>{meal.name}</Heading>
              <Text type="supporting">{formatMacroValues(meal.total)}</Text>
              <List hasDividers>
                {meal.entries.map((entry) => (
                  <ListItem
                    key={entry.id}
                    label={entry.foodName}
                    description={
                      <Text type="supporting" maxLines={2}>
                        {`${formatReference(entry.quantity)} · ${formatMacroValues(entry.macros)}`}
                      </Text>
                    }
                  />
                ))}
              </List>
            </VStack>
          ))}
        </>
      )}
    </VStack>
  )
}
