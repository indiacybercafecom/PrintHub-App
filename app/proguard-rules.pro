# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Razorpay Proguard Rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.razorpay.** {*;}
-dontwarn com.razorpay.**

# Firebase Crashlytics
-keepattributes SourceFile,LineNumberTable
-keep public class * extends com.google.firebase.crashlytics.CustomKeysAndValues
