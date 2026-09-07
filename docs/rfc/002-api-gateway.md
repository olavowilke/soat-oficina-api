# RFC-002 — Escolha do API Gateway

- **Status:** Aceito
- **Data:** 2026-08-25
- **Contexto do repositório:** Tech Challenge — Fase 3
- **Implementado em:** `lambda-auth/terraform/apigateway.tf`, `lambda.tf`, `data.tf`

## Contexto

O sistema precisa de uma porta de entrada única que: (1) exponha uma rota
pública para emissão de token por CPF; (2) proteja as rotas de negócio com
um mecanismo de autenticação plugável; (3) encaminhe o tráfego autorizado
para o cluster EKS sem expor o cluster diretamente à internet. O gateway
precisa falar com uma Lambda (`AWS_PROXY`) e com um NLB privado dentro da
VPC (`VPC_LINK`/`HTTP_PROXY`).

## Opções consideradas

### 1. Amazon API Gateway — HTTP API (v2) (escolhida)

**Prós**
- Custo por milhão de requisições e por GB de dados significativamente
  menor que a REST API (frequentemente citado como ~70% mais barato para o
  mesmo volume).
- Suporta `VPC_LINK` para NLB privado e Lambda Authorizer do tipo
  `REQUEST` com cache de resultado — exatamente os dois mecanismos usados
  aqui.
- Payload format v2 mais enxuto, latência de invocação menor que a REST
  API na prática.
- CORS declarativo nativo (`cors_configuration`), sem precisar de método
  `OPTIONS` manual por rota.

**Contras**
- Menos recursos de governança de API que a REST API: sem API Keys nativas,
  sem *usage plans*/quotas por cliente, sem *request/response mapping
  templates* completos.
- Menos madura em alguns recursos de validação de payload (JSON Schema)
  comparada à REST API.
- Nenhum suporte a WAF diretamente anexado (precisaria de CloudFront na
  frente para isso).

### 2. Amazon API Gateway — REST API (v1)

**Prós**
- Recurso mais completo: API Keys, usage plans, WAF anexável diretamente,
  *request/response mapping templates* com VTL, validação de schema JSON
  nativa.
- Suporte histórico mais longo — mais exemplos e *troubleshooting* de
  terceiros acumulados ao longo dos anos.

**Contras**
- Custo por requisição mais alto para o mesmo volume, sem benefício
  funcional relevante para este projeto (não precisamos de API Keys nem de
  usage plans agora).
- Payload/latência de integração maior; configuração de VPC Link também
  mais verbosa (precisa de um `NETWORK_LOAD_BALANCER` type explícito e
  integrações por método, não por rota agregada).
- Nenhuma vantagem de custo/operação que justifique a complexidade adicional
  neste estágio do projeto (YAGNI).

### 3. Kong (self-hosted ou Kong Konnect)

**Prós**
- Gateway agnóstico de nuvem: o mesmo desenho funcionaria em qualquer
  Kubernetes, incluindo on-premises — nenhum lock-in de provedor.
- Ecossistema de plugins maduro (rate limiting, autenticação JWT nativa,
  transformação de payload) sem precisar escrever uma Lambda Authorizer.
- Se o projeto crescer para múltiplos clusters/regiões, Kong tem
  recursos de *service mesh*/federação mais avançados que o API Gateway
  gerenciado.

**Contras**
- Precisa ser operado: outro Deployment no cluster (ou outro serviço
  gerenciado, com custo e configuração próprios), incluindo seu próprio
  banco de dados (Postgres ou Cassandra) para configuração no modo
  tradicional.
- Perde o TLS/DDoS/escala gerenciados automaticamente pelo API Gateway da
  AWS — passaria a ser responsabilidade da equipe operar isso.
- Mais uma peça de infraestrutura para manter disponível: se o Kong cair,
  cai o único ponto de entrada do sistema, e agora é um problema operacional
  nosso, não da AWS.

### 4. Traefik

**Prós**
- Leve, nativo de Kubernetes (Ingress Controller/Gateway API), configuração
  via CRDs, sem banco de dados externo para operar (ao contrário do Kong
  tradicional).
