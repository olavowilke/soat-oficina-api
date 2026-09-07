# ADR 0001 — Adoção de Clean Architecture canônica por bounded context

- **Status:** Aceito
- **Data:** 2026-07-07
- **Contexto:** Tech Challenge — Fase 2 (Bloco A, Etapa 1)
- **Referências:** [`design-docs/tech-challenge-2/clean-arch.md`](../../design-docs/tech-challenge-2/clean-arch.md), [`design-docs/tech-challenge-2/PLANEJAMENTO-FASE-2.md`](../../design-docs/tech-challenge-2/PLANEJAMENTO-FASE-2.md)

## Contexto

A Fase 1 entregou uma estrutura *Package-by-Feature* com DDD em camadas
(`domain` / `application` / `infrastructure` / `interfaces`) nos 6 bounded contexts
(`cliente`, `veiculo`, `servico`, `peca`, `ordemservico`, `auth`) + `shared`. A estrutura
já tem sabor hexagonal: interfaces `*Repository` no `domain` (ports) implementadas por
`*RepositoryImpl` no `infrastructure` (adapters), com entidades de domínio limpas e mappers
para as entidades JPA.

A Fase 2 exige a evolução para **Clean Architecture canônica**, tornando explícitas as
camadas **Controllers, UseCases, Entities, Presenters e Gateways**.

## Decisão

Adotamos a Clean Architecture "de manual", mantendo o *Package-by-Feature*. Cada bounded
context passa a expor explicitamente as camadas da arquitetura como **subpacotes**.

### A regra de ouro

> As dependências apontam **sempre para dentro**. O núcleo (`entities`, `usecases`) não
> conhece Spring, JPA nem HTTP.

```
Frameworks & Drivers (Spring MVC, Spring Data JPA, Flyway, JavaMailSender)
        ↓ (entrada)                                   ↑ (saída)
   Controller  ──>  Use Case  ──>  Entity  ──>  Use Case  ──>  Presenter  ──>  Controller
   (traduz DTO)     (orquestra,     (regra de      (resultado)   (monta o        (devolve HTTP)
                    fala c/ Gateway  negócio                      Response/
                    via interface)   pura)                        ViewModel)
                         ↓
                      Gateway (interface no núcleo)
                         ↓
                      Gateway Impl (Infra: adapta JPA/HTTP/fila)
```

### Estrutura de pacotes alvo por bounded context

```
br.com.oficina.<contexto>/
├── entities/        # Entidade de domínio + Value Objects — regra de negócio pura (sem Spring/JPA)
├── usecases/        # Um Use Case por operação; fala com Gateways via interface
├── gateways/        # Interfaces (Ports) — antes "*Repository" no domain
├── presenters/      # Monta o *Response/ViewModel a partir da saída do Use Case
├── controllers/     # Recebe DTO de entrada (Request), chama Use Case, devolve via Presenter
└── infrastructure/  # Frameworks & Drivers: *Data (JPA), *GatewayImpl, config, mappers
```

## Convenções de nomenclatura

| Camada | Padrão de nome | Exemplo | Observações |
|---|---|---|---|
| Entities | `<Substantivo>`, VOs próprios | `Servico`, `Documento`, `Placa` | POJO puro. Fábricas estáticas `novo(...)` / `reconstituir(...)`. Sem anotação de framework. |
| Gateways | `<Contexto>Gateway` | `ServicoGateway` | Interface no núcleo. Métodos falam em **entidades de domínio**, nunca em `*Data`/DTO. |
| Use Cases | `<Verbo><Substantivo>UseCase` | `CadastrarServicoUseCase`, `ListarServicosUseCase` | Um por operação. Método público único `execute(...)`. Entrada via `*Command` (record); saída = entidade de domínio (ou tipo primitivo). |
| Commands | `<Verbo><Substantivo>Command` | `CadastrarServicoCommand` | `record` imutável no pacote `usecases`. DTO de aplicação, sem anotação de validação HTTP. |
| Presenters | `<Contexto>Presenter` | `ServicoPresenter` | Converte saída do Use Case → `*Response`. Sem dependência de `ResponseEntity`/HTTP. |
| ViewModels | `<Contexto>Response` | `ServicoResponse` | `record` no pacote `presenters`. |
| Controllers | `<Contexto>Controller` | `ServicoController` | Adapter Spring MVC. Traduz `Request` → `Command`, chama Use Case, retorna `presenter.present(...)`. |
| Requests | `<Verbo><Substantivo>Request` | `CadastrarServicoRequest` | `record` no pacote `controllers`, com validação Bean Validation. |
| Persistência | `<Contexto>Data` | `ServicoData` | Entidade JPA no `infrastructure`. |
| Spring Data | `<Contexto>JpaRepository` | `ServicoJpaRepository` | `interface extends JpaRepository`. |
| Adapter do Gateway | `<Contexto>GatewayImpl` | `ServicoGatewayImpl` | `implements <Contexto>Gateway`. Traduz domínio ↔ `*Data` via mapper. |
| Mapper | `<Contexto>Mapper` | `ServicoMapper` | Estático, `toData(...)` / `toDomain(...)`. |

## Diretrizes de pragmatismo (do material)

- **Gateways vs. Repositories:** as interfaces `*Repository` do `domain` já cumprem o papel
  de Gateway. Renomeamos para `*Gateway` para tornar a nomenclatura explícita e alinhada à
  Clean Architecture. A implementação no Infra retorna **entidades de domínio**.
- **Modelo JPA separado (`*Data`):** só se justifica quando o ORM vaza (`@Id`, `@Entity`,
  proxies lazy) na regra de negócio. Como já mantemos entidade de domínio limpa + entidade
  JPA separada + mapper, **mantemos e renomeamos** `*Entity` → `*Data`.
- **Use Cases como beans Spring:** por pragmatismo, cada Use Case é anotado com `@Service` e
  `@Transactional` (stereotype/transaction do Spring são concessões aceitas). O que o teste de
  arquitetura **proíbe** no núcleo é o acoplamento a **JPA** (`jakarta.persistence`) e a **web**
  (`org.springframework.web`), além de dependências para fora (`controllers`, `presenters`,
  `infrastructure`).
- **Controller + Presenter separados:** cada adapter com sua responsabilidade. O Controller
  não formata `Response`; delega ao Presenter.
- **CQRS como Use Case:** cada Command/Query + Handler é um Use Case explícito.

> **Nota:** o que importa é a **direção das dependências** e a **separação de
> responsabilidades**. Defender as escolhas com base nos princípios já atende — não é preciso
> purismo absoluto.

## Consequências

**Positivas**
- Núcleo testável de forma isolada (Use Cases com Gateways mockados).
- Regra de dependência verificável automaticamente (ArchUnit — ver `ArchitectureTest`).
- Fronteiras explícitas facilitam a evolução das APIs (Bloco B) sem tocar no núcleo.

**Negativas / custos**
- Mais classes por operação (um Use Case por caso de uso).
- Refatoração incremental: a Fase 2 migra contexto a contexto (piloto `servico` →
  contextos de suporte → core `ordemservico`), convivendo temporariamente com a estrutura antiga.

## Plano de adoção

1. **Piloto:** `servico`, refatorado ponta a ponta como referência (Etapa 1).
2. **Contextos de suporte:** `cliente`, `veiculo`, `peca`, `auth` (Etapa 2).
3. **Core:** `ordemservico`, preservando a máquina de estados (Etapa 3).
4. **Consolidação:** ArchUnit cobrindo todos os contextos + Clean Code + cobertura ≥ 80% (Etapa 4).
