# Reglas de R8 para el release de Cívika. Cada bloque dice por qué existe.

# --- kotlinx.serialization: los serializadores generados se buscan por reflexión -------------------------------
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
# DTOs de Supabase y rutas de navegación (todas @Serializable)
-keep,includedescriptorclasses class pe.civika.**$$serializer { *; }
-keepclassmembers class pe.civika.** { *** Companion; }
-keepclasseswithmembers class pe.civika.** { kotlinx.serialization.KSerializer serializer(...); }

# --- Ktor y supabase-kt ----------------------------------------------------------------------------------------
-keep class io.ktor.** { *; }
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.ktor.**
-dontwarn io.github.jan.supabase.**
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
-dontwarn kotlinx.atomicfu.**
-dontwarn kotlinx.coroutines.debug.**
-dontwarn kotlinx.coroutines.flow.**

# --- OkHttp / Okio ---------------------------------------------------------------------------------------------
-dontwarn okhttp3.internal.platform.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# --- MapLibre Native: código JNI y estilos por reflexión --------------------------------------------------------
-keep class org.maplibre.android.** { *; }
-keep class org.maplibre.geojson.** { *; }
-dontwarn org.maplibre.android.**

# --- Koin: resuelve por reflexión de Kotlin ---------------------------------------------------------------------
-keepclassmembers class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keep class pe.civika.app.sync.ReporteSyncWorker { <init>(...); }
-dontwarn org.koin.**

# --- Room (el procesador genera todo; solo se conserva la base y sus DAO) --------------------------------------
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# --- Firebase Messaging: solo si hay google-services.json; sin él no se usa ---------------------------------------
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Los mensajes de depuración de println() (el log de errores de red) se quitan del release.
-assumenosideeffects class java.io.PrintStream { public void println(java.lang.String); }
