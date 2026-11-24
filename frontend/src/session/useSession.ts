import { useEffect, useState } from 'react'
import { onUnauthorized } from '../http.ts'
import { useAsyncResource } from '../useAsyncResource.ts'
import { fetchMe, logout, type User } from './sessionApi.ts'

export type Session =
  | { status: 'loading' }
  | { status: 'signed-in'; user: User }
  | { status: 'signed-out' }
  | { status: 'error'; message: string }

export function useSession(): { session: Session; retry: () => void; signOut: () => Promise<void> } {
  const { resource, reload } = useAsyncResource('me', fetchMe)
  const [isEnded, setIsEnded] = useState(false)

  useEffect(() => onUnauthorized(() => setIsEnded(true)), [])

  async function signOut() {
    await logout()
    setIsEnded(true)
  }

  function retry() {
    setIsEnded(false)
    reload()
  }

  return { session: toSession(resource, isEnded), retry, signOut }
}

function toSession(resource: ReturnType<typeof useAsyncResource<User | null>>['resource'], isEnded: boolean): Session {
  if (isEnded) return { status: 'signed-out' }
  if (resource.status === 'error') return { status: 'error', message: resource.message }
  if (resource.status === 'success') {
    return resource.data ? { status: 'signed-in', user: resource.data } : { status: 'signed-out' }
  }
  return { status: 'loading' }
}
