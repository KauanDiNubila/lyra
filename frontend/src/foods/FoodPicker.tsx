import { FoodSearchPanel } from './FoodSearchPanel.tsx'
import type { Food } from './foodApi.ts'
import { useFoodSearch } from './useFoodSearch.ts'

type Props = {
  query: string
  onQueryChange: (query: string) => void
  onSelect: (food: Food) => void
  emptyDescription: string
}

export function FoodPicker({ query, onQueryChange, onSelect, emptyDescription }: Props) {
  const { search, reload } = useFoodSearch(query)

  return (
    <FoodSearchPanel
      query={query}
      onQueryChange={onQueryChange}
      search={search}
      onRetry={reload}
      onSelect={onSelect}
      emptyDescription={emptyDescription}
    />
  )
}
