#!/usr/bin/env bash
# Roteiro executavel da demo da Saga (Fase 4).
#
# Uso: scripts/demo-saga.sh feliz|recusa|falha-execucao|expira
#
#   feliz           OS ate FINALIZADA, saga CONCLUIDA.
#   recusa          cliente recusa o orcamento: OS CANCELADA, saga COMPENSADA.
#   falha-execucao  a Execucao rejeita: ESTORNANDO -> COMPENSADA, estoque volta.
#   expira          o pagamento vence: OS CANCELADA, estoque volta.
#
# Variaveis:
#   ADMIN_USERNAME, ADMIN_PASSWORD  obrigatorias (admin do oficina-api).
#   API             URL do API Gateway. Padrao: SSM /oficina/homolog/gateway/endpoint.
#   CLIENTE_CPF     CPF do cliente da demo (padrao 52998224725). Criado se nao existir.
#   DRY_RUN=1       so imprime as chamadas HTTP, sem enviar e sem kubectl.
#   OFICINA_URL, BILLING_URL, EXEC_URL
#                   se as tres estiverem definidas, nao abre port-forward.
#
# Rotas de ADMIN e dos servicos internos nao passam pelo API Gateway (o
# authorizer so aceita o JWT da Lambda, emitido por CPF). A equipe fala com os
# pods por port-forward. O cliente aprova pelo Gateway, com o token do CPF.
set -euo pipefail

MODO="${1:-}"

uso() {
  cat >&2 <<'EOF'
Uso: scripts/demo-saga.sh feliz|recusa|falha-execucao|expira
EOF
}

case "$MODO" in
  feliz | recusa | falha-execucao | expira) ;;
  *) uso; exit 2 ;;
esac

DRY_RUN="${DRY_RUN:-0}"
CLIENTE_CPF="${CLIENTE_CPF:-52998224725}"

erro() { echo "erro: $*" >&2; exit 1; }
ambiente_incompleto() { echo "ambiente incompleto: $*" >&2; exit 1; }

# --- dependencias -----------------------------------------------------------

# URLs vindas de fora desligam o port-forward (util para teste local).
PF=1
if [[ "$DRY_RUN" == 1 ]] || { [[ -n "${OFICINA_URL:-}" && -n "${BILLING_URL:-}" && -n "${EXEC_URL:-}" ]]; }; then
  PF=0
fi

for cmd in curl jq; do
  command -v "$cmd" >/dev/null 2>&1 || ambiente_incompleto "falta o comando '$cmd'"
done
if [[ "$PF" == 1 ]]; then
  command -v kubectl >/dev/null 2>&1 || ambiente_incompleto "falta o comando 'kubectl'"
fi
if [[ -z "${API:-}" && "$DRY_RUN" != 1 ]]; then
  command -v aws >/dev/null 2>&1 || ambiente_incompleto "falta o comando 'aws' (ou informe API)"
fi

if [[ "$DRY_RUN" == 1 ]]; then
  ADMIN_USERNAME="${ADMIN_USERNAME:-admin-dry-run}"
  ADMIN_PASSWORD="${ADMIN_PASSWORD:-dry-run}"
  API="${API:-https://gateway.exemplo}"
fi
[[ -n "${ADMIN_USERNAME:-}" ]] || erro "defina ADMIN_USERNAME"
[[ -n "${ADMIN_PASSWORD:-}" ]] || erro "defina ADMIN_PASSWORD"

if [[ -z "${API:-}" ]]; then
  API="$(aws ssm get-parameter --name /oficina/homolog/gateway/endpoint \
    --query Parameter.Value --output text 2>/dev/null)" \
    || ambiente_incompleto "nao li o SSM /oficina/homolog/gateway/endpoint (aws logado? informe API)"
fi
API="${API%/}"

