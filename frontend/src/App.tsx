import { useState } from 'react'
import { Tab, TabList } from '@astryxdesign/core/TabList'
import { DiaryPage } from './diary/DiaryPage.tsx'
import { FoodsPage } from './foods/FoodsPage.tsx'

function App() {
  const [tab, setTab] = useState('diary')

  return (
    <>
      <TabList role="tablist" value={tab} onChange={setTab}>
        <Tab value="diary" label="Diário" panelId="page" />
        <Tab value="foods" label="Alimentos" panelId="page" />
      </TabList>
      <section id="page" role="tabpanel">
        {tab === 'diary' ? <DiaryPage /> : <FoodsPage />}
      </section>
    </>
  )
}

export default App
