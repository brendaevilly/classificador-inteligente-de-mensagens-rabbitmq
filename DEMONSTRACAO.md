# Guia de Uso e Demonstração

Este guia mostra como subir o ambiente do zero, confirmar que está saudável e gravar o vídeo de
~5 minutos cobrindo os 5 itens exigidos:

1. geração contínua de mensagens;
2. encaminhamento correto pelo Topic Exchange;
3. classificação feita pelas duas IAs;
4. crescimento das filas na interface do RabbitMQ;
5. tratamento de uma mensagem com erro via DLQ.

---

## 1. Pré-requisitos

- Docker Desktop (Windows/macOS) ou Docker Engine + Compose v2 (Linux). Confira:
  ```bash
  docker version
  docker compose version
  ```
- Portas 5672 e 15672 livres.
- ~1,5 GB de RAM livre.
- Não precisa de Java nem Maven: tudo é compilado dentro dos containers.

## 2. Subir o ambiente do zero

```bash
git clone <URL_DO_REPOSITORIO>
cd classificador-inteligente-de-mensagens-rabbitmq

cp .env.example .env            # opcional (PowerShell: Copy-Item .env.example .env)
docker compose up --build -d    # primeira vez: alguns minutos (imagens + dependências Maven)
```

### Confirmar que está saudável

```bash
docker compose ps
```

Esperado: `rabbitmq` com `Up ... (healthy)` e `generator`, `consumer-sentiment`, `consumer-support` com `Up`.

```bash
docker compose logs --tail 20 consumer-sentiment
```

Esperado: `IA de sentimento pronta ... dataset=250 exemplos`, `Acurácia estimada (validação cruzada
estratificada 5-fold): 87,2%`, a matriz de confusão e `Consumindo a fila 'review.queue' | autoAck=false | prefetch=1`.

```bash
docker network inspect msg-network --format "{{range .Containers}}{{.Name}} {{.IPv4Address}}{{println}}{{end}}"
```

Esperado: os 4 containers na rede `msg-network`.

Painel: <http://localhost:15672> → usuário `admin`, senha `admin123` (ou os do `.env`).

---

## 3. Preparação antes de gravar (2 minutos antes)

1. Suba o ambiente com `docker compose up --build -d` uns **2 minutos antes** de gravar, para as filas já
   terem acumulado algumas centenas de mensagens (o gráfico fica mais claro).
2. Deixe abertos:
   - **Terminal 1**: `docker compose logs -f generator`
   - **Terminal 2**: `docker compose logs -f consumer-sentiment consumer-support`
     (ou dois terminais, um para cada consumidor)
   - **Terminal 3**: na pasta do projeto, pronto para rodar o script de poison.
   - **Navegador**: painel do RabbitMQ já logado, na aba *Queues and Streams* (ou *Queues*).
3. Aumente a fonte do terminal (Ctrl + `+`) e use tela cheia/zoom no navegador.
4. No Windows, use o Windows Terminal (acentos e cores corretos).

---

## 4. Roteiro do vídeo (~5 minutos)

### 0:00 – 0:30 · Abertura e arquitetura

**Mostrar:** o README (diagrama de arquitetura) e `docker compose ps`.

**Falar:** "Este é o sistema distribuído de classificação de mensagens da Atividade 5. São 4 containers
na mesma rede Docker `msg-network`: o gerador em Java, o RabbitMQ com a interface de administração e
dois consumidores Java, cada um com uma IA embarcada feita com a biblioteca Smile. Tudo sobe com um
único comando, `docker compose up --build`."

### 0:30 – 1:15 · Item 1: geração contínua de mensagens

**Mostrar:** Terminal 1 (`docker compose logs -f generator`).

**Falar:** "O gerador publica continuamente 10 mensagens por segundo, configurável pela variável
`MESSAGES_PER_SECOND`. Metade são avaliações de produtos, com routing key `review.product`, e metade
são solicitações de suporte, com `support.request`. Cada mensagem é um JSON com id UUID, timestamp
ISO-8601, tipo e conteúdo, publicada como persistente. A cada segundo aparece um resumo: aqui,
10 mensagens por segundo, divididas entre os dois tipos."

Aponte uma linha `[PUBLICADO] #... review.product | id=... | "..."` e uma linha `[RESUMO] 10 msg/s`.

### 1:15 – 2:00 · Item 2: encaminhamento pelo Topic Exchange

**Mostrar:** painel → aba **Exchanges** → clicar em **messages.exchange** → seção **Bindings**.