if [[ "$PF" == 1 ]]; then
  kubectl cluster-info >/dev/null 2>&1 \
    || ambiente_incompleto "cluster inacessivel pelo kubectl (kubeconfig do oficina-homolog-eks?)"
  kubectl -n oficina get svc oficina-api >/dev/null 2>&1 || ambiente_incompleto "svc oficina-api (ns oficina)"
  kubectl -n billing get svc billing-service >/dev/null 2>&1 || ambiente_incompleto "svc billing-service (ns billing)"
  kubectl -n execucao get svc execution-service >/dev/null 2>&1 || ambiente_incompleto "svc execution-service (ns execucao)"
  OFICINA_URL="http://localhost:18080/api"
  BILLING_URL="http://localhost:18081/api/billing"
  EXEC_URL="http://localhost:18082/api/execucao"
else
  OFICINA_URL="${OFICINA_URL:-http://oficina.dry-run/api}"
  BILLING_URL="${BILLING_URL:-http://billing.dry-run/api/billing}"
  EXEC_URL="${EXEC_URL:-http://execucao.dry-run/api/execucao}"
fi

# --- limpeza ----------------------------------------------------------------

PIDS=()
FALHA_LIGADA=0
RESP="$(mktemp)"

limpar() {
  local rc=$?
  trap - EXIT INT TERM
  if [[ "$FALHA_LIGADA" == 1 ]]; then
    echo "-> desligando a simulacao de falha da Execucao"
    if ! http PUT "$EXEC_URL/admin/simulacao-falha" "" '{"ativa":false}' >/dev/null; then
      echo "AVISO: nao desliguei a simulacao de falha. Rode: PUT $EXEC_URL/admin/simulacao-falha {\"ativa\":false}" >&2
    fi
  fi
  local pid
  for pid in ${PIDS[@]+"${PIDS[@]}"}; do
    kill "$pid" 2>/dev/null || true
  done
  rm -f "$RESP"
  exit "$rc"
}
trap limpar EXIT
trap 'exit 130' INT TERM

# --- helpers ----------------------------------------------------------------

N=0
passo() { N=$((N + 1)); echo; echo "[$N] $*"; }

# Resposta falsa usada no DRY_RUN, com os campos que o roteiro le.
CANNED='{"id":"<id>","token":"<token>","accessToken":"<token>","ordemServicoId":"<id>","status":"<status>","quantidadeEstoque":0,"pagamento":{"id":"<pagamento-id>","expiraEm":null},"estado":"<estado>"}'

# http METODO URL TOKEN [CORPO] - imprime o corpo; falha se o HTTP nao for 2xx.
http() {
  local metodo="$1" url="$2" token="${3:-}" corpo="${4:-}"
  if [[ "$DRY_RUN" == 1 ]]; then
    echo "   [dry-run] $metodo $url${corpo:+ $corpo}" >&2
    echo "$CANNED"
    return 0
  fi
  local args=(-sS -o "$RESP" -w '%{http_code}' -X "$metodo" -H 'Content-Type: application/json' --max-time 20)
  if [[ -n "$token" ]]; then args+=(-H "Authorization: Bearer $token"); fi
  if [[ -n "$corpo" ]]; then args+=(-d "$corpo"); fi
  local codigo
  codigo="$(curl "${args[@]}" "$url")" || { echo "erro: falha de rede em $metodo $url" >&2; return 1; }
  if [[ "$codigo" != 2* ]]; then
    echo "erro: $metodo $url -> HTTP $codigo: $(head -c 400 "$RESP")" >&2
    return 1
  fi
  cat "$RESP"
}

# Staff: Bearer do admin. Cliente: Bearer do CPF.
staff() { http "$1" "$2" "$ADMIN_TOKEN" "${3:-}"; }

