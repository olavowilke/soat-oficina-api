# RFC-001 — Escolha do provedor de nuvem

- **Status:** Aceito
- **Data:** 2026-08-25
- **Contexto do repositório:** Tech Challenge — Fase 3

## Contexto

A Fase 3 exige operar o sistema como uma arquitetura de nuvem completa:
gateway de API, função serverless de autenticação, cluster Kubernetes com
autoscaling e banco de dados gerenciado. A especificação do curso nomeia
explicitamente os serviços a usar (API Gateway, function serverless, EKS,
RDS), o que já inclina fortemente a escolha para a AWS — mas a decisão
formal e as alternativas reais são registradas aqui.

Restrição prática adicional: o ambiente de desenvolvimento não tem
credenciais de nuvem instaladas (o Terraform é validado localmente com
`-backend=false` e aplicado só pela pipeline de CI/CD, quando os secrets
existirem no repositório). Qualquer provedor escolhido precisa ter cobertura
de camada gratuita/educacional razoável para não gerar custo antes mesmo da
demonstração.

## Opções consideradas

### 1. AWS (escolhida)

**Prós**
- Mapeamento direto com os serviços nomeados pela especificação: API
  Gateway (HTTP API), Lambda, EKS, RDS — sem precisar "traduzir" requisitos
  para equivalentes de outro provedor.
- Maior ecossistema Terraform do mercado: os módulos oficiais usados aqui
  (`terraform-aws-modules/vpc/aws`, `terraform-aws-modules/eks/aws`) são
  maduros, bem documentados e amplamente testados em produção por terceiros.
- Cobertura de Free Tier / AWS Academy conhecida pela equipe, reduzindo o
  risco de custo inesperado durante o desenvolvimento e a avaliação.
- Autenticação Lambda ↔ API Gateway ↔ EKS é um caminho de primeira classe
  (Lambda Authorizer nativo do API Gateway, VPC Link para NLB privado).

**Contras**
- IAM da AWS tem curva de aprendizado mais alta que o RBAC mais simples do
  GCP para o mesmo tipo de permissão.
- Custo do control plane do EKS (fixo, por hora) existe independente do
  tamanho do cluster — GKE tem um free tier de control plane em contas
  elegíveis.
- Cold start de Lambda em runtimes gerenciados tende a ser um pouco maior
  que o de Cloud Functions/Cloud Run no mesmo perfil de memória (mitigado
  aqui com `arm64`/Graviton e pacote pequeno).

### 2. Google Cloud Platform (GCP)

**Prós**
- GKE é frequentemente citado como a experiência gerenciada de Kubernetes
  mais madura do mercado (o próprio Kubernetes nasceu no Google), com
  Autopilot reduzindo ainda mais a operação de nós.
- Cloud Functions/Cloud Run têm cold start historicamente menor que Lambda
  para cargas Node.js pequenas.
- Modelo de billing por segundo e sustained-use discounts tornam o custo
  mais previsível sem precisar de Savings Plans.
- Cloud SQL for PostgreSQL é wire-compatible e igualmente maduro para o
  caso de uso relacional deste projeto.

**Contras**
- Nenhum serviço equivalente ao "Lambda Authorizer" plugado nativamente no
  API Gateway do jeito que a AWS oferece — o equivalente (Cloud Endpoints +
  Cloud Functions, ou Apigee) tem uma curva de configuração maior para este
  desenho específico.
- Menor cobertura de exemplos/Terraform prontos para o desenho exato
  exigido (gateway + authorizer + VPC Link + NLB privado) — mais trabalho
  de adaptação sem ganho funcional.
- A equipe tem menos familiaridade operacional com a IAM e billing do GCP,
  o que é um risco real de atraso num projeto com prazo fixo.

### 3. Microsoft Azure

**Prós**
- Azure API Management é um gateway maduro, com políticas declarativas
  (XML/OpenAPI) e um catálogo de features de segurança maior que o HTTP API
  da AWS "out of the box".
- AKS integra nativamente com Azure AD para autenticação/RBAC, o que
  simplifica cenários corporativos com SSO.
- Bom suporte a .NET (fora de escopo aqui, mas relevante em geral) e forte
  presença em clientes enterprise já usando Microsoft 365/Active Directory.

**Contras**
- Azure Functions em Node.js tem uma configuração de binding mais verbosa
  que o handler direto de Lambda para o caso simples deste projeto (uma
  função HTTP síncrona).
- Azure Database for PostgreSQL Flexible Server é comparável ao RDS em
  capacidade, mas a integração com Key Vault + Managed Identity para os
  segredos exige mais peças móveis do que Secrets Manager + IAM role.
- Menor precedente de uso da equipe e da comunidade do curso com a
  combinação API Management + AKS + Azure Functions para este tipo de
  desenho.

## Decisão

**AWS.** A especificação da Fase 3 já nomeia os serviços gerenciados
esperados (API Gateway, function serverless, cluster Kubernetes, banco
gerenciado) em termos que mapeiam diretamente para API Gateway, Lambda, EKS
e RDS. Além do alinhamento com o enunciado, o ecossistema Terraform da AWS
para este desenho específico (VPC Link + NLB privado + Lambda Authorizer) é
o mais maduro entre os três, e a cobertura de Free Tier/AWS Academy reduz o
risco de custo durante o desenvolvimento.

## Consequências

**Positivas**
- Todo o Terraform do projeto (`infra-k8s`, `infra-database`, `lambda-auth`)
  usa os módulos oficiais da AWS, com boa cobertura de documentação e
  exemplos para troubleshooting.
- O desenho de rede (VPC privada, NLB interno, VPC Link) é possível sem
  peças adicionais de terceiros (ex.: sem precisar de um Ingress Controller
  externo).

**Negativas / custos**
- Lock-in de nomenclatura e de módulos Terraform específicos da AWS: migrar
  para outro provedor no futuro exigiria reescrever os três stacks, não
  apenas trocar variáveis.
- Dependência da disponibilidade de Free Tier/AWS Academy para manter o
  custo de demonstração próximo de zero.
