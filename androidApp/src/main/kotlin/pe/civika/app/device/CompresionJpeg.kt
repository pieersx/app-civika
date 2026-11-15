package pe.civika.app.device

/** Reglas puras para que una foto quepa en el límite de 2 MB del bucket (docs/arquitectura.md §11). */
object CompresionJpeg {
    const val MAX_BYTES = 2 * 1024 * 1024

    /** Lado mayor máximo antes de codificar: una foto de calle se entiende de sobra con 2048 px. */
    const val LADO_MAXIMO_PX = 2048

    private val CALIDADES = intArrayOf(90, 80, 70, 60, 50, 40)
    private val ESCALAS = floatArrayOf(1f, 0.8f, 0.64f, 0.5f, 0.35f)

    /**
     * Prueba calidades de la más alta a la más baja y, si aún no cabe, reduce las dimensiones.
     * Devuelve los bytes del primer intento que cabe en [maxBytes], o `null` si ninguno cabe.
     */
    fun ajustar(maxBytes: Int = MAX_BYTES, codificar: (calidad: Int, escala: Float) -> ByteArray): ByteArray? {
        for (escala in ESCALAS) {
            for (calidad in CALIDADES) {
                val bytes = codificar(calidad, escala)
                if (bytes.size <= maxBytes) return bytes
            }
        }
        return null
    }

    /** `inSampleSize` (potencia de 2) para decodificar sin cargar la imagen completa en memoria. */
    fun muestreo(ancho: Int, alto: Int, ladoMaximo: Int = LADO_MAXIMO_PX): Int {
        var muestreo = 1
        val mayor = maxOf(ancho, alto)
        while (mayor / (muestreo * 2) >= ladoMaximo) muestreo *= 2
        return muestreo
    }
}
