# ADR-014: App para iPhone: login por token, escaneamento e distribuição

- **Data**: 2026-11-25
- **Status**: Proposta
- **Decisores**: Kauan Di Nubila
- **Tags**: ios, app, autenticação, distribuição, arquitetura

## Contexto e problema

O [ADR-004](004-pwa-with-astryx.md) escolheu um PWA com o Astryx. O objetivo agora é mais amplo:

- No iPhone, o Lyra deve ser um **aplicativo**, e não uma página no Safari.
- Escanear o código de barras com a câmera é um diferencial do produto. A slice 6 ([ADR-013](013-open-food-facts-barcode-lookup.md)) já entrega a busca por código no backend.
- **Ainda não está decidido se o frontend atual (React com o Astryx) fica.** Pode ser refeito com outra biblioteca de interface, ou até como app nativo. Por isso esta ADR decide só o que **não depende** dessa escolha, e deixa a tecnologia do app em aberto.

O ponto de partida:

- O backend é um monólito modular com sessão por cookie e CSRF ([ADR-012](012-user-identity-and-data-ownership.md)). O login é o do Google, por redirecionamento.
- O Google **não permite o login dentro de uma webview** (`disallowed_useragent`, política de 2021 que cita o `WKWebView` do iOS). Qualquer app que empacote uma interface web precisa de um login que não passe pela webview. Um app nativo de verdade também não usa cookie de navegador.
- O autor usa **Windows e não tem Mac**, e o build de um app do iPhone precisa do Xcode, que só roda no macOS.
- O app é, por ora, **só do autor**. Conhecidos entram depois.
- O backend ainda não está hospedado. Um app instalado no iPhone não alcança o `localhost` do computador, e a hospedagem estava fora do escopo da ADR-012.

## Fatores de decisão

1. Decidir agora só o que serve a **qualquer** tecnologia de app (Capacitor, React Native, Swift).
2. Entrar com a conta do Google sem webview.
3. Ler o código de barras com a câmera, de forma rápida e confiável.
4. **Não pagar nada** para usar o app no aparelho do autor.
5. Compilar sem um Mac.
6. Nenhuma dependência de produção nova no backend.

## Decisão

### 1. O backend aceita um segundo jeito de autenticar, para apps

- Na web, tudo continua igual: sessão por cookie e CSRF.
- Num app, o login do Google é **nativo** (o SDK do Google no iPhone, e não uma webview) e devolve um **ID token**. O app o envia para uma rota nova, `POST /api/auth/google`.
- O backend valida o ID token (assinatura pelas chaves públicas do Google, emissor, **audiência igual ao ID de cliente do app**, validade e `email_verified`), reaproveita o `SignInUseCase` (que já cria ou acha o usuário) e devolve um **token opaco de dispositivo**. O backend guarda só o hash do token, com o usuário, o rótulo do dispositivo e a data de uso.
- O app manda o token em `Authorization: Bearer ...`. Um filtro novo no `identity` o aceita **ao lado** da sessão. Como não há cookie, o CSRF não se aplica a quem manda o token.
- O token fica no armazenamento seguro do aparelho (Keychain no iPhone). Sair apaga o token no servidor e no aparelho. Apagar a conta apaga os tokens por cascata, e o teste que vigia as chaves estrangeiras já cobre a tabela nova.
- Se a interface do app for web e empacotada, o backend também precisa de **CORS** nas rotas `/api/**`, com o cabeçalho `Authorization`, para a origem do app. Um app nativo de verdade não precisa disso.
- Não há dependência de produção nova: o Spring Security já traz a validação de JWT usada no login OIDC de hoje.
- O formato exato da rota, do token e do cabeçalho fica para o PR que implementar, e a decisão não muda o login da web.

### 2. O escaneamento é uma capacidade do cliente

- A câmera é lida **no cliente**, e o que ele manda ao backend é só o texto do código, para a rota da ADR-013.
- No frontend, o escaneamento fica atrás de uma interface (`BarcodeScanner`). Na web ela cai para **digitar o código**, e os testes usam uma versão falsa. A leitura nativa entra quando a tecnologia do app estiver decidida. Se for o Capacitor, o candidato é o plugin `@capacitor-mlkit/barcode-scanning`, que usa o ML Kit do Google e lista suporte a iOS.

