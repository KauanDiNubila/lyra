import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { onUnauthorized, send } from './http.ts'

const fetchMock = vi.fn<typeof fetch>()

function clearCookie() {
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'
}

function sentHeaders(): Record<string, string> {
  return (fetchMock.mock.calls[0][1]?.headers ?? {}) as Record<string, string>
}

beforeEach(() => {
  fetchMock.mockReset()
  fetchMock.mockResolvedValue(new Response('{}'))
  vi.stubGlobal('fetch', fetchMock)
  clearCookie()
})

afterEach(() => {
  clearCookie()
  vi.unstubAllGlobals()
})

describe('the CSRF token', () => {
  it('goes in the header of a write, read from the cookie', async () => {
    document.cookie = 'XSRF-TOKEN=token-123; path=/'

    await send('/api/diary/entries/1', { method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: '{}' })

    expect(sentHeaders()).toEqual({ 'Content-Type': 'application/json', 'X-XSRF-TOKEN': 'token-123' })
  })

  it('is decoded when the cookie holds a percent-encoded value', async () => {
    document.cookie = 'XSRF-TOKEN=a%3Db%2Fc; path=/'

    await send('/api/logout', { method: 'POST' })

    expect(sentHeaders()['X-XSRF-TOKEN']).toBe('a=b/c')
  })

  it.each(['POST', 'PUT', 'PATCH', 'DELETE', 'post'])('is sent with %s', async (method) => {
    document.cookie = 'XSRF-TOKEN=token-123; path=/'

    await send('/api/anything', { method })

    expect(sentHeaders()['X-XSRF-TOKEN']).toBe('token-123')
  })

  it.each([undefined, 'GET', 'HEAD', 'OPTIONS'])('is not sent with %s', async (method) => {
    document.cookie = 'XSRF-TOKEN=token-123; path=/'

    await send('/api/anything', { method })

    expect(fetchMock.mock.calls[0][1]?.headers).toBeUndefined()
  })

  it('is not invented when there is no cookie', async () => {
    await send('/api/logout', { method: 'POST' })

    expect(fetchMock.mock.calls[0][1]?.headers).toBeUndefined()
  })

  it('is not mistaken for another cookie that ends the same way', async () => {
    document.cookie = 'OTHER-XSRF-TOKEN=wrong; path=/'

    await send('/api/logout', { method: 'POST' })

    expect(fetchMock.mock.calls[0][1]?.headers).toBeUndefined()
  })
})

describe('an answer of 401', () => {
  it('tells everyone who is listening', async () => {
    fetchMock.mockResolvedValue(new Response('{}', { status: 401 }))
    const first = vi.fn()
    const second = vi.fn()
    const stopFirst = onUnauthorized(first)
    const stopSecond = onUnauthorized(second)

    await send('/api/plans')

    expect(first).toHaveBeenCalledTimes(1)
    expect(second).toHaveBeenCalledTimes(1)
    stopFirst()
    stopSecond()
  })

  it('stops telling the one that stopped listening', async () => {
    fetchMock.mockResolvedValue(new Response('{}', { status: 401 }))
    const listener = vi.fn()
    onUnauthorized(listener)()

    await send('/api/plans')

    expect(listener).not.toHaveBeenCalled()
  })

  it('does not tell anyone about other answers', async () => {
    fetchMock.mockResolvedValue(new Response('{}', { status: 404 }))
    const listener = vi.fn()
    const stop = onUnauthorized(listener)

    await send('/api/plans')

    expect(listener).not.toHaveBeenCalled()
    stop()
  })
})
