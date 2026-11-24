export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.status = status
  }
}

const SAFE_METHODS = ['GET', 'HEAD', 'OPTIONS']
const unauthorizedListeners = new Set<() => void>()

export function onUnauthorized(listener: () => void): () => void {
  unauthorizedListeners.add(listener)
  return () => {
    unauthorizedListeners.delete(listener)
  }
}

function csrfToken(): string | null {
  const cookie = document.cookie.split('; ').find((entry) => entry.startsWith('XSRF-TOKEN='))
  return cookie ? decodeURIComponent(cookie.slice('XSRF-TOKEN='.length)) : null
}

function withCsrfToken(init: RequestInit): RequestInit {
  const method = (init.method ?? 'GET').toUpperCase()
  const token = SAFE_METHODS.includes(method) ? null : csrfToken()
  return token ? { ...init, headers: { ...init.headers, 'X-XSRF-TOKEN': token } } : init
}

export async function send(url: string, init: RequestInit = {}): Promise<Response> {
  let response: Response
  try {
    response = await fetch(url, withCsrfToken(init))
  } catch (cause) {
    if (init.signal?.aborted) throw cause
    throw new Error('Sem conexão com o servidor. Verifique a internet e tente de novo.')
  }
  if (response.status === 401) unauthorizedListeners.forEach((listener) => listener())
  return response
}

export async function ensureOk(response: Response): Promise<void> {
  if (response.ok) return
  const problem = await response.json().catch(() => null)
  throw new ApiError(
    problem?.detail ?? `O servidor não conseguiu responder (erro ${response.status}). Tente de novo.`,
    response.status,
  )
}

export async function readJson<T>(response: Response): Promise<T> {
  await ensureOk(response)
  return response.json() as Promise<T>
}

export function errorMessage(cause: unknown): string {
  return cause instanceof Error ? cause.message : String(cause)
}
