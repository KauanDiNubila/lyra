import { useRef, useState } from 'react'
import { errorMessage } from './http.ts'

export function useSubmit(): {
  run: (action: () => Promise<unknown>) => Promise<boolean>
  isRunning: boolean
  error: string | null
} {
  const running = useRef(false)
  const [isRunning, setIsRunning] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function run(action: () => Promise<unknown>): Promise<boolean> {
    if (running.current) return false
    running.current = true
    setIsRunning(true)
    setError(null)
    try {
      await action()
      return true
    } catch (cause) {
      setError(errorMessage(cause))
      return false
    } finally {
      running.current = false
      setIsRunning(false)
    }
  }

  return { run, isRunning, error }
}
