# WorkManager (pulled in by Glance widgets) creates its Room database reflectively.
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# PdfBox-Android: loads fonts/resources reflectively; BouncyCastle and desktop classes are absent.
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn com.gemalto.jp2.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
