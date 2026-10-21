package pe.civika.shared.domain

data class Incidente(
    val codigo: String,
    val titulo: String,
    val categoria: Categoria,
    val estado: Estado,
    val apoyos: Int,
    val diasAbierto: Int,
    val zona: String,
) {
    val prioridad: Int
        get() = calcularPrioridad(
            pesoCategoria = categoria.pesoPrioridad,
            apoyos = apoyos,
            diasAbierto = diasAbierto,
            plazoObjetivoDias = categoria.plazoObjetivoDias,
        )
}
