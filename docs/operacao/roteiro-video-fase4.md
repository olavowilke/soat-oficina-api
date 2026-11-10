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
- [ ] Mercado Pago em sandbox. Comprador de teste pronto.
- [ ] Flag `simulacao-falha` desligada. Confira por port-forward em `GET /api/execucao/admin/simulacao-falha`.
- [ ] Filas e DLQs vazias. Nenhuma saga presa.
- [ ] `./scripts/demo-saga.sh feliz` rodou uma vez antes, sem erro.
- [ ] New Relic aberto em três abas: trace distribuído, dashboard da saga, busca de logs.
- [ ] Um PR preparado no Billing. Mudança pequena, como uma mensagem de log. CI verde em teste prévio.
- [ ] Abas abertas: PR no GitHub, SonarCloud, Actions e `docs/arquitetura/fase4-arquitetura.png`.
- [ ] Terminal com fonte grande. Notificações do sistema desligadas.
- [ ] Segredos fora da tela: tokens, chaves AWS, credenciais do Mercado Pago.

## Bloco 1 — Arquitetura e escolha da Saga (2:00)

**Na tela:** `docs/arquitetura/fase4-arquitetura.png`. Aponte cada parte quando falar dela.

**Comandos:** nenhum.

**Falar:**

- São três serviços: OS (`oficina-api`), Billing e Execution. Cada um tem repositório e banco próprios.
- OS usa RDS Postgres. Billing usa outro RDS Postgres. Execution usa DynamoDB.
- O cliente entra pelo API Gateway. A Lambda valida o CPF. O VPC link leva ao NLB.
- O NLB tem três listeners, um por serviço. Os pods rodam no EKS, em três namespaces.
- Os serviços falam por SQS. São três filas, cada uma com DLQ.
- Escolhemos Saga orquestrada. O OS já é dono da OS e do status.
- Coreografia espalharia a lógica do fluxo por três repositórios.
- Todo passo que pode falhar tem compensação.
- O pagamento acontece na aprovação, antes da execução. Billing fala com o Mercado Pago.
- O New Relic recebe métricas, logs e traces dos três serviços.

**Se falhar:** se a imagem não abrir, use o diagrama do `README.md`. Fale o mesmo texto.

## Bloco 2 — `demo-saga.sh feliz` com status ao vivo (3:00)

**Na tela:** terminal em tela cheia.

**Comandos:**

```bash
./scripts/demo-saga.sh feliz
```

O script abre os port-forwards, faz login de admin, cria a OS e roda o fluxo.
Ele mostra o status da OS e o estado da saga a cada passo.

**Falar:**

- O script abre port-forward para os três serviços. A equipe interna não passa pelo gateway.
- Ele cria a OS e envia o orçamento. A saga começa em `AGUARDANDO_ORCAMENTO`.
- Billing gera o orçamento. A OS vai para `AGUARDANDO_APROVACAO`.
- O cliente aprova. Billing cria a preferência no Mercado Pago. A OS vai para `AGUARDANDO_PAGAMENTO`.
- O pagamento sandbox chega pelo webhook. Billing confere o pagamento na API do Mercado Pago.
- A OS vai para `AGUARDANDO_EXECUCAO` e depois `EM_EXECUCAO`.
- O mecânico avança as etapas. A OS termina em `FINALIZADA`. A saga termina em `CONCLUIDA`.
- Mostre o fim da saída: OS `FINALIZADA`, saga `CONCLUIDA`.

**Se falhar:**

- Se o port-forward cair, rode o script de novo. Ele recria os túneis.
- Se o webhook demorar, mostre o link de pagamento. Diga que o sandbox atrasa. Rode o script outra vez.
- Se a saga travar, mostre o estado com `GET /api/ordens-servico/{id}` e use a gravação do ensaio.

## Bloco 3 — `demo-saga.sh falha-execucao` e compensação (3:00)

**Na tela:** terminal. Ao lado, o diagrama.

**Comandos:**

```bash
./scripts/demo-saga.sh falha-execucao
```

O script liga a flag com `PUT /api/execucao/admin/simulacao-falha {"ativa":true}`.
Ele roda o fluxo até o pagamento. Execution rejeita o trabalho. No fim, o script desliga a flag.

