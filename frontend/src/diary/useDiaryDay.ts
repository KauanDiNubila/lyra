import { useState } from 'react'
import { errorMessage } from '../http.ts'
import { useAsyncResource, type Resource } from '../useAsyncResource.ts'
import { todayIso } from './dates.ts'
import { fetchDay, fillDay, type DiaryDay } from './diaryApi.ts'

export type LoadedDay = {
  day: DiaryDay
  hasNoPlan: boolean
  fillFailure: string | null
}

async function loadDay(date: string, isFillAsked: boolean, signal: AbortSignal): Promise<LoadedDay> {
  const day = await fetchDay(date, signal)
  const shouldFill = day.meals.length === 0 && (isFillAsked || date === todayIso())
  if (!shouldFill) return { day, hasNoPlan: false, fillFailure: null }
  try {
    const filled = await fillDay(date)
    return { day: filled ?? day, hasNoPlan: filled === null, fillFailure: null }
  } catch (cause) {
    if (signal.aborted) throw cause
    return { day, hasNoPlan: false, fillFailure: errorMessage(cause) }
  }
}

export function useDiaryDay(date: string): { day: Resource<LoadedDay>; reload: () => void; fill: () => void } {
  const [askedDate, setAskedDate] = useState<string | null>(null)
  const isFillAsked = askedDate === date
  const { resource, reload } = useAsyncResource(`${date}:${isFillAsked}`, (signal) => loadDay(date, isFillAsked, signal))

  function fill() {
    setAskedDate(date)
    reload()
  }

  return { day: resource, reload, fill }
}
