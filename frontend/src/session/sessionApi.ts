import { ApiError, ensureOk, readJson, send } from '../http.ts'

export type User = {
  id: string
  email: string
  name: string
}

export async function fetchMe(signal: AbortSignal): Promise<User | null> {
  const response = await send('/api/me', { signal })
  try {
    return await readJson<User>(response)
  } catch (cause) {
    if (cause instanceof ApiError && cause.status === 401) return null
    throw cause
  }
}

export async function logout(): Promise<void> {
  const response = await send('/api/logout', { method: 'POST' })
  await ensureOk(response)
}
