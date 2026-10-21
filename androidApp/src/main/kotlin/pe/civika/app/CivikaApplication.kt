package pe.civika.app

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import pe.civika.app.di.appModule
import pe.civika.shared.di.sharedModule

class CivikaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CivikaApplication)
            modules(sharedModule, appModule)
        }
    }
}
