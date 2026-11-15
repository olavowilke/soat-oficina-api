# Roteiro do vídeo da Fase 4

Limite do vídeo: 15 minutos. Os seis blocos somam exatamente 15:00.

| Bloco | Tema | Tempo |
|---|---|---|
| 1 | Arquitetura e escolha da Saga | 2:00 |
| 2 | `demo-saga.sh feliz` com status ao vivo | 3:00 |
| 3 | `demo-saga.sh falha-execucao` e compensação | 3:00 |
| 4 | Um PR num serviço: CI, Sonar, merge, CD no EKS | 4:00 |
| 5 | New Relic: trace, dashboard da saga, logs por `sagaId` | 2:00 |
| 6 | Encerramento | 1:00 |
| | **Total** | **15:00** |

Não há folga. No ensaio, use o plano de corte no fim.

## Checklist antes de gravar

- [ ] `DEPLOY_HOMOLOG=true` nos repositórios. Os três CDs precisam rodar.
- [ ] Ambiente de homolog no ar: EKS, RDS OS, RDS Billing, DynamoDB e as três filas.
- [ ] `kubectl get pods -A` mostra os pods de `oficina`, `billing` e `execucao` em `Running`.
- [ ] `kubectl` aponta para o cluster de homolog. `jq` está instalado.
- [ ] Credenciais AWS ativas, para o `kubectl` e para o SSM: rode `aws sso login` (ou `aws configure`).
- [ ] Variáveis do admin definidas: `export ADMIN_USERNAME=... ADMIN_PASSWORD=...`.
- [ ] Opcional: `export API=<url do gateway>`. Sem ela, o script lê o SSM.
- [ ] Billing em modo fake (`BILLING_MERCADOPAGO_MODO=fake`). O modo real usa o sandbox do Mercado Pago e o webhook. Ele precisa de credenciais.
- [ ] Flag `simulacao-falha` desligada. Envie por port-forward `PUT /api/execucao/admin/simulacao-falha {"ativa":false}`. Não existe `GET` para ela.
- [ ] `SONAR_TOKEN` e `vars.SONAR_ORGANIZATION` definidos. A organização no SonarCloud existe (passo manual).
- [ ] `NVD_API_KEY` definido, para o job OWASP.
- [ ] `main` protegida em todos os repositórios (rulesets existem).
- [ ] Filas e DLQs vazias. Nenhuma saga presa.
- [ ] `./scripts/demo-saga.sh feliz` rodou uma vez antes, sem erro.
- [ ] New Relic aberto nas abas da seção "Abas para abrir antes de gravar".
- [ ] Um PR preparado no Billing. Mudança pequena, como uma mensagem de log. CI verde em teste prévio.
- [ ] Abas abertas: todas as da seção "Abas para abrir antes de gravar".
- [ ] Terminal com fonte grande. Notificações do sistema desligadas.
- [ ] Segredos fora da tela: tokens, chaves AWS, credenciais do Mercado Pago.

## Abas para abrir antes de gravar

Abra as abas nesta ordem. Assim a ordem das abas segue a ordem dos blocos.

| Bloco | Página | Link |
|---|---|---|
| 1 | Diagrama da arquitetura | https://github.com/olavowilke/soat-oficina-api/blob/main/docs/arquitetura/fase4-arquitetura.png |
| 4 | PR do Billing | https://github.com/olavowilke/soat-billing-service/pull/3 |
| 4 | Gate do Sonar no PR | https://sonarcloud.io/summary/new_code?id=olavowilke_soat-billing-service&pullRequest=3 |
| 4 | Actions do Billing (CD) | https://github.com/olavowilke/soat-billing-service/actions |
| 5 | Trace distribuído | https://one.newrelic.com/distributed-tracing?account=8499367 |
| 5 | Dashboard "Oficina - Saga" | https://one.newrelic.com/dashboards/detail/ODQ5OTM2N3xWSVp8REFTSEJPQVJEfGRhOjEzNDk3MDUw?account=8499367 |
| 5 | Busca de logs | https://one.newrelic.com/logger?account=8499367 |
| 5 | Alertas (DLQ) | https://one.newrelic.com/alerts?account=8499367 |
| 6 | Repositórios | https://github.com/olavowilke?tab=repositories&q=soat |

O dashboard "Oficina - Saga" tem as abas "Logs por sagaId" e "Traces distribuidos". Elas servem de reserva se a busca de logs ou o trace demorarem.

Faça login no GitHub, no SonarCloud e no New Relic antes de gravar.

## Bloco 1 — Arquitetura e escolha da Saga (2:00)

**Na tela:** `docs/arquitetura/fase4-arquitetura.png`. Aponte cada parte quando falar dela.

**Comandos:** nenhum.

**Texto para ler:**

Olá. Este é o vídeo da Fase 4 do Tech Challenge.

Esta é a arquitetura da Fase 4. Dividimos a oficina em três serviços: OS, Billing e Execution.

*(Aponte os três pods.)*

