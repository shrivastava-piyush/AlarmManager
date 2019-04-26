# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in G:\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keep public class com.google.android.gms.ads.** {
    public *;
}

-keep public class com.google.ads.** {
    public *;
}

-keepattributes Signature, Exceptions, InnerClasses
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.examples.android.model.** { *; }

-keep class com.android.vending.billing.**

-dontwarn com.google.android.gms.**
-dontwarn com.sothree.**
-keep class com.sothree.**
-keep interface com.sothree.**

-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**