**Falar:** "O gerador publica em um único Topic Exchange, o `messages.exchange`. Nos bindings vemos
que a routing key `review.product` leva para a fila `review.queue` e a `support.request` leva para a
`support.queue`. No gráfico *Message rates* do exchange aparecem as ~10 mensagens por segundo
entrando." Opcional: mostre também o `dlx.exchange` com os bindings `review.dlq` e `support.dlq`.

Depois abra **Queues → review.queue → Bindings** e mostre que só recebe `review.product`
(idem para `support.queue`). Em *Features* da fila aparecem `DLX` e `DLK`.

### 2:00 – 3:00 · Item 3: classificação pelas duas IAs

**Mostrar:** Terminal 2 (logs dos consumidores).

**Falar:** "O Consumidor 1 lê somente a `review.queue` e classifica o sentimento em POSITIVA, NEGATIVA ou
NEUTRA. O Consumidor 2 lê somente a `support.queue` e classifica em problema de acesso, desempenho ou
conexão. Cada um treina o seu próprio modelo no startup, dentro do container, com um dataset rotulado
em português: pré-processamento com normalização, stopwords, bag-of-words e um classificador Naive
Bayes multinomial do Smile. No startup o log mostra a acurácia estimada por validação cruzada; sobre
mensagens do próprio gerador as duas IAs acertam mais de 99%. Para cada mensagem aparece o id, o
texto, a classe e a confiança, e depois o ACK manual: a confirmação só é enviada depois que a
mensagem foi processada com sucesso."

Aponte um `[IA-SENTIMENTO] ... classificação=NEGATIVA | confiança=...` e um
`[IA-SUPORTE] ... categoria=PROBLEMA DE CONEXÃO`, seguidos de `ACK manual enviado`.

Opcional (10 s): `docker compose logs consumer-support | Select-String "Acurácia"` (PowerShell) ou
`docker compose logs consumer-support | grep Acurácia` (bash).

### 3:00 – 3:50 · Item 4: crescimento das filas

**Mostrar:** painel → aba **Queues** (lista) e depois **review.queue** (gráficos).

**Falar:** "Os consumidores processam de propósito mais devagar que a chegada: há um atraso artificial
de 1 segundo por mensagem (`PROCESSING_DELAY_MS`) e prefetch 1. Entram cerca de 5 mensagens por
segundo em cada fila e sai 1 por segundo, então as filas crescem cerca de 4 mensagens por segundo.
Vejam a coluna *Ready* aumentando, *Unacked* igual a 1, que é a mensagem que o consumidor está
processando, e nas taxas: *incoming* ~5/s e *deliver/ack* ~1/s."

Na página da fila, mostre o gráfico **Queued messages** (Ready/Unacked/Total subindo) e **Message
rates** (Publish ~5/s, Deliver/Ack ~1/s). Atualize a lista (ou espere o refresh de 5 s) para mostrar
o número subindo.

Opcional (impacto visual): `docker compose stop consumer-support` → a `support.queue` passa a crescer
5/s e *Consumers* vai a 0; depois `docker compose start consumer-support`.

### 3:50 – 4:50 · Item 5: mensagem com erro → DLQ

**Preparar (5 s):** a fila é FIFO e já tem centenas de mensagens, então a venenosa demoraria minutos
para chegar ao consumidor. No painel, em **Queues → review.queue**, role até **Purge** e clique em
**Purge Messages** (ou rode `docker compose exec rabbitmq rabbitmqctl purge_queue review.queue`).

**Mostrar:** Terminal 3:

```powershell
.\scripts\send-poison.ps1            # Windows
```

```bash
./scripts/send-poison.sh review poison   # Linux/macOS/Git Bash
```

**Falar:** "Este script publica, pela API HTTP de gerenciamento do RabbitMQ, uma mensagem com o
conteúdo `__POISON__` na routing key `review.product`. O consumidor valida a mensagem, detecta o erro
e faz `basicNack` com `requeue=false`: a mensagem não volta para a fila nem é descartada; a
`review.queue` tem o argumento `x-dead-letter-exchange`, então o RabbitMQ a envia ao `dlx.exchange`,
que entrega na `review.dlq`."

Mostre no Terminal 2 a linha vermelha:
`ERRO -> enviada para DLQ | fila=review.queue | id=... | motivo=mensagem venenosa detectada ...`

No painel: **Queues → review.dlq** (Ready = 1) → **Get messages** → **Get Message(s)**. Mostre o
payload e o header **x-death** (`queue: review.queue`, `reason: rejected`, `count: 1`,
`exchange: messages.exchange`, `routing-keys: review.product`).

> Em *Get messages* deixe o *Ack Mode* como **Nack message requeue true** para a mensagem continuar na DLQ.

