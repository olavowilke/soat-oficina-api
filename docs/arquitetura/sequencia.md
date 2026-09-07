# Diagramas de sequência

## a) Autenticação por CPF

`POST /auth/token` é a única rota pública que emite credencial. A Lambda
(`lambda-auth/src/handler.ts`) valida o CPF, consulta o cliente no RDS e, se
tudo estiver certo, busca o segredo HS256 no Secrets Manager para assinar o
JWT. Os três ramos de erro (`400`, `404`, `403`) e o ramo de falha interna
(`500`) existem tal como no código.

```mermaid
sequenceDiagram
    actor Cliente
    participant GW as API Gateway
    participant LA as Lambda auth
    participant DB as RDS PostgreSQL
    participant SM as Secrets Manager

    Cliente->>GW: POST /auth/token {"cpf": "..."}
    GW->>LA: invoke (payload v2, AWS_PROXY)
    LA->>LA: normaliza e valida o CPF (dígitos verificadores)

    alt CPF ausente ou inválido
        LA-->>GW: 400 Bad Request
        GW-->>Cliente: 400 Bad Request
    else CPF válido — consulta o cliente
        LA->>DB: SELECT id, nome, ativo FROM clientes WHERE documento = cpf AND tipo_documento = 'CPF'
        alt cliente não encontrado
            DB-->>LA: 0 linhas
            LA-->>GW: 404 Not Found
            GW-->>Cliente: 404 Not Found
        else cliente inativo
            DB-->>LA: linha (ativo = false)
            LA-->>GW: 403 Forbidden
            GW-->>Cliente: 403 Forbidden
        else cliente ativo
            DB-->>LA: linha (id, nome, ativo = true)
            LA->>SM: GetSecretValue (segredo JWT — cacheado em memória entre invocações)
            SM-->>LA: segredo HS256
            LA->>LA: gera JWT (sub=cpf, clienteId, nome, role=CLIENTE, iss=oficina-auth, aud=oficina-api, exp)
            LA-->>GW: 200 OK {accessToken, tokenType, expiresIn, expiresAt, cliente}
            GW-->>Cliente: 200 OK
        end
    end

    Note over LA: Qualquer exceção não tratada nos passos acima<br/>(ex.: RDS indisponível) cai no catch genérico do handler
    LA-->>GW: 500 Internal Server Error (fallback)
    GW-->>Cliente: 500 Internal Server Error (fallback)
```

O `x-correlation-id` devolvido em todas as respostas vem do cabeçalho
`x-correlation-id` recebido, do `requestId` do API Gateway
(`evento.requestContext.requestId`) ou do `awsRequestId` do contexto Lambda,
nessa ordem de preferência (`lambda-auth/src/handler.ts`, `extrairCorrelationId`).

## b) Abertura de ordem de serviço (com autorização e notificação de status)

Rota protegida: `POST /api/ordens-servico/abertura`, atrás do Lambda Authorizer
(`REQUEST`, TTL de cache de 300 s). A abertura em si é transacional — cliente,
veículo, serviços e peças são resolvidos e a OS nasce com status `RECEBIDA`
— e só **registra a métrica de criação**. A notificação por e-mail só ocorre
numa transição de status posterior (`AvancarStatusUseCase` /
`NotificadorStatusOrdem`), porque `RECEBIDA` não está no conjunto de status
notificáveis (`ordemservico/usecases/NotificadorStatusOrdem.java`). O
diagrama mostra as duas interações em sequência para cobrir o ciclo completo.

