# Firebase App Distribution (prodDebug)

## Regla de versión

Cada release en App Distribution debe usar un **versionCode** y **versionName** nuevos (ambos +1 respecto al último publicado), definidos en `presentation/build.gradle` antes de compilar.

Última publicada: **2.6.9 (61)** (`prodDebug`, API `https://api.gigafiberperu.tech/ispadmin/`, grupo `gigafiber`). Siguiente: **2.6.10 (62)**.

## Comando recomendado

```bash
chmod +x scripts/firebase-distribute-prod-debug.sh   # una vez
./scripts/firebase-distribute-prod-debug.sh "Notas opcionales del release"
```

El script incrementa versión, ejecuta `assembleProdDebug` y sube al grupo `gigafiber` en el proyecto `ispadmin-687ca`.

## Manual

1. Subir `versionCode` y `versionName` en `presentation/build.gradle`.
2. `./gradlew :presentation:assembleProdDebug`
3. `npx firebase-tools appdistribution:distribute` con `--app 1:74615302027:android:87751a6bad0a78cbb0318e`, `--project ispadmin-687ca`, `--groups gigafiber`.

Requiere sesión Firebase CLI (`npx firebase-tools login`).
