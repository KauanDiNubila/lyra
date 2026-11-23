import { useEffect, useState } from 'react'
import { searchFoods, type Food } from './foodApi.ts'

export const SEARCH_DELAY_MS = 300
export const MIN_QUERY_LENGTH = 2

export type FoodSearchState =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; foods: Food[] }
  | { status: 'error'; message: string }

type Answer = { key: string; state: FoodSearchState }

export function useFoodSearch(query: string, refreshKey = 0): FoodSearchState {
  const trimmed = query.trim()
  const isSearchable = trimmed.length >= MIN_QUERY_LENGTH
  const key = `${refreshKey}:${trimmed}`
  const [answer, setAnswer] = useState<Answer | null>(null)

  useEffect(() => {
    if (!isSearchable) return
    const controller = new AbortController()
    const timer = setTimeout(() => {
      searchFoods(trimmed, controller.signal)
        .then((foods) => {
          if (!controller.signal.aborted) setAnswer({ key, state: { status: 'success', foods } })
        })
        .catch((error: unknown) => {
          if (controller.signal.aborted) return
          const message = error instanceof Error ? error.message : String(error)
          setAnswer({ key, state: { status: 'error', message } })
        })
    }, SEARCH_DELAY_MS)
    return () => {
      clearTimeout(timer)
      controller.abort()
    }
  }, [isSearchable, trimmed, key])

  if (!isSearchable) return { status: 'idle' }
  return answer?.key === key ? answer.state : { status: 'loading' }
}
