# Ajuste V15 - Comentarios y fotos

Cambios realizados:

- En `SimplePdfService.kt`, solo para el formato **Check Supervisión Segura**, se ajustó el render de comentarios:
  - fuente más pequeña,
  - salto de línea más compacto,
  - máximo 4 líneas visibles,
  - mejor distribución dentro del recuadro de comentarios.

- En `PhotoQuality.kt` se dejó el límite máximo en **5 fotos por reporte**.

- En `SimplePdfService.kt` el anexado al PDF también se limita a 5 fotos para coincidir con la interfaz.

No se modificó:

- JSON cifrado,
- `DATA_KEY`,
- marcador del JSON,
- AES-GCM,
- QR,
- chaleco,
- TOS,
- perfiles,
- debug.
