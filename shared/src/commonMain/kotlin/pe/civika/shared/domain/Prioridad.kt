package pe.civika.shared.domain

/**
 * Prioridad de un incidente (RF-09, docs/arquitectura.md §6):
 * `peso × 10 + min(apoyos, 50) + días abierto × 2`, y `+20` si se superó el plazo objetivo de la categoría.
 *
 * La misma fórmula existe en SQL para que el servidor no dependa del cliente; los casos de PrioridadTest valen
 * para ambos lados.
 *
 * Los valores negativos se tratan como 0 en vez de lanzar una excepción: un dato inconsistente que llegue del
 * servidor no debe cerrar la app. La validación estricta vive en SQL.
 */
fun calcularPrioridad(pesoCategoria: Int, apoyos: Int, diasAbierto: Int, plazoObjetivoDias: Int): Int {
    val apoyosQueCuentan = apoyos.coerceIn(0, MAX_APOYOS_QUE_CUENTAN)
    val dias = diasAbierto.coerceAtLeast(0)
    val plazoVencido = dias > plazoObjetivoDias
    return pesoCategoria * 10 + apoyosQueCuentan + dias * 2 + if (plazoVencido) BONO_PLAZO_VENCIDO else 0
}

/** Más apoyos no suben la prioridad: evita que un incidente viral tape a uno grave con pocos vecinos. */
private const val MAX_APOYOS_QUE_CUENTAN = 50

private const val BONO_PLAZO_VENCIDO = 20
