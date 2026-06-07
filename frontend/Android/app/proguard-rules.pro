# Kotlin
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Hilt
-keepclasseswithmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel *;
}

# Retrofit + Kotlinx Serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Media3
-keep class androidx.media3.** { *; }

# Coil
-dontwarn coil.**

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Google Sign-In
-keep class com.google.android.gms.** { *; }
