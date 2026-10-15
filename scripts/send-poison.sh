#!/usr/bin/env bash
# Publica UMA mensagem com erro no messages.exchange usando a API HTTP de gerenciamento
# do RabbitMQ (POST /api/exchanges/%2F/messages.exchange/publish), para demonstrar a DLQ.
#
# Uso:  ./scripts/send-poison.sh [review|support] [poison|invalid-json|missing-field] [--purge]
#   alvo  : review  -> routing key review.product  (Consumidor 1, DLQ review.dlq)   [padrão]
#           support -> routing key support.request (Consumidor 2, DLQ support.dlq)
#   tipo  : poison        -> JSON válido com content "__POISON__"                  [padrão]
#           invalid-json  -> corpo que não é JSON
#           missing-field -> JSON sem o campo obrigatório "content"
#   --purge : esvazia a fila principal antes, para a venenosa ser a próxima consumida.
#             A fila é FIFO: sem isso, ela espera todo o backlog (~1 msg/s).
#
# Variáveis opcionais: RABBITMQ_USER, RABBITMQ_PASS (lidas também do .env), RABBITMQ_MANAGEMENT_URL.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/../.env"

read_env() {
    # Lê uma variável do .env (se existir) sem executar o arquivo.
    [ -f "$ENV_FILE" ] && grep -E "^$1=" "$ENV_FILE" | tail -n 1 | cut -d '=' -f 2- | tr -d '\r' || true
}

USER_NAME="${RABBITMQ_USER:-$(read_env RABBITMQ_USER)}"
USER_NAME="${USER_NAME:-admin}"
USER_PASS="${RABBITMQ_PASS:-$(read_env RABBITMQ_PASS)}"
USER_PASS="${USER_PASS:-admin123}"
MGMT_PORT="$(read_env RABBITMQ_MANAGEMENT_PORT)"
BASE_URL="${RABBITMQ_MANAGEMENT_URL:-http://localhost:${MGMT_PORT:-15672}}"

TARGET="${1:-review}"
KIND="${2:-poison}"
PURGE="${3:-}"

case "$TARGET" in
    review)  ROUTING_KEY="review.product";  TYPE="review";  QUEUE="review.queue";  DLQ="review.dlq" ;;
    support) ROUTING_KEY="support.request"; TYPE="support"; QUEUE="support.queue"; DLQ="support.dlq" ;;
    *) echo "Alvo inválido: '$TARGET' (use review ou support)" >&2; exit 1 ;;
esac

new_uuid() {
    if [ -r /proc/sys/kernel/random/uuid ]; then
        cat /proc/sys/kernel/random/uuid
    elif command -v uuidgen >/dev/null 2>&1; then
        uuidgen | tr '[:upper:]' '[:lower:]'
    else
        od -An -N16 -tx1 /dev/urandom | tr -d ' \n' \
            | sed -E 's/^(.{8})(.{4}).(.{3}).(.{3})(.{12}).*$/\1-\2-4\3-a\4-\5/'
    fi
}

ID="$(new_uuid)"
NOW="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

case "$KIND" in
    poison)        BODY="{\"id\":\"$ID\",\"timestamp\":\"$NOW\",\"type\":\"$TYPE\",\"content\":\"__POISON__\"}" ;;
    invalid-json)  BODY="{\"id\":\"$ID\",\"type\":\"$TYPE\",\"content\": isto nao e um JSON valido" ;;
    missing-field) BODY="{\"id\":\"$ID\",\"timestamp\":\"$NOW\",\"type\":\"$TYPE\"}" ;;
    *) echo "Tipo inválido: '$KIND' (use poison, invalid-json ou missing-field)" >&2; exit 1 ;;
esac

# O corpo vai como string dentro do JSON da API: escapa \ e ".
ESCAPED_BODY="$(printf '%s' "$BODY" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g')"
REQUEST="{\"properties\":{\"delivery_mode\":2,\"content_type\":\"application/json\",\"message_id\":\"$ID\",\"app_id\":\"send-poison\"},\"routing_key\":\"$ROUTING_KEY\",\"payload\":\"$ESCAPED_BODY\",\"payload_encoding\":\"string\"}"

BACKLOG=0
if [ "$PURGE" = "--purge" ]; then
    curl -sS -u "$USER_NAME:$USER_PASS" -X DELETE "$BASE_URL/api/queues/%2F/$QUEUE/contents" >/dev/null
    echo "Fila '$QUEUE' purgada: a mensagem venenosa será a próxima a ser consumida."
else
    # As estatísticas da API são atualizadas a cada ~5 s; por isso só são consultadas sem --purge.
    BACKLOG="$(curl -sS -u "$USER_NAME:$USER_PASS" "$BASE_URL/api/queues/%2F/$QUEUE" \
        | grep -o '"messages_ready":[0-9]*' | head -n 1 | cut -d ':' -f 2)"
    BACKLOG="${BACKLOG:-0}"
fi

echo "Publicando mensagem com erro ($KIND) em messages.exchange com routing key '$ROUTING_KEY'..."
echo "  id   : $ID"
echo "  corpo: $BODY"

RESPONSE="$(curl -sS -u "$USER_NAME:$USER_PASS" -H "content-type: application/json" \
    -X POST "$BASE_URL/api/exchanges/%2F/messages.exchange/publish" -d "$REQUEST")"
echo "Resposta da API: $RESPONSE"

if printf '%s' "$RESPONSE" | grep -q '"routed":true'; then
    echo "OK! A mensagem foi roteada. O consumidor vai rejeitá-la (basicNack requeue=false)"
    echo "e ela aparecerá na fila '$DLQ' (Queues > $DLQ > Get messages, veja o header x-death)."
    if [ "$BACKLOG" -gt 20 ]; then
        echo "AVISO: há $BACKLOG mensagens na frente em '$QUEUE' (fila FIFO, ~1 msg/s): a venenosa só chega"
        echo "à DLQ em ~$(( (BACKLOG + 59) / 60 )) min. Para ver na hora: ./scripts/send-poison.sh $TARGET $KIND --purge"
    fi
else
    echo "A mensagem NÃO foi roteada. Verifique se o ambiente está no ar e as credenciais." >&2
    exit 1
fi
