import { useState } from 'react'
import { Banner } from '@astryxdesign/core/Banner'
import { BottomSheet } from '@astryxdesign/core/BottomSheet'
import { Button } from '@astryxdesign/core/Button'
import { EmptyState } from '@astryxdesign/core/EmptyState'
import { Heading } from '@astryxdesign/core/Heading'
import { HStack } from '@astryxdesign/core/HStack'
import { List, ListItem } from '@astryxdesign/core/List'
import { Spinner } from '@astryxdesign/core/Spinner'
import { Text } from '@astryxdesign/core/Text'
import { TextInput } from '@astryxdesign/core/TextInput'
import { VStack } from '@astryxdesign/core/VStack'
import { CustomFoodForm } from './CustomFoodForm.tsx'
import type { Food } from './foodApi.ts'
import { formatMacros } from './formatNutrients.ts'
import { MIN_QUERY_LENGTH, useFoodSearch } from './useFoodSearch.ts'

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
      <TextInput
        label="Buscar alimento"
        value={query}
        onChange={setQuery}
        placeholder="Ex.: arroz cozido"
        hasClear
        hasAutoFocus
      />
      {search.status === 'idle' && (
        <Text type="supporting">Digite pelo menos {MIN_QUERY_LENGTH} letras para buscar.</Text>
      )}
      {search.status === 'loading' && <Spinner label="Buscando alimentos" />}
      {search.status === 'error' && (
        <>
          <Banner status="error" title="Não foi possível buscar" description={search.message} />
          <Button label="Tentar de novo" variant="secondary" onClick={reload} />
        </>
      )}
      {search.status === 'success' && search.foods.length === 0 && (
        <EmptyState
          title="Nenhum alimento encontrado"
          description="Cadastre o alimento com os valores da embalagem ou da tabela do restaurante."
          actions={createButton}
        />
      )}
      {search.status === 'success' && search.foods.length > 0 && (
        <List header="Resultados" hasDividers>
          {search.foods.map((food) => (
            <ListItem
              key={food.id}
              label={food.name}
              description={<Text type="supporting" maxLines={2}>{formatMacros(food)}</Text>}
              endContent={food.source === 'CUSTOM' ? <Text type="supporting">Meu</Text> : undefined}
            />
          ))}
        </List>
      )}
      {search.status === 'success' && search.foods.some((food) => food.source === 'TACO') && (
        <Text type="supporting">
          Fonte dos alimentos da TACO: NEPA/Unicamp, Tabela Brasileira de Composição de Alimentos, 4. ed., 2011.
        </Text>
      )}
      <BottomSheet label="Novo alimento" purpose="form" isOpen={isCreating} onOpenChange={setIsCreating}>
        {isCreating && <CustomFoodForm initialName={query.trim()} onCreated={showCreated} />}
      </BottomSheet>
    </VStack>
  )
}
