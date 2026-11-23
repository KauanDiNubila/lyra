import { describe, expect, it } from 'vitest'
import { shiftDay, todayIso } from './dates.ts'

describe('todayIso', () => {
  it('uses the local date, not the UTC date', () => {
    expect(todayIso(new Date('2026-11-20T01:30:00Z'))).toBe('2026-11-19')
  })

  it('pads the month and the day', () => {
    expect(todayIso(new Date(2026, 0, 5, 12))).toBe('2026-01-05')
  })
})

describe('shiftDay', () => {
  it.each([
    ['2026-11-19', 1, '2026-11-20'],
    ['2026-11-19', -1, '2026-11-18'],
    ['2026-11-30', 1, '2026-12-01'],
    ['2026-01-01', -1, '2025-12-31'],
    ['2028-02-28', 1, '2028-02-29'],
  ])('moves %s by %i day to %s', (date, days, expected) => {
    expect(shiftDay(date, days)).toBe(expected)
  })
})
