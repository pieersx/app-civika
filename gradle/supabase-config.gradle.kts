// Expone `supabaseUrl` y `supabaseAnonKey` a los módulos que apliquen este script.
// Orden: variables de entorno SUPABASE_URL y SUPABASE_ANON_KEY (CI, despliegue o una prueba puntual contra otro
// entorno) y, si faltan, `supabase.url` y `supabase.anonKey` de `local.properties`.
// Solo la clave pública `anon`; la `service_role` jamás va aquí.
import java.util.Properties

val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

fun valor(clave: String, variable: String): String {
    val deEntorno = providers.environmentVariable(variable).orNull?.trim().orEmpty()
    if (deEntorno.isNotEmpty()) return deEntorno
    return propiedadesLocales.getProperty(clave)?.trim().orEmpty()
}

// `-Pcivika.demo=true` fuerza el modo demostración aunque haya claves configuradas.
val forzarDemo = providers.gradleProperty("civika.demo").orNull == "true"

extra["supabaseUrl"] = if (forzarDemo) "" else valor("supabase.url", "SUPABASE_URL")
extra["supabaseAnonKey"] = if (forzarDemo) "" else valor("supabase.anonKey", "SUPABASE_ANON_KEY")
