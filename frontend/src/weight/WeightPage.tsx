import { useState, type FormEvent } from 'react'
import { Banner } from '@astryxdesign/core/Banner'
import { Button } from '@astryxdesign/core/Button'
import { DateInput } from '@astryxdesign/core/DateInput'
import { EmptyState } from '@astryxdesign/core/EmptyState'
import { Heading } from '@astryxdesign/core/Heading'
import { Icon } from '@astryxdesign/core/Icon'
import { IconButton } from '@astryxdesign/core/IconButton'
import { List, ListItem } from '@astryxdesign/core/List'
import { NumberInput } from '@astryxdesign/core/NumberInput'
import { Spinner } from '@astryxdesign/core/Spinner'
import { VStack } from '@astryxdesign/core/VStack'
import { todayIso } from '../diary/dates.ts'
import { useAsyncResource } from '../useAsyncResource.ts'
import { useSubmit } from '../useSubmit.ts'
import { deleteWeight, listWeights, recordWeight } from './weightApi.ts'
import { formatDate, formatKilograms, newestFirst, weightProblem } from './weightRules.ts'

export function WeightPage() {
  const [date, setDate] = useState(() => todayIso())
  const [kilograms, setKilograms] = useState<number | null>(null)
  const { resource: weights, reload } = useAsyncResource('weights', listWeights)
  const save = useSubmit()
  const remove = useSubmit()
  const problem = weightProblem(kilograms)
  const existing = weights.status === 'success' ? weights.data.find((weight) => weight.date === date) : undefined

  function submit(event: FormEvent) {
    event.preventDefault()
    if (kilograms === null || problem) return
    void save.run(async () => {
      await recordWeight(date, kilograms)
      setKilograms(null)
      reload()
    })
  }

  function removeWeight(weightDate: string) {
    void remove.run(async () => {
      await deleteWeight(weightDate)
      reload()
    })
  }

  return (
    <VStack gap={4} padding={4}>
      <Heading level={1}>Peso</Heading>
      <form onSubmit={submit} aria-label="Registrar peso">
        <VStack gap={3}>
          {save.error && <Banner status="error" title="Não foi possível salvar o peso" description={save.error} />}
          <DateInput label="Data" value={date} onChange={(value) => value && setDate(value)} />
          <NumberInput
            label="Peso"
            units="kg"
            step={0.1}
            description={
              existing && `Já existe ${formatKilograms(existing.kilograms)} neste dia. Salvar troca o valor.`
            }
            value={kilograms}
            onChange={setKilograms}
            status={problem ? { type: 'error', message: problem } : undefined}
            isRequired
          />
          <Button
            type="submit"
            label="Salvar peso"
            variant="primary"
            isDisabled={kilograms === null || problem !== null}
            isLoading={save.isRunning}
          />
        </VStack>
      </form>
      {remove.error && <Banner status="error" title="Não foi possível apagar o peso" description={remove.error} />}
      {weights.status === 'loading' && <Spinner label="Carregando os pesos" />}
      {weights.status === 'error' && (
        <>
          <Banner status="error" title="Não foi possível carregar os pesos" description={weights.message} />
          <Button label="Tentar de novo" variant="secondary" onClick={reload} />
        </>
      )}
      {weights.status === 'success' && weights.data.length === 0 && (
        <EmptyState title="Nenhum peso registrado" description="Registre o peso de hoje para começar a acompanhar." />
      )}
      {weights.status === 'success' && weights.data.length > 0 && (
        <List header="Histórico" hasDividers>
          {newestFirst(weights.data).map((weight) => (
            <ListItem
              key={weight.date}
              label={formatKilograms(weight.kilograms)}
              description={weight.change ? `${formatDate(weight.date)} · ${weight.change}` : formatDate(weight.date)}
              endContent={
                <IconButton
                  label={`Apagar o peso de ${formatDate(weight.date)}`}
                  icon={<Icon icon="close" />}
                  variant="ghost"
                  isDisabled={remove.isRunning}
                  onClick={() => removeWeight(weight.date)}
                />
              }
            />
          ))}
        </List>
      )}
    </VStack>
  )
}
