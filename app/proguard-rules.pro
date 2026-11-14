# kotlinx.serialization ships its own keep rules for @Serializable classes.
# Keep enum names stable, since they are written to the saved JSON.
-keepclassmembers enum com.aadil.onward.model.** { *; }