Opcional: `.\scripts\send-poison.ps1 -Target support -Kind invalid-json` (purgue a `support.queue`
antes) → mensagem com JSON inválido em `support.dlq`.

### 4:50 – 5:00 · Encerramento

**Falar:** "Resumindo: geração contínua, roteamento por Topic Exchange, duas IAs independentes
classificando, filas crescendo pelo processamento mais lento com ACK manual, e mensagens com erro
isoladas na DLQ com o header x-death para rastreabilidade. Obrigado."

---

## 5. Comandos úteis (copiar e colar)

### Logs

```bash
docker compose logs -f generator
docker compose logs -f consumer-sentiment
docker compose logs -f consumer-support
docker compose logs -f consumer-sentiment consumer-support
docker compose logs -f rabbitmq
docker compose logs --tail 50 generator          # só as últimas 50 linhas
```

### Estado das filas pelo terminal

```bash
docker compose exec rabbitmq rabbitmqctl list_queues name messages_ready messages_unacknowledged consumers
docker compose exec rabbitmq rabbitmqctl list_bindings source_name routing_key destination_name
docker compose exec rabbitmq rabbitmqctl list_exchanges name type
```

### Alterar a taxa do gerador

PowerShell:

```powershell
$env:MESSAGES_PER_SECOND = "20"; docker compose up -d generator
Remove-Item Env:MESSAGES_PER_SECOND; docker compose up -d generator   # volta ao padrão/.env
```

bash:

```bash
MESSAGES_PER_SECOND=20 docker compose up -d generator
docker compose up -d generator                                        # volta ao padrão/.env
```

Ou edite `MESSAGES_PER_SECOND` no `.env` e rode `docker compose up -d generator`.

### Mostrar só o resumo por segundo do gerador (menos poluição no log)

```powershell
$env:LOG_EACH_MESSAGE = "false"; docker compose up -d generator
```

```bash
LOG_EACH_MESSAGE=false docker compose up -d generator
```

### Alterar a velocidade dos consumidores

```powershell
$env:PROCESSING_DELAY_MS = "300"; docker compose up -d consumer-sentiment consumer-support   # mais rápidos
$env:PROCESSING_DELAY_MS = "2000"; docker compose up -d consumer-sentiment consumer-support  # mais lentos
Remove-Item Env:PROCESSING_DELAY_MS; docker compose up -d consumer-sentiment consumer-support
```

```bash
PROCESSING_DELAY_MS=300 docker compose up -d consumer-sentiment consumer-support
```

Com `PROCESSING_DELAY_MS=100` (≈10 msg/s por consumidor) as filas param de crescer e esvaziam.

### Parar um consumidor (fila cresce mais rápido)

```bash
docker compose stop consumer-support      # support.queue cresce ~5 msg/s, Consumers = 0
docker compose start consumer-support     # volta a consumir
```

### Escalar consumidores (competing consumers na mesma fila)

```bash
docker compose up -d --scale consumer-sentiment=3
docker compose up -d --scale consumer-sentiment=1
```

### Mensagens venenosas

```powershell
.\scripts\send-poison.ps1                                    # review + __POISON__
.\scripts\send-poison.ps1 -Target support                    # support + __POISON__
.\scripts\send-poison.ps1 -Target review -Kind invalid-json  # corpo que não é JSON
.\scripts\send-poison.ps1 -Target support -Kind missing-field
# Se o PowerShell bloquear scripts:
powershell -ExecutionPolicy Bypass -File .\scripts\send-poison.ps1
```

```bash
./scripts/send-poison.sh review poison
./scripts/send-poison.sh support invalid-json
./scripts/send-poison.sh support missing-field
```

Ou no próprio gerador (a cada N mensagens, alternando filas e tipos de erro):

```powershell
$env:POISON_EVERY_N = "50"; docker compose up -d generator
Remove-Item Env:POISON_EVERY_N; docker compose up -d generator
```

```bash
POISON_EVERY_N=50 docker compose up -d generator
```

### Purgar filas

```bash
docker compose exec rabbitmq rabbitmqctl purge_queue review.queue
docker compose exec rabbitmq rabbitmqctl purge_queue support.queue
docker compose exec rabbitmq rabbitmqctl purge_queue review.dlq
docker compose exec rabbitmq rabbitmqctl purge_queue support.dlq
```

### Testar resiliência

```bash
docker compose restart rabbitmq       # os serviços reconectam sozinhos (retry/backoff + auto-recovery)
docker compose stop consumer-sentiment   # veja no log o shutdown gracioso com o total processado
```

### Parar e limpar

