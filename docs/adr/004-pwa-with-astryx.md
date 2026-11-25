# ADR-004: Frontend como PWA construído com Astryx

- **Data**: 2026-11-18
- **Status**: Proposta, complementada pela [ADR-014](014-iphone-app-login-scan-and-distribution.md) (o app do iPhone: login por token, escaneamento e distribuição; a tecnologia do app está em aberto)
- **Decisores**: Kauan Di Nubila
- **Tags**: frontend, mobile

## Contexto e problema

O Lyra precisa funcionar no celular (registrar comida em viagem) e no desktop (editar o plano alimentar). O design system escolhido para avaliar é o [Astryx](https://github.com/meta/astryx), o design system de código aberto da Meta: React 19, licença MIT, mais de 150 componentes, atualmente em beta (0.7.x). O pacote nativo dele (`@astryxdesign/native`) é experimental, privado e explicitamente sem suporte para produção, então o Astryx roda só na web hoje.

## Fatores de decisão

1. Uma única base de código para celular e desktop.
2. Avaliar de verdade o Astryx, que só roda na web.
3. Leitura de código de barras com a câmera do celular.
4. Usável com uma conexão fraca.

## Opções consideradas

1. **Progressive Web App** com React, Vite e Astryx.
2. **O mesmo PWA empacotado com o Capacitor** para as lojas de aplicativos.
3. **App em React Native**, sem o Astryx.

## Decisão

Opção escolhida: **Progressive Web App com React, Vite e Astryx**.

- Vite em vez de Next.js: o Lyra é um app atrás de login, sem necessidade de renderização no servidor nem de SEO.
- Instalável por meio de um web app manifest; um service worker guarda em cache o shell do app e o diário de hoje para leitura offline.
- Código de barras: a API `BarcodeDetector` do navegador onde existir (Chrome no Android) e uma biblioteca JavaScript como alternativa no Safari do iOS, que não a suporta.

O Capacitor fica como caminho de evolução caso publicar nas lojas se torne um objetivo; ele empacota o mesmo build sem reescrever nada. O React Native é rejeitado porque não usaria o Astryx e significaria manter um segundo frontend.

### Consequências

- Bom: um frontend só, instalável no celular desde o primeiro dia.
- Bom: o backend não se importa; qualquer cliente nativo futuro usa a mesma API HTTP.
- Ruim: o Astryx está em beta, então atualizações menores podem quebrar componentes; as versões são fixadas e atualizadas de forma deliberada.
- Ruim: os gráficos do Astryx só estão na tag canary; os gráficos de progresso podem precisar de outra biblioteca até ficarem estáveis.
- Ruim: PWAs no iOS têm limites (remoção de armazenamento, notificações push só quando instalado).
