# CI/CD

Na Fase 4 há seis repositórios. Cada um tem um par de workflows GitHub Actions:
`ci.yml` valida o código em PR e `cd.yml` faz o deploy.
Só existe o ambiente `homolog`. Um push em `main` implanta em homolog quando a variável `DEPLOY_HOMOLOG` vale `true`.

## Repositórios × workflows × gatilhos

| Repositório | Workflow | Gatilho | O que faz |
|---|---|---|---|
| `soat-lambda-auth` | `ci.yml` | `push` e `pull_request` | Typecheck, testes, cobertura, build do bundle (Node 22) e `terraform fmt`/`validate` |
| `soat-lambda-auth` | `cd.yml` | `push` em `main` ou `workflow_dispatch` | Empacota a Lambda, roda `terraform apply` e testa `/auth/token` |
| `soat-infra-k8s` | `ci.yml` | `push` e `pull_request` | `terraform fmt` e `validate` |
| `soat-infra-k8s` | `cd.yml` | `push` em `main` ou `workflow_dispatch` | `terraform apply`: VPC, EKS, NLB, ECR, add-ons e New Relic |
| `soat-infra-database` | `ci.yml` | `push` e `pull_request` | `terraform fmt` e `validate` |
| `soat-infra-database` | `cd.yml` | `push` em `main` ou `workflow_dispatch` | `terraform apply`: RDS, Secrets Manager e SSM |
| `soat-oficina-api` | `ci.yml` | `push` e `pull_request` | `mvn verify` (JUnit, Testcontainers, ArchUnit, JaCoCo 80%), validação do kustomize, Sonar, job `owasp` e `terraform fmt`/`validate` |
| `soat-oficina-api` | `cd.yml` | `push` em `main` ou `workflow_dispatch` | `terraform apply` (fila de respostas e role do pod), build e push da imagem, `kubectl apply` dos manifestos, `rollout status` e teste de saúde pelo gateway |
| `soat-billing-service` | `ci.yml` | `push` e `pull_request` | `mvn verify` (JaCoCo 80%), kustomize, Sonar, job `owasp` e `terraform fmt`/`validate` |
| `soat-billing-service` | `cd.yml` | `push` em `main` ou `workflow_dispatch` | `terraform apply` (fila, RDS, ECR, rotas do gateway), imagem, `kubectl apply`, `rollout status` e teste de saúde |
| `soat-execution-service` | `ci.yml` | `push` e `pull_request` | `mvn verify` (JaCoCo 80%), Sonar, job `owasp`, `terraform fmt`/`validate` e kustomize |
| `soat-execution-service` | `cd.yml` | `push` em `main` ou `workflow_dispatch` | `terraform apply` (fila, DynamoDB, ECR, rotas do gateway), imagem, `kubectl apply`, `rollout status` e teste de saúde |

> O CI dos três serviços Java tem o passo Sonar e o job `owasp`.
> O passo Sonar só roda com `SONAR_TOKEN`. Sem o token, ele é pulado sem aviso.
> O job `owasp` precisa de `NVD_API_KEY`. Sem a chave, ele imprime um aviso.

## Dono dos manifestos Kubernetes

Na Fase 3, `soat-infra-k8s` era dono dos manifestos da `oficina-api` e `soat-oficina-api` só trocava a imagem.
Na Fase 4 isso mudou. Cada serviço é dono dos próprios manifestos, na pasta `k8s/`.

| Repositório | É dono de |
|---|---|
| `soat-infra-k8s` | Cluster, rede, NLB, ECR da OS, add-ons (`metrics-server`, `nri-bundle`) e dashboards e alertas do New Relic |
| `soat-oficina-api` | `k8s/` da OS (namespace `oficina`), fila `os-saga-replies`, role do pod e Secret `oficina-secrets` |
| `soat-billing-service` | `k8s/` do Billing (namespace `billing`), RDS, fila, ECR, rotas do gateway e Secret `billing-secrets` |
| `soat-execution-service` | `k8s/` do Execution (namespace `execucao`), DynamoDB, fila, ECR, rotas do gateway e Secret `execution-service-secrets` |

Cada `cd.yml` fixa a imagem `sha-<git sha>` com `kustomize edit set image` antes do `kubectl apply`.
Isso gera um só rollout, sem imagem provisória. O `soat-infra-k8s` não aplica mais nenhum manifesto de aplicação.

## Ordem de apply entre stacks

```
infra-k8s  →  infra-database  →  lambda-auth  →  serviços (oficina-api, billing-service, execution-service)
```

Por quê, nessa ordem exata:

1. **`infra-k8s` primeiro** — não depende de nenhum outro stack. Cria a VPC,
   as sub-redes, o cluster EKS, o NLB interno, o VPC Link e o repositório
   ECR, e publica tudo isso em SSM (`infra-k8s/terraform/ssm.tf`) para os
   próximos dois stacks consumirem (ADR-0005).