```mermaid
sequenceDiagram
    actor Cliente
    participant GW as API Gateway
    participant AUTHZ as Lambda Authorizer
    participant NLB as NLB interno
    participant API as oficina-api (pod)
    participant DB as RDS PostgreSQL
    participant SMTP as Servidor de e-mail
    participant NR as New Relic

    rect rgb(235, 245, 255)
    Note over Cliente,NR: 1) Abertura da OS
    Cliente->>GW: POST /api/ordens-servico/abertura (Authorization: Bearer JWT)

    alt resultado do authorizer em cache (dentro dos 300s)
        GW->>GW: reusa o context (clienteId, cpf, nome, role) da chamada anterior
    else cache expirado ou primeira chamada
        GW->>AUTHZ: invoke REQUEST authorizer (header Authorization)
        AUTHZ->>AUTHZ: verificarToken — HS256, valida iss/aud/exp
        alt token ausente ou inválido
            AUTHZ-->>GW: isAuthorized = false
            GW-->>Cliente: 403 Forbidden
        else token válido
            AUTHZ-->>GW: isAuthorized = true, context {clienteId, cpf, nome, role}
            GW->>GW: cacheia o resultado por authorizer_result_ttl_in_seconds (300s)
        end
    end

    GW->>NLB: HTTP_PROXY via VPC Link<br/>(overwrite header x-correlation-id=$context.requestId,<br/>x-cliente-id=$context.authorizer.clienteId)
    NLB->>API: encaminha para o Service oficina-api (NodePort 30080)
    API->>API: JwtAuthenticationFilter revalida o JWT localmente (defesa em profundidade — mesmo segredo HS256, via Secrets Manager)
    API->>API: AbrirOrdemServicoUseCase — valida cliente e veículo, adiciona serviços/peças, reserva estoque
    API->>DB: INSERT ordens_servico + itens_servico + itens_peca (transação única)
    DB-->>API: OS persistida (status = RECEBIDA)
    API->>NR: Micrometer counter oficina.ordens.criadas{origem=api}
    API-->>GW: 201 Created {id, status, itens, orçamento}
    GW-->>Cliente: 201 Created
    end

    rect rgb(255, 245, 230)
    Note over Cliente,NR: 2) Mudança de status subsequente (ex.: RECEBIDA -> EM_DIAGNOSTICO -> ... -> AGUARDANDO_APROVACAO)
    Cliente->>GW: PATCH /api/ordens-servico/{id}/status (Bearer JWT)
    GW->>NLB: HTTP_PROXY via VPC Link (autorização conforme acima)
    NLB->>API: encaminha para o Service oficina-api
    API->>API: AvancarStatusUseCase — valida transição permitida, atualiza status
    API->>DB: UPDATE ordens_servico
    DB-->>API: status atualizado
    API->>NR: Micrometer timer oficina.ordens.tempo.status{status=anterior}
    API->>API: NotificadorStatusOrdem.notificar (status no conjunto notificável?)

    alt status notificável (AGUARDANDO_APROVACAO, EM_EXECUCAO, FINALIZADA, ENTREGUE, CANCELADA) e e-mail habilitado
        API->>SMTP: envia e-mail de atualização (JavaMailSender)
        alt envio ok
            SMTP-->>API: ok
        else falha no envio
            API->>NR: Micrometer counter oficina.integracoes.falhas{integracao=email}
            Note over API: exceção capturada em NotificadorStatusOrdem — não quebra a transação já commitada
        end
    else status não notificável ou sem e-mail cadastrado
        API->>API: log informativo, nenhuma notificação enviada
    end

    API-->>GW: 200 OK {id, novo status}
    GW-->>Cliente: 200 OK
    end
```

Notas:

- O JWT chega intacto ao pod: o API Gateway não o transforma, apenas injeta
  `x-correlation-id` e `x-cliente-id` como cabeçalhos adicionais
  (`request_parameters` em `lambda-auth/terraform/apigateway.tf`).
- A revalidação do JWT pela aplicação (`JwtAuthenticationFilter` +
  `JwtService`) é defesa em profundidade, não uma segunda autenticação
  independente: usa o mesmo segredo HS256, obtido do Secrets Manager via
  variável de ambiente do Deployment.
- Se qualquer item da abertura falhar (ex.: estoque insuficiente de uma
  peça), a exceção reverte a transação inteira — nenhuma OS parcial é
  persistida (`AbrirOrdemServicoUseCase`, `@Transactional`).
- Uma falha ao persistir (não ao notificar) incrementa
  `oficina.ordens.falhas`, consumida pela condição de alerta
  `falha_processamento_os` (`infra-k8s/terraform/newrelic.tf`).
