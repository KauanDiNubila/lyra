import type { Weight } from './weightApi.ts'

const kilograms = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 })
const signedKilograms = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2, signDisplay: 'exceptZero' })
const longDate = new Intl.DateTimeFormat('pt-BR', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })

export const MIN_KILOGRAMS = 20
export const MAX_KILOGRAMS = 500

export function formatKilograms(value: number): string {
  return `${kilograms.format(value)} kg`
}

export function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-').map(Number)
  return longDate.format(new Date(year, month - 1, day))
}

export function toKilograms(text: string): number | null {
  if (text.trim() === '') return null
  const value = Number(text)
  return Number.isNaN(value) ? null : value
}

export function weightProblem(value: number | null): string | null {
  if (value === null) return null
  if (value < MIN_KILOGRAMS || value > MAX_KILOGRAMS) return `O peso precisa estar entre ${MIN_KILOGRAMS} e ${MAX_KILOGRAMS} kg.`
  if (!/^\d+(\.\d{1,2})?$/.test(String(value))) return 'Use no máximo duas casas decimais.'
  return null
}

export type WeightRow = Weight & { change: string | null }

export function newestFirst(weights: Weight[]): WeightRow[] {
  return weights
    .map((weight, index) => {
      const previous = weights[index - 1]
      const change = previous ? `${signedKilograms.format(weight.kilograms - previous.kilograms)} kg` : null
      return { ...weight, change }
    })
    .reverse()
}
