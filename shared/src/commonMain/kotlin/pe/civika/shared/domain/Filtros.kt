package pe.civika.shared.domain

/** Filtros del mapa (RF-08): por categoría (`service_code`) y por familia de estado. Vacío significa «todos». */
data class FiltroMapa(
    val categorias: Set<String> = emptySet(),
    val familias: Set<Familia> = emptySet(),
) {
    val activo: Boolean get() = categorias.isNotEmpty() || familias.isNotEmpty()

    fun aplica(incidente: Incidente): Boolean =
        (categorias.isEmpty() || incidente.categoria.serviceCode in categorias) &&
            (familias.isEmpty() || incidente.estado.familia in familias)

    fun conCategoria(serviceCode: String): FiltroMapa =
        copy(categorias = if (serviceCode in categorias) categorias - serviceCode else categorias + serviceCode)

    fun conFamilia(familia: Familia): FiltroMapa =
        copy(familias = if (familia in familias) familias - familia else familias + familia)
}

fun filtrar(incidentes: List<Incidente>, filtro: FiltroMapa): List<Incidente> = incidentes.filter(filtro::aplica)

/** Filtros simples de «Mis reportes» (RF-12). */
enum class FiltroMisReportes {
    TODOS,

    /** Lo que sigue sin resolver: abierto o en atención, y también lo que todavía no se envió. */
    ABIERTOS,
    RESUELTOS,

    /** Incidentes de otros que el usuario apoyó. */
    MIS_APOYOS,
}

/** ¿Entra este incidente del servidor en el filtro? Los envíos locales pendientes los decide quien los muestra. */
fun FiltroMisReportes.aplica(mi: MiIncidente): Boolean = when (this) {
    FiltroMisReportes.TODOS -> true
    FiltroMisReportes.ABIERTOS -> mi.incidente.estado.familia in FAMILIAS_POR_ATENDER
    FiltroMisReportes.RESUELTOS -> mi.incidente.estado == Estado.RESUELTO
    FiltroMisReportes.MIS_APOYOS -> mi.relacion == Relacion.APOYO
}

/** Los envíos que aún no llegaron cuentan como abiertos, y como apoyo si son «es el mismo problema». */
fun FiltroMisReportes.aplica(pendiente: ReportePendiente): Boolean = when (this) {
    FiltroMisReportes.TODOS, FiltroMisReportes.ABIERTOS -> true
    FiltroMisReportes.RESUELTOS -> false
    FiltroMisReportes.MIS_APOYOS -> pendiente.tipo == TipoEnvio.APOYO
}