**Falar:**

- Ligamos a flag de falha no Execution. Ela rejeita só o próximo trabalho.
- O cliente aprova e paga, como no bloco anterior.
- Execution responde `ExecucaoRejeitada`, com motivo `SIMULACAO`.
- O OS começa a compensação. A saga vai para `ESTORNANDO`.
- Billing estorna o pagamento no Mercado Pago. Responde `PagamentoEstornado`.
- O OS devolve as peças ao estoque. A OS fica `CANCELADA`. A saga fica `COMPENSADA`.
- Mostre o estoque antes e depois. Ele volta ao valor inicial.
- A flag volta a ficar desligada. Cada passo da compensação é idempotente.

**Se falhar:**

- Se a flag não ligar, confira o port-forward do Execution. Rode o `PUT` à mão.
- Se o estorno demorar, espere 30 segundos. Mostre a saga em `ESTORNANDO` e explique o passo.
- Depois de qualquer falha, desligue a flag: `PUT /api/execucao/admin/simulacao-falha {"ativa":false}`.

## Bloco 4 — Um PR num serviço: CI, Sonar, merge, CD (4:00)

**Na tela:** PR do Billing no GitHub, depois SonarCloud, depois Actions, depois o terminal.

**Comandos:**

```bash
gh pr checks <numero> --repo <org>/<repo-billing> --watch
gh pr merge <numero> --repo <org>/<repo-billing> --squash
gh run watch --repo <org>/<repo-billing>
kubectl -n billing rollout status deploy/billing
kubectl -n billing get pods
```

Troque `<org>`, `<repo-billing>` e `<numero>` antes de gravar.

**Falar:**

- A `main` tem proteção. O PR exige CI e Sonar verdes.
- O CI roda build, testes unitários e de integração, cobertura JaCoCo e OWASP.
- Mostre o gate do SonarCloud aprovado. Cite a cobertura.
- Mostre que o botão de merge só libera com os checks verdes.
- Faça o merge. O CD começa sozinho.
- O CD builda a imagem, publica e aplica no EKS.
- Mostre o `rollout status` e os pods novos em `Running`.
- Cada serviço tem seu pipeline. Um deploy não toca nos outros.

**Se falhar:**

- Se o CI demorar, grave o PR já com os checks verdes. Pule a espera na edição.
- Se o gate do Sonar falhar, mostre a falha. Diga que o merge fica bloqueado. Troque para o PR reserva.
- Se o CD falhar, mostre o último deploy verde em Actions e o `kubectl get pods`.

## Bloco 5 — New Relic (2:00)

**Na tela:** New Relic. Aba 1: trace distribuído. Aba 2: dashboard da saga. Aba 3: logs.

**Comandos:** nenhum. Use o `sagaId` mostrado no fim do bloco 3.

**Falar:**

- Abra o trace distribuído. Ele cruza OS, SQS, Billing e Execution.
- O cabeçalho `newrelic` viaja nos atributos da mensagem. Por isso o trace não quebra na fila.
- Abra o dashboard da saga: sagas por estado, taxa de compensação, tempo por passo e profundidade das DLQs.
- Um alerta vigia a profundidade das DLQs.
- Abra os logs. Filtre por `sagaId`. Os três serviços aparecem na mesma busca.
- Os logs são JSON. Cada linha tem `correlationId` e `sagaId`.

**Se falhar:**

- Se o trace não aparecer, rode `./scripts/demo-saga.sh feliz` e espere 1 minuto.
- Se o dashboard vier vazio, troque o período para "última hora".
- Se nada aparecer, mostre as capturas de tela do ensaio.

## Bloco 6 — Encerramento (1:00)

**Na tela:** diagrama da arquitetura. Depois a lista de repositórios.

**Comandos:** nenhum.

**Falar:**

- Resumo: três serviços, três bancos, Saga orquestrada, filas com DLQ.
- Mostramos o caminho feliz e a compensação.
- Mostramos CI, Sonar, CD e observabilidade.
- Cite os repositórios e onde achar a documentação.
- Agradeça e encerre.

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
