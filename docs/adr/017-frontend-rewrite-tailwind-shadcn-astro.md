# ADR-017: Frontend refeito com Tailwind e shadcn/ui, e uma landing page em Astro

- **Data**: 2026-11-26
- **Status**: Proposta, substitui a [ADR-004](004-pwa-with-astryx.md) na escolha do design system
- **Decisores**: Kauan Di Nubila
- **Tags**: frontend, design, landing, mobile

## Contexto e problema

A [ADR-004](004-pwa-with-astryx.md) escolheu um PWA com React, Vite e o Astryx, em parte para avaliar o Astryx. A [ADR-014](014-iphone-app-login-scan-and-distribution.md) deixou em aberto se o frontend com o Astryx continuaria. Agora o objetivo mudou:

- O Lyra precisa de uma **landing page pública**, com acabamento visual de produto e animações, que apresente o app antes do login.
- A interface do app (diário, plano, alimentos, peso, conta e o menu de navegação) vai ser **refeita do zero**, com o mesmo cuidado visual da landing.
- O Astryx continua em beta (0.7.x), os gráficos dele estão só na tag canary, e o ecossistema de componentes animados (Magic UI, Aceternity e outros) é construído sobre Tailwind e shadcn/ui, não sobre StyleX.
- O app vai ser empacotado com o Capacitor para o iPhone ([ADR-014](014-iphone-app-login-scan-and-distribution.md)), então o build do app precisa continuar estático.

O frontend atual tem cerca de 5 mil linhas. Parte delas não depende do Astryx: o cliente HTTP com CSRF (`http.ts`), os clientes da API (`*Api.ts`) e as regras puras (`dates`, `planDraft`, `weightRules`, `formatNutrients`, `meals`), todas com testes.

## Fatores de decisão

1. A landing carrega rápido e é indexável por buscadores.
2. O app continua um build estático que o Capacitor empacota e que funciona como PWA.
3. Um único sistema visual para a landing e para o app.
4. Componentes acessíveis, com o código no repositório e sem depender de uma versão beta.
5. Animações de qualidade, que respeitam `prefers-reduced-motion`.
6. Não perder o que já está testado e conversa com o backend.

## Opções consideradas

### Onde fica a landing

1. **Astro em `landing/`**, separado do app.
2. **Next.js** para a landing e para o app.
3. **Uma rota dentro do app Vite.**

### Base de componentes e estilo

1. **Tailwind CSS v4 e shadcn/ui**, com Motion para as animações.
2. **Manter o Astryx no app** e usar outra base só na landing.

## Decisão

### Landing em Astro

- A landing vive em `landing/`, um projeto Astro próprio. O Astro gera HTML estático, o que atende a velocidade e a indexação, e só carrega JavaScript nas partes interativas (ilhas React), como o hero e as animações.
- O Next.js foi descartado pelo mesmo motivo da ADR-004: o app precisa de um build estático para o Capacitor, e o export estático tira o que o Next tem de vantagem.
- A rota dentro do app foi descartada porque a landing renderizaria no cliente, ficaria fraca em busca e carregaria o peso visual dela no bundle do app.
- A hospedagem da landing segue a ADR de hospedagem e fica fora desta ADR.

### App em Vite, React, Tailwind e shadcn/ui

- O app continua em `frontend/`, com React 19, Vite e o `vite-plugin-pwa`, mantendo o que a ADR-004 decidiu sobre o PWA (manifest, cache do shell e leitura offline do diário de hoje).
- O Astryx e o StyleX saem. Entram o **Tailwind CSS v4** (`tailwindcss`, `@tailwindcss/vite`) e os componentes do **shadcn/ui**, copiados para o repositório. Os componentes do shadcn/ui trazem como dependências o Radix UI (acessibilidade dos componentes), `class-variance-authority`, `clsx` e `tailwind-merge` (variantes e junção de classes) e `lucide-react` (ícones).
- As animações usam o **Motion** (`motion`). Toda animação tem um caminho com `prefers-reduced-motion` e trata interrupção e clique repetido.
- Componentes de vitrine (Magic UI, Aceternity) entram só copiados e adaptados ao sistema visual, um de cada vez, e principalmente na landing.

### Um sistema visual para os dois projetos

- Os tokens de design (cores, tipografia, raios, espaçamentos, curvas e durações de animação) ficam num arquivo CSS único na raiz do repositório, importado pelo tema do Tailwind no app e na landing.
- Um `DESIGN.md` descreve o sistema e é a referência para as revisões feitas com o Impeccable.

### O que é reaproveitado

- O cliente HTTP, os clientes da API e as regras puras passam para a interface nova com os testes deles. Só `dates.ts` e `planDraft.ts` importam tipos do Astryx (`ISODateString`, `ISOTimeString`), que viram tipos próprios.
- Todas as telas, os componentes e os testes de tela são reescritos.

### Como a troca acontece

- O app ainda não está hospedado e só o autor o usa, então a troca não precisa manter as duas interfaces vivas ao mesmo tempo.
- O primeiro PR remove o Astryx, monta o Tailwind, o shadcn/ui, o shell de navegação e a primeira tela. Cada tela seguinte entra em um PR próprio, seguindo a [ADR-007](007-branching-and-ci.md). Enquanto isso, as telas ainda não refeitas ficam fora do app.
- A landing é um PR à parte e não depende da ordem das telas.
- O `CLAUDE.md` e o `frontend/CLAUDE.md` (gerado pelo Astryx) são atualizados no primeiro PR de código, quando o Astryx deixar de existir no projeto.

### Fora desta decisão

- A tecnologia do hero 3D da landing. Um efeito WebGPU (por exemplo o `vgpu`, da Vercel Labs) ou WebGL é decidido no PR da landing, com a regra de que sempre exista uma imagem ou vídeo de fallback para navegadores sem suporte e para `prefers-reduced-motion`.

## Consequências

- Bom: a landing é estática e leve, e só as partes animadas carregam JavaScript.
- Bom: o shadcn/ui fica no repositório, então uma atualização de biblioteca não quebra componentes sem aviso, ao contrário do Astryx em beta.
- Bom: o app continua um build estático, válido para o PWA e para o Capacitor.
- Bom: a camada de API e as regras testadas não são reescritas.
- Ruim: passam a existir **dois projetos de frontend**, com duas configurações de build e de CI.
- Ruim: o código dos componentes do shadcn/ui passa a ser nosso para manter.
- Ruim: o Tailwind espalha o estilo nas classes do JSX, e a disciplina de usar os tokens em vez de valores soltos depende de revisão.
- Ruim: durante a troca, o app em `develop` tem menos telas do que hoje.
- Ruim: várias dependências novas no frontend, listadas acima.
