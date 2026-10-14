#!/bin/sh
# Entrypoint do container RabbitMQ.
#
# Quando há definições para carregar no boot, o RabbitMQ NÃO cria o usuário padrão
# (RABBITMQ_DEFAULT_USER/PASS). Para não deixar credenciais fixas em arquivo, este script
# gera /etc/rabbitmq/definitions.json juntando:
#   - o usuário administrador e suas permissões, lidos de RABBITMQ_USER / RABBITMQ_PASS;
#   - a topologia versionada em definitions.template.json (fonte única de verdade).
# Depois entrega o controle ao entrypoint oficial da imagem.
set -eu

TEMPLATE=/opt/topologia/definitions.template.json
OUTPUT=/etc/rabbitmq/definitions.json

RABBITMQ_USER="${RABBITMQ_USER:-admin}"
RABBITMQ_PASS="${RABBITMQ_PASS:-admin123}"

# Escapa \ e " para inserir os valores com segurança em strings JSON.
json_escape() {
    printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g'
}
USER_JSON=$(json_escape "$RABBITMQ_USER")
PASS_JSON=$(json_escape "$RABBITMQ_PASS")

{
    printf '{\n'
    printf '  "users": [{"name": "%s", "password": "%s", "tags": ["administrator"]}],\n' "$USER_JSON" "$PASS_JSON"
    printf '  "permissions": [{"user": "%s", "vhost": "/", "configure": ".*", "write": ".*", "read": ".*"}],\n' "$USER_JSON"
    # Template sem a chave de abertura da primeira linha (e sem CR, caso o arquivo venha do Windows).
    tr -d '\r' < "$TEMPLATE" | sed '1s/^[[:space:]]*{//'
} > "$OUTPUT"
chmod 644 "$OUTPUT"

echo "[render-definitions] definitions.json gerado para o usuário '$RABBITMQ_USER' (topologia: $TEMPLATE)"
exec docker-entrypoint.sh "$@"
