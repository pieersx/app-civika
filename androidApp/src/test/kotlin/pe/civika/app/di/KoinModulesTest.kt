package pe.civika.app.di

import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify
import pe.civika.shared.di.sharedModule
import kotlin.test.Test

/** ADR-10: Koin resuelve al ejecutar, así que verificamos el grafo en las pruebas (verify() solo existe en JVM). */
class KoinModulesTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun elGrafoDeDependenciasEstaCompleto() {
        module { includes(sharedModule, appModule) }.verify()
    }
}
