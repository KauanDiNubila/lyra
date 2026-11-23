import { useEffect, useEffectEvent, useState } from 'react'
import { errorMessage } from './http.ts'

export type Resource<T> =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; data: T }
  | { status: 'error'; message: string }

type Answer<T> = { key: string; attemptKey: string; resource: Resource<T> }

function currentResource<T>(key: string | null, attemptKey: string, answer: Answer<T> | null): Resource<T> {
  if (key === null) return { status: 'idle' }
  if (answer?.attemptKey === attemptKey) return answer.resource
  if (answer?.key === key && answer.resource.status === 'success') return answer.resource
  return { status: 'loading' }
}

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
          if (!controller.signal.aborted) setAnswer({ key, attemptKey, resource: { status: 'success', data } })
        })
        .catch((error: unknown) => {
          if (controller.signal.aborted) return
          setAnswer({ key, attemptKey, resource: { status: 'error', message: errorMessage(error) } })
        })
    }, delayMs)
    return () => {
      clearTimeout(timer)
      controller.abort()
    }
  }, [key, attemptKey, delayMs])

  return { resource: currentResource(key, attemptKey, answer), reload: () => setAttempt((count) => count + 1) }
}
