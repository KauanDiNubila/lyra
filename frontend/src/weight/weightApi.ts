import { ensureOk, readJson, send } from '../http.ts'

export type Weight = {
  date: string
  kilograms: number
}

export async function listWeights(signal: AbortSignal): Promise<Weight[]> {
  const response = await send('/api/weights', { signal })
  return readJson<Weight[]>(response)
}

export async function recordWeight(date: string, kilograms: number): Promise<Weight> {
  const response = await send(`/api/weights/${date}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ kilograms }),
  })
  return readJson<Weight>(response)
}

export async function deleteWeight(date: string): Promise<void> {
  const response = await send(`/api/weights/${date}`, { method: 'DELETE' })
  await ensureOk(response)
}