- Bom suporte a certificados automáticos (Let's Encrypt) e métricas
  Prometheus nativas.
- Roda dentro do próprio cluster, o que reduz o número de componentes
  externos ao Kubernetes.

**Contras**
- Rodar como Ingress Controller dentro do EKS reintroduz o problema que o
  desenho atual evita: o cluster precisaria de um Load Balancer público
  (ALB/NLB público) apontando para o Traefik, expondo a rede do cluster
  mais diretamente à internet do que o desenho com API Gateway + VPC Link
  privado.
- Autenticação por CPF/JWT precisaria ser reimplementada como middleware do
  Traefik (ou um serviço auxiliar), perdendo o encaixe nativo com Lambda que
  o API Gateway oferece.
- Nenhuma vantagem de custo clara sobre o HTTP API para o volume deste
  projeto, e adiciona outro componente para atualizar/observar/proteger.

## Decisão

**Amazon API Gateway — HTTP API (v2)**, com Lambda Authorizer (`REQUEST`,
cache de 300 s) para as rotas protegidas e integração `VPC_LINK` para o NLB
interno do EKS. É a opção com menor custo operacional e de infraestrutura
adicional, cobre exatamente os dois mecanismos que o projeto precisa
(invocar Lambda e alcançar um NLB privado) e não introduz nenhum componente
extra para manter disponível além do que a AWS já gerencia.

### Por que o API Gateway vive no repositório `lambda-auth`, não no `infra-k8s`

Esta é uma decisão que vale registrar com honestidade, porque à primeira
vista parece estranho o gateway — que roteia principalmente para o
cluster — estar no repositório da função serverless.

O motivo real é uma dependência circular **entre stacks Terraform**, não
dentro de um único recurso:

- O API Gateway precisa do **ARN de invocação da Lambda** de autenticação
  (`aws_lambda_function.auth.invoke_arn`) para criar a integração
  `AWS_PROXY` da rota `POST /auth/token` e para o Lambda Authorizer.
- `lambda-auth`, por sua vez, já depende de `infra-k8s` para existir:
  lê `vpc-link-id`, `nlb-listener-arn`, as sub-redes privadas e o Security
  Group `db-client` via SSM (ADR-0005) para colocar a Lambda na VPC certa.

Se o API Gateway vivesse em `infra-k8s`, o sentido da dependência se
inverteria e as duas stacks passariam a depender uma da outra ao mesmo
tempo: `infra-k8s` precisaria do `invoke_arn` publicado por `lambda-auth`,
e `lambda-auth` já precisa do VPC Link e da rede publicados por
`infra-k8s`. Isso é um ciclo genuíno entre dois stacks — nenhuma ordem de
`apply` o resolve, porque cada lado espera um parâmetro SSM que só o outro
publica. Mantendo o API Gateway no mesmo stack Terraform que a Lambda
(`lambda-auth`), a dependência passa a ter uma única direção:
`infra-k8s → infra-database → lambda-auth` (ADR-0005), e o `invoke_arn` é
uma referência local dentro do mesmo `terraform apply`, não um parâmetro
publicado entre stacks.

Dentro do próprio stack `lambda-auth` existe um problema menor e
independente: a permissão de invocação (`aws_lambda_permission`) do
Authorizer e da função de auth normalmente referenciaria o ID da API no seu
`source_arn`, mas a API só existe depois que a permissão é avaliada na
mesma ordem de criação — uma circularidade *intra-stack*, não a razão da
escolha do repositório. Isso é resolvido com um `source_arn` de
conta/região (`arn:aws:execute-api:<região>:<conta>:*/*`,
`lambda-auth/terraform/lambda.tf:79-93`), que dispensa a referência
cruzada ao ID da API. É um *trade-off* de segurança real, não gratuito:
qualquer API Gateway criado nessa conta e região poderia invocar a função,
não só a API deste projeto. O risco é limitado pelo IAM da conta (só quem
tem permissão de criar API Gateway nessa conta pode se beneficiar disso) e
pela política de recurso da Lambda ser a única linha de defesa nesse ponto
— se a conta hospedar outros API Gateways no futuro, vale revisar para um
`source_arn` restrito ao ID da API específica assim que ela existir de
forma estável.

## Consequências

**Positivas**
- Nenhuma dependência circular de Terraform entre stacks; a ordem de apply
  documentada (`infra-k8s → infra-database → lambda-auth`) é suficiente
  para tudo resolver na primeira tentativa.
- Custo e latência de gateway otimizados para o volume deste projeto.
- Autorização plugável (Lambda Authorizer) sem introduzir um componente
  adicional a operar dentro do cluster.

**Negativas / custos**
- Quem olha o `infra-k8s` esperando encontrar o API Gateway (por ele rotear
  para o cluster) precisa saber que ele está em `lambda-auth` — documentado
  aqui e no README de cada stack para mitigar a surpresa.
- Recursos de gateway mais avançados (API Keys, usage plans, WAF nativo) não
  estão disponíveis nesta escolha; se precisarem no futuro, a migração para
  REST API ou para uma camada adicional (CloudFront + WAF na frente do HTTP
  API) é possível sem redesenhar o restante do sistema.
