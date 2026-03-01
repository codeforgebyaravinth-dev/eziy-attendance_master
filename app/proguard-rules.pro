# Retrofit & OkHttp
-keepattributes Signature, InnerClasses, Annotation
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# Gson DTO Models
-keep class in.eziy.attendancemaster.data.remote.dto.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Eclipse Paho MQTT
-keep class org.eclipse.paho.client.mqttv3.** { *; }
-dontwarn org.eclipse.paho.client.mqttv3.**

# Jetpack Security Crypto
-keep class androidx.security.crypto.** { *; }
