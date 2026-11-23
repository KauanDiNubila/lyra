import type { Food, MeasurementUnit } from './foodApi.ts'

const oneDecimal = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 })

export const unitLabels: Record<MeasurementUnit, string> = {
  GRAM: 'g',
  MILLILITER: 'ml',
  PORTION: 'porção',
}

export function formatReference({ amount, unit }: Food['referenceAmount']): string {
  const separator = unit === 'PORTION' ? ' ' : ''
  const label = unit === 'PORTION' && amount !== 1 ? 'porções' : unitLabels[unit]
  return `${oneDecimal.format(amount)}${separator}${label}`
}

export function formatMacros(food: Food): string {
  const { kcal, protein, carbohydrate, fat } = food.nutrients
  return [
    `${oneDecimal.format(kcal)} kcal`,
    `P ${oneDecimal.format(protein)} g`,
    `C ${oneDecimal.format(carbohydrate)} g`,
    `G ${oneDecimal.format(fat)} g`,
  ].join(' · ') + ` em ${formatReference(food.referenceAmount)}`
}
