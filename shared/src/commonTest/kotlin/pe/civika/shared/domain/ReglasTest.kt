package pe.civika.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class DiasAbiertosTest {

    @Test
    fun creadoAyerALas23DeLimaYListadoHoyALas0030DaUnDia() {
        // 23:00 en Lima = 04:00 UTC del día siguiente; 00:30 en Lima = 05:30 UTC.
        val creado = Instant.parse("2026-11-02T04:00:00Z")
        val ahora = Instant.parse("2026-11-02T05:30:00Z")
        assertEquals(1, diasCalendarioLima(creado, ahora))
    }

    @Test
    fun elMismoDiaDaCero() {
        val creado = Instant.parse("2026-11-02T13:00:00Z") // 08:00 en Lima
        val ahora = Instant.parse("2026-11-03T04:59:00Z") // 23:59 en Lima, mismo día
        assertEquals(0, diasCalendarioLima(creado, ahora))
    }

    @Test
    fun elRelojAtrasadoNuncaDaDiasNegativos() {
        val creado = Instant.parse("2026-11-05T15:00:00Z")
        val ahora = Instant.parse("2026-11-01T15:00:00Z")
        assertEquals(0, diasCalendarioLima(creado, ahora))
    }

    @Test
    fun unaSemanaSonSieteDias() {
        assertEquals(7, diasCalendarioLima(Instant.parse("2026-11-01T15:00:00Z"), Instant.parse("2026-11-08T15:00:00Z")))
    }
}

class ValidacionesTest {

    @Test
    fun laDescripcionNecesitaDiezCaracteresSinContarLosEspaciosDeLosExtremos() {
        assertEquals(Validacion.VACIO, Validaciones.descripcion("   "))
        assertEquals(Validacion.MUY_CORTO, Validaciones.descripcion("  bache    "))
        assertEquals(Validacion.OK, Validaciones.descripcion("Bache hond"))
    }

    @Test
    fun laDescripcionAdmiteHastaDosMilCaracteres() {
        assertEquals(Validacion.OK, Validaciones.descripcion("a".repeat(2000)))
        assertEquals(Validacion.MUY_LARGO, Validaciones.descripcion("a".repeat(2001)))
    }

    @Test
    fun elCorreoDebeTenerArrobaYDominio() {
        assertEquals(Validacion.OK, Validaciones.correo(" ana@unmsm.edu.pe "))
        assertEquals(Validacion.FORMATO, Validaciones.correo("ana@unmsm"))
        assertEquals(Validacion.FORMATO, Validaciones.correo("ana unmsm.edu.pe"))
        assertEquals(Validacion.VACIO, Validaciones.correo(""))
    }

    @Test
    fun laContrasenaPideOchoCaracteres() {
        assertEquals(Validacion.MUY_CORTO, Validaciones.contrasena("1234567"))
        assertEquals(Validacion.OK, Validaciones.contrasena("12345678"))
        assertEquals(Validacion.MUY_LARGO, Validaciones.contrasena("a".repeat(73)))
    }

    @Test
    fun elNombreVaDeDosAOchentaCaracteres() {
        assertEquals(Validacion.MUY_CORTO, Validaciones.nombre("A"))
        assertEquals(Validacion.OK, Validaciones.nombre("Ana"))
        assertEquals(Validacion.MUY_LARGO, Validaciones.nombre("a".repeat(81)))
    }

    @Test
    fun elMotivoDeDenunciaVaDeTresAQuinientosCaracteres() {
        assertEquals(Validacion.MUY_CORTO, Validaciones.motivoDenuncia("no"))
        assertEquals(Validacion.OK, Validaciones.motivoDenuncia("Es falso"))
        assertEquals(Validacion.MUY_LARGO, Validaciones.motivoDenuncia("a".repeat(501)))
    }

    @Test
    fun unReporteLlevaDeUnaATresFotos() {
        assertEquals(Validacion.VACIO, Validaciones.cantidadDeFotos(0))
        assertEquals(Validacion.OK, Validaciones.cantidadDeFotos(3))
        assertEquals(Validacion.MUY_LARGO, Validaciones.cantidadDeFotos(4))
    }
}

class ZonaPilotoTest {

