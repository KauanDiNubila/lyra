import { useEffect, useEffectEvent, useState } from 'react'

export type Resource<T> =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; data: T }
  | { status: 'error'; message: string }

type Answer<T> = { key: string; resource: Resource<T> }

export function useAsyncResource<T>(
  key: string | null,
  load: (signal: AbortSignal) => Promise<T>,
  delayMs = 0,
): { resource: Resource<T>; reload: () => void } {
  const [attempt, setAttempt] = useState(0)
  const attemptKey = `${attempt}:${key}`
  const [answer, setAnswer] = useState<Answer<T> | null>(null)
  const loadLatest = useEffectEvent(load)

  useEffect(() => {
    if (key === null) return
    const controller = new AbortController()
    const timer = setTimeout(() => {
      loadLatest(controller.signal)
        .then((data) => {
          if (!controller.signal.aborted) setAnswer({ key: attemptKey, resource: { status: 'success', data } })
        })
        .catch((error: unknown) => {
          if (controller.signal.aborted) return
          const message = error instanceof Error ? error.message : String(error)
          setAnswer({ key: attemptKey, resource: { status: 'error', message } })
        })
    }, delayMs)
    return () => {
      clearTimeout(timer)
      controller.abort()
    }
  }, [key, attemptKey, delayMs])

  const resource: Resource<T> = key === null
    ? { status: 'idle' }
    : answer?.key === attemptKey ? answer.resource : { status: 'loading' }
  return { resource, reload: () => setAttempt((count) => count + 1) }
}
