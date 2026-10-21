package pe.civika.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * RF-09 y docs/arquitectura.md §6:
 * prioridad = peso × 10 + min(apoyos, 50) + días abierto × 2, y +20 si se superó el plazo objetivo.
 */
class PrioridadTest {

    @Test
    fun sumaPesoApoyosYDias() {
        // 3×10 + 10 + 2×2 = 44
        assertEquals(44, calcularPrioridad(pesoCategoria = 3, apoyos = 10, diasAbierto = 2, plazoObjetivoDias = 5))
    }

    @Test
    fun losApoyosCuentanComoMaximo50() {
        // 1×10 + 50 (no 80) + 0 = 60
        assertEquals(60, calcularPrioridad(pesoCategoria = 1, apoyos = 80, diasAbierto = 0, plazoObjetivoDias = 5))
    }

    @Test
    fun elDiaDelPlazoTodaviaNoEstaVencido() {
        // 2×10 + 0 + 5×2 = 30, sin el +20
        assertEquals(30, calcularPrioridad(pesoCategoria = 2, apoyos = 0, diasAbierto = 5, plazoObjetivoDias = 5))
    }

    @Test
    fun superarElPlazoSuma20() {
        // 2×10 + 0 + 6×2 + 20 = 52
        assertEquals(52, calcularPrioridad(pesoCategoria = 2, apoyos = 0, diasAbierto = 6, plazoObjetivoDias = 5))
    }

    @Test
    fun losValoresNegativosCuentanComoCero() {
        // Un dato inconsistente del servidor no debe cerrar la app: 3×10 + 0 + 0 = 30
        assertEquals(30, calcularPrioridad(pesoCategoria = 3, apoyos = -3, diasAbierto = -1, plazoObjetivoDias = 5))
    }
}
