# CI/CD

Cada um dos 4 repositórios (hoje, diretórios autocontidos deste monorepo —
ver ADR-0004) tem seu próprio par de workflows GitHub Actions: `ci.yml`
(validação em PR e push) e `cd.yml` (deploy automático). Todos os `cd.yml`
resolvem o ambiente alvo (`homolog` ou `prod`) pela mesma expressão:
`github.event.inputs.ambiente || (github.ref_name == 'main' && 'prod' ||
'homolog')` — ou seja, push em `main` implanta em `prod`, push em `homolog`
implanta em `homolog`, e `workflow_dispatch` permite escolher manualmente.
Esse nome também é usado como GitHub Environment (`environment: ...`), o que
permite escopar secrets/variables e regras de proteção por ambiente no
GitHub, além dos secrets globais do repositório.

## Repositórios × workflows × gatilhos × ambiente

| Repositório | Workflow | Gatilho | O que faz | Ambiente |
|---|---|---|---|---|
| `lambda-auth` | `ci.yml` | `push` (`main`, `homolog`), `pull_request` (opened/synchronize/reopened) | Typecheck + testes + cobertura + build do bundle (Node 22) e `terraform fmt`/`validate` | — (só validação) |
| `lambda-auth` | `cd.yml` | `push` (`main` → prod, `homolog` → homolog), `workflow_dispatch` (escolhe ambiente) | Build/empacota a Lambda, `terraform apply`, smoke test em `/auth/token` (espera `400` para CPF inválido) | `homolog` / `prod` |
| `infra-k8s` | `ci.yml` | `push` (`main`, `homolog`), `pull_request` | `terraform fmt`/`validate` + `kubectl kustomize` dos overlays `homolog` e `prod` (falha se algum patch for inválido) | — (só validação) |
| `infra-k8s` | `cd.yml` | `push` (`main`/`homolog`), `workflow_dispatch` (ambiente + tag de imagem opcional) | `terraform apply` (VPC, EKS, API Gateway, NLB, New Relic), cria/atualiza o Secret `oficina-secrets` a partir de SSM/Secrets Manager, lê a imagem hoje em execução no `Deployment` (ou cai para `<ECR>:<ambiente>` se ele ainda não existir — ver nota abaixo) e a injeta de volta antes de `kubectl apply -k`, `rollout status` | `homolog` / `prod` |
| `infra-database` | `ci.yml` | `push` (`main`, `homolog`), `pull_request` | `terraform fmt`/`validate` | — (só validação) |
| `infra-database` | `cd.yml` | `push` (`main`/`homolog`), `workflow_dispatch` (ambiente) | `terraform apply` (RDS, Secrets Manager, parâmetros SSM), resumo do endpoint no Job Summary | `homolog` / `prod` |
| `oficina-api` | `ci.yml` | `push` (`main`), `pull_request` | `mvn verify` (JUnit + Testcontainers + ArchUnit) e upload do relatório JaCoCo | — (só validação) |
| `oficina-api` | `cd.yml` | `push` (`main`/`homolog`), `workflow_dispatch` (ambiente) | Build e push da imagem Docker no ECR (tags `sha-<git sha>` e `<ambiente>`), `kubectl set image` no `Deployment` já existente (sem kustomize, sem overlay — ver nota abaixo), `rollout status`, smoke test em `/api/actuator/health` via API Gateway | `homolog` / `prod` |

