import { Scale } from 'lucide-react'
import { ErrorAlert } from '@/components/ErrorAlert'
import { Button } from '@/components/ui/button'
import { Spinner } from '@/components/ui/spinner'
import { LoginPage } from './session/LoginPage.tsx'
import { useSession } from './session/useSession.ts'
import { useSubmit } from './useSubmit.ts'
import { WeightPage } from './weight/WeightPage.tsx'

function App() {
  const { session, retry, signOut } = useSession()
  const exit = useSubmit()

  if (session.status === 'loading') {
    return (
      <main className="flex min-h-dvh items-center justify-center">
        <Spinner className="size-6 text-muted-foreground" aria-label="Carregando a sessão" />
      </main>
    )
  }
  if (session.status === 'error') {
    return (
      <main className="mx-auto flex min-h-dvh max-w-sm flex-col justify-center gap-4 px-6">
        <ErrorAlert title="Não foi possível abrir o app">{session.message}</ErrorAlert>
        <Button variant="outline" onClick={retry}>
          Tentar de novo
        </Button>
      </main>
    )
  }
  if (session.status === 'signed-out') return <LoginPage />

  return (
    <div className="min-h-dvh pb-20 md:pb-0">
      <header className="sticky top-0 z-10 border-b bg-background">
        <div className="mx-auto flex h-14 max-w-3xl items-center gap-4 px-4">
          <span className="text-lg font-semibold tracking-tight">Lyra</span>
          <nav
            aria-label="Principal"
            className="fixed inset-x-0 bottom-0 flex justify-around border-t bg-background md:static md:justify-start md:border-0 md:bg-transparent"
          >
            <a
              href="#peso"
              aria-current="page"
              className="flex flex-col items-center gap-1 px-4 py-2 text-xs font-medium text-foreground md:flex-row md:rounded-md md:bg-muted md:text-sm"
            >
              <Scale className="size-5 md:size-4" aria-hidden />
              Peso
            </a>
          </nav>
          <span className="ml-auto truncate text-sm text-muted-foreground">{session.user.name}</span>
          <Button variant="ghost" className="h-11 md:h-8" disabled={exit.isRunning} onClick={() => void exit.run(signOut)}>
            {exit.isRunning && <Spinner aria-hidden />}
            Sair
          </Button>
        </div>
      </header>
      {exit.error && (
        <div className="mx-auto max-w-3xl px-4 pt-4">
          <ErrorAlert title="Não foi possível sair">{exit.error}</ErrorAlert>
        </div>
      )}
      <main id="peso" className="mx-auto max-w-3xl px-4 py-6">
        <WeightPage />
      </main>
    </div>
  )
}

export default App
