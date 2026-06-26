# Límite de comentarios - Check Supervisión Segura

Se agregó límite de **220 caracteres** al campo de comentarios del formato **Check Supervisión Segura**.

Archivo modificado:

`app/src/main/java/com/sinopec/formatoshsecampo/features/supervisiondiaria/SupervisionDiariaScreen.kt`

Cambios:
- Campo visual: `Comentarios (máx. 220 caracteres)`.
- Filtro Android: `InputFilter.LengthFilter(220)`.
- Seguridad adicional al guardar en JSON y borrador: `.take(220)`.

No se modificó:
- Cifrado AES-GCM.
- `DATA_KEY`.
- `MARKER`.
- PDF/QR.
- TOS.
- Chaleco.
- Perfiles.
- Debug.
