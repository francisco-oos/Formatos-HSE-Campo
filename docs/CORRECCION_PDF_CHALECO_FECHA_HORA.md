# Corrección PDF Chaleco Salvavidas

Cambios aplicados:

1. En `SimplePdfService.kt`:
   - `addGenericPage` ahora recibe el JSON ya generado.
   - El encabezado del formato de chaleco imprime:
     - Folio
     - Fecha
     - Hora
   - Se aumentó el inicio del contenido de `130f` a `165f` para evitar que el folio se traslape con el cuerpo.
   - Se intenta cargar el logo usando `loadLogoBitmap()` igual que otros formatos. Si no encuentra logo, usa texto SINOPEC como respaldo.

2. En `InspeccionChalecoScreen.kt`:
   - Se captura `fecha` y `hora` una sola vez al generar el reporte.
   - El JSON del chaleco usa esa misma fecha/hora.
   - Las líneas visibles también incluyen fecha/hora, pero el PDF las filtra del cuerpo para no duplicarlas porque ya van en el encabezado.

Archivos modificados:

- `app/src/main/java/com/sinopec/formatoshsecampo/core/pdf/SimplePdfService.kt`
- `app/src/main/java/com/sinopec/formatoshsecampo/features/inspeccionchaleco/InspeccionChalecoScreen.kt`
