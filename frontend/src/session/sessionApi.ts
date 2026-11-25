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

const REVOKE_DELAY_MS = 1000

export async function downloadExport(): Promise<void> {
  const response = await send('/api/me/export')
  await ensureOk(response)
  const url = URL.createObjectURL(await response.blob())
  const link = document.createElement('a')
  link.href = url
  link.download = 'lyra-export.json'
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), REVOKE_DELAY_MS)
}

export async function deleteAccount(): Promise<void> {
  const response = await send('/api/me', { method: 'DELETE' })
  await ensureOk(response)
}
