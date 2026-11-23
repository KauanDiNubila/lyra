import { describe, expect, it } from 'vitest'
import { formatReference } from './formatNutrients.ts'

describe('formatReference', () => {
  it.each([
    [{ amount: 100, unit: 'GRAM' as const }, '100g'],
    [{ amount: 250, unit: 'MILLILITER' as const }, '250ml'],
    [{ amount: 0.5, unit: 'PORTION' as const }, '0,5 porção'],
    [{ amount: 1, unit: 'PORTION' as const }, '1 porção'],
    [{ amount: 1.5, unit: 'PORTION' as const }, '1,5 porções'],
    [{ amount: 2, unit: 'PORTION' as const }, '2 porções'],
  ])('writes %j as %s', (reference, expected) => {
    expect(formatReference(reference)).toBe(expected)
  })
})
