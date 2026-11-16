package pe.civika.app.device

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompresionJpegTest {

    @Test
    fun siLaPrimeraCalidadYaCabeNoSigueBuscando() {
        var intentos = 0

        val bytes = CompresionJpeg.ajustar(maxBytes = 1_000) { _, _ ->
            intentos++
            ByteArray(900)
        }

        assertEquals(900, bytes?.size)
        assertEquals(1, intentos)
    }

    @Test
    fun bajaLaCalidadHastaQueCabe() {
        val bytes = CompresionJpeg.ajustar(maxBytes = 1_000) { calidad, _ -> ByteArray(calidad * 15) }

        // 90→1350, 80→1200, 70→1050, 60→900 (cabe)
        assertEquals(900, bytes?.size)
    }

    @Test
    fun siNoCabeConNingunaCalidadReduceLasDimensiones() {
        val escalasProbadas = mutableSetOf<Float>()

        val bytes = CompresionJpeg.ajustar(maxBytes = 1_000) { _, escala ->
            escalasProbadas += escala
            ByteArray((2_000 * escala).toInt())
        }

        assertNotNull(bytes)
        assertTrue(bytes.size <= 1_000)
        assertTrue(escalasProbadas.size > 1)
    }

    @Test
    fun siNadaCabeDevuelveNuloEnVezDeSubirUnaFotoQueElBucketRechazaria() {
        assertNull(CompresionJpeg.ajustar(maxBytes = 10) { _, _ -> ByteArray(100) })
    }

    @Test
    fun elLimiteEsDeDosMegabytesComoElBucket() {
        assertEquals(2 * 1024 * 1024, CompresionJpeg.MAX_BYTES)
    }

    @Test
    fun elMuestreoReduceFotosGrandesSinPasarseDeLaMitad() {
        assertEquals(1, CompresionJpeg.muestreo(1920, 1080))
        assertEquals(2, CompresionJpeg.muestreo(4160, 3120)) // 12 MP: se lee a la mitad (2080 px)
        assertEquals(2, CompresionJpeg.muestreo(8000, 6000))
        assertEquals(4, CompresionJpeg.muestreo(12000, 9000))
    }
}