> `oficina-api/ci.yml` só dispara `push` em `main` (não em `homolog`) — os
> demais três repositórios validam push tanto em `main` quanto em `homolog`.
> Um workflow adicional, `claude-review.yml`, roda revisão automatizada de
> PR nesta raiz do monorepo; não faz parte do caminho de deploy e não está
> na tabela acima.
>
> **Divisão de dono entre `infra-k8s` e `oficina-api` (ajuste da Tarefa 8):**
> `oficina-api/cd.yml` não lê nem aplica nenhum manifesto/overlay kustomize —
> depois do split, `infra-k8s/k8s/` simplesmente não existe dentro do
> checkout de `soat-oficina-api`. `infra-k8s` é dono do estado desejado dos
> manifestos (réplicas, HPA, PDB etc.) e quem os aplica (`kubectl apply -k`);
> `oficina-api` é dono só da imagem e troca a tag do `Deployment` já
> existente via `kubectl set image`, sem tocar em mais nenhum campo do
> recurso. Essa divisão é o que torna as duas pipelines seguras para rodar
> de forma independente uma da outra — com uma ressalva que já causou um bug
> real: `infra-k8s/k8s/base/kustomization.yaml` chegou a fixar
> `images[].newTag: latest`, uma tag que a pipeline de `oficina-api` nunca
> publica (ela só publica `sha-<sha>` e `<ambiente>`). Cada `kubectl apply -k`
> de `infra-k8s` reaplicaria essa tag inexistente por cima da imagem real que
> `oficina-api` acabou de publicar, desfazendo o deploy mais recente. A
> correção: `infra-k8s/cd.yml` agora lê a imagem hoje em execução no
> `Deployment` (`kubectl get deployment ... -o jsonpath=...`) e a injeta de
> volta via `kustomize edit set image` antes do `apply` — o apply se torna
> um no-op para a imagem. No *bootstrap* (`Deployment` ainda não existe
> neste ambiente), cai para `<ECR>:<ambiente>` com aviso explícito no log;
> qualquer outra falha do `kubectl get` (não um "not found" genuíno) aborta
> o job em vez de arriscar um fallback silencioso sobre um erro real de
> conectividade/permissão — a mesma classe de bug que motivou esta correção.
> `images:` não tem mais tag estática nenhuma em `kustomization.yaml`.

## Ordem de apply entre stacks