```bash
docker compose stop                   # pausa tudo, mantém filas/mensagens
docker compose down                   # remove containers e rede (mantém o volume)
docker compose down -v                # remove também o volume (filas zeradas)
docker compose down -v --rmi local    # e as imagens construídas
```

### Rodar os testes (sem Maven instalado)

```powershell
.\scripts\mvn-docker.ps1
```

```bash
./scripts/mvn-docker.sh
```

---

## 6. Dicas para a gravação

- Grave em 1080p, com o terminal em fonte grande e o navegador com zoom de 110–125%.
- Suba o ambiente uns 2 minutos antes: as filas já terão centenas de mensagens e os gráficos ficam
  expressivos.
- Se o log do gerador estiver poluído demais, use `LOG_EACH_MESSAGE=false` (só o resumo por segundo)
  e mostre também `docker compose logs --tail 10 generator`.
- Purgue a fila antes de rodar o script de poison (seção 4.5), senão a mensagem demora para chegar ao
  consumidor.
- No *Get messages* da DLQ, use *Nack message requeue true* para não remover a mensagem.
- Ferramentas de gravação: OBS Studio, Xbox Game Bar (Win + G) ou Clipchamp.
- Ensaie uma vez com cronômetro; o roteiro cabe em 5 minutos falando com calma.

## 7. Como o professor reproduz

```bash
git clone <URL_DO_REPOSITORIO>
cd classificador-inteligente-de-mensagens-rabbitmq
docker compose up --build
# painel: http://localhost:15672  (admin / admin123)
# DLQ:    ./scripts/send-poison.sh   ou   .\scripts\send-poison.ps1
```

Não é preciso criar o `.env`: o `docker-compose.yml` tem valores padrão para todas as variáveis.

---

## 8. FAQ – problemas comuns

**A porta 5672 ou 15672 já está em uso.**
Outro RabbitMQ (ou serviço) está rodando. Pare-o (`docker ps`, `docker stop <id>`) ou mude no `.env`:
`RABBITMQ_AMQP_PORT=5673` e `RABBITMQ_MANAGEMENT_PORT=15673`, e acesse <http://localhost:15673>.

**Os serviços mostram "RabbitMQ indisponível ... Nova tentativa".**
Normal nos primeiros segundos (ou após `docker compose restart rabbitmq`). Eles tentam de novo com
backoff exponencial (até 15 s entre tentativas) e conectam sozinhos.

**`ACCESS_REFUSED - Login was refused`.**
A senha dos serviços difere da do broker. O usuário é recriado a cada boot a partir do `.env`; rode
`docker compose up -d --force-recreate` após mudar o `.env`.

**O painel não aceita `guest/guest`.**
Este setup não usa `guest`. Use `admin`/`admin123` ou o que estiver no seu `.env`.

**"Topologia ainda não disponível".**
O RabbitMQ não carregou o definitions. Veja `docker compose logs rabbitmq` (deve haver
`[render-definitions] definitions.json gerado` e `Importing concurrently 4 queues`). Confirme que a
pasta `rabbitmq/` existe no diretório onde rodou o compose.

**A mensagem venenosa não chegou à DLQ.**
Ela está na fila atrás do backlog (FIFO). Purgue a fila e envie de novo, ou aguarde. Confira também se
o script respondeu `routed: true`.

**Pouca memória / Docker lento.**
No Docker Desktop: *Settings → Resources* (ou `.wslconfig` no Windows) e reserve ao menos 2 GB. Para
liberar espaço: `docker compose down -v` e `docker system prune`. Filas enormes após horas rodando:
purgue as filas.

**Docker no Windows / WSL 2.**
- O Docker Desktop precisa estar aberto e com o ícone "Engine running".
- Erro `open //./pipe/dockerDesktopLinuxEngine`: o Docker Desktop não está rodando; abra-o e espere.
- Use o backend WSL 2 (*Settings → General → Use the WSL 2 based engine*).
- Se o repositório estiver no OneDrive e o build ficar lento, clone em uma pasta fora dele (ex.: `C:\dev`).
- Scripts `.sh` com CRLF: o `.gitattributes` força LF; se necessário, `git config core.autocrlf input`
  e clone de novo, ou rode `sed -i 's/\r$//' scripts/*.sh`.

**PowerShell bloqueia o `send-poison.ps1`.**
`powershell -ExecutionPolicy Bypass -File .\scripts\send-poison.ps1` ou
`Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`.

**Acentos aparecem quebrados nos logs.**
Use o Windows Terminal ou execute `chcp 65001` antes do `docker compose logs`.

**Quero recomeçar do zero.**
`docker compose down -v --rmi local` e depois `docker compose up --build`.
