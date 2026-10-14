<#
.SYNOPSIS
    Executa o Maven dentro de um container (não precisa de Maven/JDK instalados no Windows).

.EXAMPLE
    .\scripts\mvn-docker.ps1                     # mvn -B clean package  (compila e roda TODOS os testes)
    .\scripts\mvn-docker.ps1 -B -q test          # argumentos repassados ao mvn
    .\scripts\mvn-docker.ps1 -B test -pl evaluation -am   # só a medição de acurácia sobre mensagens do gerador
#>
$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$m2 = Join-Path $HOME ".m2"
New-Item -ItemType Directory -Force $m2 | Out-Null

$mvnArgs = if ($args.Count -gt 0) { $args } else { @("-B", "clean", "package") }

docker run --rm -v "${root}:/ws" -v "${m2}:/root/.m2" -w /ws maven:3.9-eclipse-temurin-17 mvn @mvnArgs
exit $LASTEXITCODE
