import { Card } from '@astryxdesign/core/Card'
import { Heading } from '@astryxdesign/core/Heading'
import { Text } from '@astryxdesign/core/Text'
import { VStack } from '@astryxdesign/core/VStack'

function App() {
  return (
    <main>
      <Card padding={6}>
        <VStack gap={2}>
          <Heading level={1}>Lyra</Heading>
          <Text type="supporting">Seu plano é o padrão. O diário registra o que mudou.</Text>
        </VStack>
      </Card>
    </main>
  )
}

export default App
