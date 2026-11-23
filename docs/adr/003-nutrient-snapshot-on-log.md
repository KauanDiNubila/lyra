# ADR-003: Snapshot dos nutrientes quando um item é registrado

- **Data**: 2026-11-18
- **Status**: Proposta
- **Decisores**: Kauan Di Nubila
- **Tags**: domínio, dados

## Contexto e problema

Uma entrada do diário se refere a um alimento. Os dados do alimento mudam: o usuário edita um alimento personalizado, o plano é editado, ou o Open Food Facts (colaborativo) corrige um produto. Se a entrada do diário guardar só uma referência ao alimento, os dias passados mudam seus totais em silêncio, e o histórico em que o usuário se apoia para analisar a dieta deixa de ser confiável.

## Fatores de decisão

1. Os dias passados do diário nunca podem mudar, a menos que o usuário os edite.
2. O cálculo precisa poder ser reproduzido a partir do que está guardado na entrada.

## Opções consideradas

1. **Só referência**: a entrada guarda o id do alimento e a quantidade; os nutrientes são lidos do alimento na hora da consulta.
2. **Snapshot**: a entrada guarda o id do alimento mais uma cópia do nome, da quantidade de referência e dos nutrientes no momento do registro.
3. **Alimentos versionados**: toda mudança em um alimento cria uma nova versão; as entradas apontam para uma versão.

## Decisão

Opção escolhida: **snapshot**.

A entrada do diário guarda `foodId` (para reuso e estatísticas), `foodName`, `referenceAmount` e `nutrients`, copiados no momento em que ela é criada ou trocada. Mudar a quantidade de uma entrada mantém o snapshot dela. Mudar o alimento de uma entrada gera um novo snapshot.

Os alimentos versionados são rejeitados porque dão a mesma garantia com mais tabelas e mais consultas.

### Consequências

- Bom: o histórico é estável e todo total pode ser recalculado só a partir da entrada.
- Bom: as fontes externas podem sair do ar ou mudar sem afetar os dias passados.
- Ruim: dados duplicados por entrada (algumas colunas); desprezível para um usuário só.
- Ruim: corrigir um alimento errado não conserta os dias passados automaticamente; uma futura ação "reaplicar o alimento às entradas passadas" pode fazer isso de forma explícita.
