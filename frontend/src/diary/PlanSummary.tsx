import { List, ListItem } from '@astryxdesign/core/List'
import { Text } from '@astryxdesign/core/Text'
import { formatMacroDifference, formatMacroValues } from '../foods/formatNutrients.ts'
import type { DiaryDay } from './diaryApi.ts'

type Props = {
  day: DiaryDay
}

function Row({ label, value }: { label: string; value: string }) {
  return <ListItem label={label} description={<Text type="supporting">{value}</Text>} />
}

export function PlanSummary({ day }: Props) {
  const hasPlan = day.meals.some((meal) => meal.entries.some((entry) => entry.planned !== null))
  if (!hasPlan && day.targets === null) return null

  return (
    <List header="Comparação" hasDividers>
      {hasPlan && <Row label="Plano" value={formatMacroValues(day.plannedTotal)} />}
      {hasPlan && <Row label="Diferença para o plano" value={formatMacroDifference(day.differenceFromPlan)} />}
      {day.targets && <Row label="Metas" value={formatMacroValues(day.targets)} />}
      {day.differenceFromTargets && (
        <Row label="Diferença para as metas" value={formatMacroDifference(day.differenceFromTargets)} />
      )}
    </List>
  )
}
