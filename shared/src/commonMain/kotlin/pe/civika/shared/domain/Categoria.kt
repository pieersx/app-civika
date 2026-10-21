package pe.civika.shared.domain

/**
 * Categoría de incidente. Los valores viven en la tabla `categorias` de Supabase (RNF-08); estos son los del seed
 * del piloto, usados por los repositorios fake.
 */
data class Categoria(
    val serviceCode: String,
    val nombre: String,
    val pesoPrioridad: Int,
    val radioDuplicadoM: Int,
    val ventanaDias: Int,
    val plazoObjetivoDias: Int,
)

object CategoriasPiloto {
    val VIAL = Categoria("vial", "Pistas y veredas", pesoPrioridad = 3, radioDuplicadoM = 50, ventanaDias = 30, plazoObjetivoDias = 15)
    val RESIDUOS = Categoria("residuos", "Residuos", pesoPrioridad = 2, radioDuplicadoM = 30, ventanaDias = 30, plazoObjetivoDias = 5)
    val ACCESIBILIDAD = Categoria("accesibilidad", "Accesibilidad", pesoPrioridad = 3, radioDuplicadoM = 25, ventanaDias = 30, plazoObjetivoDias = 10)

    val todas = listOf(VIAL, RESIDUOS, ACCESIBILIDAD)
}
