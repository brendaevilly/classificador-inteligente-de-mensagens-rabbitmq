#!/usr/bin/env bash
# Executa o Maven dentro de um container (não precisa de Maven/JDK instalados).
#   ./scripts/mvn-docker.sh                        -> mvn -B clean package (compila e roda TODOS os testes)
#   ./scripts/mvn-docker.sh -B -q test             -> argumentos repassados ao mvn
#   ./scripts/mvn-docker.sh -B test -pl evaluation -am
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
mkdir -p "$HOME/.m2"
if [ "$#" -eq 0 ]; then
    set -- -B clean package
fi
exec docker run --rm -v "$ROOT:/ws" -v "$HOME/.m2:/root/.m2" -w /ws maven:3.9-eclipse-temurin-17 mvn "$@"
