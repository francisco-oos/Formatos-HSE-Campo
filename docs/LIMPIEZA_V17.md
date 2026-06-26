# Limpieza V17

Esta versión limpia conserva la lógica funcional de la app y elimina archivos innecesarios del paquete del proyecto.

## No se tocó
- JSON cifrado.
- DATA_KEY.
- MARKER.
- AES-GCM.
- QR.
- PDF.
- Debug.
- Perfiles.
- Chaleco funcional.

## Se eliminó del ZIP
- Carpeta `.git/`.
- APKs generadas.
- Cachés/carpetas `build/` y `.gradle/`.
- `local.properties`.
- Imágenes de referencia grandes en `docs/plantillas_editables`.
- Recursos PNG no usados directamente por la app.

## Se conservó
- Código fuente.
- Plantillas usadas por la app en `app/src/main/res/drawable`.
- SVG editables en `docs/plantillas_editables`.
- Documentación del proyecto.
- Gradle Wrapper.

## Para usar
1. Descomprime este ZIP.
2. Ábrelo en Android Studio.
3. Espera el Sync de Gradle.
4. Ejecuta `Build > Clean Project`.
5. Ejecuta `Build > Rebuild Project`.
