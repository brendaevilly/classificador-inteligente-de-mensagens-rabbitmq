# Evidências (capturas de tela)

Esta pasta guarda os prints que comprovam o funcionamento do sistema. Tire as capturas com o ambiente
rodando há pelo menos 1–2 minutos (`docker compose up --build -d`) e salve-as aqui **com os nomes
sugeridos** abaixo, em PNG.

Painel do RabbitMQ: <http://localhost:15672> (usuário/senha do `.env`; padrão `admin` / `admin123`).

## Checklist

| ✔ | Arquivo | O que capturar | Como chegar |
|---|---|---|---|
| [ ] | `01-rabbitmq-overview.png` | Aba **Overview**: gráficos *Queued messages* e *Message rates* (publish ~10/s), totais de conexões/canais/filas/consumidores | Painel → *Overview* |
| [ ] | `02-filas-crescendo.png` | Aba **Queues**: `review.queue` e `support.queue` com *Ready* alto (centenas) e *Unacked* = 1; `review.dlq`/`support.dlq` visíveis. De preferência 2 prints com ~30 s de intervalo, ou a página de uma fila com o gráfico subindo | Painel → *Queues and Streams* (ou *Queues*) → clique em `review.queue` |
| [ ] | `03-exchange-bindings.png` | **messages.exchange** (tipo *topic*) com os bindings `review.product → review.queue` e `support.request → support.queue` | Painel → *Exchanges* → `messages.exchange` → seção *Bindings* |
| [ ] | `04-dlq-com-mensagem.png` | **review.dlq** (ou `support.dlq`) com *Ready* ≥ 1 e o resultado de *Get messages* mostrando o payload e o header **x-death** (`reason: rejected`, `queue: review.queue`) | Purgue `review.queue`, rode `.\scripts\send-poison.ps1` (ou `./scripts/send-poison.sh`), depois *Queues* → `review.dlq` → *Get messages* (Ack mode: *Nack message requeue true*) |
| [ ] | `05-logs-consumidor1.png` | Logs do Consumidor 1 com linhas `[IA-SENTIMENTO] id=... classificação=... confiança=... texto=...` e `ACK manual enviado`; se possível, também o topo com a acurácia do startup | `docker compose logs -f consumer-sentiment` |
| [ ] | `06-logs-consumidor2.png` | Logs do Consumidor 2 com linhas `[IA-SUPORTE] id=... categoria=PROBLEMA DE ...` e `ACK manual enviado` | `docker compose logs -f consumer-support` |
| [ ] | `07-logs-gerador.png` | Logs do gerador com `[PUBLICADO] ... review.product / support.request` e `[RESUMO] 10 msg/s` | `docker compose logs -f generator` |

### Capturas opcionais (enriquecem a entrega)

| ✔ | Arquivo | O que capturar | Como chegar |
|---|---|---|---|
| [ ] | `08-docker-compose-ps.png` | Os 4 containers `Up`, RabbitMQ `(healthy)` | `docker compose ps` |
| [ ] | `09-rede-msg-network.png` | Os 4 containers na mesma rede | `docker network inspect msg-network --format "{{range .Containers}}{{.Name}} {{.IPv4Address}}{{println}}{{end}}"` |
| [ ] | `10-log-erro-dlq.png` | Linha `ERRO -> enviada para DLQ \| fila=... \| id=... \| motivo=...` no consumidor | `docker compose logs consumer-sentiment` logo após o script de poison |
| [ ] | `11-fila-argumentos-dlx.png` | Página da `review.queue` mostrando *Features*/*Arguments* `x-dead-letter-exchange` e `x-dead-letter-routing-key` | *Queues* → `review.queue` → *Details* |
| [ ] | `12-acuracia-startup.png` | Log de startup de um consumidor com dataset, vocabulário, acurácia 5-fold e matriz de confusão | `docker compose logs consumer-support \| more` (topo) |
| [ ] | `13-testes-maven.png` | Saída do `.\scripts\mvn-docker.ps1` com `BUILD SUCCESS` e a acurácia sobre mensagens do gerador | `.\scripts\mvn-docker.ps1` |

## Dicas

- Windows: `Win + Shift + S` recorta uma área da tela; cole e salve como PNG com o nome da tabela.
- Use o Windows Terminal com fonte grande para os logs.
- Para os logs ficarem menos poluídos no print do gerador, opcionalmente use
  `LOG_EACH_MESSAGE=false` (apenas o resumo por segundo) – mas o print `07` deve mostrar pelo menos
  algumas linhas `[PUBLICADO]`.
- Evite expor senhas reais nos prints (o padrão `admin123` é apenas para demonstração).
