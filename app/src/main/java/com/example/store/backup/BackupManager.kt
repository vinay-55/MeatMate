package com.example.store.backup

import android.content.Context
import android.util.Log
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

object BackupManager {

    private const val TAG = "BackupManager"
    private const val BACKUP_FILENAME = "store_backup.json"

    // Initialize auth helper (must be called once from an Activity)
    fun init(activity: androidx.activity.ComponentActivity) {
        AuthManager.init(activity)
    }

    // Manual trigger (UI) - suspend function to perform backup now
    suspend fun backupNow(context: Context): BackupResult {
        return withContext(Dispatchers.IO) {
            try {
                val token = AuthManager.getAccessToken()
                if (token.isNullOrEmpty()) return@withContext BackupResult(false, "Not signed in")

                val db = DatabaseProvider.getDatabase(context)
                val list = db.businessDao().getAllBusiness()

                val jsonArr = JSONArray()
                for (b in list) {
                    val o = JSONObject()
                    o.put("id", b.id)
                    o.put("product", b.product)
                    o.put("price", b.price)
                    o.put("weight", b.weight)
                    o.put("total", b.total)
                    o.put("date", b.date)
                    if (b.slno != null) o.put("slno", b.slno) else o.put("slno", JSONObject.NULL)
                    jsonArr.put(o)
                }

                val root = JSONObject()
                root.put("backupVersion", 1)
                root.put("backupTimestamp", System.currentTimeMillis())
                root.put("records", jsonArr)

                val content = root.toString()

                val existingId = findBackupFileId(token)
                val success = uploadFile(token, existingId, content, update = existingId != null)
                if (success) BackupResult(true, "Backup uploaded") else BackupResult(false, "Upload failed")

            } catch (e: Exception) {
                Log.e(TAG, "backupNow failed", e)
                BackupResult(false, e.message ?: "Unknown error")
            }
        }
    }

    // Restore if a backup exists in appDataFolder. Returns RestoreResult.
    suspend fun restoreIfAvailable(context: Context): RestoreResult {
        return withContext(Dispatchers.IO) {
            try {
                val token = AuthManager.getAccessToken()
                if (token.isNullOrEmpty()) return@withContext RestoreResult(false, 0, 0, "Not signed in")

                val fileId = findBackupFileId(token)
                if (fileId == null) return@withContext RestoreResult(false, 0, 0, "No backup found")

                val content = downloadFileContent(token, fileId) ?: return@withContext RestoreResult(false, 0, 0, "Failed to download")

                val result = importFromJsonString(context, content)
                RestoreResult(result.success, result.imported, result.skipped, result.error)
            } catch (e: Exception) {
                RestoreResult(false, 0, 0, e.message)
            }
        }
    }

    private fun findBackupFileId(token: String): String? {
        val client = OkHttpClient()
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("www.googleapis.com")
            .addPathSegments("drive/v3/files")
            .addQueryParameter("q", "name='${BACKUP_FILENAME}' and 'appDataFolder' in parents and trashed=false")
            .addQueryParameter("spaces", "appDataFolder")
            .addQueryParameter("fields", "files(id,name)")
            .build()

        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                Log.w(TAG, "List request failed: ${resp.code}")
                return null
            }
            val body = resp.body?.string() ?: return null
            val root = JSONObject(body)
            val arr = root.optJSONArray("files") ?: JSONArray()
            if (arr.length() > 0) return arr.getJSONObject(0).getString("id")
            return null
        }
    }

    private fun downloadFileContent(token: String, fileId: String): String? {
        val client = OkHttpClient()
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("www.googleapis.com")
            .addPathSegments("drive/v3/files/$fileId")
            .addQueryParameter("alt", "media")
            .build()

        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                Log.w(TAG, "Download failed: ${resp.code}")
                return null
            }
            return resp.body?.string()
        }
    }

    private fun uploadFile(token: String, fileId: String?, content: String, update: Boolean): Boolean {
        val client = OkHttpClient()
        val metadata = JSONObject()
        metadata.put("name", BACKUP_FILENAME)
        metadata.put("mimeType", "application/json")
        metadata.put("parents", JSONArray().put("appDataFolder"))

        val metadataPart = metadata.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val contentPart = content.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("metadata", null, metadataPart)
            .addFormDataPart("file", BACKUP_FILENAME, contentPart)
            .build()

        val urlBuilder = HttpUrl.Builder()
            .scheme("https")
            .host("www.googleapis.com")
        if (update && fileId != null) {
            urlBuilder.addPathSegments("upload/drive/v3/files/$fileId")
            urlBuilder.addQueryParameter("uploadType", "multipart")
        } else {
            urlBuilder.addPathSegments("upload/drive/v3/files")
            urlBuilder.addQueryParameter("uploadType", "multipart")
        }

        val req = Request.Builder()
            .url(urlBuilder.build())
            .header("Authorization", "Bearer $token")
            .post(multipart)
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                Log.w(TAG, "Upload failed: ${resp.code}")
                return false
            }
            return true
        }
    }

    private suspend fun importFromJsonString(context: Context, jsonText: String): ImportResult {
        return withContext(Dispatchers.IO) {
            val db = DatabaseProvider.getDatabase(context)
            var imported = 0
            var skipped = 0
            try {
                val root = JSONObject(jsonText)
                val arr = root.optJSONArray("records") ?: JSONArray()

                val existing = db.businessDao().getAllBusiness()

                fun existsDuplicate(candidate: BusinessEntity): Boolean {
                    return existing.any { e ->
                        e.product == candidate.product &&
                                e.date == candidate.date &&
                                ((e.slno == null && candidate.slno == null) || (e.slno == candidate.slno)) &&
                                e.price == candidate.price &&
                                e.weight == candidate.weight &&
                                e.total == candidate.total
                    }
                }

                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val id = o.optInt("id", 0)
                    val product = o.getString("product")
                    val price = o.getDouble("price")
                    val weight = o.getDouble("weight")
                    val total = o.getDouble("total")
                    val date = o.getString("date")
                    val slno = if (o.isNull("slno")) null else o.optInt("slno")

                    val candidate = BusinessEntity(
                        id = id,
                        product = product,
                        price = price,
                        weight = weight,
                        total = total,
                        date = date,
                        slno = slno
                    )

                    if (existsDuplicate(candidate)) {
                        skipped++
                        continue
                    }

                    try {
                        db.businessDao().insertBusiness(candidate)
                    } catch (ex: Exception) {
                        try {
                            db.businessDao().insertBusiness(candidate.copy(id = 0))
                        } catch (ex2: Exception) {
                            Log.w(TAG, "Failed to insert after retry: $candidate", ex2)
                            skipped++
                            continue
                        }
                    }
                    imported++
                }

                ImportResult(true, imported, skipped, null)
            } catch (e: Exception) {
                ImportResult(false, imported, skipped, e.message)
            }
        }
    }

    data class ImportResult(val success: Boolean, val imported: Int, val skipped: Int, val error: String?)
    data class RestoreResult(val success: Boolean, val imported: Int, val skipped: Int, val error: String?)
    data class BackupResult(val success: Boolean, val message: String)
}