2. **`infra-database` em segundo** — lê a VPC e as sub-redes privadas de
   `infra-k8s` via SSM (`data "aws_ssm_parameter"` em
   `infra-database/terraform/data.tf`) para criar o `aws_db_subnet_group`.
   Publica host/porta/nome do banco e o ARN do segredo de credenciais para
   `lambda-auth` e para a pipeline de `infra-k8s` (que monta o Secret
   `oficina-secrets`) consumirem.
3. **`lambda-auth` em terceiro** — lê rede e Security Group de `infra-k8s` e
   host/porta/segredo do banco de `infra-database` (ambos via SSM) para
   colocar a Lambda de autenticação na mesma VPC/SG dos nós do EKS e
   permitir que ela consulte o RDS. Cria também o API Gateway, o Lambda
   Authorizer e o segredo JWT compartilhado — só depois de rede e banco
   existirem é que a Lambda tem para onde se conectar.
4. **Serviços por último** (`oficina-api`, `billing-service`, `execution-service`) — cada pipeline
   roda o Terraform do próprio serviço (fila, banco, role do pod, rotas do gateway),
   publica a imagem no ECR e aplica os manifestos de `k8s/`.
   Os serviços precisam do cluster, do NLB e do gateway prontos, e leem esses dados no SSM.
   O Billing e o Execution criam o listener do NLB (8081 e 8082) e as rotas deles no gateway.
   O `cd.yml` de cada um cria o Secret do pod com dados do SSM e do Secrets Manager.
   O teste final chama a rota de saúde pelo API Gateway.

Inverter essa ordem faz o `apply` falhar cedo e de forma legível: um
`data "aws_ssm_parameter"` sem o parâmetro correspondente publicado ainda
não gera um estado inconsistente, só um erro de "parameter not found" no
`plan`.

## Regras de proteção de branch

Cada repositório protege a branch `main` com o ruleset `protecao-main`:

- PR obrigatório para chegar em `main`, sem push direto. O ruleset pede zero aprovações.
- O check `build-and-test` é obrigatório e estrito. O PR precisa estar atualizado com `main`.
- Os jobs `owasp` e `Terraform fmt e validate` são informativos. Eles rodam, mas não bloqueiam o merge.
- Force-push e exclusão da branch ficam bloqueados.
- Nenhum *bypass* existe.

O script `scripts/proteger-branches.sh <repo> <check>...`, em `soat-infra-k8s`, cria ou atualiza o ruleset.
Ele usa `gh api`. Rodar de novo atualiza o ruleset, sem duplicar.

## Secrets e variables exigidos

Configurados no GitHub, no repositório ou no Environment `homolog`.

| Nome | Tipo | Usado por | Para quê |
|---|---|---|---|
| `AWS_DEPLOY_ROLE_ARN` | Secret | todos os `cd.yml` | Role assumida por OIDC. Não existe chave de acesso estática |
| `TF_STATE_BUCKET` | Secret | todos os `cd.yml` | Bucket S3 do estado do Terraform |
| `TF_LOCK_TABLE` | Secret | todos os `cd.yml` | Tabela DynamoDB de lock do estado |
| `DEPLOY_HOMOLOG` | Variable | `oficina-api`, `billing-service`, `execution-service` | Precisa ser `true` para o deploy rodar |
| `AWS_REGION` | Variable | todos os `cd.yml` | Região AWS. Sem ela, o padrão é `us-east-1` |
| `NEW_RELIC_LICENSE_KEY` | Secret | `infra-k8s`, `oficina-api`, `billing-service`, `execution-service` | Chave do agente e do `nri-bundle` |
| `NEW_RELIC_API_KEY` | Secret | `infra-k8s` | Chave do provider NerdGraph (dashboards e alertas) |
| `NEW_RELIC_ACCOUNT_ID` | Variable | `infra-k8s` | Conta onde dashboards e alertas são criados |
| `UPTIME_CHECK_URL` | Variable | `infra-k8s` | URL do monitor sintético. Só existe depois do primeiro apply de `lambda-auth` |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | Secrets | `oficina-api` | Login de ADMIN do Secret `oficina-secrets` |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Secrets | `oficina-api` | Credenciais SMTP do e-mail de status |
| `SONAR_TOKEN` | Secret | `oficina-api`, `billing-service`, `execution-service` | Token do SonarCloud. Sem ele, o passo Sonar é pulado |
| `SONAR_ORGANIZATION` | Variable | `oficina-api`, `billing-service`, `execution-service` | Organização do SonarCloud |
| `NVD_API_KEY` | Secret | `oficina-api`, `billing-service`, `execution-service` | Chave da base NVD para o job `owasp`. Sem ela, o job é pulado |

O Mercado Pago do Billing não usa secret do GitHub. O Terraform cria o segredo
`oficina/homolog/billing/mercadopago` no Secrets Manager. Preencha `access_token` e `webhook_secret` nele.
O ConfigMap do Billing fixa `BILLING_MERCADOPAGO_MODO=fake`. Para usar o sandbox, preencha o segredo e mude o modo para `real`.

Os recursos do New Relic são opcionais. Sem `NEW_RELIC_API_KEY` e `NEW_RELIC_ACCOUNT_ID`,
o Terraform deixa dashboards e alertas com `count = 0`.
