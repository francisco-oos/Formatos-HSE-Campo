# Arquitectura - Formatos HSE Campo

La app está separada por módulos para no mezclar la app estable anterior.

## Capas

- `domain`: contratos comunes (`HseReport`, `HseFormat`).
- `features/home`: menú principal de selección de formatos.
- `features/supervisiondiaria`: formato Supervisión Segura existente como referencia funcional.
- `features/tarjetaobservacion`: nuevo formato Tarjeta de Observación de Seguridad, frente/reverso.
- `features/supervisionword`: formato basado en Word `SSM-HSE-F-20`.
- `core/crypto`: AES-256-GCM.
- `core/pdf`: PDF, QR, marcador de JSON cifrado, hash SHA-256 y compartir.
- `core/photo`: miniaturas, cámara/galería, compresión y límite de fotos.
- `core/version`: caducidad/trial local y preparación para control remoto.
- `ui`: componentes visuales nativos reutilizables.

## Flujo por formato

1. Usuario elige formato.
2. Captura datos.
3. Agrega fotos desde cámara o galería.
4. La pantalla construye un `HseReport`.
5. `toJson()` genera el JSON estructurado.
6. `CryptoService` cifra el JSON con AES-256-GCM.
7. `SimplePdfService` genera el PDF visual.
8. `PdfPayloadEmbedder` incrusta el JSON cifrado al final del PDF.
9. El PDF se comparte desde el chooser del sistema.

## JSON esperado

Cada formato conserva la estructura base:

- `tipo_formato`
- `version_formato`
- `app_version`
- `fecha`
- `hora`
- `folio`
- `datos_generales`
- `observaciones`
- `checklist`
- `respuestas`
- `comentarios`
- `acciones`
- `fotos_anexas`
- `created_at_device`

No todos los formatos llenan todas las secciones desde V0.1.0, pero la arquitectura ya está lista para crecer.

## Regla para agregar nuevos formatos

Crear una carpeta nueva en `features/nombreformato` con:

- Pantalla de captura.
- Modelo o `HseReport`.
- Conversión JSON.
- Líneas visibles o generador PDF específico.
- Uso de `PhotoAttachmentPanel`.
