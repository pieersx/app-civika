package pe.civika.shared.data

import pe.civika.shared.domain.CategoriasPiloto
import pe.civika.shared.domain.Estado
import pe.civika.shared.domain.Incidente

/** Datos de ejemplo de la zona piloto para avanzar sin backend y para las pruebas. */
class FakeIncidenteRepository : IncidenteRepository {

    override suspend fun listar(): List<Incidente> = incidentes

    private val incidentes = listOf(
        Incidente("CVK-000124", "Bache profundo en la pista", CategoriasPiloto.VIAL, Estado.DERIVADO, apoyos = 25, diasAbierto = 3, zona = "Alrededores de la Ciudad Universitaria"),
        Incidente("CVK-000118", "Vereda sin rampa en el cruce", CategoriasPiloto.ACCESIBILIDAD, Estado.EN_REVISION, apoyos = 11, diasAbierto = 1, zona = "Ciudad Universitaria UNMSM"),
        Incidente("CVK-000109", "Basura acumulada en la esquina", CategoriasPiloto.RESIDUOS, Estado.EN_ATENCION, apoyos = 15, diasAbierto = 7, zona = "Alrededores de la Ciudad Universitaria"),
        Incidente("CVK-000102", "Pista hundida frente al mercado", CategoriasPiloto.VIAL, Estado.EN_ATENCION, apoyos = 9, diasAbierto = 6, zona = "Alrededores de la Ciudad Universitaria"),
        Incidente("CVK-000131", "Contenedor desbordado", CategoriasPiloto.RESIDUOS, Estado.REPORTADO, apoyos = 4, diasAbierto = 0, zona = "Ciudad Universitaria UNMSM"),
        Incidente("CVK-000087", "Rampa bloqueada por un poste", CategoriasPiloto.ACCESIBILIDAD, Estado.RESUELTO, apoyos = 18, diasAbierto = 9, zona = "Ciudad Universitaria UNMSM"),
        Incidente("CVK-000090", "Hueco grande en la avenida", CategoriasPiloto.VIAL, Estado.RECHAZADO, apoyos = 0, diasAbierto = 4, zona = "Alrededores de la Ciudad Universitaria"),
    )
}
