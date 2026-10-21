package pe.civika.shared.domain

/**
 * Transiciones de estado permitidas y quién puede hacerlas (docs/arquitectura.md §5).
 *
 * La app y el panel la usan para mostrar solo las acciones válidas. La RPC `cambiar_estado` valida lo mismo en el
 * servidor, porque el servidor nunca confía en el cliente. Que la entidad solo toque *sus* incidentes lo controla RLS.
 */
object TransicionesEstado {

    private val permitidas: Map<Pair<Estado, Estado>, Set<Rol>> = mapOf(
        (Estado.REPORTADO to Estado.DERIVADO) to setOf(Rol.SISTEMA),
        (Estado.REPORTADO to Estado.EN_REVISION) to setOf(Rol.SISTEMA),
        (Estado.DERIVADO to Estado.EN_REVISION) to setOf(Rol.SISTEMA, Rol.ENTIDAD),
        (Estado.EN_ATENCION to Estado.EN_REVISION) to setOf(Rol.SISTEMA, Rol.ENTIDAD),
        (Estado.EN_REVISION to Estado.DERIVADO) to setOf(Rol.MODERADOR),
        (Estado.EN_REVISION to Estado.RECHAZADO) to setOf(Rol.MODERADOR),
        (Estado.DERIVADO to Estado.EN_ATENCION) to setOf(Rol.ENTIDAD),
        (Estado.EN_ATENCION to Estado.RESUELTO) to setOf(Rol.ENTIDAD),
        (Estado.RESUELTO to Estado.EN_ATENCION) to setOf(Rol.SISTEMA),
    )

    fun permitida(desde: Estado, hacia: Estado, rol: Rol): Boolean =
        rol in permitidas[desde to hacia].orEmpty()

    fun siguientes(desde: Estado, rol: Rol): Set<Estado> =
        permitidas
            .filter { (transicion, roles) -> transicion.first == desde && rol in roles }
            .keys
            .mapTo(mutableSetOf()) { it.second }
}
