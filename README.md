# Lyra

Um rastreador de dieta feito em torno do plano que o seu nutricionista passou.

A maioria dos rastreadores começa de um dia vazio e faz você digitar tudo de novo. Quem segue um plano prescrito come mais ou menos a mesma coisa todo dia, então esse esforço é desperdiçado. O Lyra inverte o fluxo: **o plano é o padrão, e o diário só registra o que mudou.**

## Como funciona

1. **Cadastre o plano uma vez.** Refeições, alimentos, quantidades e metas diárias (calorias, proteína, carboidrato e gordura).
2. **Abra qualquer dia já preenchido.** O Lyra copia o plano ativo para o diário daquele dia.
3. **Registre só as diferenças.** Mais arroz, um alimento trocado, um item pulado, algo extra. Os totais por refeição e por dia são recalculados, e você vê como o dia ficou em relação ao plano e às metas.

Os alimentos vêm da tabela brasileira TACO, e você pode cadastrar os seus. Seus planos, seu diário e seus alimentos personalizados são só seus: cada conta é isolada das outras.

## Estado do projeto

O Lyra está em desenvolvimento e ainda não está disponível ao público.

- Pronto: alimentos (busca na TACO e alimentos personalizados), diário, plano alimentar, preenchimento do diário a partir do plano, login com Google e dados separados por usuário.
- Próximo: exportar e apagar a conta.
- Planejado: quick add para comida de restaurante, código de barras com o Open Food Facts, peso corporal e um app instalável que lê o diário de hoje offline.

O escopo completo e o raciocínio estão no [RFC-001](docs/rfc/001-lyra-mvp.md).

## Feito com

Java 25 e Spring Boot no backend (arquitetura hexagonal, PostgreSQL) e um PWA em React com Tailwind CSS e shadcn/ui no frontend, mais uma landing page em Astro. As decisões de arquitetura estão registradas em [docs/adr](docs/adr/).

## Rodando localmente

Você precisa de JDK 25, Node.js e Docker.

```bash
cd backend && ./mvnw spring-boot:test-run
cd frontend && npm install && npm run dev
```

Depois abra `http://localhost:5173`. O backend sobe com valores falsos do Google, então o login só funciona com valores reais: crie um cliente OAuth no console do Google (tipo "Web application", origem `http://localhost:5173`, URI de redirecionamento `http://localhost:5173/login/oauth2/code/google`) e exporte `GOOGLE_CLIENT_ID` e `GOOGLE_CLIENT_SECRET` antes de subir o backend. Nunca coloque esses valores no repositório.

O `JAVA_HOME` da máquina precisa apontar para o JDK 25. Para rodar as verificações:

```bash
cd backend && ./mvnw verify
cd frontend && npm run lint && npm test && npm run build
```

Os testes do backend usam Testcontainers, então o Docker precisa estar rodando. Os comandos, as regras de arquitetura e o fluxo de trabalho estão no [CLAUDE.md](CLAUDE.md).

## Fontes de dados e créditos

- **TACO**, a Tabela Brasileira de Composição de Alimentos, da Universidade Estadual de Campinas.
- **Open Food Facts** para códigos de barras, quando essa slice for construída.

## Licença

Ainda não decidida.
