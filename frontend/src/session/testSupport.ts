import { vi } from 'vitest'

export const fetchMock = vi.fn<typeof fetch>()

export const USER = { id: 'user-1', email: 'ana@example.com', name: 'Ana Souza' }

export function calls(path: string, method?: string) {
  return fetchMock.mock.calls.filter(([url, init]) => String(url) === path && (!method || init?.method === method))
}
