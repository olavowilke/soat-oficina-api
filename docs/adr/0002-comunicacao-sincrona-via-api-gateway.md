# ADR 0002 — Comunicação síncrona REST via API Gateway (sem fila neste momento)

- **Status:** Aceito
- **Data:** 2026-08-25
- **Contexto:** Tech Challenge — Fase 3
- **Referências:** [RFC-002](../rfc/002-api-gateway.md), `lambda-auth/terraform/apigateway.tf`, `infra-k8s/terraform/nlb.tf`

## Contexto

Toda comunicação entre o cliente externo e o sistema — emissão de token,
abertura de ordem de serviço, consulta de status, avanço de status — é
request/response síncrona: HTTP do cliente até o API Gateway, e HTTP (via
VPC Link + NLB) até os pods `oficina-api`. Não existe fila (SQS, EventBridge,
Kafka) nem processamento assíncrono em nenhum ponto do fluxo de negócio
principal. A única operação verdadeiramente assíncrona do sistema é o envio
de e-mail de notificação de status, e mesmo essa é feita in-process,
best-effort e sem retry automatizado (`NotificadorStatusOrdem` captura a
exceção e segue, sem enfileirar a tentativa).

## Decisão

Adotamos comunicação **síncrona ponta a ponta** para os fluxos de negócio
(autenticação, abertura de OS, consulta e avanço de status): Cliente → API
Gateway → (Lambda | VPC Link → NLB → pod `oficina-api`) → RDS → resposta
HTTP. Nenhuma fila foi introduzida nesta fase.

Razões para não introduzir fila agora:

- O volume e o padrão de acesso deste sistema (uma oficina, operações
  request/response de baixa latência esperada — ex.: abrir uma OS e já
  saber o orçamento) não têm um requisito de desacoplamento temporal que
  justifique uma fila: o cliente espera uma resposta imediata, não um
  "processamento em segundo plano".
- Introduzir uma fila (SQS/EventBridge) adicionaria infraestrutura,
  monitoramento e modos de falha (mensagens presas, *dead-letter queue*,
  reprocessamento) sem um problema real que resolvesse — seria
  complexidade especulativa (YAGNI) para este estágio do projeto.
- A única operação que teria benefício claro de ser assíncrona — o envio de
  e-mail — já é isolada da transação principal por design
  (`NotificadorStatusOrdem` roda depois do `save()` da OS e nunca propaga
  exceção para o Use Case chamador), o que cobre o requisito prático
  ("uma falha de e-mail não derruba a abertura/avanço da OS") sem precisar
  de uma fila de verdade.

## Consequências

**Positivas**
- Menos componentes de infraestrutura para provisionar, observar e manter
  disponíveis (nenhum broker de mensagens, nenhuma fila, nenhuma
  dead-letter queue).
- Rastreabilidade simples: cada requisição HTTP tem um `correlationId`
  único, visível do log de acesso do API Gateway até o log da aplicação —
  não há uma cadeia de mensagens assíncronas para correlacionar.
- Modelo mental mais simples para quem opera o sistema: uma falha aparece
  como um erro HTTP (ou uma métrica `oficina.ordens.falhas`/
  `oficina.integracoes.falhas`), não como uma mensagem presa numa fila.

**Negativas / custos**
- Falhas transitórias na notificação por e-mail (ex.: SMTP fora do ar por
  alguns minutos) não são reprocessadas automaticamente — o cliente
  simplesmente não recebe aquele e-mail específico, e a única evidência é a
  métrica `oficina.integracoes.falhas` e o log de erro.
- Se o volume de OS ou a necessidade de processamento em lote crescerem
  (ex.: notificações em massa, integração com um ERP externo lento), o
  desenho síncrono atual precisaria evoluir para incluir uma fila — decisão
  que fica em aberto para uma fase futura, não descartada permanentemente.
