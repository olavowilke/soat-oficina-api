# ADR 0006 — Microsserviços e Saga orquestrada

- **Status:** Aceito
- **Data:** 2026-11-10
- **Contexto:** Tech Challenge — Fase 4
- **Substitui:** [ADR 0002](0002-comunicacao-sincrona-via-api-gateway.md) e [ADR 0004](0004-quatro-repositorios.md)
- **Referências:** [ADR 0007](0007-mensageria-sqs.md), [ADR 0008](0008-dynamodb-na-execucao.md), [`fase4-arquitetura.png`](../arquitetura/fase4-arquitetura.png)

## Contexto

A Fase 4 pede que o monolito vire microsserviços. O fluxo crítico da OS precisa
ficar consistente, mesmo quando um passo falha. Cada serviço tem repositório,
banco, CI/CD e deploy próprios.

O monolito concentrava a OS, o orçamento e o estoque numa só transação.
Com três serviços, essa transação não existe mais. Cada serviço grava só no
seu banco, então a consistência vem de uma Saga com compensação.

## Decisão

### Três serviços

| Serviço | Repositório | Dono de | Banco |
|---|---|---|---|
| OS Service | `soat-oficina-api` | OS, clientes, veículos, catálogo, estoque e o orquestrador da Saga | RDS Postgres `oficina-homolog` |
| Billing Service | `soat-billing-service` | Orçamento, pagamento, estorno e Mercado Pago | RDS Postgres `oficina-homolog-billing` |
| Execution Service | `soat-execution-service` | Fila de execução, etapas do reparo e histórico | DynamoDB `oficina-homolog-execucoes` |

- Nenhum serviço lê ou grava o banco de outro.
- A Lambda de autenticação continua a ler `clientes` no banco da OS. Ela não é um serviço de negócio.
- O OS Service mantém o nome `soat-oficina-api`. Isso evita quebrar ECR, SSM e pipelines.
- Os quatro repositórios da Fase 3 continuam. Entram `soat-billing-service` e `soat-execution-service`, num total de seis.

### Saga orquestrada

O OS Service é o orquestrador. Ele envia comandos às filas dos outros serviços e lê as respostas na sua fila.
Ele guarda o estado em `saga_ordem_servico`: id da saga, id da OS, estado, motivo da falha e datas.

Motivos da escolha:

- Um só lugar mostra o fluxo inteiro. Isso facilita achar onde uma OS parou.
- O OS Service já é dono da máquina de estados da OS.
- Na coreografia, a lógica do fluxo se espalharia por três repositórios.
- A compensação de cada passo fica num único `switch`, fácil de testar.

### Pagamento antes da execução

O cliente aprova o orçamento, paga, e só então a execução começa.
Por isso a compensação de uma execução rejeitada inclui o estorno.

### Gateway

Cada serviço novo cria no seu Terraform o listener do NLB, o alvo e as rotas do API Gateway.
As rotas do cliente passam pelo authorizer de CPF. Saúde, Swagger e webhook são públicos.
A equipe usa `kubectl port-forward` para rotas de staff, como na Fase 3.

## Alternativas descartadas

| Alternativa | Por que não |
|---|---|
| Coreografia | O fluxo ficaria espalhado em três repositórios. Seguir uma OS exigiria juntar logs de três serviços |
| Cadeia de chamadas REST | Não é uma Saga. Uma falha no meio deixaria dados inconsistentes sem compensação |
| Manter o monolito | Não cumpre o desafio |

## Consequências

**Positivas**
- A consistência não depende de uma transação distribuída.
- Cada serviço escala e faz deploy sozinho.
- Cada compensação é idempotente e fica gravada no estado da saga.

**Negativas / custos**
- A consistência é eventual. Entre dois passos, a OS fica em um estado intermediário.
- O OS Service vira ponto central do fluxo. Se ele cair, a saga para até ele voltar.
- O número de componentes cresce: três deploys, três bancos, três filas e as DLQs.

## Limites conhecidos

- **Orçamento `GERADO` não expira.** Se o cliente não responde, a saga fica em `AGUARDANDO_APROVACAO`. A equipe recusa o orçamento no Billing por `kubectl port-forward`.
- **O link do Mercado Pago não expira.** O Billing cancela o pagamento após o prazo (`BILLING_PAGAMENTO_PRAZO`, 30 minutos). Se o cliente paga depois, o Billing estorna sozinho e grava em `estornos_avulsos`.
- **Estorno que falha.** Se o estorno de uma compensação falha depois de 3 entregas, a saga vai para `FALHOU`. A OS fica `AGUARDANDO_EXECUCAO` e um ADMIN age (ver README, seção Operação).
- **Estorno tardio que falha.** O Billing audita a falha em `estornos_avulsos` e responde erro ao webhook. O Mercado Pago repete o aviso. Se a falha continuar, um operador estorna no painel do Mercado Pago.
- **Mensagem na DLQ.** Um alerta do New Relic avisa. O reprocessamento é manual.