# aguardar_status_os ESTADO [TIMEOUT=30] - espera o status da OS.
aguardar_status_os() {
  local alvo="$1" limite="${2:-30}" atual="" fim=$((SECONDS + ${2:-30}))
  if [[ "$DRY_RUN" == 1 ]]; then echo "   [dry-run] esperaria OS = $alvo"; return 0; fi
  while :; do
    atual="$(staff GET "$OFICINA_URL/ordens-servico/$OS_ID" | jq -r '.status')"
    if [[ "$atual" == "$alvo" ]]; then echo "   OS: $atual"; return 0; fi
    if ((SECONDS >= fim)); then break; fi
    sleep 1
  done
  echo "erro: a OS nao chegou em $alvo em ${limite}s (ultimo estado: $atual)" >&2
  return 1
}

# aguardar_estado_saga ESTADO [TIMEOUT=30] - espera a saga mais recente e
# imprime cada estado novo que ver (o ESTORNANDO pode passar rapido).
aguardar_estado_saga() {
  local alvo="$1" limite="${2:-30}" atual="" visto="" fim=$((SECONDS + ${2:-30}))
  if [[ "$DRY_RUN" == 1 ]]; then echo "   [dry-run] esperaria saga = $alvo"; return 0; fi
  while :; do
    atual="$(staff GET "$OFICINA_URL/ordens-servico/$OS_ID/saga" | jq -r 'sort_by(.criadoEm) | last | .estado // "?"')"
    if [[ "$atual" != "$visto" ]]; then echo "   saga: $atual"; visto="$atual"; fi
    if [[ "$atual" == "$alvo" ]]; then return 0; fi
    if ((SECONDS >= fim)); then break; fi
    sleep 1
  done
  echo "erro: a saga nao chegou em $alvo em ${limite}s (ultimo estado: $atual)" >&2
  return 1
}

estoque() {
  staff GET "$OFICINA_URL/pecas/$PECA_ID" | jq -r '.quantidadeEstoque'
}

placa_aleatoria() {
  local L=ABCDEFGHJKLMNPRSTUVWXYZ
  printf '%s%s%s%s%s%s%s' "${L:RANDOM%${#L}:1}" "${L:RANDOM%${#L}:1}" "${L:RANDOM%${#L}:1}" \
    "$((RANDOM % 10))" "${L:RANDOM%${#L}:1}" "$((RANDOM % 10))" "$((RANDOM % 10))"
}

# --- port-forward -----------------------------------------------------------

abrir_port_forward() { # NAMESPACE SERVICO PORTA_LOCAL URL_SAUDE
  local ns="$1" svc="$2" porta="$3" saude="$4"
  kubectl -n "$ns" port-forward "svc/$svc" "$porta:8080" >/dev/null 2>&1 &
  local pid=$!
  PIDS+=("$pid")
  for _ in $(seq 1 30); do
    if ! kill -0 "$pid" 2>/dev/null; then
      ambiente_incompleto "port-forward de $svc caiu (porta $porta ocupada?)"
    fi
    if curl -fsS -o /dev/null --max-time 2 "$saude" 2>/dev/null; then
      echo "   $svc pronto em localhost:$porta"
      return 0
    fi
    sleep 1
  done
  ambiente_incompleto "$svc nao respondeu em $saude em 30s"
}

# --- roteiro ----------------------------------------------------------------

echo "Demo da Saga - modo: $MODO"
echo "Gateway: $API"

if [[ "$PF" == 1 ]]; then
  passo "abrindo port-forwards (oficina-api, billing-service, execution-service)"
  abrir_port_forward oficina oficina-api 18080 "$OFICINA_URL/actuator/health"
  abrir_port_forward billing billing-service 18081 "$BILLING_URL/actuator/health"
  abrir_port_forward execucao execution-service 18082 "$EXEC_URL/actuator/health"
fi

passo "login do administrador (direto no pod, fora do Gateway)"
ADMIN_TOKEN="$(http POST "$OFICINA_URL/auth/login" "" \
  "$(jq -nc --arg u "$ADMIN_USERNAME" --arg p "$ADMIN_PASSWORD" '{username:$u,password:$p}')" | jq -r '.token')"
[[ -n "$ADMIN_TOKEN" && "$ADMIN_TOKEN" != null ]] || erro "login sem token"

