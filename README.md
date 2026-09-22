# Formatos HSE Campo

Proyecto Android independiente basado en lo aprendido con **Supervisión Segura**, preparado para capturar varios formatos HSE/campo desde celular.

## Formatos iniciales

1. Supervisión Segura.
2. Tarjeta de Observación de Seguridad, formato dos caras.
3. Lista de Chequeo / Formato Supervisión Segura basado en Word.

## Tecnología

- Android Studio.
- Kotlin.
- Vistas Android nativas.
- Generación PDF con `PdfDocument`.
- QR con folio usando ZXing.
- JSON cifrado AES-256-GCM.
- JSON cifrado embebido dentro del PDF con marcador estable.
- SHA-256 preparado para integridad.
- Fotos anexas con miniaturas, cámara/galería y botón quitar.
- Compartir con chooser del sistema, compatible con WhatsApp normal, WhatsApp Business, correo, Drive, etc.
- Control de caducidad/trial en `BuildConfig.LOCAL_EXPIRES_AT`.

## Dónde ajustar la compresión de fotos

Archivo:

`app/src/main/java/com/sinopec/formatoshsecampo/core/photo/PhotoQuality.kt`

Valores principales:

```kotlin
const val JPEG_QUALITY: Int = 72
const val MAX_IMAGE_SIDE_PX: Int = 1600
```

- Sube `JPEG_QUALITY` si quieres mejor calidad y PDFs más pesados.
- Baja `JPEG_QUALITY` si quieres PDF más ligero.
- Baja `MAX_IMAGE_SIDE_PX` si las fotos siguen pesadas.

El punto exacto donde se aplica está en:

`PhotoManager.kt`

```kotlin
resized.compress(Bitmap.CompressFormat.JPEG, PhotoQuality.JPEG_QUALITY, fos)
```

## Dónde ajustar caducidad/trial

Archivo:

`app/build.gradle.kts`

```kotlin
buildConfigField("String", "LOCAL_EXPIRES_AT", "\"2026-12-31\"")
```

## Próximo paso recomendado

Abrir en Android Studio, dejar que sincronice Gradle y probar primero el menú + generación PDF de cada formato.

## v20 - corrección compartir con Hermes

- Corrección de cierre al pulsar Generar y compartir.
- El perfil operativo se cifra con una clave AES válida de 16 bytes.
- Si el cifrado del perfil falla, el PDF se sigue compartiendo normalmente.
- No se agrega JSON extra al archivo PDF; el PDF conserva su JSON cifrado embebido original para el lector de escritorio.
- El perfil operativo solo viaja como extra seguro entre apps propias.

## v21 perfil operativo
- Se amplió el perfil operativo enviado a Hermes con nombre, ID, puesto/categoría, proyecto, brigada, volante y área/departamento.
- El perfil también queda dentro del JSON cifrado embebido como `perfil_operativo` sin obligar a imprimir esos campos en el PDF.
- El formato LISTA_SUPERVISION_SEGURA se identifica como "Check Supervisión Segura" al compartir.
