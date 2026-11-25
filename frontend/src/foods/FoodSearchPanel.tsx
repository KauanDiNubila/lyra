import type { ReactNode } from 'react'
import { Banner } from '@astryxdesign/core/Banner'
import { Button } from '@astryxdesign/core/Button'
import { EmptyState } from '@astryxdesign/core/EmptyState'
import { List, ListItem } from '@astryxdesign/core/List'
import { Spinner } from '@astryxdesign/core/Spinner'
import { Text } from '@astryxdesign/core/Text'
import { TextInput } from '@astryxdesign/core/TextInput'
import type { Resource } from '../useAsyncResource.ts'
import { OPEN_FOOD_FACTS_CREDIT, TACO_CREDIT } from './sourceCredits.ts'
import type { Food } from './foodApi.ts'
import { formatMacros } from './formatNutrients.ts'
import { MIN_QUERY_LENGTH } from './useFoodSearch.ts'

type Props = {
  query: string
  onQueryChange: (query: string) => void
  search: Resource<Food[]>
  onRetry: () => void
  onSelect?: (food: Food) => void
  emptyActions?: ReactNode
  emptyDescription?: string
}

const sourceLabels: Record<Food['source'], string | null> = {
  TACO: null,
  CUSTOM: 'Meu',
  OPEN_FOOD_FACTS: 'Embalado',
}

const DEFAULT_EMPTY_DESCRIPTION = 'Cadastre o alimento com os valores da embalagem ou da tabela do restaurante.'

export function FoodSearchPanel({
  query,
  onQueryChange,
  search,
  onRetry,
  onSelect,
  emptyActions,
  emptyDescription = DEFAULT_EMPTY_DESCRIPTION,
}: Props) {
  return (
    <>
      <TextInput
        label="Buscar alimento"
        value={query}
        onChange={onQueryChange}
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
          <Button label="Tentar de novo" variant="secondary" onClick={onRetry} />
        </>
      )}
      {search.status === 'success' && search.data.length === 0 && (
        <EmptyState
          title="Nenhum alimento encontrado"
          description={emptyDescription}
          actions={emptyActions}
        />
      )}
      {search.status === 'success' && search.data.length > 0 && (
        <List header="Resultados" hasDividers>
          {search.data.map((food) => (
            <ListItem
              key={food.id}
              label={food.name}
              description={<Text type="supporting" maxLines={2}>{formatMacros(food)}</Text>}
              endContent={sourceLabels[food.source] && <Text type="supporting">{sourceLabels[food.source]}</Text>}
              onClick={onSelect && (() => onSelect(food))}
            />
          ))}
        </List>
      )}
      {search.status === 'success' && search.data.some((food) => food.source === 'TACO') && (
        <Text type="supporting">{TACO_CREDIT}</Text>
      )}
      {search.status === 'success' && search.data.some((food) => food.source === 'OPEN_FOOD_FACTS') && (
        <Text type="supporting">{OPEN_FOOD_FACTS_CREDIT}</Text>
      )}
    </>
  )
}
