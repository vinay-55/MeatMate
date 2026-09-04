package com.example.store.backup

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.store.data.local.BusinessEntity
import com.example.store.data.local.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

object BackupManagerImpl {

    private const val TAG = "BackupManagerImpl"

    private const val BACKUP_FILENAME =
        "store_backup.json"

    /*
     * Metadata of the backup file stored in Google Drive.
     */
    data class BackupFileMeta(
        val id: String?,
        val size: Long?,
        val modifiedTime: Long?
    )

    /*
     * Initialize authentication.
     */
    fun init(activity: androidx.activity.ComponentActivity) {
        AuthManager.init(activity)
    }

    // ============================================================
    // GENERATE BACKUP CONTENT
    // ============================================================

    /*
     * Generates the complete current Room database
     * as JSON.
     *
     * This is used by:
     *
     * 1. Manual backup
     * 2. Automatic backup
     * 3. Data-size calculation
     */
    suspend fun generateBackupContent(
        context: Context
    ): String {

        return withContext(Dispatchers.IO) {

            val db =
                DatabaseProvider.getDatabase(context)

            val list =
                db.businessDao().getAllBusiness()

            Log.i(
                TAG,
                "Generating backup for ${list.size} records"
            )

            val jsonArr =
                JSONArray()

            for (b in list) {

                val o =
                    JSONObject()

                o.put(
                    "id",
                    b.id
                )

                o.put(
                    "product",
                    b.product
                )

                o.put(
                    "price",
                    b.price
                )

                o.put(
                    "weight",
                    b.weight
                )

                o.put(
                    "total",
                    b.total
                )

                o.put(
                    "date",
                    b.date
                )

                if (b.slno != null) {

                    o.put(
                        "slno",
                        b.slno
                    )

                } else {

                    o.put(
                        "slno",
                        JSONObject.NULL
                    )
                }

                jsonArr.put(o)
            }

            val root =
                JSONObject()

            root.put(
                "backupVersion",
                1
            )

            root.put(
                "backupTimestamp",
                System.currentTimeMillis()
            )

            root.put(
                "records",
                jsonArr
            )

            root.toString()
        }
    }

    // ============================================================
    // GET EXISTING BACKUP METADATA
    // ============================================================

    /*
     * Finds the existing store_backup.json file
     * inside Google Drive's appDataFolder.
     *
     * Returns:
     *
     * id           -> Drive file ID
     * size         -> backup file size in bytes
     * modifiedTime -> last modification time
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getBackupFileMetadata(
        token: String
    ): BackupFileMeta? {

        return withContext(Dispatchers.IO) {

            try {

                val client =
                    OkHttpClient()

                val url =
                    HttpUrl.Builder()
                        .scheme("https")
                        .host("www.googleapis.com")
                        .addPathSegments(
                            "drive/v3/files"
                        )
                        .addQueryParameter(
                            "q",
                            "name='$BACKUP_FILENAME' " +
                                    "and 'appDataFolder' in parents " +
                                    "and trashed=false"
                        )
                        .addQueryParameter(
                            "spaces",
                            "appDataFolder"
                        )
                        .addQueryParameter(
                            "fields",
                            "files(id,name,size,modifiedTime)"
                        )
                        .build()

                val request =
                    Request.Builder()
                        .url(url)
                        .header(
                            "Authorization",
                            "Bearer $token"
                        )
                        .get()
                        .build()

                client
                    .newCall(request)
                    .execute()
                    .use { response ->

                        if (!response.isSuccessful) {

                            Log.w(
                                TAG,
                                "Metadata request failed: " +
                                        "${response.code}"
                            )

                            return@withContext null
                        }

                        val body =
                            response.body?.string()
                                ?: return@withContext null

                        val root =
                            JSONObject(body)

                        val files =
                            root.optJSONArray("files")
                                ?: JSONArray()

                        if (files.length() == 0) {

                            Log.i(
                                TAG,
                                "No existing backup found"
                            )

                            return@withContext null
                        }

                        val file =
                            files.getJSONObject(0)

                        val id =
                            file.optString(
                                "id",
                                null
                            )

                        val size =
                            if (file.has("size")) {
                                file.optLong("size")
                            } else {
                                null
                            }

                        val modifiedTime =
                            if (file.has("modifiedTime")) {

                                try {

                                    Instant
                                        .parse(
                                            file.optString(
                                                "modifiedTime"
                                            )
                                        )
                                        .toEpochMilli()

                                } catch (e: Exception) {

                                    Log.w(
                                        TAG,
                                        "Could not parse modifiedTime",
                                        e
                                    )

                                    null
                                }

                            } else {
                                null
                            }

                        Log.i(
                            TAG,
                            "Existing backup found: " +
                                    "id=$id, size=$size, " +
                                    "modifiedTime=$modifiedTime"
                        )

                        BackupFileMeta(
                            id = id,
                            size = size,
                            modifiedTime = modifiedTime
                        )
                    }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "getBackupFileMetadata failed",
                    e
                )

                null
            }
        }
    }

    // ============================================================
    // BACKUP NOW
    // ============================================================

    /*
     * Performs a complete backup.
     *
     * forceReplace = false
     *     Existing backup is NOT overwritten.
     *
     * forceReplace = true
     *     Existing backup IS overwritten.
     *
     * Manual backup uses false initially and asks the user
     * for confirmation in BackupActivity.
     *
     * Automatic backup uses true.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun backupNow(
        context: Context,
        forceReplace: Boolean = false
    ): BackupResult {

        Log.i(
            TAG,
            "backupNow started. forceReplace=$forceReplace"
        )

        return withContext(Dispatchers.IO) {

            try {

                val token =
                    AuthManager.getAccessToken()

                if (token.isNullOrEmpty()) {

                    Log.w(
                        TAG,
                        "No access token"
                    )

                    return@withContext BackupResult(
                        false,
                        "Not signed in"
                    )
                }

                /*
                 * Generate the COMPLETE current Room data.
                 */
                val content =
                    generateBackupContent(context)

