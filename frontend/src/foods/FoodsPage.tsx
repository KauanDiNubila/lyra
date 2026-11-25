import { useState } from 'react'
import { BottomSheet } from '@astryxdesign/core/BottomSheet'
import { Button } from '@astryxdesign/core/Button'
import { Heading } from '@astryxdesign/core/Heading'
import { HStack } from '@astryxdesign/core/HStack'
import { VStack } from '@astryxdesign/core/VStack'
import { BarcodeForm } from './BarcodeForm.tsx'
import { CustomFoodForm } from './CustomFoodForm.tsx'
import type { Food } from './foodApi.ts'
import { FoodSearchPanel } from './FoodSearchPanel.tsx'
import { useFoodSearch } from './useFoodSearch.ts'

type Sheet = 'none' | 'create' | 'barcode'

export function FoodsPage() {
  const [query, setQuery] = useState('')
  const [sheet, setSheet] = useState<Sheet>('none')
  const { search, reload } = useFoodSearch(query)

  function showFood(food: Food) {
    setSheet('none')
    setQuery(food.name)
    reload()
  }

  function sheetToggle(name: Sheet) {
    return (isOpen: boolean) => setSheet(isOpen ? name : 'none')
  }

  const createButton = <Button label="Novo alimento" variant="secondary" onClick={() => setSheet('create')} />

  return (
    <VStack gap={4} padding={4}>
      <HStack justify="between" align="center">
        <Heading level={1}>Alimentos</Heading>
        <HStack gap={2}>
          <Button label="Código de barras" variant="secondary" onClick={() => setSheet('barcode')} />
          {createButton}
        </HStack>
      </HStack>
      <FoodSearchPanel
        query={query}
        onQueryChange={setQuery}
        search={search}
        onRetry={reload}
        emptyActions={createButton}
      />
      <BottomSheet label="Novo alimento" purpose="form" isOpen={sheet === 'create'} onOpenChange={sheetToggle('create')}>
        {sheet === 'create' && <CustomFoodForm initialName={query.trim()} onCreated={showFood} />}
      </BottomSheet>
      <BottomSheet label="Código de barras" purpose="form" isOpen={sheet === 'barcode'} onOpenChange={sheetToggle('barcode')}>
        {sheet === 'barcode' && <BarcodeForm onFound={showFood} onCreateInstead={() => setSheet('create')} />}
      </BottomSheet>
    </VStack>
  )
}
