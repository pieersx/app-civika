import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// AGP 9 trae Kotlin integrado: no hace falta el plugin kotlin-android.
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
}

// FCM (RF-11): el plugin de Google Services solo se aplica si existe el archivo de configuración.
// Sin él, la app compila igual y las notificaciones push quedan apagadas (la bandeja in-app sigue funcionando).
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.googleServices.get().pluginId)
}

// URL y clave pública `anon` de Supabase: local.properties o variables de entorno (ver el script).
apply(from = rootProject.file("gradle/supabase-config.gradle.kts"))
val supabaseUrl = extra["supabaseUrl"] as String
val supabaseAnonKey = extra["supabaseAnonKey"] as String

// Firma del release: keystore.properties (ignorado por git) o variables de entorno. Sin datos, queda sin firmar.
val propiedadesFirma = Properties().apply {
    val archivo = file("keystore.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

fun datoDeFirma(clave: String, variable: String): String? =
    (propiedadesFirma.getProperty(clave) ?: providers.environmentVariable(variable).orNull)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

val almacenFirma = datoDeFirma("storeFile", "ANDROID_KEYSTORE_FILE")
val claveAlmacen = datoDeFirma("storePassword", "ANDROID_KEYSTORE_PASSWORD")
val aliasFirma = datoDeFirma("keyAlias", "ANDROID_KEY_ALIAS")
val claveFirma = datoDeFirma("keyPassword", "ANDROID_KEY_PASSWORD")
val hayFirma = listOf(almacenFirma, claveAlmacen, aliasFirma, claveFirma).all { it != null } &&
    file(almacenFirma!!).exists()

fun comoLiteral(texto: String) = "\"" + texto.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "pe.civika.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "pe.civika.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"

        // Solo español: evita empaquetar las traducciones de las librerías.
        androidResources.localeFilters += "es"

        buildConfigField("String", "SUPABASE_URL", comoLiteral(supabaseUrl))
        buildConfigField("String", "SUPABASE_ANON_KEY", comoLiteral(supabaseAnonKey))
    }

    signingConfigs {
        if (hayFirma) {
            create("release") {
                storeFile = file(almacenFirma!!)
                storePassword = claveAlmacen
                keyAlias = aliasFirma
                keyPassword = claveFirma
            }
        }
    }

    buildTypes {
        debug {
            // Solo en debug se permite HTTP sin cifrar, para llegar al Supabase local (http://10.0.2.2:54321).
            manifestPlaceholders["usesCleartextTraffic"] = "true"
            // `-Pcivika.r8Debug=true` pasa el debug por R8 con las mismas reglas del release: sirve para probar en el
            // emulador (con Supabase local por http) que serialización y Ktor sobreviven a la minificación.
            if (providers.gradleProperty("civika.r8Debug").orNull == "true") {
                isMinifyEnabled = true
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            }
        }
        release {
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        lintConfig = file("lint.xml")
        abortOnError = true
        checkReleaseBuilds = true
    }

    packaging {
        resources.excludes += setOf("META-INF/{AL2.0,LGPL2.1}", "META-INF/INDEX.LIST", "META-INF/io.netty.versions.properties")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    // Room guarda el esquema de cada versión para poder escribir y probar migraciones.
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

// El release no se publica en modo demostración: sin URL y clave de Supabase, falla con un mensaje claro.
val comprobarConfiguracionRelease = tasks.register("comprobarConfiguracionRelease") {
    val url = supabaseUrl
    val clave = supabaseAnonKey
    doLast {
        if (url.isBlank() || clave.isBlank()) {
            throw GradleException(
                "Falta la configuración de Supabase para el release. Define supabase.url y supabase.anonKey en " +
                    "local.properties, o las variables de entorno SUPABASE_URL y SUPABASE_ANON_KEY. " +
                    "Solo la clave pública anon; nunca la service_role.",
            )
        }
        if (!url.startsWith("https://")) {
            throw GradleException("El release necesita una URL de Supabase con https:// (supabase.url = $url).")
        }
    }
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(comprobarConfiguracionRelease)
}

dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.compose.viewmodel)

    // Cola de reportes sin conexión (ADR-6) y preferencias
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.datastore.preferences)

    // Dispositivo: cámara, mapa, ubicación, imágenes y notificaciones
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.exifinterface)
    implementation(libs.maplibre.android)
    implementation(libs.play.services.location)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(platform(libs.koin.bom))
    testImplementation(libs.koin.test)
}
