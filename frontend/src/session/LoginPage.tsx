import { useEffect, useState } from 'react'
import { Banner } from '@astryxdesign/core/Banner'
import { Button } from '@astryxdesign/core/Button'
import { Heading } from '@astryxdesign/core/Heading'
import { Text } from '@astryxdesign/core/Text'
import { VStack } from '@astryxdesign/core/VStack'

function hasFailed(): boolean {
  return new URLSearchParams(window.location.search).get('login') === 'failed'
}

export function LoginPage() {
  const [failed] = useState(hasFailed)

  useEffect(() => {
    if (hasFailed()) window.history.replaceState(null, '', window.location.pathname)
  }, [])

  return (
    <VStack gap={4} padding={4}>
      <Heading level={1}>Lyra</Heading>
      <Text type="supporting">Seu diário alimentar, a partir do plano que você já segue.</Text>
      {failed && (
        <Banner
          status="error"
          title="Não foi possível entrar"
          description="Use uma conta Google com o e-mail verificado e tente de novo."
        />
      )}
      <Button label="Entrar com Google" variant="primary" href="/oauth2/authorization/google" />
    </VStack>
  )
}
