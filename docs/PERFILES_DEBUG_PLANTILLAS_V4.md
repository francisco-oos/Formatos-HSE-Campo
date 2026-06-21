# Cambios V4 - Debug, perfiles temporales y plantilla Lista de Chequeo

## 1. Debug centralizado

Se agregó:

`app/src/main/java/com/sinopec/formatoshsecampo/core/debug/DebugConfig.kt`

Ahí puedes activar/desactivar el llenado automático de prueba por formulario:

```kotlin
object DebugConfig {
    const val SUPERVISION_DIARIA = false
    const val TARJETA_OBSERVACION = false
    const val LISTA_CHEQUEO_SUPERVISION = false
    const val INSPECCION_CHALECO = false
}
```

Cambia a `true` solo el formato que estés probando.

## 2. Perfiles temporales

Se agregó:

`app/src/main/java/com/sinopec/formatoshsecampo/core/profile/UserProfileStore.kt`

La app guarda localmente máximo los últimos 4 perfiles usados en el teléfono.

Campos que puede recordar según el formato:

- Nombre
- ID empleado
- Puesto/categoría
- Departamento del jefe
- Área/departamento
- Volante
- Brigada
- Proyecto

Al entrar a cada formulario carga automáticamente el último perfil usado.

Botón nuevo:

`No soy yo / cambiar perfil`

Permite seleccionar uno de los perfiles guardados o capturar uno nuevo.

## 3. Lista de Chequeo Supervisión Segura

El PDF ahora prioriza la plantilla nueva:

`app/src/main/res/drawable/lista_chequeo_supervision_editable_template.png`

Si esa imagen no existe, usa como respaldo la anterior:

`lista_chequeo_supervision_segura.png`

La fuente editable está en:

`docs/plantillas_editables/lista_chequeo_supervision/lista_chequeo_supervision_segura_template.svg`

## 4. Seguridad JSON

No se modificó:

- DATA_KEY
- MARKER
- AES-GCM
- CryptoService
- PdfPayloadEmbedder

El lector Python debe seguir funcionando igual siempre que el proyecto mantenga la misma clave.
