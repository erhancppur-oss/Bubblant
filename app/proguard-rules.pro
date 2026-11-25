# Our game code (small) - keep names for readable crash reports.
-keep class com.bubblant.game.** { *; }

# WorkManager (pulled in by Google Mobile Ads) creates its Room database by reflection.
# Without these rules R8 strips WorkDatabase_Impl and the app crashes on launch.
-keep class * extends androidx.room.RoomDatabase { <init>(...); }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class androidx.work.impl.** { *; }
-keep class androidx.startup.** { *; }
-keep class * implements androidx.startup.Initializer { <init>(); }
