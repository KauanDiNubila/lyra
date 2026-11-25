import { useState, type FormEvent } from 'react'
import { Banner } from '@astryxdesign/core/Banner'
import { Button } from '@astryxdesign/core/Button'
import { EmptyState } from '@astryxdesign/core/EmptyState'
import { Text } from '@astryxdesign/core/Text'
import { TextInput } from '@astryxdesign/core/TextInput'
import { VStack } from '@astryxdesign/core/VStack'
import { ApiError } from '../http.ts'
import { useSubmit } from '../useSubmit.ts'
import { lookupBarcode, type Food } from './foodApi.ts'
import { OPEN_FOOD_FACTS_CREDIT } from './sourceCredits.ts'

type Props = {
  onFound: (food: Food) => void
  onCreateInstead?: () => void
}

const BARCODE = /^\d{8,14}$/

export function BarcodeForm({ onFound, onCreateInstead }: Props) {
  const [code, setCode] = useState('')
  const [triedCode, setTriedCode] = useState<string | null>(null)
  const [notFoundCode, setNotFoundCode] = useState<string | null>(null)
  const { run, isRunning, error } = useSubmit()
  const trimmed = code.trim()
  const isValid = BARCODE.test(trimmed)

  function submit(event: FormEvent) {
    event.preventDefault()
    if (!isValid) return
    setTriedCode(trimmed)
    void run(async () => {
      try {
        onFound(await lookupBarcode(trimmed))
      } catch (cause) {
        if (cause instanceof ApiError && cause.status === 404) {
          setNotFoundCode(trimmed)
          return
        }
        throw cause
      }
    })
  }

  return (
    <form onSubmit={submit} aria-label="Buscar por código de barras">
      <VStack gap={3}>
        {error && triedCode === trimmed && (
          <Banner status="error" title="Não foi possível buscar o produto" description={error} />
        )}
        <TextInput
          label="Código de barras"
          description="Os números embaixo das barras, de 8 a 14 dígitos."
          value={code}
          onChange={setCode}
          inputMode="numeric"
          enterKeyHint="search"
          hasAutoFocus
        />
        <Button type="submit" label="Buscar produto" variant="primary" isDisabled={!isValid} isLoading={isRunning} />
        {notFoundCode === trimmed && (
          <EmptyState
            title="Produto não encontrado"
            description="O Open Food Facts não tem esse produto, ou não tem os valores nutricionais dele."
            actions={onCreateInstead && <Button label="Cadastrar alimento" variant="secondary" onClick={onCreateInstead} />}
          />
        )}
        <Text type="supporting">{OPEN_FOOD_FACTS_CREDIT}</Text>
      </VStack>
    </form>
  )
}
