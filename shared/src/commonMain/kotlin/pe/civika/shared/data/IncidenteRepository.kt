package pe.civika.shared.data

import pe.civika.shared.domain.Incidente

/** Fuente de incidentes. Hoy la implementa [FakeIncidenteRepository]; en el Sprint 2, supabase-kt. */
interface IncidenteRepository {
    suspend fun listar(): List<Incidente>
}
