import { useState } from 'react'
import { AlertDialog } from '@astryxdesign/core/AlertDialog'
import { Banner } from '@astryxdesign/core/Banner'
import { Button } from '@astryxdesign/core/Button'
import { Heading } from '@astryxdesign/core/Heading'
import { Text } from '@astryxdesign/core/Text'
import { VStack } from '@astryxdesign/core/VStack'
import { useSubmit } from '../useSubmit.ts'
import { downloadExport, type User } from './sessionApi.ts'

type Props = {
  user: User
  onDeleteAccount: () => Promise<void>
}

export function AccountPage({ user, onDeleteAccount }: Props) {
  const exporting = useSubmit()
  const deleting = useSubmit()
  const [isConfirming, setIsConfirming] = useState(false)

  async function confirmDeletion() {
    const isDeleted = await deleting.run(onDeleteAccount)
    if (!isDeleted) setIsConfirming(false)
  }

  function changeConfirmation(isOpen: boolean) {
    if (!deleting.isRunning) setIsConfirming(isOpen)
  }

  return (
    <VStack gap={4} padding={4}>
      <Heading level={1}>Conta</Heading>
      <VStack gap={1}>
        <Text type="label">{user.name}</Text>
        <Text type="supporting">{user.email}</Text>
      </VStack>
      <VStack gap={2}>
        <Heading level={2}>Exportar meus dados</Heading>
        <Text type="supporting">Baixa um arquivo com seus alimentos, seus planos e todo o seu diário.</Text>
        {exporting.error && <Banner status="error" title="Não foi possível exportar" description={exporting.error} />}
        <Button
          label="Exportar meus dados"
          variant="secondary"
          isLoading={exporting.isRunning}
          onClick={() => void exporting.run(downloadExport)}
        />
      </VStack>
      <VStack gap={2}>
        <Heading level={2}>Apagar minha conta</Heading>
        <Text type="supporting">
          Apaga seus alimentos, seus planos e seu diário. Isso não pode ser desfeito. Exporte antes se quiser guardar uma cópia.
        </Text>
        {deleting.error && <Banner status="error" title="Não foi possível apagar a conta" description={deleting.error} />}
        <Button label="Apagar minha conta" variant="destructive" onClick={() => setIsConfirming(true)} />
      </VStack>
      <AlertDialog
        isOpen={isConfirming}
        onOpenChange={changeConfirmation}
        title="Apagar a conta?"
        description="Seus alimentos, seus planos e todo o seu diário serão apagados para sempre. Isso não pode ser desfeito."
        actionLabel="Apagar a conta"
        cancelLabel="Cancelar"
        isActionLoading={deleting.isRunning}
        onAction={() => void confirmDeletion()}
      />
    </VStack>
  )
}
