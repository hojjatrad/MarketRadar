# Keep Gson model fields (with @SerializedName) so reflection still works if minify is enabled.
-keep class com.arena.marketradar.data.model.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keepattributes Signature
-keepattributes *Annotation*
