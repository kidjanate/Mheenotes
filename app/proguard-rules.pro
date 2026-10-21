# WorkManager (pulled in by Glance widgets) creates its Room database reflectively.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