### 3. Distribuição em duas fases

- **Fase 1, agora, só o autor, sem pagar.** O app é assinado com um **Apple ID gratuito** (a "Personal Team"). Na pesquisa, os limites relatados são: a assinatura vale **7 dias** (depois disso o app precisa ser reassinado), até 3 apps ativos e 3 dispositivos, 10 identificadores de app por semana, **sem TestFlight, sem distribuição a outras pessoas** e sem capacidades pagas (Sign in with Apple, push, iCloud, domínios associados). A Apple não documenta esses números, então eles podem mudar.
- Sem Mac, o fluxo previsto é: **compilar em uma máquina macOS na nuvem** (GitHub Actions), baixar o app e **instalá-lo pelo Windows com o Sideloadly** ou o AltStore, que assinam com o Apple ID gratuito. A renovação semanal é um passo manual (ou automático, se o AltStore ficar ligado na mesma rede).
- **Fase 2, quando conhecidos forem usar.** Aí vale a assinatura do **Apple Developer Program** (US$ 99 por ano), que dá o **TestFlight**. Os testadores externos exigem a revisão do beta pela Apple na primeira versão de cada build.
- A publicação na **App Store pública** não faz parte desta ADR. Ela traz a revisão da Apple, a política de privacidade e, segundo a regra 4.8 das diretrizes, um login equivalente em privacidade ao Google, normalmente o Sign in with Apple. Isso pede uma emenda antes de publicar.
- Os segredos (chave `.p8`, certificados, ID de cliente) ficam nos segredos do GitHub, e **nunca no repositório**.

### 4. O que fica em aberto

- **A tecnologia do app.** Hoje o candidato é o **Capacitor**, empacotando o frontend web. Ele serve a qualquer framework de interface web, então continua válido se o Astryx for trocado. Se o app virar nativo de verdade (React Native, Swift), só este capítulo é revisto: o login por token e o escaneamento como capacidade do cliente continuam valendo.
- **A hospedagem**, que é a ADR-015 e é pré-requisito para qualquer teste real no iPhone.

## Consequências

- Bom: o backend fica pronto para qualquer app, sem esperar a decisão do frontend.
- Bom: o autor usa o app no próprio iPhone sem pagar nada, e a conta paga só entra quando mais gente precisar.
- Bom: o token por dispositivo se revoga sozinho ao apagar a conta e vale para um futuro app Android.
- Ruim: o backend passa a ter **dois jeitos de autenticar** (cookie e token), com um filtro novo e uma tabela nova, e isso é superfície de segurança a mais.
- Ruim: com a conta gratuita, o app **para de abrir depois de 7 dias** se não for reassinado, e **conhecidos não conseguem instalar** sem a conta paga ou sem a mesma rotina de reassinatura.
- Ruim: **sem Mac**, cada teste no iPhone passa por um build na nuvem e uma instalação pelo Sideloadly. O escaneamento nativo só se testa no aparelho.
- Ruim: **a hospedagem passa a ser pré-requisito**.

## Pontos a confirmar na prática

Estes itens vieram de pesquisa em fontes secundárias e não foram verificados em um aparelho:

- se o GitHub Actions gera o app sem assinatura e o Sideloadly o aceita e o instala direito;
- se o login nativo do Google funciona com a conta gratuita (a validade e o identificador do app mudam a cada assinatura);
- se o iPhone exige o **modo de desenvolvedor** ligado para esses apps;
- os minutos de máquina macOS do plano do GitHub para um repositório privado, e quanto cada build gasta;
- o iOS mínimo exigido pelo plugin de escaneamento, e qual campo (`iOSClientId` ou outro) o plugin de login usa para a audiência do ID token;
- os limites exatos da conta gratuita da Apple, que a Apple não documenta;
- se a revisão do TestFlight externo aplica a regra 4.8 a um app só para conhecidos.

## Fora do escopo

- A hospedagem do backend e do banco (ADR-015).
- Escolher a tecnologia do app e refazer o frontend.
- Publicar na App Store, o Sign in with Apple e a política de privacidade.
- Um app Android, notificações push e escrita offline.
- O modo offline da slice 8 dentro do app.
