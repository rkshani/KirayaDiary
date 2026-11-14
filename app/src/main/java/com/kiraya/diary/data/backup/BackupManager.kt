package com.kiraya.diary.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.kiraya.diary.BuildConfig
import com.kiraya.diary.core.coroutines.IoDispatcher
import com.kiraya.diary.data.files.PhotoStorage
import com.kiraya.diary.data.local.KirayaDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Makes a single zip with the database, photos and settings, saved wherever the user picks
 * (phone storage, SD card, USB...) through the Storage Access Framework. No internet is used.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: KirayaDatabase,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    class InvalidBackupException : IOException("Not a Kiraya Diary backup")

    data class BackupInfo(val createdAt: Long, val appVersion: String, val photoCount: Int)

    private val dbFile: File get() = context.getDatabasePath(KirayaDatabase.NAME)
    private val photosDir: File get() = File(context.filesDir, PhotoStorage.DIR)
    private val settingsFile: File get() = File(context.filesDir, "datastore/settings.preferences_pb")

    suspend fun backup(target: Uri): BackupInfo = withContext(io) {
        // Move everything from the write-ahead log into the main database file.
        database.query("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }

        val photos = photosDir.listFiles()?.filter { it.isFile }.orEmpty()
        val info = BackupInfo(System.currentTimeMillis(), BuildConfig.VERSION_NAME, photos.size)
        val out = context.contentResolver.openOutputStream(target, "wt") ?: throw IOException("Cannot write")
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(META))
            zip.write(
                JSONObject()
                    .put("app", APP_ID)
                    .put("format", FORMAT)
                    .put("dbVersion", database.openHelper.readableDatabase.version)
                    .put("appVersion", info.appVersion)
                    .put("createdAt", info.createdAt)
                    .toString()
                    .toByteArray(),
            )
            zip.closeEntry()
            addFile(zip, dbFile, DB_ENTRY)
            if (settingsFile.exists()) addFile(zip, settingsFile, SETTINGS_ENTRY)
            photos.forEach { addFile(zip, it, PHOTOS_PREFIX + it.name) }
        }
        info
    }

    /** Reads a backup's details without restoring it. */
    suspend fun inspect(source: Uri): BackupInfo = withContext(io) {
        var meta: JSONObject? = null
        var hasDb = false
        var photos = 0
        val input = context.contentResolver.openInputStream(source) ?: throw InvalidBackupException()
        ZipInputStream(input.buffered()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { e ->
                when {
                    e.name == META -> meta = JSONObject(zip.readBytes().decodeToString())
                    e.name == DB_ENTRY -> hasDb = true
                    e.name.startsWith(PHOTOS_PREFIX) && !e.isDirectory -> photos++
                }
            }
        }
        val m = meta
        if (m == null || m.optString("app") != APP_ID || !hasDb) throw InvalidBackupException()
        BackupInfo(m.optLong("createdAt"), m.optString("appVersion"), photos)
    }

    /**
     * Replaces all app data with the backup. The app must restart afterwards ([restartApp]),
     * because the open database is closed here.
     */
    suspend fun restore(source: Uri) = withContext(io) {
        inspect(source)
        val staging = File(context.cacheDir, "restore").apply {
            deleteRecursively()
            mkdirs()
        }
        val input = context.contentResolver.openInputStream(source) ?: throw InvalidBackupException()
        ZipInputStream(input.buffered()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { e ->
                if (e.isDirectory) return@forEach
                val out = File(staging, e.name)
                // Block "../" paths (zip slip).
                if (!out.canonicalPath.startsWith(staging.canonicalPath + File.separator)) throw InvalidBackupException()
                out.parentFile?.mkdirs()
                out.outputStream().use { zip.copyTo(it) }
            }
        }
        val newDb = File(staging, DB_ENTRY)
        if (!newDb.exists()) throw InvalidBackupException()

        database.close()
        listOf("", "-wal", "-shm", "-journal").forEach { File(dbFile.path + it).delete() }
        newDb.copyTo(dbFile, overwrite = true)

        photosDir.deleteRecursively()
        photosDir.mkdirs()
        File(staging, PHOTOS_PREFIX.trimEnd('/')).listFiles()?.forEach { it.copyTo(File(photosDir, it.name), overwrite = true) }

        val newSettings = File(staging, SETTINGS_ENTRY)
        if (newSettings.exists()) {
            settingsFile.parentFile?.mkdirs()
            newSettings.copyTo(settingsFile, overwrite = true)
        }
        staging.deleteRecursively()
    }

    /** Starts the app again so it opens the restored database. */
    fun restartApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (intent != null) context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }

    private fun addFile(zip: ZipOutputStream, file: File, name: String) {
        if (!file.exists()) return
        zip.putNextEntry(ZipEntry(name))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    companion object {
        private const val APP_ID = "com.kiraya.diary"
        private const val FORMAT = 1
        private const val META = "meta.json"
        private const val DB_ENTRY = "database/kiraya.db"
        private const val SETTINGS_ENTRY = "settings/settings.preferences_pb"
        private const val PHOTOS_PREFIX = "photos/"
    }
}
