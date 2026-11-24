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
import { VStack } from '@astryxdesign/core/VStack'
import { useAsyncResource } from '../useAsyncResource.ts'
import { useSubmit } from '../useSubmit.ts'
import { PlanForm } from './PlanForm.tsx'
import { activatePlan, createPlan, fetchPlan, fetchPlans, replacePlan } from './planApi.ts'
import { emptyForm, formFromPlan } from './planDraft.ts'

type Sheet = { mode: 'new' } | { mode: 'edit'; id: string }

export function PlansPage() {
  const { resource: plans, reload } = useAsyncResource('plans', fetchPlans)
  const [sheet, setSheet] = useState<Sheet | null>(null)
  const activation = useSubmit()

  function showSaved() {
    setSheet(null)
    reload()
  }

  function activate(id: string) {
    void activation.run(async () => {
      await activatePlan(id)
      reload()
    })
  }

  return (
    <VStack gap={4} padding={4}>
      <HStack justify="between" align="center">
        <Heading level={1}>Plano</Heading>
        <Button label="Novo plano" variant="primary" onClick={() => setSheet({ mode: 'new' })} />
      </HStack>
      {activation.error && (
        <Banner status="error" title="Não foi possível ativar o plano" description={activation.error} />
      )}
      {plans.status === 'loading' && <Spinner label="Carregando os planos" />}
      {plans.status === 'error' && (
        <>
          <Banner status="error" title="Não foi possível carregar os planos" description={plans.message} />
          <Button label="Tentar de novo" variant="secondary" onClick={reload} />
        </>
      )}
      {plans.status === 'success' && plans.data.length === 0 && (
        <EmptyState
          title="Nenhum plano cadastrado"
          description="Cadastre o plano alimentar uma vez e o diário passa a abrir preenchido por ele."
          actions={<Button label="Criar plano" variant="secondary" onClick={() => setSheet({ mode: 'new' })} />}
        />
      )}
      {plans.status === 'success' && plans.data.length > 0 && (
        <List hasDividers>
          {plans.data.map((plan) => (
            <ListItem
              key={plan.id}
              label={plan.name}
              onClick={() => setSheet({ mode: 'edit', id: plan.id })}
              endContent={
                plan.active ? (
                  <Text type="supporting">Ativo</Text>
                ) : (
                  <Button
                    label="Ativar"
                    aria-label={`Ativar ${plan.name}`}
                    size="sm"
                    variant="secondary"
                    isDisabled={activation.isRunning}
                    onClick={() => activate(plan.id)}
                  />
                )
              }
            />
          ))}
        </List>
      )}
      <BottomSheet
        label={sheet?.mode === 'edit' ? 'Editar plano' : 'Novo plano'}
        purpose="form"
        isOpen={sheet !== null}
        onOpenChange={(isOpen) => !isOpen && setSheet(null)}
      >
        {sheet?.mode === 'new' && <PlanForm initial={emptyForm()} save={createPlan} onSaved={showSaved} />}
        {sheet?.mode === 'edit' && <EditPlan id={sheet.id} onSaved={showSaved} />}
      </BottomSheet>
    </VStack>
  )
}

type EditProps = {
  id: string
  onSaved: () => void
}

function EditPlan({ id, onSaved }: EditProps) {
  const { resource: plan, reload } = useAsyncResource(id, (signal) => fetchPlan(id, signal))

  if (plan.status === 'success') {
    return <PlanForm initial={formFromPlan(plan.data)} save={(request) => replacePlan(id, request)} onSaved={onSaved} />
  }
  if (plan.status === 'error') {
    return (
      <VStack gap={3}>
        <Banner status="error" title="Não foi possível carregar o plano" description={plan.message} />
        <Button label="Tentar de novo" variant="secondary" onClick={reload} />
      </VStack>
    )
  }
  return <Spinner label="Carregando o plano" />
}
