package pe.civika.shared.domain

/**
 * Categoría de incidente. Los valores viven en la tabla `categorias` de Supabase (RNF-08); [CategoriasPiloto] son
 * los del seed del piloto, usados por los repositorios fake.
 */
data class Categoria(
    val id: Int,
    val serviceCode: String,
    val nombre: String,
    val pesoPrioridad: Int,
    val radioDuplicadoM: Int,
    val ventanaDias: Int,
    val plazoObjetivoDias: Int,
    val activa: Boolean = true,
)

object CategoriasPiloto {
    val VIAL = Categoria(1, "vial", "Pistas y veredas", pesoPrioridad = 3, radioDuplicadoM = 50, ventanaDias = 30, plazoObjetivoDias = 15)
    val RESIDUOS = Categoria(2, "residuos", "Residuos", pesoPrioridad = 2, radioDuplicadoM = 30, ventanaDias = 30, plazoObjetivoDias = 5)
    val ACCESIBILIDAD = Categoria(3, "accesibilidad", "Accesibilidad", pesoPrioridad = 3, radioDuplicadoM = 25, ventanaDias = 30, plazoObjetivoDias = 10)

    val todas = listOf(VIAL, RESIDUOS, ACCESIBILIDAD)
}
