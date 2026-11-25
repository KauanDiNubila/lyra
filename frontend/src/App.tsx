import { useState } from 'react'
import { Banner } from '@astryxdesign/core/Banner'
import { Button } from '@astryxdesign/core/Button'
import { HStack } from '@astryxdesign/core/HStack'
import { Spinner } from '@astryxdesign/core/Spinner'
import { Tab, TabList } from '@astryxdesign/core/TabList'
import { Text } from '@astryxdesign/core/Text'
import { VStack } from '@astryxdesign/core/VStack'
import { DiaryPage } from './diary/DiaryPage.tsx'
import { FoodsPage } from './foods/FoodsPage.tsx'
import { PlansPage } from './plans/PlansPage.tsx'
import { AccountPage } from './session/AccountPage.tsx'
import { LoginPage } from './session/LoginPage.tsx'
import { useSession } from './session/useSession.ts'
import { useSubmit } from './useSubmit.ts'

function App() {
  const { session, retry, signOut, deleteAccount } = useSession()
  const [tab, setTab] = useState('diary')
  const exit = useSubmit()

  if (session.status === 'loading') {
    return (
      <VStack padding={4}>
        <Spinner label="Carregando a sessão" />
      </VStack>
    )
  }
  if (session.status === 'error') {
    return (
      <VStack gap={3} padding={4}>
        <Banner status="error" title="Não foi possível abrir o app" description={session.message} />
        <Button label="Tentar de novo" variant="secondary" onClick={retry} />
      </VStack>
    )
  }
  if (session.status === 'signed-out') return <LoginPage />

  return (
    <>
      <HStack justify="between" align="center" padding={4}>
        <Text type="label">{session.user.name}</Text>
        <Button label="Sair" variant="ghost" isLoading={exit.isRunning} onClick={() => void exit.run(signOut)} />
      </HStack>
      {exit.error && <Banner status="error" title="Não foi possível sair" description={exit.error} />}
      <TabList role="tablist" value={tab} onChange={setTab}>
        <Tab value="diary" label="Diário" panelId="diary-page" />
        <Tab value="plans" label="Plano" panelId="plans-page" />
        <Tab value="foods" label="Alimentos" panelId="foods-page" />
        <Tab value="account" label="Conta" panelId="account-page" />
      </TabList>
      <section id="diary-page" role="tabpanel" hidden={tab !== 'diary'}>
        <DiaryPage />
      </section>
      <section id="plans-page" role="tabpanel" hidden={tab !== 'plans'}>
        <PlansPage />
      </section>
      <section id="foods-page" role="tabpanel" hidden={tab !== 'foods'}>
        <FoodsPage />
      </section>
      <section id="account-page" role="tabpanel" hidden={tab !== 'account'}>
        {tab === 'account' && <AccountPage user={session.user} onDeleteAccount={deleteAccount} />}
      </section>
    </>
  )
}

export default App