Cada serviço tem o próprio repositório e o próprio banco. A OS usa um RDS Postgres. O Billing usa outro RDS Postgres, e o Execution usa DynamoDB.

*(Aponte a entrada.)*

O cliente entra pelo API Gateway. Uma Lambda valida o CPF do cliente. Um VPC link leva a chamada até um NLB.

O NLB tem três listeners, um por serviço. Os pods rodam num único cluster EKS, em três namespaces.

*(Aponte as filas.)*

Os serviços conversam por SQS. São três filas, e cada uma tem a sua DLQ.

Para coordenar o fluxo, escolhemos uma Saga orquestrada. A OS já era dona da ordem de serviço e do status, então ela orquestra. Com coreografia, a lógica do fluxo ficaria espalhada por três repositórios.

Todo passo que pode falhar tem uma compensação.

O pagamento acontece na aprovação do orçamento, antes da execução. O Billing cobra pelo Mercado Pago. No homolog, ele usa um adaptador fake.

*(Aponte o New Relic.)*

Por fim, os três serviços mandam métricas, logs e traces para o New Relic.

**Se falhar:** se a imagem não abrir, use o arquivo `docs/arquitetura/fase4-arquitetura.png` direto do repositório. Fale o mesmo texto.

## Bloco 2 — `demo-saga.sh feliz` com status ao vivo (3:00)

**Na tela:** terminal em tela cheia.

**Comandos:**

```bash
./scripts/demo-saga.sh feliz
```

O script abre os port-forwards, faz login de admin, cria a OS e roda o fluxo.
Ele mostra o status da OS e o estado da saga a cada passo.

**Texto para ler:**

Agora vamos rodar o caminho feliz da saga.

*(Rode o comando. Fale enquanto a saída aparece.)*

O script abre um port-forward para cada serviço. A equipe interna fala direto com os pods, sem passar pelo gateway.

Primeiro, o admin faz login e cria a ordem de serviço. Em seguida, ele envia a OS para orçamento, e a saga começa.

O Billing gera o orçamento, e a OS passa para aguardando aprovação.

O cliente aprova o orçamento pelo gateway, com o token do CPF. O Billing cria o pagamento, e a OS passa para aguardando pagamento.

A página de pagamento fake aprova, e o Billing confirma o pagamento. Com o Mercado Pago real, essa confirmação chega por webhook.

A OS passa para aguardando execução e, logo depois, para em execução.

O mecânico avança as etapas: em reparo, em teste e concluída.

*(Mostre o fim da saída.)*

No fim, a OS está finalizada, e a saga está concluída. O estoque da peça caiu de 10 para 8, porque a OS usou duas peças.

**Se falhar:**

- Se o port-forward cair, rode o script de novo. Ele recria os túneis.
- Se a aprovação fake falhar, confira se o Billing está em modo fake. Rode o script outra vez.
- Se a saga travar, mostre o estado com `GET /api/ordens-servico/{id}/saga` e use a gravação do ensaio.

## Bloco 3 — `demo-saga.sh falha-execucao` e compensação (3:00)

**Na tela:** terminal. Ao lado, o diagrama.

**Comandos:**

```bash
./scripts/demo-saga.sh falha-execucao
```

O script liga a flag com `PUT /api/execucao/admin/simulacao-falha {"ativa":true}`.
Ele roda o fluxo até o pagamento. Execution rejeita o trabalho. No fim, o script desliga a flag.

**Texto para ler:**

Agora vamos ver o que acontece quando um passo falha.

*(Rode o comando.)*

O script liga uma flag de falha no Execution. A flag rejeita só o próximo trabalho.

O cliente aprova e paga, como no caminho feliz.

Quando o comando chega, o Execution rejeita o trabalho com o motivo simulação.

A OS recebe a rejeição e começa a compensação. A saga passa para estornando.

O Billing estorna o pagamento e avisa a OS. Depois, a OS devolve as peças ao estoque.

*(Mostre o fim da saída.)*

A OS termina cancelada, e a saga termina compensada. O estoque voltou para 10, o mesmo valor do início.

No fim, o script desliga a flag. Cada passo da compensação é idempotente, então uma mensagem repetida não estorna duas vezes.

*(Copie o sagaId da saída. Você vai usar no bloco 5.)*

**Se falhar:**

- Se a flag não ligar, confira o port-forward do Execution. Rode o `PUT` à mão.
- Se o estorno demorar, espere 30 segundos. Mostre a saga em `ESTORNANDO` e explique o passo.
- Depois de qualquer falha, desligue a flag: `PUT /api/execucao/admin/simulacao-falha {"ativa":false}`.

## Bloco 4 — Um PR num serviço: CI, Sonar, merge, CD (4:00)

**Na tela:** PR do Billing no GitHub, depois SonarCloud, depois Actions, depois o terminal.

**Comandos:**

```bash
gh pr checks 3 --repo olavowilke/soat-billing-service --watch
gh pr merge 3 --repo olavowilke/soat-billing-service --squash
gh run watch --repo olavowilke/soat-billing-service
kubectl -n billing rollout status deploy/billing-service
kubectl -n billing get pods
```

