import { useAsyncResource, type Resource } from '../useAsyncResource.ts'
import { fetchDay, type DiaryDay } from './diaryApi.ts'

export function useDiaryDay(date: string): { day: Resource<DiaryDay>; reload: () => void } {
  const { resource, reload } = useAsyncResource(date, (signal) => fetchDay(date, signal))
  return { day: resource, reload }
}
