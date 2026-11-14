# Kiraya Diary release rules.
# Room, Hilt, WorkManager, DataStore, Biometric and Compose ship their own consumer rules.

# Enum names are stored in the database and settings (unit type, payment method, theme...).
-keepclassmembers enum com.kiraya.diary.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Workers are created by WorkManager through HiltWorkerFactory.
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Keep line numbers for readable crash stack traces, hide file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
