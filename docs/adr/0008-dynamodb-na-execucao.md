# ADR 0008 — DynamoDB no Execution Service

- **Status:** Aceito
- **Data:** 2026-11-10
- **Contexto:** Tech Challenge — Fase 4
- **Referências:** [ADR 0006](0006-microsservicos-e-saga-orquestrada.md), [RFC 004](../rfc/004-banco-de-dados.md), `soat-execution-service/terraform/dynamodb.tf`

## Contexto

O desafio pede ao menos um banco SQL e um NoSQL. O OS Service e o Billing usam Postgres.
O Execution Service precisa de um banco NoSQL.

O Execution guarda uma fila de reparos e o histórico de etapas de cada OS.
Ele busca sempre por OS ou por etapa. Não faz junções nem relatórios.

## Decisão

Usamos **Amazon DynamoDB** em modo sob demanda (`PAY_PER_REQUEST`), numa tabela única `oficina-homolog-execucoes`.

| Item | Chaves | Conteúdo |
|---|---|---|
| Execução da OS | `pk = EXEC#<osId>`, `sk = META` | etapa atual, serviços, observações, motivo de rejeição e histórico de etapas |
| Falha simulada | `pk = CONFIG`, `sk = SIMULACAO_FALHA` | flag `ativa` da demo |

- O índice `gsi-etapa` (`gsi1pk = ETAPA#<etapa>`, `gsi1sk` = data de criação) lista as execuções por etapa. Ele projeta todos os atributos.
- O histórico de etapas é uma lista dentro do item. Ele só cresce, nunca muda.
- A escrita inicial usa condição (`attribute_not_exists`). Uma entrega repetida de `IniciarExecucao` não cria uma segunda execução.
- O avanço de etapa compara o campo `versao` na escrita. Dois avanços ao mesmo tempo geram 409.
- Sem criptografia própria: a tabela usa a chave padrão gerenciada pela AWS.
- Recuperação por ponto no tempo desligada, para reduzir custo em homolog.

## Alternativas descartadas

| Alternativa | Por que não |
|---|---|
| Um terceiro Postgres | Não cumpre a exigência de um banco NoSQL |
| MongoDB (DocumentDB) | Custo fixo alto e mais um servidor para operar |
| Redis | Não é banco durável para o histórico |

## Consequências

**Positivas**
- Sem servidor. O custo segue o uso e fica perto de zero em homolog.
- O item único por OS casa com o acesso: uma leitura traz tudo.
- A escrita condicional dá idempotência sem tabela extra.

**Negativas / custos**
- Sem junções. Qualquer relatório novo exige um índice ou uma cópia dos dados.
- A fila ativa usa o índice. `contarAtivas` e `listarFila` fazem uma consulta por etapa ativa.
- Um item DynamoDB tem limite de 400 KB. O histórico de uma OS cabe com folga, mas o limite existe.
- O teste local precisa do LocalStack com `dynamodb` e uma tabela criada à mão.
