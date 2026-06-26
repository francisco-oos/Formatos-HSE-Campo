package com.sinopec.formatoshsecampo.core.config

/**
 * Catálogos controlados para capturas HSE.
 * Mantener aquí las listas evita datos escritos con variantes diferentes
 * y facilita ampliar opciones en versiones futuras.
 */
object FormOptions {
    val brigadas = listOf("371")

    val proyectos = listOf("ALACTE")

    val puestos = listOf(
        "Cabo",
        "Checador",
        "Tirador",
        "Sobrestante",
        "Observador",
        "Obrero",
        "Conductor",
        "Op. Drone",
        "Técnico en Mantenimiento",
        "Ingeniero Electrónico",
        "Jefe de Departamento",
        "Oficina"
    )

    val tiposObservacionTarjeta = listOf(
        "Acto Inseguro",
        "Condición Insegura",
        "Casi Accidente",
        "Observación de Seguridad Realizada",
        "Acción Positiva",
        "Oportunidad de Mejora",
        "Intervención de Seguridad",
        "Otro"
    )

    val areasDepartamentos = listOf(
        "Adquisición de datos (Registro)",
        "Perforación",
        "Topografía",
        "Gestoría",
        "Logística",
        "Operaciones",
        "Inmuebles",
        "QC",
        "Administracion",
        "HSE",
        "RRHH",
        "Transportes",
        "Comedor"
    )

    val volantes = listOf(
        "Base",
        "V1",
        "V2",
        "V3",
        "V4",
        "V5",
        "V6",
        "V7",
        "V8",
        "V9",
        "V10"
    )
}
