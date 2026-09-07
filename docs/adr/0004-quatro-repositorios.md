# ADR 0004 — Quatro repositórios (lambda-auth, infra-k8s, infra-database, oficina-api)

- **Status:** Aceito
- **Data:** 2026-08-25
- **Contexto:** Tech Challenge — Fase 3
- **Referências:** `design-docs/tech-challenge-3/PLANEJAMENTO-FASE-3.md` (seção "Mapa dos 4 repositórios"), `docs/adr/0005-contrato-entre-stacks-via-ssm.md`

## Contexto

A especificação da Fase 3 exige que o sistema seja entregue como **4
repositórios distintos**, cada um com seu próprio pipeline de CI/CD e ciclo
de deploy: a aplicação, o cluster Kubernetes, o banco de dados gerenciado e
a função serverless de autenticação. Durante o desenvolvimento, os quatro
convivem como diretórios autocontidos neste monorepo
(`lambda-auth/`, `infra-k8s/`, `infra-database/`, `oficina-api/`), cada um
com seu próprio `.github/workflows/{ci,cd}.yml` e, quando aplicável, seu
próprio `terraform/` — já preparados para existir como repositório
independente sem reestruturação. Exceção: os workflows de `oficina-api`
ainda vivem em `.github/workflows/` na raiz do monorepo (não dentro de
`oficina-api/`) — `scripts/split-repos.sh` os copia para dentro do
diretório publicado no momento do split (ver `oficina-api/README.md`,
nota de organização do monorepo). A publicação como 4 repositórios GitHub
separados, com proteção de branch aplicada, é responsabilidade de um script
dedicado (ver Consequências).

## Decisão

Organizar o sistema em **4 repositórios**, um por responsabilidade:

| # | Repositório (diretório hoje) | Conteúdo | Pipeline |
|---|---|---|---|
| 1 | `lambda-auth` | Function serverless de autenticação por CPF, Lambda Authorizer, Terraform da própria função | `ci`: typecheck + testes + build do bundle + `terraform validate` · `cd`: `terraform apply` (homolog/prod) |
| 2 | `infra-k8s` | Terraform de VPC, EKS, API Gateway (rotas + VPC Link), addons (metrics-server, New Relic) e manifestos Kubernetes (kustomize) | `ci`: `terraform fmt`/`validate` + validação dos overlays kustomize · `cd`: `apply` + `kubectl apply -k` |
| 3 | `infra-database` | Terraform do RDS PostgreSQL, Secrets Manager, e a documentação do modelo de dados (ER + justificativa) | `ci`: `terraform fmt`/`validate` · `cd`: `apply` |
| 4 | `oficina-api` | Aplicação Spring Boot (Clean Architecture), observabilidade, métricas de negócio | `ci`: `mvn verify` + JaCoCo · `cd`: build de imagem → ECR → `kubectl set image` no `Deployment` já existente (não roda kustomize nem conhece manifestos de outro repositório — `infra-k8s` é quem aplica e é dono do estado desejado deles) |

Cada repositório é dono de um único domínio de infraestrutura ou aplicação
e publica/consome contrato apenas via SSM Parameter Store (ADR-0005), nunca
via `terraform_remote_state` cruzado.

## Consequências

**Positivas**
- Cada repositório tem um ciclo de vida e um pipeline independentes: um
  ajuste na Lambda de autenticação não dispara `plan`/`apply` do cluster ou
  do banco.
- Times/pessoas diferentes poderiam ser donos de repositórios diferentes
  sem conflito de permissão de push — a separação já reflete um limite
  natural de propriedade.
- Falha de CI num repositório (ex.: teste da aplicação quebrado) não bloqueia
  o deploy de outro (ex.: uma correção urgente no banco).

**Negativas / custos**
- Mudanças que atravessam mais de um domínio (ex.: uma nova claim no JWT
  que a aplicação também precisa validar) exigem coordenar um PR em mais de
  um repositório, sem uma transação atômica entre eles.
- A ordem de apply entre stacks (`infra-k8s → infra-database →
  lambda-auth`, ver `docs/arquitetura/cicd.md`) precisa ser conhecida e
  respeitada por quem opera — não é imposta automaticamente pelo Git em
  repositórios sem relação de dependência declarada entre si.
- A publicação como 4 repositórios GitHub de fato — cada um com seu próprio
  commit inicial (snapshot do diretório via `git archive`, sem reescrever o
  histórico do monorepo — ver comentário de cabeçalho do script), push e
  *ruleset* de proteção de branch (PR obrigatório com 1 aprovação, sem push
  direto, sem force-push, sem exclusão da branch, e um *status check*
  obrigatório do `ci.yml` correspondente) — é feita pelo script de split
  dedicado `scripts/split-repos.sh`, que aplica o mesmo ruleset aos quatro e
  também adiciona o colaborador `soat-architecture` (permissão de leitura)
  em cada um, confirmando ao final que ele foi de fato adicionado aos 4. O
  script é idempotente: na 1ª publicação de um repositório, empurra `main`
  direto (o ruleset ainda não existe); em qualquer execução seguinte, `main`
  já está protegida por essa mesma execução anterior, então o script abre um
  Pull Request em vez de tentar um push que o próprio ruleset recusaria —
  detalhado em `docs/arquitetura/cicd.md`, seção "Regras de proteção de
  branch". Só escreve no GitHub quando chamado sem `--dry-run` — quem
  decide publicar é sempre quem opera o script, nunca uma etapa automática.
  Enquanto esse script não roda, os quatro "repositórios" existem como
  diretórios autocontidos dentro deste monorepo, já estruturados para o
  split sem reorganização adicional. A proteção de branch resultante é
  aplicada na plataforma GitHub no momento da publicação — não fica visível
  neste repositório antes disso.
- O nome do *status check* exigido no ruleset de cada repositório é inferido
  do `name:` do job de CI relevante em cada `ci.yml` (ou do id do job,
  quando ele não define `name:`) — não foi confirmado contra uma execução
  real do GitHub Actions (os 4 repositórios ainda não foram publicados). Se
  o nome não bater exatamente com o que o Actions reporta na primeira
  execução real, o merge fica bloqueado esperando um check que nunca chega;
  isso é esperado e corrigido ajustando o mapa `CI_CHECK_NAMES` no início do
  script, não um defeito silencioso. Cada repositório lista **todos** os
  checks de CI que precisa passar (`infra-k8s`, por exemplo, exige tanto o
  job `terraform` quanto o job `kustomize` do seu `ci.yml`) — listar só um
  deixaria um PR mergear com o outro job quebrado.