    @Test
    fun elCentroDelCampusEstaDentro() {
        assertTrue(ZonaPiloto.contiene(ZonaPiloto.centro.latitud, ZonaPiloto.centro.longitud))
    }

    @Test
    fun laPlazaDeArmasDeLimaEstaFuera() {
        assertFalse(ZonaPiloto.contiene(-12.0464, -77.0428))
    }

    @Test
    fun losBordesDelSeedCuentanComoDentro() {
        assertTrue(ZonaPiloto.contiene(ZonaPiloto.LAT_SUR, ZonaPiloto.LON_OESTE))
        assertTrue(ZonaPiloto.contiene(ZonaPiloto.LAT_NORTE, ZonaPiloto.LON_ESTE))
    }

    @Test
    fun laDistanciaEntreDosPuntosCercanosEsDeUnosMetros() {
        // 0,0001° de latitud son unos 11 metros.
        val metros = distanciaMetros(-12.0575, -77.0845, -12.0576, -77.0845)
        assertTrue(metros in 10.0..12.5, "metros = $metros")
    }

    @Test
    fun masCercanosOrdenaPorDistanciaYDejaAlFinalLosQueNoTienenCoordenadas() {
        fun incidente(id: String, lat: Double?, lon: Double?, estado: Estado = Estado.DERIVADO) =
            Incidente(id, id, id, CategoriasPiloto.VIAL, estado, 0, 0, "zona", lat, lon)

        val orden = masCercanos(
            listOf(
                incidente("lejos", -12.0650, -77.0900),
                incidente("sin", null, null),
                incidente("cerca", -12.0576, -77.0846),
                incidente("resuelto", -12.0575, -77.0845, Estado.RESUELTO),
            ),
            latitud = -12.0575,
            longitud = -77.0845,
        ).map { it.id }

        assertEquals(listOf("cerca", "lejos", "sin"), orden)
    }
}

class IncidenteTest {

    private fun incidente(estado: Estado, dias: Int) =
        Incidente("1", "CVK-000001", "Bache", CategoriasPiloto.RESIDUOS, estado, 0, dias, "zona")

    @Test
    fun elPlazoVenceCuandoPasanMasDiasQueElObjetivoDeLaCategoria() {
        assertFalse(incidente(Estado.DERIVADO, 5).plazoVencido) // plazo de residuos: 5 días
        assertTrue(incidente(Estado.DERIVADO, 6).plazoVencido)
        assertEquals(-1, incidente(Estado.DERIVADO, 6).diasParaElPlazo)
    }

    @Test
    fun unIncidenteResueltoNoTienePlazoVencido() {
        assertFalse(incidente(Estado.RESUELTO, 40).plazoVencido)
    }

    @Test
    fun elAutorNoApoyaNiDenunciaSuPropioIncidente() {
        val detalle = DetalleIncidente(incidente(Estado.DERIVADO, 1), null, null, yoApoyo = false, esMio = true, miVerificacion = null, motivoRechazo = null)
        assertFalse(detalle.puedeApoyar)
        assertFalse(detalle.puedeDenunciar)
        assertTrue(detalle.loSigo)
    }

    @Test
    fun soloVerificaQuienSigueUnIncidenteResuelto() {
        val base = DetalleIncidente(incidente(Estado.RESUELTO, 1), null, null, yoApoyo = false, esMio = false, miVerificacion = null, motivoRechazo = null)
        assertFalse(base.puedeVerificar)
        assertTrue(base.copy(yoApoyo = true).puedeVerificar)
        assertFalse(base.copy(yoApoyo = true, incidente = incidente(Estado.EN_ATENCION, 1)).puedeVerificar)
        assertFalse(base.puedeApoyar) // ya está cerrado
    }

    @Test
    fun soloLosErroresDelServidorSobreLosDatosSonDefinitivos() {
        assertTrue(ErrorCivika.FueraDeZona().esDefinitivo)
        assertTrue(ErrorCivika.DatosInvalidos("x").esDefinitivo)
        assertFalse(ErrorCivika.SinConexion().esDefinitivo)
        assertFalse(ErrorCivika.SesionRequerida().esDefinitivo)
    }
}
