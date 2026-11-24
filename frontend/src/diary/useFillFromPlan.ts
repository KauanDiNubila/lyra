import { useEffect, useEffectEvent, useRef, useState } from 'react'
import { ApiError, errorMessage } from '../http.ts'
import type { Resource } from '../useAsyncResource.ts'
import { todayIso } from './dates.ts'
import { fillDay, type DiaryDay } from './diaryApi.ts'

type Outcome = { kind: 'no-plan' } | { kind: 'failed'; message: string }

export function useFillFromPlan(date: string, day: Resource<DiaryDay>, reload: () => void) {
  const [attempted, setAttempted] = useState<string[]>([])
  const [filling, setFilling] = useState<string | null>(null)
  const [outcomes, setOutcomes] = useState<Record<string, Outcome>>({})
  const inFlight = useRef(new Set<string>())

  const isEmpty = day.status === 'success' && day.data.meals.length === 0
  const shouldAutoFill = isEmpty && date >= todayIso() && !attempted.includes(date)

  function setOutcome(target: string, outcome: Outcome | null) {
    setOutcomes((current) => {
      const { [target]: _previous, ...others } = current
      return outcome ? { ...others, [target]: outcome } : others
    })
  }

  async function fillDate(target: string) {
    if (inFlight.current.has(target)) return
    inFlight.current.add(target)
    setFilling(target)
    setOutcome(target, null)
    try {
      await fillDay(target)
      reload()
    } catch (cause) {
      const isNoPlan = cause instanceof ApiError && cause.status === 404
      setOutcome(target, isNoPlan ? { kind: 'no-plan' } : { kind: 'failed', message: errorMessage(cause) })
    } finally {
      inFlight.current.delete(target)
      setFilling((current) => (current === target ? null : current))
    }
  }

  const autoFill = useEffectEvent((target: string) => {
    setAttempted((current) => [...current, target])
    void fillDate(target)
  })

  useEffect(() => {
    if (shouldAutoFill) autoFill(date)
  }, [shouldAutoFill, date])

  const outcome = outcomes[date]
  return {
    isEmpty,
    isFilling: filling === date || shouldAutoFill,
    hasNoPlan: outcome?.kind === 'no-plan',
    failure: outcome?.kind === 'failed' ? outcome.message : null,
    fill: () => void fillDate(date),
  }
}
