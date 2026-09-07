# ADR 0003 — Autoscaling da aplicação via HPA (CPU/memória) com metrics-server

- **Status:** Aceito
- **Data:** 2026-08-25
- **Contexto:** Tech Challenge — Fase 3
- **Referências:** `infra-k8s/k8s/base/hpa.yaml`, `infra-k8s/k8s/overlays/*/kustomization.yaml`, `infra-k8s/terraform/addons.tf`

## Contexto

A especificação da Fase 3 exige um cluster Kubernetes com escalabilidade
para o Deployment `oficina-api`. O cluster precisa reagir a variações de
carga aumentando e diminuindo o número de réplicas automaticamente, sem
intervenção manual, e sem depender de um sistema de métricas externo já
provisionado por outro componente.

## Decisão

Usamos o **Horizontal Pod Autoscaler (`autoscaling/v2`)** nativo do
Kubernetes, escalando por dois recursos:

- CPU: utilização média de 60% (`averageUtilization: 60`);
- Memória: utilização média de 75% (`averageUtilization: 75`).

O `base/hpa.yaml` define uma faixa padrão (`minReplicas: 2`,
`maxReplicas: 6`), mas nenhum dos dois ambientes usa esse padrão sem
ajuste — os dois overlays o sobrescrevem via patch JSON:

- `homolog` (`infra-k8s/k8s/overlays/homolog/kustomization.yaml`) reduz
  para **1–3**. Faz sentido porque o `Deployment` em homolog já roda com
  `replicas: 1` (patch no mesmo overlay) — homolog não precisa de alta
  disponibilidade, só de um ambiente estável para validar antes do deploy
  em prod, então o piso do HPA acompanha o piso do Deployment.
- `prod` (`infra-k8s/k8s/overlays/prod/kustomization.yaml`) aumenta para
  **2–6**. O `Deployment` em prod roda com `replicas: 2` como piso — duas
  réplicas sobrevivem à perda de uma delas (ou de um nó, com o
  `topologySpreadConstraints` do base) sem cair a zero — e o teto (6,
  contra 6 do base) é o maior valor que o cluster de prod realmente
  comporta hoje, não um número arbitrário — ver a aritmética na seção
  "Consequências" abaixo (esse teto chegou a ser fixado em 10 numa versão
  anterior deste ADR; corrigido depois de checar a capacidade real do
  cluster).

O `behavior` do HPA (definido no base e herdado por ambos os ambientes,
sem patch) favorece reação rápida para escalar (janela de estabilização de
30 s, até 2 pods por vez) e reação mais conservadora para reduzir (janela
de 120 s, 1 pod por vez), para evitar oscilação (*flapping*) sob carga
instável.

O HPA depende do **metrics-server**, instalado como *addon* Helm
(`infra-k8s/terraform/addons.tf`, `helm_release.metrics_server`) — sem ele,
o HPA não tem de onde ler `cpu`/`memory` de uso corrente dos pods. Os
`resources.requests` de CPU/memória são obrigatórios no
`Deployment` (`infra-k8s/k8s/base/deployment.yaml`) porque o HPA calcula
utilização como percentual do *request*, não do *limit*.

## Consequências

**Positivas**
- Escalabilidade automática sem depender de nenhum serviço externo de
  métricas (ex.: Prometheus Operator completo) — `metrics-server` é a peça
  mínima necessária, alinhada ao princípio geral do projeto de minimizar
  componentes extras a operar (mesmo raciocínio usado para não instalar um
  AWS Load Balancer Controller).
- `PodDisruptionBudget` (`minAvailable: 1`) e
  `topologySpreadConstraints` (espalhar pods entre nós) trabalham junto
  com o HPA para que escalar/reduzir réplicas ou drenar um nó nunca derrube
  o serviço inteiro.
- Métricas de CPU/memória do `metrics-server` também alimentam `kubectl top`,
  útil para diagnóstico manual sem precisar do New Relic.

**Negativas / custos**
- Escala por CPU/memória é um proxy indireto de carga real (número de
  requisições, latência) — um pico de tráfego com baixo custo de CPU por
  requisição pode não disparar o HPA a tempo. Escalar por métrica
  customizada (ex.: requisições por segundo via Prometheus Adapter) daria
  uma resposta mais fiel à carga real, mas exigiria instalar e manter mais
  um componente (Prometheus Adapter) sem necessidade comprovada neste
  estágio.
- Em prod, `minReplicas: 2` significa que o piso de custo do cluster nunca
  cai abaixo de duas réplicas, mesmo em períodos de tráfego zero. Em
  homolog o piso é 1 réplica (menor custo, sem redundância) — coerente com
  o ambiente não precisar de alta disponibilidade, mas significa que uma
  falha do único pod em homolog derruba o ambiente até o Kubernetes
  religá-lo, sem uma segunda réplica para absorver o tráfego enquanto isso.
- **O teto do HPA de prod é limitado pela capacidade real do cluster, não
  só pelo tráfego esperado** — sem essa checagem, `maxReplicas` vira um
  número decorativo que o HPA persegue sem nunca alcançar. A aritmética,
  para poder ser conferida:
  - `environments/prod.tfvars`: `node_instance_types = ["t3.large"]`
    (2 vCPU / 8 GiB por nó), `node_desired_size = 2`, `node_max_size = 4`.
    **Sem Cluster Autoscaler** instalado neste projeto (`addons.tf` só
    instala `metrics-server`) — nada aciona o ASG para crescer de 2 para
    4 nós sozinho; o teto de fato é o `desired_size` (**2 nós**), não o
    `max_size` da ASG.
  - Alocável por nó (fórmula oficial de *kube-reserved* da AMI do EKS,
    2 vCPU): CPU ≈ 2000m − (6% do 1º core + 1% do 2º core) = 2000m − 70m
    = **1930m**; memória ≈ 8192Mi − 640Mi (kube-reserved, 8 GiB) − 100Mi
    (limiar de *eviction*) = **7452Mi**. Esses números ainda não descontam
    o que os próprios `DaemonSet`s do cluster (kube-proxy, VPC CNI,
    `nri-bundle`) consomem em cada nó — a capacidade real de sobra para a
    aplicação é um pouco menor que o calculado abaixo.
  - *Request* por pod em prod (`patch-recursos.yaml`): `500m` CPU /
    `768Mi` memória.
  - Pods por nó: por CPU, `1930m ÷ 500m` = 3 (arredondado para baixo); por
    memória, `7452Mi ÷ 768Mi` ≈ 9. **CPU é o recurso que limita** — 3 pods
    por nó.
  - Com os 2 nós que realmente existem (`node_desired_size`, sem Cluster
    Autoscaler): **3 × 2 = 6 pods** é o teto real do cluster hoje.
  - Antes desta correção, `maxReplicas: 10` deixava até 4 réplicas
    exigidas pelo HPA sem nenhum nó com CPU alocável disponível — elas
    ficariam presas em `Pending` indefinidamente (sem Cluster Autoscaler
    para provisionar mais nós), sem nenhum erro visível fora de
    `kubectl get pods`/`kubectl describe hpa`. `maxReplicas` foi reduzido
    para **6** (`infra-k8s/k8s/overlays/prod/kustomization.yaml`) para
    bater com essa capacidade real.
  - Para elevar esse teto de verdade no futuro: aumentar `node_max_size`
    **e** instalar o Cluster Autoscaler (hoje ausente), ou usar um tipo de
    instância maior — não basta subir `maxReplicas` sozinho.
