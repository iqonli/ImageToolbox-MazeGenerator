-keepattributes Signature,InnerClasses,EnclosingMethod
-keep class com.t8rin.imagetoolbox.core.settings.domain.model.** { *; }
-keep class com.t8rin.imagetoolbox.core.domain.image.model.** { *; }

# Retained rules from upstream for reflective settings/DTOs and native image codecs.
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }
-keepnames enum com.t8rin.imagetoolbox.**
-keepclassmembers,allowoptimization enum com.t8rin.imagetoolbox.** {
    public static final ** *;
}
-keepnames class com.t8rin.imagetoolbox.core.domain.model.ColorModel
-keepnames class com.t8rin.imagetoolbox.core.domain.model.GradientPalette
-keep class com.t8rin.imagetoolbox.core.domain.model.IntegerSize { *; }
-keep class com.t8rin.imagetoolbox.core.data.json.PresetJson { *; }
-keep class com.t8rin.imagetoolbox.core.data.json.ResizeTypeJson { *; }
-keep class com.t8rin.imagetoolbox.core.data.json.ImageScaleModeJson { *; }
-keep class org.beyka.tiffbitmapfactory.** { *; }
-keep class com.hashsequence.coilresvg.** { *; }
-keep class com.sun.jna.** { *; }
-dontwarn java.awt.Component
-dontwarn java.awt.GraphicsEnvironment
-dontwarn java.awt.HeadlessException
-dontwarn java.awt.Window
