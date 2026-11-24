import { formatReference } from '../foods/formatNutrients.ts'
import type { DiaryEntry } from './diaryApi.ts'

export function planNote(entry: DiaryEntry): string | null {
  const { planned } = entry
  if (planned === null) return null
  const plannedQuantity = formatReference(planned.quantity)
  switch (entry.origin) {
    case 'ADJUSTED':
      return `Ajustado (plano: ${plannedQuantity})`
    case 'SWAPPED':
      return `Trocado (plano: ${planned.foodName}, ${plannedQuantity})`
    case 'SKIPPED':
      return `Pulado (plano: ${plannedQuantity})`
    default:
      return 'Planejado'
  }
}