passo "cliente com CPF $CLIENTE_CPF (cria se nao existir)"
CLIENTE_ID="$(staff GET "$OFICINA_URL/clientes" | jq -r --arg cpf "$CLIENTE_CPF" '
  (if type == "array" then . elif type == "object" and has("content") then .content else [] end)
  | map(select((.documento // "") | gsub("[^0-9]"; "") == $cpf)) | first | .id // empty')"
if [[ -z "$CLIENTE_ID" ]]; then
  CLIENTE_ID="$(staff POST "$OFICINA_URL/clientes" "$(jq -nc --arg cpf "$CLIENTE_CPF" \
    '{nome:"Cliente Demo Saga",email:"demo.saga@oficina.com",telefone:"11999990000",documento:$cpf,tipoDocumento:"CPF"}')" \
    | jq -r '.id')"
  echo "   criado: $CLIENTE_ID"
else
  echo "   ja existia: $CLIENTE_ID"
fi

passo "veiculo, servico e peca (estoque inicial 10)"
SUFIXO="$(date +%H%M%S)"
VEICULO_ID="$(staff POST "$OFICINA_URL/veiculos" "$(jq -nc --arg c "$CLIENTE_ID" --arg p "$(placa_aleatoria)" \
  '{clienteId:$c,placa:$p,marca:"Volkswagen",modelo:"Golf GTI",ano:2022,cor:"Preto"}')" | jq -r '.id')"
SERVICO_ID="$(staff POST "$OFICINA_URL/servicos" "$(jq -nc --arg n "Revisao demo $SUFIXO" \
  '{nome:$n,descricao:"Revisao da demo da saga",preco:890.00,tempoEstimadoMinutos:240}')" | jq -r '.id')"
PECA_ID="$(staff POST "$OFICINA_URL/pecas" "$(jq -nc --arg n "Filtro de oleo demo $SUFIXO" \
  '{nome:$n,descricao:"Filtro original",precoUnitario:75.50,quantidadeEstoqueInicial:10}')" | jq -r '.id')"
echo "   veiculo=$VEICULO_ID servico=$SERVICO_ID peca=$PECA_ID"
ESTOQUE_ANTES="$(estoque)"
echo "   estoque da peca antes da OS: $ESTOQUE_ANTES"

passo "abrindo a OS com 1 servico e 2 pecas, e movendo para EM_DIAGNOSTICO"
OS_ID="$(staff POST "$OFICINA_URL/ordens-servico/abertura" "$(jq -nc --arg c "$CLIENTE_ID" --arg v "$VEICULO_ID" \
  --arg s "$SERVICO_ID" --arg p "$PECA_ID" \
  '{clienteId:$c,veiculoId:$v,observacoes:"Demo da saga",servicoIds:[$s],pecas:[{pecaId:$p,quantidade:2}]}')" \
  | jq -r '.ordemServicoId')"
echo "   OS: $OS_ID"
staff PATCH "$OFICINA_URL/ordens-servico/$OS_ID/status" '{"novoStatus":"EM_DIAGNOSTICO"}' >/dev/null
echo "   estoque depois de abrir a OS: $(estoque)"

passo "token do cliente pelo Gateway (POST /auth/token, Lambda)"
CLIENTE_TOKEN="$(http POST "$API/auth/token" "" "$(jq -nc --arg cpf "$CLIENTE_CPF" '{cpf:$cpf}')" | jq -r '.accessToken')"
[[ -n "$CLIENTE_TOKEN" && "$CLIENTE_TOKEN" != null ]] || erro "a Lambda nao devolveu token"

passo "enviando a OS para orcamento (inicia a saga)"
staff POST "$OFICINA_URL/ordens-servico/$OS_ID/enviar-orcamento" >/dev/null
aguardar_status_os AGUARDANDO_APROVACAO

passo "lendo o orcamento no Billing"
ORCAMENTO="$(http GET "$BILLING_URL/orcamentos/os/$OS_ID" "")"
ORCAMENTO_ID="$(jq -r '.id' <<<"$ORCAMENTO")"
echo "   orcamento: $ORCAMENTO_ID valor: $(jq -r '.valorTotal' <<<"$ORCAMENTO")"

if [[ "$MODO" == recusa ]]; then
  passo "cliente RECUSA o orcamento pelo Gateway"
  http POST "$API/api/billing/orcamentos/$ORCAMENTO_ID/recusar" "$CLIENTE_TOKEN" >/dev/null
  aguardar_status_os CANCELADA
  aguardar_estado_saga COMPENSADA
else
  passo "cliente APROVA o orcamento pelo Gateway (x-cliente-id vem do token)"
  APROVADO="$(http POST "$API/api/billing/orcamentos/$ORCAMENTO_ID/aprovar" "$CLIENTE_TOKEN")"
  PAGAMENTO_ID="$(jq -r '.pagamento.id' <<<"$APROVADO")"
  EXPIRA_EM="$(jq -r '.pagamento.expiraEm // empty' <<<"$APROVADO")"
  echo "   pagamento: $PAGAMENTO_ID (expira em: ${EXPIRA_EM:-?})"
  aguardar_status_os AGUARDANDO_PAGAMENTO

  if [[ "$MODO" == expira ]]; then
    # Prazo vem do Billing (BILLING_PAGAMENTO_PRAZO, padrao da demo PT2M).
    # O job de expiracao roda a cada BILLING_PAGAMENTO_VERIFICACAO (PT30S).
    ESPERA=210
    if [[ -n "$EXPIRA_EM" ]]; then
      ESPERA="$(jq -nr --arg t "$EXPIRA_EM" '
        (($t | sub("\\.[0-9]+"; "") | fromdateiso8601) - now | floor | if . < 0 then 0 else . end) + 90')"
    fi
    passo "nao pago: esperando o pagamento vencer (ate ${ESPERA}s)"
    aguardar_status_os CANCELADA "$ESPERA"
    aguardar_estado_saga COMPENSADA
  else
    if [[ "$MODO" == falha-execucao ]]; then
      passo "ligando a simulacao de falha da Execucao"
      FALHA_LIGADA=1
      http PUT "$EXEC_URL/admin/simulacao-falha" "" '{"ativa":true}' >/dev/null
    fi

    passo "pagamento aprovado (fake, rota publica do Gateway)"
    http POST "$API/api/billing/public/fake-pagamentos/$PAGAMENTO_ID/aprovar" "" >/dev/null

    if [[ "$MODO" == falha-execucao ]]; then
      passo "a Execucao rejeita; o Billing estorna (ESTORNANDO -> COMPENSADA)"
      aguardar_estado_saga COMPENSADA 90
      aguardar_status_os CANCELADA
    else
      passo "esperando a OS entrar em execucao"
      aguardar_status_os EM_EXECUCAO
      for ETAPA in EM_REPARO EM_TESTE CONCLUIDA; do
        passo "equipe move a execucao para $ETAPA"
        http POST "$EXEC_URL/ordens/$OS_ID/etapa" "" "$(jq -nc --arg e "$ETAPA" '{etapa:$e}')" >/dev/null
      done
      aguardar_status_os FINALIZADA
      aguardar_estado_saga CONCLUIDA
    fi
  fi
fi

# --- resultado --------------------------------------------------------------

passo "resultado"
if [[ "$DRY_RUN" == 1 ]]; then
  echo "   [dry-run] sem resultado real"
  exit 0
fi
echo "OS $OS_ID: $(staff GET "$OFICINA_URL/ordens-servico/$OS_ID" | jq -r '.status')"
echo "Saga:"
staff GET "$OFICINA_URL/ordens-servico/$OS_ID/saga" | jq '[.[] | {estado, motivoFalha, criadoEm, atualizadoEm}]'
echo "Estoque da peca: antes da OS = $ESTOQUE_ANTES, agora = $(estoque)"
