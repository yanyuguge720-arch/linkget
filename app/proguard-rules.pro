# Keep line numbers and metadata useful for crash reports.
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }

# OkHttp / Okio ship consumer ProGuard rules, but silence optional-platform warnings.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
