# Keep kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.dinana.blog.**$$serializer { *; }
-keepclassmembers class com.dinana.blog.** {
    *** Companion;
}
-keepclasseswithmembers class com.dinana.blog.** {
    kotlinx.serialization.KSerializer serializer(...);
}
