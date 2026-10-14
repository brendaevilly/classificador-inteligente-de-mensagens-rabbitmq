# syntax=docker/dockerfile:1
# Container 4 - Consumidor 2 (classificação de solicitações de suporte com Smile).
# Build multi-stage: o contexto é a RAIZ do repositório (projeto Maven multi-módulo).

# ---------- Etapa 1: build com Maven ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY common/pom.xml common/
COPY generator/pom.xml generator/
COPY consumer-sentiment/pom.xml consumer-sentiment/
COPY consumer-support/pom.xml consumer-support/
COPY evaluation/pom.xml evaluation/
COPY common/src common/src
COPY consumer-support/src consumer-support/src
# Os testes rodam fora do build da imagem (ver README); aqui só empacotamos o fat jar.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -pl consumer-support -am package -DskipTests

# ---------- Etapa 2: runtime com JRE enxuto ----------
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /build/consumer-support/target/consumer-support-*-all.jar /app/app.jar
USER app
ENV LANG=C.UTF-8 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Dfile.encoding=UTF-8"
# "exec" faz o java ser o PID 1 e receber o SIGTERM do "docker stop" (shutdown gracioso).
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
