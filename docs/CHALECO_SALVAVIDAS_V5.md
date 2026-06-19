# Inspección de Chaleco Salvavidas V5

Cambios aplicados:

- Se reactivó el botón del módulo en `HomeScreen.kt`.
- Se integraron las imágenes nombradas por sección:
  - `chaleco_salvavidas_general.png`
  - `chaleco_cintas_correas.png`
  - `chaleco_costuras.png`
  - `chaleco_hebillas_broches.png`
  - `chaleco_material_flotacion.png`
  - `chaleco_reflejantes.png`
- Cada parte abre una ventana interactiva con su imagen guía, checklist, estado y observación.
- Se agregaron catálogos controlados a chaleco: Brigada, Proyecto, Área/Departamento, Volante y Puesto.
- Se agregó `DEBUG_CHALECO`. Para rellenar datos de prueba cambia:

```kotlin
private val DEBUG_CHALECO = true
```

Ubicación:

`app/src/main/java/com/sinopec/formatoshsecampo/features/inspeccionchaleco/InspeccionChalecoScreen.kt`

No se cambió la lógica existente de PDF, JSON ni fotos; el módulo usa `SimplePdfService`, `PhotoAttachmentPanel` y el JSON embebido igual que los demás formatos.
