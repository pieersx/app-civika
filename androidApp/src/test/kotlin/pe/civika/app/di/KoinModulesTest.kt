package pe.civika.app.di

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify
import pe.civika.shared.di.crearSharedModule
import kotlin.test.Test

/** ADR-10: Koin resuelve al ejecutar, así que verificamos el grafo en las pruebas (verify() solo existe en JVM). */
class KoinModulesTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun elGrafoDeDependenciasEstaCompletoEnModoDemostracion() {
        module { includes(crearSharedModule(null), appModule) }.verify(
            // Tipos que Koin aporta en ejecución (contexto, estado guardado y el id del detalle).
            extraTypes = listOf(Context::class, SavedStateHandle::class, String::class),
        )
    }
}
