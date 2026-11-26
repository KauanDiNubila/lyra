import { useState, type FormEvent } from 'react'
import { X } from 'lucide-react'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Spinner } from '@/components/ui/spinner'
import { todayIso } from '../diary/dates.ts'
import { useAsyncResource } from '../useAsyncResource.ts'
import { useSubmit } from '../useSubmit.ts'
import { deleteWeight, listWeights, recordWeight } from './weightApi.ts'
import { formatDate, formatKilograms, newestFirst, weightProblem } from './weightRules.ts'

function toKilograms(text: string): number | null {
  if (text.trim() === '') return null
  const value = Number(text)
  return Number.isNaN(value) ? null : value
}

export function WeightPage() {
  const [date, setDate] = useState<string>(() => todayIso())
  const [text, setText] = useState('')
  const { resource: weights, reload } = useAsyncResource('weights', listWeights)
  const save = useSubmit()
  const remove = useSubmit()
  const kilograms = toKilograms(text)
  const problem = weightProblem(kilograms)
  const existing = weights.status === 'success' ? weights.data.find((weight) => weight.date === date) : undefined

  function submit(event: FormEvent) {
    event.preventDefault()
    if (kilograms === null || problem) return
    void save.run(async () => {
      await recordWeight(date, kilograms)
      setText('')
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
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-semibold tracking-tight">Peso</h1>
      <form onSubmit={submit} aria-label="Registrar peso" className="flex flex-col gap-4">
        {save.error && (
          <Alert variant="destructive">
            <AlertTitle>Não foi possível salvar o peso</AlertTitle>
            <AlertDescription>{save.error}</AlertDescription>
          </Alert>
        )}
        <FieldGroup className="gap-4">
          <Field>
            <FieldLabel htmlFor="weight-date">Data</FieldLabel>
            <Input
              id="weight-date"
              type="date"
              required
              value={date}
              onChange={(event) => event.target.value && setDate(event.target.value)}
            />
          </Field>
          <Field data-invalid={problem !== null}>
            <FieldLabel htmlFor="weight-kilograms">Peso (kg)</FieldLabel>
            <Input
              id="weight-kilograms"
              type="number"
              inputMode="decimal"
              step="0.1"
              required
              aria-invalid={problem !== null}
              aria-describedby="weight-kilograms-hint"
              value={text}
              onChange={(event) => setText(event.target.value)}
            />
            <div id="weight-kilograms-hint">
              {existing && (
                <FieldDescription>
                  Já existe {formatKilograms(existing.kilograms)} neste dia. Salvar troca o valor.
                </FieldDescription>
              )}
              {problem && <FieldError>{problem}</FieldError>}
            </div>
          </Field>
        </FieldGroup>
        <Button type="submit" size="lg" disabled={kilograms === null || problem !== null || save.isRunning}>
          {save.isRunning && <Spinner aria-hidden />}
          Salvar peso
        </Button>
      </form>
      {remove.error && (
        <Alert variant="destructive">
          <AlertTitle>Não foi possível apagar o peso</AlertTitle>
          <AlertDescription>{remove.error}</AlertDescription>
        </Alert>
      )}
      {weights.status === 'loading' && (
        <Spinner className="mx-auto size-6 text-muted-foreground" aria-label="Carregando os pesos" />
      )}
      {weights.status === 'error' && (
        <div className="flex flex-col gap-3">
          <Alert variant="destructive">
            <AlertTitle>Não foi possível carregar os pesos</AlertTitle>
            <AlertDescription>{weights.message}</AlertDescription>
          </Alert>
          <Button variant="outline" onClick={reload}>
            Tentar de novo
          </Button>
        </div>
      )}
      {weights.status === 'success' && weights.data.length === 0 && (
        <Empty className="border">
          <EmptyHeader>
            <EmptyTitle>Nenhum peso registrado</EmptyTitle>
            <EmptyDescription>Registre o peso de hoje para começar a acompanhar.</EmptyDescription>
          </EmptyHeader>
        </Empty>
      )}
      {weights.status === 'success' && weights.data.length > 0 && (
        <section aria-labelledby="weight-history" className="flex flex-col gap-2">
          <h2 id="weight-history" className="text-sm font-medium text-muted-foreground">
            Histórico
          </h2>
          <ul className="divide-y rounded-lg border">
            {newestFirst(weights.data).map((weight) => (
              <li key={weight.date} className="flex items-center gap-3 px-4 py-3">
                <div className="flex flex-1 flex-col">
                  <span className="font-medium tabular-nums">{formatKilograms(weight.kilograms)}</span>
                  <span className="text-sm text-muted-foreground">
                    {weight.change ? `${formatDate(weight.date)} · ${weight.change}` : formatDate(weight.date)}
                  </span>
                </div>
                <Button
                  variant="ghost"
                  size="icon"
                  aria-label={`Apagar o peso de ${formatDate(weight.date)}`}
                  disabled={remove.isRunning}
                  onClick={() => removeWeight(weight.date)}
                >
                  <X aria-hidden />
                </Button>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  )
}
