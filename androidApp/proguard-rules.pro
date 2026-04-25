# SwiggyMind ProGuard Rules

# ────────────────────────────────────────────
# Kotlin Serialization
# ────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class com.rudra.swiggymind.**$$serializer { *; }
-keepclassmembers class com.rudra.swiggymind.** {
    *** Companion;
}
-keepclasseswithmembers class com.rudra.swiggymind.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# ────────────────────────────────────────────
# Ktor
# ────────────────────────────────────────────
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn io.ktor.**

# ────────────────────────────────────────────
# Coil
# ────────────────────────────────────────────
-dontwarn coil.**

# ────────────────────────────────────────────
# Room
# ────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.**

# ────────────────────────────────────────────
# Hilt / Dagger
# ────────────────────────────────────────────
-dontwarn com.google.errorprone.annotations.*
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# ────────────────────────────────────────────
# Domain Models (used in serialization)
# ────────────────────────────────────────────
-keep class com.rudra.swiggymind.domain.model.** { *; }
-keep class com.rudra.swiggymind.data.local.** { *; }
-keep class com.rudra.swiggymind.ai.OpenRouterClient$** { *; }