```
infra-k8s  →  infra-database  →  lambda-auth  →  aplicação (oficina-api)
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
4. **Aplicação (`oficina-api`) por último** — sua pipeline não roda
   Terraform nem conhece manifestos Kubernetes; ela só publica a imagem no
   ECR (criado por `infra-k8s`) e troca a tag do `Deployment` já existente
   (aplicado por `infra-k8s/cd.yml`) via `kubectl set image`. Precisa que o
   cluster, o Secret `oficina-secrets` (com `DB_URL`/`JWT_SECRET`) e o
   endpoint do API Gateway já existam para o smoke test final (`curl` no
   endpoint via API Gateway) fazer sentido.

Inverter essa ordem faz o `apply` falhar cedo e de forma legível: um
`data "aws_ssm_parameter"` sem o parâmetro correspondente publicado ainda
não gera um estado inconsistente, só um erro de "parameter not found" no
`plan`.

## Regras de proteção de branch

Cada um dos 4 repositórios protege a branch `main`:

- Pull request obrigatório para chegar em `main` (sem push direto), com pelo
  menos uma aprovação exigida antes do merge.
- **Todos** os status checks de CI relevantes obrigatórios — não só o
  primeiro job de `ci.yml`. `lambda-auth` e `infra-k8s` têm dois jobs de CI
  cada (`node`+`terraform`, e `terraform`+`kustomize`, respectivamente); os
  dois são exigidos em ambos, senão um PR poderia mergear com um dos dois
  jobs quebrado.
- Force-push bloqueado.
- Exclusão da branch bloqueada.

Essas regras são aplicadas como um *ruleset* do GitHub (`gh api
repos/<owner>/<repo>/rulesets`, nome `protecao-main`) por
`scripts/split-repos.sh` no momento em que cada diretório é publicado como
repositório independente — o mesmo ruleset para os quatro, e reaplicado
(`PATCH`, não duplicado) se o script rodar de novo sobre um repositório que
já o tem. O mesmo script também adiciona o colaborador `soat-architecture`
com permissão de leitura (`pull`) em cada um dos 4 repositórios e confirma,
ao final, que ele aparece como colaborador nos 4 — a exigência da spec de
"confirmar que o usuário foi adicionado". Nada disso roda quando o script é
chamado com `--dry-run`: só nesse caso é seguro dizer que nenhuma escrita
acontece no GitHub.

> O nome de cada *status check* exigido é inferido do `name:` (ou do id) de
> cada job de CI relevante, lido a partir de cada `ci.yml` — não foi
> confirmado contra uma execução real do GitHub Actions, porque os 4
> repositórios ainda não foram publicados. Se algum nome não bater
> exatamente com o que o Actions reporta na primeira execução real, o merge
> fica bloqueado esperando um check que nunca chega; corrige-se ajustando o
> mapa `CI_CHECK_NAMES` no início do script.

**O próprio script obedece a proteção que instala.** Só é seguro empurrar
`main` direto na 1ª publicação de cada repositório — antes do ruleset
existir. Em qualquer execução seguinte (repositório já publicado, `main` já
protegida por uma execução anterior), `scripts/split-repos.sh` não tenta
`push` em `main`: ele empurra o snapshot novo para uma branch descartável e
abre um Pull Request (`gh pr create --base main`), deixando o merge para
uma pessoa revisar e aprovar — nenhum *bypass actor* foi adicionado ao
ruleset só para permitir push automático; isso esvaziaria a proteção que a
spec pede para demonstrar. `homolog` não é alvo do ruleset (só `main` é
protegida) e continua recebendo push forçado direto a cada execução.

Enquanto os quatro "repositórios" existirem como diretórios deste monorepo
(antes do split), a proteção de branch vigente é a deste próprio repositório
(`main`), não uma por diretório. Essa proteção de branch, uma vez aplicada,
é uma configuração da plataforma GitHub — não fica visível neste
repositório, só no `ruleset` de cada repositório publicado.

## Secrets e variables exigidos

Configurados no GitHub (repositório e/ou por Environment `homolog`/`prod`)
para as pipelines de CD funcionarem:

| Nome | Tipo | Usado por | Para quê |
|---|---|---|---|
| `AWS_DEPLOY_ROLE_ARN` | Secret | `lambda-auth`, `infra-k8s`, `infra-database`, `oficina-api` (todos os `cd.yml`) | Role assumida via OIDC (`aws-actions/configure-aws-credentials`) — nenhuma chave de acesso estática |
| `TF_STATE_BUCKET` | Secret | `infra-k8s`, `infra-database`, `lambda-auth` (`cd.yml`, `terraform init`) | Bucket S3 do backend remoto do Terraform |
| `TF_LOCK_TABLE` | Secret | `infra-k8s`, `infra-database`, `lambda-auth` (`cd.yml`, `terraform init`) | Tabela DynamoDB de lock do state |
| `NEW_RELIC_LICENSE_KEY` | Secret | `infra-k8s` (`cd.yml`, Terraform e Secret `oficina-secrets`) | Chave de licença do agente/infra do New Relic (`nri-bundle`, agente Java) |
| `NEW_RELIC_API_KEY` | Secret | `infra-k8s` (`cd.yml`, Terraform) | API key do provider NerdGraph (dashboard, política e condições de alerta) |
| `NEW_RELIC_ACCOUNT_ID` | Variable | `infra-k8s` (`cd.yml`, Terraform) | Conta New Relic onde dashboard/alertas são criados |
| `UPTIME_CHECK_URL` | Variable | `infra-k8s` (`cd.yml`, Terraform) | URL pública checada pelo monitor sintético (preenchida só depois do primeiro apply de `lambda-auth`, que publica o endpoint do API Gateway) |
| `AWS_REGION` | Variable | todos os `cd.yml` | Região AWS alvo (default `us-east-1` se ausente) |
| `WEBHOOK_ORCAMENTO_TOKEN`, `MAIL_USERNAME`, `MAIL_PASSWORD` | Secrets | `infra-k8s` (`cd.yml`, monta o Secret `oficina-secrets`) | Credenciais do webhook de orçamento e do SMTP de notificação — não são publicadas por nenhuma stack Terraform, vêm direto de secrets do GitHub |

Os New Relic (`NEW_RELIC_ACCOUNT_ID`/`NEW_RELIC_API_KEY`/`UPTIME_CHECK_URL`)
e o synthetic monitor são opcionais por design: as variáveis correspondentes
no Terraform têm default vazio e os recursos ficam com `count = 0` até
serem preenchidas (`infra-k8s/terraform/newrelic.tf`), então o `plan`/`apply`
funciona mesmo sem esses secrets configurados — só sem os recursos do New
Relic que dependem deles.
