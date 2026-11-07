package pe.civika.shared.data

import pe.civika.shared.domain.Denuncia

/** Fuente de la cola de moderación. Hoy la implementa [FakeDenunciaRepository]; en el Sprint 4, supabase-kt. */
interface DenunciaRepository {
    suspend fun listarPendientes(): List<Denuncia>
}
