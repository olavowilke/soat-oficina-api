# RFC-003 — Estratégia de autenticação do cliente por CPF

- **Status:** Aceito
- **Data:** 2026-08-25
- **Contexto do repositório:** Tech Challenge — Fase 3
- **Implementado em:** `lambda-auth/src/handler.ts`, `jwt.ts`, `authorizer.ts`, `config.ts`

## Contexto

A especificação da Fase 3 exige uma *function* serverless que autentique o
cliente pelo CPF (sem senha) e devolva uma credencial usável nas chamadas
subsequentes às rotas protegidas. A credencial precisa ser verificável tanto
pelo Lambda Authorizer do API Gateway (para autorizar a requisição antes de
chegar ao cluster) quanto, opcionalmente, pela própria aplicação (defesa em
profundidade). O cliente não tem conta de usuário/senha tradicional — a
identidade é o CPF cadastrado e ativo no RDS.

## Opções consideradas

### 1. Lambda própria emitindo JWT HS256 com segredo compartilhado (escolhida)

**Prós**
- Controle total do fluxo: a Lambda decide exatamente o que constitui
  "autenticado" (CPF válido + cliente existente + `ativo = true`), sem
  depender do modelo de usuário de um serviço de identidade externo.
- HS256 com segredo simétrico é simples de operar: um único segredo no
  Secrets Manager, lido por três consumidores (Lambda auth, Lambda
  Authorizer, aplicação Spring) sem infraestrutura de chave pública/privada.
- Nenhum custo de MAU (*monthly active user*) — relevante para um projeto
  de demonstração/curso onde o número de "logins" de teste pode ser alto em
  relação ao tráfego real.
- Cold start pequeno: pacote Node.js enxuto (`jose`, `pg`), sem SDK pesado
  de um provedor de identidade.

**Contras**
- É código próprio para manter: validação de CPF, emissão e verificação de
  JWT, tratamento de erros — responsabilidade que um serviço gerenciado de
  identidade assumiria por nós.
- Segredo simétrico compartilhado entre três componentes é uma superfície
  de risco maior que um par de chaves assimétrico: qualquer vazamento do
  segredo permite forjar tokens em qualquer um dos três lugares que o
  possuem.
- Sem funcionalidades prontas de um IdP (MFA, recuperação de conta,
  federação social, rotação de sessão) — não são necessárias para este
  caso de uso (não há senha), mas seriam um ganho se o escopo crescesse.

### 2. Amazon Cognito (User Pool)

**Prós**
- Serviço gerenciado com integração nativa ao API Gateway (authorizer
  `COGNITO_USER_POOLS` sem precisar escrever uma Lambda Authorizer).
- Recursos prontos: MFA, recuperação de senha, federação com provedores
  sociais/SAML, políticas de senha configuráveis.
- Tokens JWT assinados com RS256 e rotação de chaves gerenciada
  automaticamente pela AWS (via JWKS), sem a equipe operar segredo nenhum.

**Contras**
- O modelo de usuário do Cognito é pensado para login com
  usuário/senha (ou federado) — o requisito aqui é "autenticar só pelo
  CPF, sem senha", o que exigiria contornar o fluxo padrão (ex.: um fluxo
  customizado com Lambda triggers de autenticação, ou pré-cadastrar cada
  cliente como um usuário do pool a cada novo registro no RDS) — complexidade
  adicional sem necessidade real, dado que o cadastro de clientes já vive no
  RDS da aplicação.
- Duplicaria a fonte de verdade da identidade do cliente: o cliente já
  existe como registro em `clientes` (RDS); ter também um usuário Cognito
  por cliente significa manter dois cadastros sincronizados.
- Custo por MAU acima de um patamar gratuito generoso, mas ainda assim uma
  variável de custo a mais para um projeto que já tem o cadastro de clientes
  resolvido no banco relacional.

### 3. JWT RS256 com JWKS (chave assimétrica própria, sem Cognito)

**Prós**
- Separação de responsabilidades mais forte que HS256: só quem **emite**
  token (a Lambda auth) precisa da chave privada; quem **verifica**
  (Authorizer, aplicação) só precisa da chave pública, publicada via
  endpoint JWKS. Um vazamento da chave pública não permite forjar tokens.
- Rotação de chave mais segura: dá para ter duas chaves ativas (atual +
  anterior) publicadas no JWKS ao mesmo tempo durante a transição, sem
  invalidar tokens em voo.
- Mesmo modelo usado por praticamente todo provedor de identidade do
  mercado (OIDC), o que facilita uma eventual migração futura para um IdP
  real.

**Contras**
- Mais peças para operar: geração e armazenamento seguro do par de chaves,
  um endpoint JWKS público (ou um bucket/CDN servindo o `.well-known`),
  versionamento de `kid` (key ID) nos tokens emitidos.
- Verificação por JWKS normalmente busca a chave pública por rede (com
  cache) — mais uma dependência externa na Lambda Authorizer, que hoje é
  propositalmente simples e sem I/O de rede fora do próprio Secrets
  Manager.
- Ganho de segurança real é menor neste desenho específico do que pareceria
  à primeira vista: os três consumidores do segredo HS256 (Lambda auth,
  Authorizer, aplicação) já são todos componentes controlados pelo mesmo
  time/pipeline — não há uma parte externa e não confiável que precisaria
  apenas da chave pública.

## Decisão

**Lambda própria + JWT HS256 com segredo compartilhado via Secrets
Manager.** O requisito é autenticar por CPF sem senha e sem cadastro de
usuário tradicional — o que não se encaixa bem no modelo padrão do Cognito
sem gambiarras. HS256 é suficiente porque todos os três consumidores do
segredo (Lambda auth, Lambda Authorizer, aplicação Spring) são componentes
do mesmo sistema, sob o mesmo controle de deploy; o ganho de isolar emissor
de verificador que o RS256/JWKS traria não se aplica aqui da mesma forma que
se aplicaria a um cenário com consumidores de terceiros.

## Consequências

**Positivas**
- Fluxo de autenticação inteiramente sob controle do time, sem exigir
  sincronizar um cadastro paralelo de usuários.
- Um único segredo, gerado uma vez pelo Terraform (`random_password`) e
  nunca versionado, resolve emissão e verificação nos três pontos que
  precisam dele.
- Testável localmente sem depender de infraestrutura de nuvem (o teste
  unitário e o servidor de desenvolvimento da Lambda usam o mesmo segredo
  via variável de ambiente).

**Negativas / custos**
- Rotação do segredo HS256 é um evento manual/coordenado: como os três
  consumidores leem o mesmo valor, trocar o segredo exige atualizar os três
  ao mesmo tempo (ou aceitar uma janela de tokens antigos inválidos).
- Se no futuro o sistema precisar aceitar chamadas de terceiros não
  confiáveis que só deveriam poder *verificar* tokens (nunca emiti-los), a
  migração para RS256 + JWKS (Opção 3) é o caminho natural — o formato de
  claims já usado (`sub`, `clienteId`, `nome`, `role`, `iss`, `aud`) se
  mantém compatível com essa evolução.
