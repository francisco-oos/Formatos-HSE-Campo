# Supervisión Segura - plantilla editable

Este ajuste migra el formato visual de **Supervisión Segura** a una plantilla editable.

## Archivos importantes

### Plantilla editable

```text
docs/plantillas_editables/supervision_segura/supervision_segura_template.svg
```

Ábrela con **Inkscape**. Ahí puedes mover líneas, títulos, secciones, preguntas y cuadros.

### Imagen usada por Android

```text
app/src/main/res/drawable/supervision_segura_template.png
```

Esta imagen se exporta desde el SVG y es la que usa Android como fondo del PDF.

### Referencia visual

```text
docs/plantillas_editables/supervision_segura/referencia_pdf_actual.pdf
```

Es el PDF bueno que se usó como referencia para recrear la plantilla.

## Flujo recomendado de edición

1. Abrir el SVG en Inkscape.
2. Editar diseño, textos, preguntas o cuadros.
3. Exportar como PNG con el mismo tamaño/proporción.
4. Reemplazar:

```text
app/src/main/res/drawable/supervision_segura_template.png
```

5. Si se mueven celdas o cuadros, ajustar coordenadas en:

```text
app/src/main/java/com/sinopec/formatoshsecampo/core/pdf/SimplePdfService.kt
```

Buscar:

```kotlin
renderSupervisionSeguraAsBitmap
```

Ahí están las coordenadas de:

- fecha/hora
- volante
- categoría
- nombre
- ID empleado
- checks SI/NO/CAMBIO
- comentarios
- QR

## Seguridad / JSON cifrado

No se cambió el sistema de cifrado.

Se conserva:

- `DATA_KEY`
- `MARKER`
- AES-GCM
- JSON cifrado embebido al final del PDF
- compatibilidad con `decifrado_JSON.PY`

El SVG y el PNG son solo diseño visual; no contienen el JSON real del reporte.

## Corrección de teclado y botón inferior

También se ajustó la interfaz para que:

- El teclado no tape el campo que se está escribiendo.
- El botón `Generar PDF y compartir` no quede debajo de la barra de navegación Android.

Archivos modificados:

```text
app/src/main/AndroidManifest.xml
app/src/main/java/com/sinopec/formatoshsecampo/MainActivity.kt
app/src/main/java/com/sinopec/formatoshsecampo/ui/Ui.kt
```
