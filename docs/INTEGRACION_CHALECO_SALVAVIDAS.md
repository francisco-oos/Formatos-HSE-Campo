# Integración — Inspección de Chaleco Salvavidas

Este ZIP ya viene integrado sobre el estado actual del proyecto `Formatos-HSE-Campo`.
No reemplaza el flujo de cámara, galería, PDF ni JSON existente; reutiliza `PhotoAttachmentPanel`, `SimplePdfService`, `CryptoService` y el menú principal.

## Archivos agregados

- `app/src/main/java/com/sinopec/formatoshsecampo/features/inspeccionchaleco/InspeccionChalecoScreen.kt`
- `app/src/main/res/drawable/chaleco_salvavidas_frente.png`
- `app/src/main/res/drawable/chaleco_salvavidas_espalda.png`
- `docs/INTEGRACION_CHALECO_SALVAVIDAS.md`
- `docs/EJEMPLO_JSON_CHALECO.json`

## Archivos modificados

- `domain/HseFormat.kt`
  - Se agregó `INSPECCION_CHALECO`.

- `features/home/HomeScreen.kt`
  - Se mejoró la interfaz de entrada con tarjetas profesionales.
  - Se agregó la tarjeta: `Inspección de Chaleco Salvavidas`.

- `ui/Ui.kt`
  - Se agregó soporte para fondos redondeados reutilizables.

- `core/pdf/SimplePdfService.kt`
  - Se agregó soporte para generar PDF del nuevo formato usando el render genérico existente.
  - El JSON sigue embebido y cifrado como los otros formatos.

## Cómo probar

1. Abre el proyecto en Android Studio.
2. Sincroniza Gradle.
3. Ejecuta la app.
4. En el menú principal selecciona `Inspección de Chaleco Salvavidas`.
5. Llena datos básicos.
6. Toca cada parte del chaleco y marca:
   - Correcto
   - Observación
   - Requiere reemplazo
7. Agrega fotos si aplica.
8. Presiona `Generar PDF y compartir`.

## JSON generado

El JSON se genera dentro del mismo flujo `HseReport.toJson()` y se embebe cifrado dentro del PDF.
Incluye:

- Datos generales
- Partes inspeccionadas
- Estado de cada parte
- Checklist por parte
- Observación por parte
- Resumen automático
- Dictamen final
- Número de fotos anexas

## Dictamen automático

- Si todo está correcto: `APTO PARA USO`
- Si existe una observación: `APTO CON OBSERVACIONES`
- Si existe al menos un reemplazo: `NO APTO PARA USO`

## Nota

No se usa número de serie, como se solicitó.
