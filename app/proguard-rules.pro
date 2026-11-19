# CalcPro Release ProGuard / R8 Rules
-keepattributes Signature
-keepattributes *Annotation*

# Keep Kotlin Serialization routes & Moshi DTOs
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class com.example.data.remote.** { *; }
-keep class com.example.ui.navigation.** { *; }

# Keep Google Play Billing & AdMob classes
-keep class com.android.billingclient.** { *; }
-keep class com.google.android.gms.ads.** { *; }
