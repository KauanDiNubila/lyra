import type { Food, MeasurementUnit } from './foodApi.ts'

const oneDecimal = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 })

export const unitLabels: Record<MeasurementUnit, string> = {
  GRAM: 'g',
  MILLILITER: 'ml',
  PORTION: 'porção',
}

export function quantityLabel(unit: MeasurementUnit): string {
  return `Quantidade (${unitLabels[unit]})`
}

export function formatReference({ amount, unit }: Food['referenceAmount']): string {
  const separator = unit === 'PORTION' ? ' ' : ''
  const label = unit === 'PORTION' && amount > 1 ? 'porções' : unitLabels[unit]
  return `${oneDecimal.format(amount)}${separator}${label}`
}

export type MacroValues = {
  kcal: number
  protein: number
  carbohydrate: number
  fat: number
}

export function formatMacroValues({ kcal, protein, carbohydrate, fat }: MacroValues): string {
  return [
    `${oneDecimal.format(kcal)} kcal`,
    `P ${oneDecimal.format(protein)} g`,
    `C ${oneDecimal.format(carbohydrate)} g`,
    `G ${oneDecimal.format(fat)} g`,
  ].join(' · ')
}

export function formatMacros(food: Food): string {
  return `${formatMacroValues(food.nutrients)} em ${formatReference(food.referenceAmount)}`
}

const signedOneDecimal = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1, signDisplay: 'exceptZero' })

export function formatKcalDifference(kcal: number): string {
  return `${signedOneDecimal.format(kcal)} kcal`
}

export function formatMacroDifference({ kcal, protein, carbohydrate, fat }: MacroValues): string {
  return [
    formatKcalDifference(kcal),
    `P ${signedOneDecimal.format(protein)} g`,
    `C ${signedOneDecimal.format(carbohydrate)} g`,
    `G ${signedOneDecimal.format(fat)} g`,
  ].join(' · ')
}

export function formatKcal(kcal: number): string {
  return `${oneDecimal.format(kcal)} kcal`
}
