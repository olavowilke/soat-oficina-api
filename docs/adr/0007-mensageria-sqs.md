# ADR 0007 — Mensageria com Amazon SQS

- **Status:** Aceito
- **Data:** 2026-11-10
- **Contexto:** Tech Challenge — Fase 4
- **Referências:** [ADR 0006](0006-microsservicos-e-saga-orquestrada.md), `terraform/sqs.tf`, `src/main/java/br/com/oficina/saga/`

## Contexto

A Saga precisa de mensagens entre os três serviços. O desafio não fixa o broker.
O projeto roda em escala de aluno, na AWS, com custo baixo.

## Decisão

Usamos **Amazon SQS** com o Spring Cloud AWS 3.3.

### Filas

| Fila | Dona | Quem publica | Quem consome |
|---|---|---|---|
| `oficina-homolog-billing-commands` | Billing | OS Service | Billing |
| `oficina-homolog-execution-commands` | Execution | OS Service | Execution |
| `oficina-homolog-os-saga-replies` | OS Service | Billing e Execution | OS Service |

- Cada fila tem uma DLQ com sufixo `-dlq` e `maxReceiveCount = 3`.
- Cada repositório cria suas filas no próprio Terraform e publica URL e ARN no SSM (`/oficina/homolog/queues/<nome>/url`).
- Local e testes usam LocalStack.

### Mensagem

Todas as mensagens são JSON com este envelope (`MensagemSaga`):
`messageId`, `sagaId`, `osId`, `type`, `correlationId`, `occurredAt`, `payload`.
Os atributos SQS `type`, `correlationId` e `newrelic` repetem os dados de rastreio.

### Entrega confiável

- **Outbox.** OS Service e Billing gravam a mensagem na tabela `outbox`, na mesma transação do dado. Um job envia a cada 1 segundo.
- **Idempotência.** Cada consumidor ignora um `messageId` repetido. Os serviços Postgres usam a tabela `mensagens_processadas`. O Execution usa escrita condicional no DynamoDB.
- **Confirmação.** O consumidor só confirma a mensagem se o processamento termina sem erro (`ON_SUCCESS`).
- **Resposta adiantada.** Se uma resposta chega antes da anterior, o OS Service lança erro. A mensagem volta à fila e chega de novo.
- **Resposta desconhecida.** Uma resposta para uma saga que o OS Service não conhece vira log WARN e é descartada.

### Estorno e contador de entregas

O comando `EstornarPagamento` usa o `ApproximateReceiveCount` do SQS.
Se o provedor recusa o estorno antes da 3ª entrega, o Billing não responde e a fila entrega de novo.
Na 3ª entrega, o Billing publica `EstornoFalhou`. Assim a mensagem não vai para a DLQ sem resposta à saga.
`BILLING_ESTORNO_MAX_ENTREGAS` precisa ser igual ao `maxReceiveCount` da fila.

### Rastreio

O `correlationId` e o cabeçalho `newrelic` viajam nos atributos da mensagem.
O consumidor aceita o cabeçalho com `TransportType.Other`, porque o `newrelic-api` 8.16.0 não tem `TransportType.Queue`.
O consumidor põe `correlationId` e `sagaId` no MDC enquanto trata a mensagem.

## Alternativas descartadas

| Alternativa | Por que não |
|---|---|
| Kafka (MSK) | Custo e operação altos para o volume |
| SNS + SQS | Sem tópicos de eventos para vários consumidores, o SNS só soma uma peça |
| EventBridge | Não dá fila por serviço com DLQ e redrive tão simples |
| RabbitMQ no cluster | Mais um componente para operar e atualizar |

## Consequências

**Positivas**
- Sem servidor para operar. O custo é por mensagem.
- DLQ e retentativa vêm prontas.
- O Pod Identity dá acesso às filas sem chave estática.

**Negativas / custos**
- A entrega é *at-least-once*. Todo consumidor precisa ser idempotente.
- O SQS padrão não garante ordem. O OS Service trata respostas fora de ordem.
- Mensagem na DLQ não reprocessa sozinha. Um alerta do New Relic avisa e um operador age.
- O outbox soma até 1 segundo de latência por mensagem.
