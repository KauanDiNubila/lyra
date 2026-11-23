export async function send(url: string, init: RequestInit = {}): Promise<Response> {
  try {
    return await fetch(url, init)
  } catch (cause) {
    if (init.signal?.aborted) throw cause
    throw new Error('Sem conexão com o servidor. Verifique a internet e tente de novo.')
  }
}

export async function ensureOk(response: Response): Promise<void> {
  if (response.ok) return
  const problem = await response.json().catch(() => null)
  throw new Error(problem?.detail ?? `O servidor não conseguiu responder (erro ${response.status}). Tente de novo.`)
}

export async function readJson<T>(response: Response): Promise<T> {
  await ensureOk(response)
  return response.json() as Promise<T>
}

export function errorMessage(cause: unknown): string {
  return cause instanceof Error ? cause.message : String(cause)
}
