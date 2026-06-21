# Plantillas editables - Formatos HSE Campo

Esta versión conserva el flujo seguro actual:

- No se cambió `DATA_KEY`.
- No se cambió el marcador `%%SUPERVISION_SEGURA_JSON_ENCRYPTED_BASE64:`.
- No se cambió AES-GCM ni la forma de embebido del JSON.
- La sección de chaleco no se modificó.

## Corrección aplicada

En `SimplePdfService.kt`, el formato **Supervisión Segura** generaba una imagen interna en alta resolución. El PDF estaba dibujando esa imagen sin escalarla, por eso solo se veía una esquina. Ahora se dibuja usando `RectF(0f, 0f, 595f, 842f)` para ajustarla al tamaño real de la página.

## Dónde están las plantillas

```text
docs/plantillas_editables/
├── supervision_segura/
│   ├── supervision_segura_template.svg
│   └── supervision_segura_template_preview.png
├── tarjeta_observacion/
│   ├── tarjeta_observacion_frente.svg
│   ├── tarjeta_observacion_frente_preview.png
│   ├── tarjeta_observacion_reverso.svg
│   └── tarjeta_observacion_reverso_preview.png
├── lista_chequeo_supervision/
│   ├── lista_chequeo_supervision_segura_template.svg
│   └── lista_chequeo_supervision_segura_template_preview.png
└── referencias_png_actuales/
    ├── supervision_segura_template.png
    └── lista_chequeo_supervision_segura.png
```

## Cómo editarlas

Abrir los archivos `.svg` con **Inkscape**.

Cada plantilla tiene dos capas principales:

- `diseno_base`: líneas, títulos, logos y estructura visual.
- `campos_dinamicos`: zonas donde Android escribe datos, checks, comentarios o QR.

## Cómo aplicar cambios visuales

Por ahora el flujo sigue siendo:

```text
SVG editable
↓ exportar PNG
app/src/main/res/drawable/
↓
SimplePdfService.kt escribe encima por coordenadas
↓
PDF final + JSON cifrado embebido
```

Si mueves cuadros o columnas, posiblemente habrá que ajustar coordenadas en `SimplePdfService.kt`.

## Recursos PNG agregados

También se generaron PNG de referencia en `app/src/main/res/drawable/`:

- `tarjeta_observacion_frente_template.png`
- `tarjeta_observacion_reverso_template.png`
- `lista_chequeo_supervision_editable_template.png`

Estos recursos quedan disponibles para una migración posterior. No se cambió la lógica estable de TOS ni la del checklist para evitar romper la beta.
