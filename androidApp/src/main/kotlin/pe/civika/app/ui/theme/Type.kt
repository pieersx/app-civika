package pe.civika.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import pe.civika.app.R

// Fuentes empaquetadas (licencia OFL, ver assets/licencias). Los .ttf son estáticos: un archivo por peso.
val Bricolage = FontFamily(
    Font(R.font.bricolage_grotesque_semibold, FontWeight.SemiBold),
    Font(R.font.bricolage_grotesque_bold, FontWeight.Bold),
)

val Atkinson = FontFamily(
    Font(R.font.atkinson_hyperlegible_next_regular, FontWeight.Normal),
    Font(R.font.atkinson_hyperlegible_next_bold, FontWeight.Bold),
)

private fun estilo(familia: FontFamily, peso: FontWeight, tamano: Int, interlineado: Int) = TextStyle(
    fontFamily = familia,
    fontWeight = peso,
    fontSize = tamano.sp,
    lineHeight = interlineado.sp,
)

private val Base = Typography()

/** Tabla «Tipografía» de la dirección visual. Bricolage solo en display, headline y titleLarge. */
val CivikaTipografia = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = Bricolage, fontWeight = FontWeight.Bold),
    displayMedium = Base.displayMedium.copy(fontFamily = Bricolage, fontWeight = FontWeight.Bold),
    displaySmall = estilo(Bricolage, FontWeight.Bold, 36, 44),
    headlineLarge = Base.headlineLarge.copy(fontFamily = Bricolage, fontWeight = FontWeight.Bold),
    headlineMedium = estilo(Bricolage, FontWeight.Bold, 28, 36),
    headlineSmall = estilo(Bricolage, FontWeight.SemiBold, 24, 32),
    titleLarge = estilo(Bricolage, FontWeight.SemiBold, 22, 28),
    titleMedium = estilo(Atkinson, FontWeight.Bold, 16, 24),
    titleSmall = Base.titleSmall.copy(fontFamily = Atkinson, fontWeight = FontWeight.Bold),
    bodyLarge = estilo(Atkinson, FontWeight.Normal, 16, 24),
    bodyMedium = estilo(Atkinson, FontWeight.Normal, 14, 20),
    bodySmall = Base.bodySmall.copy(fontFamily = Atkinson),
    labelLarge = estilo(Atkinson, FontWeight.Bold, 14, 20),
    labelMedium = estilo(Atkinson, FontWeight.Bold, 12, 16),
    labelSmall = Base.labelSmall.copy(fontFamily = Atkinson, fontWeight = FontWeight.Bold),
)
