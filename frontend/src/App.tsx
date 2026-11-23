import { useState } from 'react'
import { Tab, TabList } from '@astryxdesign/core/TabList'
import { DiaryPage } from './diary/DiaryPage.tsx'
import { FoodsPage } from './foods/FoodsPage.tsx'

function App() {
  const [tab, setTab] = useState('diary')

  return (
    <>
      <TabList role="tablist" value={tab} onChange={setTab}>
        <Tab value="diary" label="Diário" panelId="diary-page" />
        <Tab value="foods" label="Alimentos" panelId="foods-page" />
      </TabList>
      <section id="diary-page" role="tabpanel" hidden={tab !== 'diary'}>
        <DiaryPage />
      </section>
      <section id="foods-page" role="tabpanel" hidden={tab !== 'foods'}>
        <FoodsPage />
      </section>
    </>
  )
}

export default App
