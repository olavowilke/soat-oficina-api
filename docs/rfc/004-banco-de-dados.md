# RFC-004 — Escolha do banco de dados gerenciado

- **Status:** Aceito
- **Data:** 2026-08-25
- **Contexto do repositório:** Tech Challenge — Fase 3
- **Implementado em:** `infra-database/terraform/rds.tf`
- **Justificativa formal e diagrama ER completos:** [`infra-database/docs/modelo-dados.md`](../../infra-database/docs/modelo-dados.md)

> Esta RFC resume, no formato padrão do repositório, a decisão que já está
> documentada em detalhe no link acima — que é a referência canônica para o
> ER e para a análise coluna a coluna. Não duplicamos o diagrama aqui.

## Contexto

A aplicação (`oficina-api`) já existe com um schema relacional normalizado
(8 tabelas: `clientes`, `veiculos`, `servicos`, `pecas`, `ordens_servico`,
`itens_servico`, `itens_peca`, `usuarios`), migrations versionadas via
Flyway (`V1`–`V7`, mais `V8` na Fase 3) e acesso via JPA/Hibernate. A Fase 3
exige um banco **gerenciado** na nuvem. O domínio é intrinsecamente
relacional: uma ordem de serviço agrega itens de serviço e de peça,
referencia cliente e veículo, e precisa ser escrita de forma atômica
(criar a ordem e seus itens numa única transação).

## Opções consideradas

### 1. Amazon RDS PostgreSQL (escolhida)

**Prós**
- Zero esforço de migração: mesmo dialeto SQL que a aplicação já usa em
  desenvolvimento (Testcontainers com Postgres) — nenhuma linha de
  JPA/Flyway muda.
- SQL completo, `JOIN`s nativos, integridade referencial com `FOREIGN KEY`
  e `CHECK` constraints — exatamente o que o schema já usa e depende.
- Custo previsível: dimensionamento vertical simples (`instance_class`) e
  armazenamento `gp3` com autoscaling (`allocated_storage` →
  `max_allocated_storage`), sem modelo de cobrança por unidade de
  capacidade abstrata.
- Multi-AZ configurável (`multi_az = true` em produção) cobre o requisito
  de alta disponibilidade sem operação manual de failover.

**Contras**
- Escala verticalmente até o limite da instância; escalar horizontalmente
  (réplicas de leitura, sharding) exige trabalho manual de configuração que
  não vem "de graça" como em serviços serverless de banco.
- Custo mínimo existe mesmo em ambientes ociosos (a instância roda 24/7),
  ao contrário de opções verdadeiramente serverless.

### 2. Aurora Serverless v2

**Prós**
- Wire-compatible com PostgreSQL — migração igualmente trivial em termos de
  dialeto SQL e de driver JDBC.
- Escala automaticamente a capacidade (ACUs) conforme a carga, sem
  redimensionar manualmente uma instância.
- Multi-AZ e réplicas de leitura mais simples de adicionar depois, se o
  tráfego crescer de forma imprevisível.

**Contras**
- Modelo de cobrança por ACU (Aurora Capacity Unit) é menos previsível e
  mais difícil de orçar antecipadamente do que uma `instance_class` fixa,
  para o volume conhecido e estável desta oficina (uma unidade, tráfego
  moderado).
- Complexidade operacional adicional (monitorar ACUs, entender o
  comportamento de scale-to-zero/cold start em cargas esporádicas) sem
  benefício real quando a carga já é previsível.
- Nenhuma vantagem de dialeto sobre o RDS Postgres tradicional — o ganho é
  só de elasticidade de capacidade, que não é o problema deste projeto.

### 3. Amazon DynamoDB

**Prós**
- Escala horizontal e latência de leitura/escrita por chave extremamente
  previsíveis, mesmo sob picos de tráfego massivos e imprevisíveis.
- Modelo *pay-per-request* pode ser mais barato que uma instância sempre
  ligada para cargas muito baixas e esporádicas.
- Multi-region nativo (Global Tables), caso o sistema precisasse operar em
  múltiplas regiões geográficas simultaneamente.

**Contras**
- Modelo de dados é chave-valor/documento (single-table design); o domínio
  desta oficina precisa de `JOIN`s e filtros ad-hoc (ordens por status, por
  cliente, por período) que exigiriam GSIs cuidadosamente desenhados e
  desnormalização manual — não há integridade referencial nem transação
  multi-item no mesmo nível de um RDBMS.
- Esforço de migração alto: todo o schema JPA/Flyway existente precisaria
  ser redesenhado do zero para um modelo de acesso por chave, sem
  reaproveitar nada da modelagem atual.
- Otimizado para escala massiva/imprevisível — não é o problema que este
  projeto tem (uma oficina, tráfego moderado e previsível), o que tornaria a
  reescrita um custo sem retorno (YAGNI).

## Decisão

**Amazon RDS PostgreSQL 16**, instância única (não Aurora), Multi-AZ em
produção. A análise completa, critério por critério, está em
[`infra-database/docs/modelo-dados.md`](../../infra-database/docs/modelo-dados.md#1-por-que-postgresql-gerenciado-rds).
Resumo da razão: o domínio e as consultas são relacionais por natureza, o
schema já existe e funciona com Postgres puro, e nem o Aurora Serverless v2
nem o DynamoDB resolvem um problema que este projeto realmente tem — eles
adicionam elasticidade ou escala horizontal para um volume que já é
conhecido e estável.

## Consequências

**Positivas**
- Nenhuma reescrita de camada de persistência: `mvn verify` com
  Testcontainers continua validando o mesmo dialeto usado em produção.
- Custo e capacidade previsíveis, fáceis de justificar/orçar para um
  ambiente de demonstração.
- Caminho de evolução aberto: se o tráfego crescer de forma imprevisível no
  futuro, migrar para Aurora Serverless v2 é direto (mesmo dialeto),
  preservando essa opção sem pagar o custo dela agora.

**Negativas / custos**
- Escala horizontal (réplicas de leitura, particionamento) não é automática
  — precisaria ser adicionada manualmente se o volume crescer além do que o
  dimensionamento vertical cobre.
- A instância tem custo mesmo ociosa, ao contrário de um modelo
  verdadeiramente sob demanda.