                val contentBytes =
                    content.toByteArray(
                        Charsets.UTF_8
                    )

                Log.i(
                    TAG,
                    "Backup data size: " +
                            "${contentBytes.size} bytes"
                )

                /*
                 * Check whether a cloud backup already exists.
                 */
                val existingMeta =
                    getBackupFileMetadata(token)

                if (
                    existingMeta?.id != null &&
                    !forceReplace
                ) {

                    Log.i(
                        TAG,
                        "Backup already exists and " +
                                "forceReplace=false"
                    )

                    return@withContext BackupResult(
                        false,
                        "Backup already exists"
                    )
                }

                /*
                 * If existing file exists:
                 *
                 * PATCH the same Drive file.
                 *
                 * If no file exists:
                 *
                 * POST a new Drive file.
                 */
                val success =
                    uploadFile(
                        token = token,
                        fileId = existingMeta?.id,
                        content = content,
                        update = existingMeta?.id != null
                    )

                if (success) {

                    Log.i(
                        TAG,
                        "Backup upload/update successful"
                    )

                    BackupResult(
                        true,
                        "Backup uploaded"
                    )

                } else {

                    Log.w(
                        TAG,
                        "Backup upload/update failed"
                    )

                    BackupResult(
                        false,
                        "Upload failed"
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "backupNow failed",
                    e
                )

                BackupResult(
                    false,
                    e.message
                        ?: "Unknown error"
                )
            }
        }
    }

    // ============================================================
    // RESTORE
    // ============================================================

    /*
     * Restores the available cloud backup.
     *
     * Existing duplicate records are skipped by
     * importFromJsonString().
     */
    suspend fun restoreIfAvailable(
        context: Context
    ): RestoreResult {

        return withContext(Dispatchers.IO) {

            try {

                val token =
                    AuthManager.getAccessToken()

                if (token.isNullOrEmpty()) {

                    return@withContext RestoreResult(
                        false,
                        0,
                        0,
                        "Not signed in"
                    )
                }

                Log.i(
                    TAG,
                    "Restore: checking for backup"
                )

                val fileId =
                    findBackupFileId(token)

                if (fileId == null) {

                    Log.i(
                        TAG,
                        "Restore: no backup found"
                    )

                    return@withContext RestoreResult(
                        false,
                        0,
                        0,
                        "No backup found"
                    )
                }

                Log.i(
                    TAG,
                    "Restore: found file $fileId"
                )

                val content =
                    downloadFileContent(
                        token,
                        fileId
                    )

                if (content == null) {

                    return@withContext RestoreResult(
                        false,
                        0,
                        0,
                        "Failed to download backup"
                    )
                }

                Log.i(
                    TAG,
                    "Restore: downloaded backup"
                )

                val result =
                    importFromJsonString(
                        context,
                        content
                    )

                RestoreResult(
                    result.success,
                    result.imported,
                    result.skipped,
                    result.error
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Restore failed",
                    e
                )

                RestoreResult(
                    false,
                    0,
                    0,
                    e.message
                )
            }
        }
    }

    // ============================================================
    // FIND BACKUP FILE ID
    // ============================================================

    private fun findBackupFileId(
        token: String
    ): String? {

        val client =
            OkHttpClient()

        val url =
            HttpUrl.Builder()
                .scheme("https")
                .host("www.googleapis.com")
                .addPathSegments(
                    "drive/v3/files"
                )
                .addQueryParameter(
                    "q",
                    "name='$BACKUP_FILENAME' " +
                            "and 'appDataFolder' in parents " +
                            "and trashed=false"
                )
                .addQueryParameter(
                    "spaces",
                    "appDataFolder"
                )
                .addQueryParameter(
                    "fields",
                    "files(id,name)"
                )
                .build()

        val request =
            Request.Builder()
                .url(url)
                .header(
                    "Authorization",
                    "Bearer $token"
                )
                .get()
                .build()

        client
            .newCall(request)
            .execute()
            .use { response ->

                if (!response.isSuccessful) {

                    Log.w(
                        TAG,
                        "Find backup failed: " +
                                response.code
                    )

                    return null
                }

                val body =
                    response.body?.string()
                        ?: return null

                val root =
                    JSONObject(body)

                val files =
                    root.optJSONArray("files")
                        ?: JSONArray()

                if (files.length() == 0) {

                    return null
                }

                return files
                    .getJSONObject(0)
                    .getString("id")
            }
    }
    // ============================================================
    // DOWNLOAD BACKUP
    // ============================================================

    private fun downloadFileContent(
        token: String,
        fileId: String
    ): String? {

        val client =
            OkHttpClient()

        val url =
            HttpUrl.Builder()
                .scheme("https")
                .host("www.googleapis.com")
                .addPathSegments(
                    "drive/v3/files/$fileId"
                )
                .addQueryParameter(
                    "alt",
                    "media"
                )
                .build()

        val request =
            Request.Builder()
                .url(url)
                .header(
                    "Authorization",
                    "Bearer $token"
                )
                .get()
                .build()

        client
            .newCall(request)
            .execute()
            .use { response ->

                if (!response.isSuccessful) {

                    Log.w(
                        TAG,
                        "Download failed: ${response.code}"
                    )

                    return null
                }

                return response.body?.string()
            }
    }


    // ============================================================
    // UPLOAD / UPDATE BACKUP
    // ============================================================

    private fun uploadFile(
        token: String,
        fileId: String?,
        content: String,
        update: Boolean
    ): Boolean {

        val client =
            OkHttpClient()

        val metadata =
            JSONObject()

        metadata.put(
            "name",
            BACKUP_FILENAME
        )

        metadata.put(
            "mimeType",
            "application/json"
        )

        /*
         * IMPORTANT:
         *
         * When creating a new file, specify appDataFolder.
         *
         * When updating an existing file, do NOT send parents.
         */
        if (!update || fileId == null) {

            metadata.put(
                "parents",
                JSONArray().put(
                    "appDataFolder"
                )
            )
        }

        val metadataPart =
            metadata
                .toString()
                .toRequestBody(
                    "application/json; charset=utf-8"
                        .toMediaTypeOrNull()
                )

        val contentPart =
            content.toRequestBody(
                "application/json; charset=utf-8"
                    .toMediaTypeOrNull()
            )

        val multipart =
            MultipartBody.Builder()
                .setType(
                    MultipartBody.FORM
                )
                .addFormDataPart(
                    "metadata",
                    null,
                    metadataPart
                )
                .addFormDataPart(
                    "file",
                    BACKUP_FILENAME,
                    contentPart
                )
                .build()

        val urlBuilder =
            HttpUrl.Builder()
                .scheme("https")
                .host("www.googleapis.com")

        /*
         * Existing file:
         *
         * PATCH the same Drive file.
         *
         * This means there will still be only
         * one store_backup.json file.
         */
        if (
            update &&
            fileId != null
        ) {

            urlBuilder
                .addPathSegments(
                    "upload/drive/v3/files/$fileId"
                )
                .addQueryParameter(
                    "uploadType",
                    "multipart"
                )

        } else {

            /*
             * No existing file:
             *
             * Create a new file.
             */
            urlBuilder
                .addPathSegments(
                    "upload/drive/v3/files"
                )
                .addQueryParameter(
                    "uploadType",
                    "multipart"
                )
        }

        val method =
            if (
                update &&
                fileId != null
            ) {
                "PATCH"
            } else {
                "POST"
            }

        Log.i(
            TAG,
            "Upload: method=$method fileId=$fileId"
        )

        val request =
            Request.Builder()
                .url(
                    urlBuilder.build()
                )
                .header(
                    "Authorization",
                    "Bearer $token"
                )
                .method(
                    method,
                    multipart
                )
                .build()

        client
            .newCall(request)
            .execute()
            .use { response ->

                if (!response.isSuccessful) {

                    val errorBody =
                        response.body?.string()

                    Log.w(
                        TAG,
                        "Upload failed: " +
                                "${response.code} " +
                                errorBody
                    )

                    return false
                }

                Log.i(
                    TAG,
                    "Upload successful: " +
                            response.code
                )

                return true
            }
    }


    // ============================================================
    // IMPORT RESTORED DATA
    // ============================================================

    private suspend fun importFromJsonString(
        context: Context,
        jsonText: String
    ): ImportResult {

        return withContext(Dispatchers.IO) {

            val db =
                DatabaseProvider.getDatabase(
                    context
                )

            var imported = 0
            var skipped = 0

            try {

                val root =
                    JSONObject(jsonText)

                val arr =
                    root.optJSONArray(
                        "records"
                    ) ?: JSONArray()

                /*
                 * Get current local records once.
                 */
                val existing =
                    db.businessDao()
                        .getAllBusiness()

                fun existsDuplicate(
                    candidate: BusinessEntity
                ): Boolean {

                    return existing.any { e ->

                        e.product ==
                                candidate.product &&

                                e.date ==
                                candidate.date &&

                                (
                                        (
                                                e.slno == null &&
                                                        candidate.slno == null
                                                ) ||
                                                (
                                                        e.slno ==
                                                                candidate.slno
                                                        )
                                        ) &&

                                e.price ==
                                candidate.price &&

                                e.weight ==
                                candidate.weight &&

                                e.total ==
                                candidate.total
                    }
                }

                /*
                 * Import every record from the backup.
                 */
                for (
                i in 0 until arr.length()
                ) {

                    val o =
                        arr.getJSONObject(i)

                    val id =
                        o.optInt(
                            "id",
                            0
                        )

                    val product =
                        o.getString(
                            "product"
                        )

                    val price =
                        o.getDouble(
                            "price"
                        )

                    val weight =
                        o.getDouble(
                            "weight"
                        )

                    val total =
                        o.getDouble(
                            "total"
                        )

                    val date =
                        o.getString(
                            "date"
                        )

                    val slno =
                        if (
                            o.isNull("slno")
                        ) {
                            null
                        } else {
                            o.optInt(
                                "slno"
                            )
                        }

                    val candidate =
                        BusinessEntity(
                            id = id,
                            product = product,
                            price = price,
                            weight = weight,
                            total = total,
                            date = date,
                            slno = slno
                        )

                    /*
                     * Skip duplicate records.
                     */
                    if (
                        existsDuplicate(
                            candidate
                        )
                    ) {

                        skipped++

                        continue
                    }

                    /*
                     * First try the original ID.
                     */
                    try {

                        db.businessDao()
                            .insertBusiness(
                                candidate
                            )

                    } catch (ex: Exception) {

                        /*
                         * If the ID already exists,
                         * let Room generate a new ID.
                         */
                        try {

                            db.businessDao()
                                .insertBusiness(
                                    candidate.copy(
                                        id = 0
                                    )
                                )

                        } catch (ex2: Exception) {

                            Log.w(
                                TAG,
                                "Failed to insert restored record",
                                ex2
                            )

                            skipped++

                            continue
                        }
                    }

                    imported++
                }

                Log.i(
                    TAG,
                    "Restore import completed: " +
                            "imported=$imported " +
                            "skipped=$skipped"
                )

                ImportResult(
                    success = true,
                    imported = imported,
                    skipped = skipped,
                    error = null
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Import failed",
                    e
                )

                ImportResult(
                    success = false,
                    imported = imported,
                    skipped = skipped,
                    error = e.message
                )
            }
        }
    }


    // ============================================================
    // RESULT CLASSES
    // ============================================================

    data class ImportResult(
        val success: Boolean,
        val imported: Int,
        val skipped: Int,
        val error: String?
    )

    data class RestoreResult(
        val success: Boolean,
        val imported: Int,
        val skipped: Int,
        val error: String?
    )

    data class BackupResult(
        val success: Boolean,
        val message: String
    )
}