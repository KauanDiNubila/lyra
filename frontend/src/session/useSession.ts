import { useEffect, useState } from 'react'
import { onUnauthorized } from '../http.ts'
import { useAsyncResource, type Resource } from '../useAsyncResource.ts'
import { deleteAccount as requestAccountDeletion, fetchMe, logout, type User } from './sessionApi.ts'

export type Session =
  | { status: 'loading' }
  | { status: 'signed-in'; user: User }
  | { status: 'signed-out' }
  | { status: 'error'; message: string }

export function useSession(): {
  session: Session
  retry: () => void
  signOut: () => Promise<void>
  deleteAccount: () => Promise<void>
} {
  const { resource, reload } = useAsyncResource('me', fetchMe)
  const [isEnded, setIsEnded] = useState(false)

  useEffect(() => onUnauthorized(() => setIsEnded(true)), [])

  async function signOut() {
    await logout()
    setIsEnded(true)
  }

  async function deleteAccount() {
    await requestAccountDeletion()
    setIsEnded(true)
  }

  function retry() {
    setIsEnded(false)
    reload()
  }

  return { session: toSession(resource, isEnded), retry, signOut, deleteAccount }
}

function toSession(resource: Resource<User | null>, isEnded: boolean): Session {
  if (isEnded) return { status: 'signed-out' }
  if (resource.status === 'error') return { status: 'error', message: resource.message }
  if (resource.status === 'success') {
    return resource.data ? { status: 'signed-in', user: resource.data } : { status: 'signed-out' }
  }
  return { status: 'loading' }
}
