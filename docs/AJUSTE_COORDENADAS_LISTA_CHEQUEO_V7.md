# Ajuste coordenadas Lista de Chequeo V7

Se recalibraron únicamente las coordenadas de escritura del formato **Lista de Chequeo Supervisión Segura** para la plantilla nueva:

`app/src/main/res/drawable/lista_chequeo_supervision_editable_template.png`

Archivo modificado:

`app/src/main/java/com/sinopec/formatoshsecampo/core/pdf/SimplePdfService.kt`

Cambios realizados:

- Datos generales movidos debajo de las etiquetas para evitar encimarse.
- Checks SI/NO recalibrados por cada renglón real de la nueva plantilla.
- Observaciones movidas al área correcta.
- Firmas movidas a los recuadros inferiores.
- QR ajustado a la esquina inferior derecha.

No se modificó:

- DATA_KEY
- Marker de JSON cifrado
- AES-GCM
- Chaleco
- TOS
- Funciones debug
- Perfiles temporales