O PR é o [#3 do Billing](https://github.com/olavowilke/soat-billing-service/pull/3). Não faça o merge antes de gravar.

**Texto para ler:**

Agora vamos ver o caminho de uma mudança até o EKS.

*(Abra o PR do Billing.)*

Este PR muda uma mensagem de log do Billing. A branch main é protegida, então nenhuma mudança entra sem PR.

O PR só libera o merge com o CI verde.

O CI compila, roda os testes de unidade e de integração e mede a cobertura com o JaCoCo. Depois, ele roda a análise do SonarCloud.

*(Abra a aba do SonarCloud.)*

O quality gate do Sonar passou. Se o gate falhar, o CI falha junto, e o merge fica bloqueado.

*(Leia a cobertura que aparece na tela.)*

*(Volte ao PR.)*

Há também um job do OWASP dependency-check. Ele aponta CVEs nas dependências, mas não é obrigatório. Ele avisa e não bloqueia o merge.

Agora eu faço o merge.

*(Rode o gh pr merge. Depois abra a aba do Actions.)*

O merge dispara o CD sozinho. O CD gera a imagem, publica no ECR e aplica no EKS.

*(Rode o rollout status e o get pods.)*

O rollout terminou, e os pods novos estão rodando.

Cada serviço tem o próprio pipeline. Um deploy do Billing não toca nos outros serviços.

**Se falhar:**

- Se o CI demorar, grave o PR já com os checks verdes. Pule a espera na edição.
- Se o gate do Sonar falhar, mostre a falha. Diga que o merge fica bloqueado. Troque para o PR reserva.
- Se o CD falhar, mostre o último deploy verde em Actions e o `kubectl get pods`.

## Bloco 5 — New Relic (2:00)

**Na tela:** New Relic. Aba 1: trace distribuído. Aba 2: dashboard da saga. Aba 3: logs.

**Comandos:** nenhum. Use o `sagaId` mostrado no fim do bloco 3.

**Texto para ler:**

Por fim, a observabilidade no New Relic.

*(Abra a aba do trace distribuído. Escolha um trace com três serviços.)*

Este trace cruza a OS, as filas, o Billing e o Execution.

O cabeçalho do New Relic viaja nos atributos de cada mensagem da fila. Por isso, o trace não quebra quando passa pelo SQS.

*(Abra o dashboard "Oficina - Saga".)*

Este é o dashboard da saga. Ele mostra as sagas por estado, os resultados por hora, as mensagens em cada DLQ e os spans de cada serviço.

*(Abra a aba de alertas.)*

Um alerta vigia as DLQs. Se uma mensagem fica parada numa DLQ por cinco minutos, o alerta dispara.

*(Abra a aba "Logs por sagaId". Filtre pelo sagaId do bloco 3.)*

Aqui filtramos os logs pelo sagaId. Os três serviços aparecem na mesma busca.

Os logs são JSON, e cada linha traz o correlationId e o sagaId.

**Se falhar:**

- Se o trace não aparecer, rode `./scripts/demo-saga.sh feliz` e espere 1 minuto.
- Se o dashboard vier vazio, troque o período para "última hora".
- Se nada aparecer, mostre as capturas de tela do ensaio.

## Bloco 6 — Encerramento (1:00)

**Na tela:** diagrama da arquitetura. Depois a lista de repositórios.

**Comandos:** nenhum.

**Texto para ler:**

Para fechar, um resumo.

*(Mostre o diagrama.)*

Dividimos a oficina em três serviços, cada um com o próprio banco. Eles coordenam o fluxo por uma Saga orquestrada, sobre filas com DLQ.

Mostramos o caminho feliz e a compensação de ponta a ponta. Também mostramos o CI, o Sonar, o CD e a observabilidade.

*(Abra a lista de repositórios.)*

O código está em seis repositórios: os três serviços, a infra do Kubernetes, a infra do banco e a Lambda de autenticação. Cada repositório tem um README com a documentação.

Obrigado.

**Se falhar:** não há ação ao vivo. Se faltar tempo, pule a lista de repositórios.

## Plano de corte

Se o ensaio passar de 15:00, corte nesta ordem:

1. Bloco 4: grave o PR já com checks verdes e pule a espera.
2. Bloco 2: mostre só o início e o fim da saída.
3. Bloco 5: mostre só o trace e os logs.

## Ensaio

Faça o ensaio com cronômetro quando a AWS estiver no ar. Preencha a tabela.

| Bloco | Tempo previsto | Tempo real | Observação |
|---|---|---|---|
| 1 Arquitetura e Saga | 2:00 | | |
| 2 `demo-saga.sh feliz` | 3:00 | | |
| 3 `demo-saga.sh falha-execucao` | 3:00 | | |
| 4 PR, CI, Sonar, CD | 4:00 | | |
| 5 New Relic | 2:00 | | |
| 6 Encerramento | 1:00 | | |
| **Total** | **15:00** | | |
