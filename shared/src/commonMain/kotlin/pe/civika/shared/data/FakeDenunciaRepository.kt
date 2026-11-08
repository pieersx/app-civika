package pe.civika.shared.data

import pe.civika.shared.domain.Denuncia
import pe.civika.shared.domain.MotivoDenuncia

/** Cola de moderación de ejemplo, con los tres casos del mockup del panel (docs/diseno/direccion-visual.md). */
class FakeDenunciaRepository : DenunciaRepository {

    override suspend fun listarPendientes(): List<Denuncia> = denuncias

    private val denuncias = listOf(
        Denuncia(
            id = "DEN-000003",
            motivo = MotivoDenuncia.ROSTROS,
            titulo = "Basura en la puerta del colegio",
            codigoIncidente = "CVK-000127",
            meta = "denunciado por 2 vecinos, hace 1 hora",
            detalle = "En la foto se ven dos niños con uniforme. El problema es real, pero la imagen expone a menores.",
        ),
        Denuncia(
            id = "DEN-000002",
            motivo = MotivoDenuncia.DUPLICADO,
            titulo = "Hueco grande en la avenida",
            codigoIncidente = "CVK-000129",
            meta = "a 22 m de CVK-000124, hace 3 horas",
            detalle = "Misma categoría y casi la misma ubicación que «Bache profundo en la pista», que ya tiene 25 apoyos.",
        ),
        Denuncia(
            id = "DEN-000001",
            motivo = MotivoDenuncia.LENGUAJE,
            titulo = "Vereda rota hace meses",
            codigoIncidente = "CVK-000130",
            meta = "denunciado por 1 vecino, ayer",
            detalle = "La descripción incluye insultos contra un funcionario. La foto y la ubicación sí son útiles.",
        ),
    )
}
