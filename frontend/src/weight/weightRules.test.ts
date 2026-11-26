import { describe, expect, it } from 'vitest'
import { toKilograms } from './weightRules.ts'

describe('toKilograms', () => {
  it.each([
    ['82.4', 82.4],
    [' 82 ', 82],
    ['', null],
    ['   ', null],
    ['abc', null],
  ])('reads %j as %j', (text, kilograms) => {
    expect(toKilograms(text)).toBe(kilograms)
  })
})
