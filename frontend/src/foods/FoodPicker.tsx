import { useState } from 'react'
import { Button } from '@astryxdesign/core/Button'
import { VStack } from '@astryxdesign/core/VStack'
import { BarcodeForm } from './BarcodeForm.tsx'
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
  const [isScanning, setIsScanning] = useState(false)
  const { search, reload } = useFoodSearch(query)

  if (isScanning) {
    return (
      <VStack gap={3}>
        <Button label="Buscar por nome" variant="ghost" onClick={() => setIsScanning(false)} />
        <BarcodeForm onFound={onSelect} />
      </VStack>
    )
  }

  return (
    <VStack gap={3}>
      <Button label="Código de barras" variant="secondary" onClick={() => setIsScanning(true)} />
      <FoodSearchPanel
        query={query}
        onQueryChange={onQueryChange}
        search={search}
        onRetry={reload}
        onSelect={onSelect}
        emptyDescription={emptyDescription}
      />
    </VStack>
  )
}
