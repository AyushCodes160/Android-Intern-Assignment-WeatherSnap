# Keep Retrofit / Gson DTO field names
-keep class com.weathersnap.data.remote.dto.** { *; }
-keepattributes Signature, *Annotation*
-dontwarn okhttp3.**
-dontwarn retrofit2.**
