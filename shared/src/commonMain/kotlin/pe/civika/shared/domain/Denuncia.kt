package pe.civika.shared.domain

/**
 * Denuncia de vecinos sobre el contenido de un reporte (RF-16), para la cola de moderación del panel.
 * Se revisa antes de que el reporte siga su curso: el problema puede ser real y aun así no poderse publicar.
 */
data class Denuncia(
    val id: String,
    val motivo: MotivoDenuncia,
    val titulo: String,
    /** Reporte denunciado: el código que se muestra en la tarjeta y en el aviso de resolución. */
    val codigoIncidente: String,
    /** Línea de contexto del mockup: quién denunció y cuándo. */
    val meta: String,
    val detalle: String,
)

/** Cada motivo define la acción principal de la tarjeta de moderación. */
enum class MotivoDenuncia(val etiqueta: String, val accionPrincipal: String) {
    ROSTROS("Foto con rostros visibles", "Ocultar la foto y aprobar"),
    DUPLICADO("Posible duplicado", "Aprobar como nuevo"),
    LENGUAJE("Lenguaje ofensivo", "Editar el texto y aprobar"),
}
