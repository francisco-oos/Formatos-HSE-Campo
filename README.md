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
