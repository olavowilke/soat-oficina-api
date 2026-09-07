# ADR 0005 — Contrato entre stacks via SSM Parameter Store (sem remote state cruzado)

- **Status:** Aceito
- **Data:** 2026-08-25
- **Contexto:** Tech Challenge — Fase 3
- **Referências:** `infra-k8s/terraform/ssm.tf`, `infra-database/terraform/ssm.tf`, `lambda-auth/terraform/ssm.tf` e `data.tf`, `docs/arquitetura/cicd.md`

## Contexto

Os três stacks Terraform do sistema (`infra-k8s`, `infra-database`,
`lambda-auth`) precisam trocar informação entre si: `infra-database` precisa
da VPC e das sub-redes privadas criadas por `infra-k8s`; `lambda-auth`
precisa da VPC, das sub-redes, do Security Group dos nós, do VPC Link, do
listener do NLB (todos de `infra-k8s`) e do host/porta/segredo do banco
(de `infra-database`). Cada stack é um repositório e um estado Terraform
independentes (ADR-0004).

## Decisão

Cada stack **publica** os valores que outros precisam como
`aws_ssm_parameter` sob o prefixo `/oficina/<env>/...` (ex.:
`/oficina/prod/network/vpc-id`, `/oficina/prod/database/host`,
`/oficina/prod/gateway/endpoint`) e **consome** os valores de outros stacks
via `data "aws_ssm_parameter"`, nunca via `terraform_remote_state` apontando
para o state file de outro stack.

Isso implica uma **ordem de apply obrigatória**: `infra-k8s` primeiro (não
depende de nenhum outro stack), depois `infra-database` (lê VPC/sub-redes de
`infra-k8s`), depois `lambda-auth` (lê rede de `infra-k8s` e banco de
`infra-database`) — documentada em `docs/arquitetura/cicd.md`.

## Consequências

**Positivas**
- Nenhum stack precisa de permissão de leitura no bucket S3 de state de
  outro stack — `data "aws_ssm_parameter"` só exige permissão de leitura em
  SSM, uma superfície de IAM bem menor e mais fácil de auditar do que dar
  acesso cruzado a arquivos de state (que contêm segredos em texto claro
  quando não criptografados corretamente).
- Cada stack pode evoluir sua estrutura interna de recursos livremente —
  renomear um módulo, reorganizar recursos — sem quebrar os consumidores,
  desde que o **valor publicado** no parâmetro SSM continue estável. Com
  `terraform_remote_state`, um consumidor lê a estrutura de outputs inteira
  do state de outro stack, o que é mais frágil a mudanças internas.
- O contrato é explícito e legível: os arquivos `ssm.tf` de cada stack são,
  na prática, a "API pública" daquele stack — dá para saber o que um stack
  expõe sem precisar inspecionar o `terraform.tfstate` de outro.

**Negativas / custos**
- A ordem de apply vira uma regra operacional que precisa ser lembrada e
  respeitada manualmente (ou automatizada na pipeline) — não há verificação
  automática do Terraform impedindo alguém de rodar `lambda-auth` antes de
  `infra-database` existir; o `apply` simplesmente falharia ao tentar ler um
  parâmetro SSM inexistente.
- Um parâmetro SSM renomeado ou removido sem coordenação quebra o consumidor
  silenciosamente até o próximo `plan`/`apply` (que falha ao não encontrar o
  parâmetro) — não há um teste de contrato automatizado entre os três
  stacks além da própria execução do Terraform.
- Mais um recurso para gerenciar por stack (os parâmetros SSM em si), ainda
  que de custo desprezível no tier gratuito do SSM Standard.
