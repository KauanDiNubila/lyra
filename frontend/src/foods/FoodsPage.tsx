import { useState } from 'react'
import { BottomSheet } from '@astryxdesign/core/BottomSheet'
import { Button } from '@astryxdesign/core/Button'
import { Heading } from '@astryxdesign/core/Heading'
import { HStack } from '@astryxdesign/core/HStack'
import { VStack } from '@astryxdesign/core/VStack'
import { CustomFoodForm } from './CustomFoodForm.tsx'
import type { Food } from './foodApi.ts'
import { FoodSearchPanel } from './FoodSearchPanel.tsx'
import { useFoodSearch } from './useFoodSearch.ts'

export function FoodsPage() {
  const [query, setQuery] = useState('')
  const [isCreating, setIsCreating] = useState(false)
  const { search, reload } = useFoodSearch(query)

  function showCreated(food: Food) {
    setIsCreating(false)
    setQuery(food.name)
    reload()
  }

  const createButton = <Button label="Novo alimento" variant="secondary" onClick={() => setIsCreating(true)} />

  return (
    <VStack gap={4} padding={4}>
      <HStack justify="between" align="center">
        <Heading level={1}>Alimentos</Heading>
        {createButton}
      </HStack>
      <FoodSearchPanel
        query={query}
        onQueryChange={setQuery}
        search={search}
        onRetry={reload}
        emptyActions={createButton}
      />
      <BottomSheet label="Novo alimento" purpose="form" isOpen={isCreating} onOpenChange={setIsCreating}>
        {isCreating && <CustomFoodForm initialName={query.trim()} onCreated={showCreated} />}
      </BottomSheet>
    </VStack>
  )
}
