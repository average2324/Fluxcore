# Keep app/game classes stable to avoid runtime reflection issues in engine/ad integrations.
-keep class com.orbitflux.** { *; }

# Keep libGDX Android bootstrap/runtime classes used by backend/native loaders.
-keep class com.badlogic.gdx.backends.android.** { *; }
-keep class com.badlogic.gdx.graphics.g2d.freetype.** { *; }
-keep class com.badlogic.gdx.graphics.g2d.GlyphLayout { *; }
-keep class com.badlogic.gdx.graphics.g2d.GlyphLayout$GlyphRun { *; }
-keep class com.badlogic.gdx.utils.ReflectionPool { *; }
-keepclassmembers class com.badlogic.gdx.** implements com.badlogic.gdx.utils.Pool$Poolable {
    <init>();
}

# Keep Google Mobile Ads and UMP public APIs.
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.ump.** { *; }

-dontwarn com.badlogic.gdx.**
-dontwarn kotlin.**
