# Sequência de commits

Comandos prontos para versionar o projeto em commits lógicos (Conventional Commits, em português).
Execute **na raiz do repositório**, na ordem. Cada `git add` inclui apenas os arquivos daquela parte.

> O repositório local já existe, está na branch `main` e já tem o remoto `origin`
> (`https://github.com/brendaevilly/classificador-inteligente-de-mensagens-rabbitmq.git`) com o
> commit inicial (LICENSE). Por isso os passos de `git init` / `git remote add` estão marcados como
> **opcionais** e só são necessários se você recriar o repositório do zero.

## 0. (Opcional) Preparação – somente se o repositório ainda não existir

```bash
git init
git branch -M main
git remote add origin <URL_DO_SEU_REPOSITORIO>   # ex.: https://github.com/<usuario>/<repo>.git
```

Confira o estado antes de começar:

```bash
git status
```

## 1. chore: estrutura inicial do repositório

```bash
git add .gitignore .dockerignore .gitattributes .env.example pom.xml
git commit -m "chore: estrutura inicial do repositório (pom pai multi-módulo, gitignore, dockerignore, gitattributes e .env.example)"
```

## 2. feat(rabbitmq): topologia do RabbitMQ

```bash
git add rabbitmq/definitions.template.json rabbitmq/rabbitmq.conf rabbitmq/render-definitions.sh
git commit -m "feat(rabbitmq): topologia com topic exchange, filas, DLX e DLQs via definitions (fonte única de verdade)"
```

## 3. feat(common): módulo compartilhado (mensagem, conexão e NLP)

```bash
git add common/pom.xml \
  common/src/main/java/br/sd/classificador/common/Topology.java \
  common/src/main/java/br/sd/classificador/common/Env.java \
  common/src/main/java/br/sd/classificador/common/message \
  common/src/main/java/br/sd/classificador/common/rabbit \
  common/src/main/java/br/sd/classificador/common/nlp \
  common/src/main/resources \
  common/src/test
git commit -m "feat(common): modelo da mensagem com validação, conexão com retry/backoff e pipeline de NLP com Smile"
```

## 4. feat(dlq): ACK manual, nack sem requeue e scripts de mensagem venenosa

```bash
git add common/src/main/java/br/sd/classificador/common/consumer \
  scripts/send-poison.sh scripts/send-poison.ps1
git commit -m "feat(dlq): consumidor com ACK manual, basicNack sem requeue para DLQ e scripts de poison message"
```

## 5. feat(generator): gerador de mensagens

```bash
git add generator/pom.xml generator/Dockerfile generator/src
git commit -m "feat(generator): gerador contínuo de avaliações e solicitações de suporte com publisher confirms e Dockerfile"
```

## 6. feat(consumer-sentiment): Consumidor 1 com IA de sentimento

```bash
git add consumer-sentiment/pom.xml consumer-sentiment/Dockerfile consumer-sentiment/src
git commit -m "feat(consumer-sentiment): consumidor 1 com IA de sentimento (Smile Naive Bayes), dataset, Dockerfile e testes"
```

## 7. feat(consumer-support): Consumidor 2 com IA de suporte

```bash
git add consumer-support/pom.xml consumer-support/Dockerfile consumer-support/src
git commit -m "feat(consumer-support): consumidor 2 com IA de classificação de suporte (Smile), dataset, Dockerfile e testes"
```

## 8. feat(compose): orquestração completa

```bash
git add docker-compose.yml
git commit -m "feat(compose): docker-compose com 4 serviços, rede msg-network, healthcheck e depends_on"
```

## 9. test: avaliação de acurácia e utilitários de build

```bash
git add evaluation/pom.xml evaluation/src scripts/mvn-docker.sh scripts/mvn-docker.ps1
git commit -m "test(evaluation): acurácia das IAs sobre mensagens do gerador e scripts de Maven via Docker"
```

## 10. docs: documentação

```bash
git add README.md DEMONSTRACAO.md COMMITS.md docs/evidencias/README.md
git commit -m "docs: README completo, guia de demonstração, checklist de evidências e sequência de commits"
```

## 11. (Depois de tirar os prints) docs: evidências

```bash
git add docs/evidencias/*.png
git commit -m "docs(evidencias): capturas de tela do RabbitMQ e dos logs"
```

## 12. Enviar e conferir

```bash
git status                 # deve estar limpo
git log --oneline          # confere a sequência
git push -u origin main
```

> Se o push for recusado porque o remoto tem commits que você não tem localmente, rode
> `git pull --rebase origin main` e depois `git push -u origin main`.

### No PowerShell

Os comandos acima funcionam igual no PowerShell; apenas troque a continuação de linha `\` por
crase `` ` `` (ou escreva o `git add` em uma única linha). Exemplo do passo 3 em uma linha:

```powershell
git add common/pom.xml common/src/main/java/br/sd/classificador/common/Topology.java common/src/main/java/br/sd/classificador/common/Env.java common/src/main/java/br/sd/classificador/common/message common/src/main/java/br/sd/classificador/common/rabbit common/src/main/java/br/sd/classificador/common/nlp common/src/main/resources common/src/test
```

Passo 4 em uma linha:

```powershell
git add common/src/main/java/br/sd/classificador/common/consumer scripts/send-poison.sh scripts/send-poison.ps1
```
