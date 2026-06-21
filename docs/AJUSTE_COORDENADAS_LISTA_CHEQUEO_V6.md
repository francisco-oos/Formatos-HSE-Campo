# Ajuste coordenadas Lista de Chequeo Supervisión Segura V6

Cambios aplicados únicamente al formato `SUPERVISION_WORD` / Lista de Chequeo Supervisión Segura.

## Archivos actualizados

- `app/src/main/res/drawable/lista_chequeo_supervision_segura.png`
- `app/src/main/res/drawable/lista_chequeo_supervision_editable_template.png`
- `docs/plantillas_editables/lista_chequeo_supervision_segura/lista_chequeo_supervision_segura.png`
- `docs/plantillas_editables/lista_chequeo_supervision_segura/lista_chequeo_supervision_segura_template.svg`
- `app/src/main/java/com/sinopec/formatoshsecampo/core/pdf/SimplePdfService.kt`

## Qué se ajustó

- Se reemplazó la imagen base por la plantilla nueva de `1617x1976 px`.
- Se recalibraron coordenadas de:
  - Brigada
  - Proyecto
  - Departamento supervisado
  - Nombre de quien supervisa
  - Puesto
  - Supervisor del trabajo
  - Fecha/hora
  - Checks SI/NO de las 20 preguntas
  - Observaciones
  - Firmas
  - QR

## Qué NO se tocó

- Chaleco salvavidas.
- Debug existente.
- Perfiles temporales.
- JSON cifrado.
- `DATA_KEY`.
- `MARKER`.
- AES-GCM.

La compatibilidad con el lector Python del JSON cifrado debe mantenerse intacta.
