import { useAsyncResource, type Resource } from '../useAsyncResource.ts'
import { searchFoods, type Food } from './foodApi.ts'

export const SEARCH_DELAY_MS = 300
export const MIN_QUERY_LENGTH = 2

export function useFoodSearch(query: string): { search: Resource<Food[]>; reload: () => void } {
  const trimmed = query.trim()
  const key = trimmed.length >= MIN_QUERY_LENGTH ? trimmed : null
  const { resource, reload } = useAsyncResource(key, (signal) => searchFoods(trimmed, signal), SEARCH_DELAY_MS)
  return { search: resource, reload }
}
