import { useEffect, useState } from 'react'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'

function hasFailed(): boolean {
  return new URLSearchParams(window.location.search).get('login') === 'failed'
}

export function LoginPage() {
  const [failed] = useState(hasFailed)

  useEffect(() => {
    if (hasFailed()) window.history.replaceState(null, '', window.location.pathname)
  }, [])

  return (
    <main className="mx-auto flex min-h-dvh max-w-sm flex-col justify-center gap-6 px-6">
      <div className="flex flex-col gap-2">
        <h1 className="text-3xl font-semibold tracking-tight">Lyra</h1>
        <p className="text-muted-foreground">Seu diário alimentar, a partir do plano que você já segue.</p>
      </div>
      {failed && (
        <Alert variant="destructive">
          <AlertTitle>Não foi possível entrar</AlertTitle>
          <AlertDescription>Use uma conta Google com o e-mail verificado e tente de novo.</AlertDescription>
        </Alert>
      )}
      <Button asChild size="lg">
        <a href="/oauth2/authorization/google">Entrar com Google</a>
      </Button>
    </main>
  )
}
