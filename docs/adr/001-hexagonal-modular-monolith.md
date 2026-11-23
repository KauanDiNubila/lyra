# ADR-001: Arquitetura hexagonal em um monolito modular

- **Data**: 2026-11-18
- **Status**: Proposta
- **Decisores**: Kauan Di Nubila
- **Tags**: arquitetura, backend

## Contexto e problema

O Lyra depende de vários sistemas externos para a mesma necessidade (dados de alimentos da TACO, do Open Food Facts, da USDA e, mais tarde, estimativa por IA) e será consumido por mais de um cliente (PWA agora, talvez um app nativo depois). As regras centrais, o cálculo de macros e a lógica de plano versus diário, precisam continuar testáveis sem nenhum deles.

## Fatores de decisão

1. Regras de domínio testáveis com testes unitários simples, sem framework nem banco de dados.
2. Trocar ou adicionar uma fonte de alimentos sem mexer no domínio.
3. Poucas peças móveis para um projeto de uma pessoa só.

## Opções consideradas

1. **Arquitetura hexagonal (ports and adapters)**, seguindo *Get Your Hands Dirty on Clean Architecture* (Hombergs).
2. **Clean Architecture** com todo o seu vocabulário de anéis (entidades, casos de uso, presenters, modelos de requisição e resposta).
3. **Arquitetura em camadas** (controller, service, repository).
4. **Microsserviços**.

## Decisão

Opção escolhida: **arquitetura hexagonal dentro de um monolito modular**.

Cada módulo (`food`, `plan`, `diary`, `weight`, `identity`) tem a mesma estrutura:

```
<module>/
  domain/
  application/
    port/in/
    port/out/
    service/
  adapter/
    in/web/
    out/persistence/
    out/<external>/
```

Regras:

- `domain` não depende de nada fora do JDK em tempo de execução. Anotações do Lombok, que só existem na compilação, são permitidas ([ADR-005](005-backend-stack.md)).
- `application` depende apenas de `domain`.
- Os adaptadores dependem de `application` e `domain`, nunca o contrário.
- Um módulo só fala com outro pelas portas de entrada (inbound ports) do outro.

Essas regras são garantidas por testes do ArchUnit ([ADR-002](002-testing-strategy.md)).

A Clean Architecture é rejeitada porque suas camadas extras (presenters, modelos de requisição e resposta separados por caso de uso) acrescentam arquivos sem acrescentar valor nesse tamanho. A arquitetura em camadas é rejeitada porque o domínio acaba dependendo do JPA e de clientes externos. Os microsserviços são rejeitados porque uma pessoa só pagaria o custo operacional de vários artefatos implantáveis sem uma necessidade de escala que o justifique.

### Consequências

- Bom: o problema da fonte de alimentos vira apenas adicionar um adaptador.
- Bom: os testes de domínio e de application rodam em milissegundos.
- Ruim: mais pacotes e mais mapeamento entre as entidades JPA e os objetos de domínio.
- Ruim: as fronteiras dos módulos são garantidas por testes, não pelo build; separar em módulos Maven é possível depois, se os testes se mostrarem insuficientes.
