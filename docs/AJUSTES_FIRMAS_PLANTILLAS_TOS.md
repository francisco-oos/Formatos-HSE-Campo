# Ajustes aplicados: firmas, plantillas y TOS

## 1. Lista de Chequeo Supervisión Segura

- Se actualizó `app/src/main/res/drawable/lista_chequeo_supervision_editable_template.png` usando la imagen correcta con las 20 preguntas y los recuadros de firma.
- Se completó la plantilla editable:

```text
docs/plantillas_editables/lista_chequeo_supervision/lista_chequeo_supervision_segura_template.svg
```

- Se agregó captura de firma con el dedo en el formulario:
  - Firma del supervisor del trabajo.
  - Firma de quien realizó la supervisión.

Las firmas se guardan dentro del JSON cifrado en el bloque:

```json
"firmas": {
  "supervisor_trabajo_png_b64": "...",
  "quien_supervisa_png_b64": "...",
  "supervisor_trabajo_presente": true,
  "quien_supervisa_presente": true
}
```

El cifrado no fue cambiado.

## 2. Tarjeta de Observación de Seguridad

- Se corrigió la captura para que los encabezados no tengan selección:
  - Conducta Personal.
  - Ambiente de Trabajo.
  - Equipo/Herramientas.
  - Procedimientos.

Ahora solo se seleccionan sus puntos internos.

- El PDF de Tarjeta de Observación ahora usa las plantillas PNG corregidas:

```text
app/src/main/res/drawable/tarjeta_observacion_frente_template.png
app/src/main/res/drawable/tarjeta_observacion_reverso_template.png
```

Estas imágenes fueron tomadas desde `docs/plantillas_editables/tarjeta_observacion/`.

## 3. Supervisión Segura diaria

- Se actualizó el PNG base desde la plantilla ubicada en `docs`.

```text
app/src/main/res/drawable/supervision_segura_template.png
```

## 4. Seguridad del JSON

No se modificó:

```text
DATA_KEY
MARKER
AES-GCM
CryptoService
PdfPayloadEmbedder
```

Tu script Python debe seguir leyendo los PDF generados con la misma clave.
