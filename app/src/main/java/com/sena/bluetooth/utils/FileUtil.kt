package com.sena.bluetooth.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.FileUtils
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream


/**
 * FileName: FilelUti
 * Author: JiaoCan
 * Date: 2024/5/9 10:52
 */

object FileUtil {

    fun createFileInDownload(context: Context, fileName: String, mimeType: String?): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues()
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            mimeType?.let { values.put(MediaStore.Downloads.MIME_TYPE, it) }
            values.put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/aitmed")

            return context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        } else {
            val dir =
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path + File.separator + "aitmed")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, fileName)

            return Uri.fromFile(file)
        }
    }

    fun uriToPath(context: Context, uri: Uri): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return getFilePath(context, uri)
        }

        if (DocumentsContract.isDocumentUri(context, uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            if ("com.android.externalstorage.documents" == uri.authority) {
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                if ("primary" == type) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
            } else if ("com.android.providers.downloads.documents" == uri.authority) {
                val contentUri = ContentUris.withAppendedId(Uri.parse("content://download/public_downloads"), docId.toLong())
                return getFilePath(contentUri, context, null, null)
            } else if ("com.android.providers.media.documents" == uri.authority) {
                val split = docId.split(":").toTypedArray()
                val contentUri: Uri? = when (split[0]) {
                    "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    else -> null
                }
                val selection = MediaStore.Images.Media._ID + "=?"
                val selectionArgs = arrayOf(try { split[1] } catch (e: Exception) { "" })
                return contentUri?.let { getFilePath(it, context, selection, selectionArgs) }
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            return if ("com.google.android.apps.photos.content" == uri.authority) {
                uri.lastPathSegment
            } else {
                getFilePath(uri, context, null, null)
            }
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }

        return null
    }

    @SuppressLint("Range", "Recycle")
    @RequiresApi(api = Build.VERSION_CODES.Q)
    fun getFilePath(context: Context, uri: Uri): String? {
        var file: File? = null

        if (ContentResolver.SCHEME_FILE == uri.scheme) {
            file = File(uri.path!!)
        } else if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            val cr = context.contentResolver
            cr.query(uri, null, null, null)?.let { cursor ->
                cursor.moveToFirst()
                cr.openInputStream(uri)?.let {
                    val displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    val cache = File(context.externalCacheDir, displayName)
                    val os = FileOutputStream(cache)
                    FileUtils.copy(it, os)
                    file  = cache

                    it.close()
                    os.close()
                }
            }
        }

        return file?.absolutePath
    }

    @SuppressLint("Range")
    fun getFilePath(uri: Uri, context: Context, selection: String?, selectionArgs: Array<String>?): String? {
        var path: String? = null
        var cursor: Cursor? = null
        val column = MediaStore.Images.Media.DATA
        val projection = arrayOf(column)
        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(column))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return path
    }
//
//    @RequiresApi(Build.VERSION_CODES.Q)
//    @SuppressLint("Range", "Recycle")
//    fun getFileByUri(context: Context, uri: Uri): File? {
//        var file: File? = null
//        if (ContentResolver.SCHEME_FILE == uri.scheme) {
//            file = File(uri.path!!)
//        } else if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
//            val cr = context.contentResolver
//            cr.query(uri, null, null, null)?.let { cursor ->
//                cursor.moveToFirst()
//
//                cr.openInputStream(uri)?.let {
//                    val displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
//                    val cache = File(context.externalCacheDir, "${((Math.random() + 1) * 10000).roundToInt()}${displayName}")
//                    val fos = FileOutputStream(cache)
//                    FileUtils.copy(it, fos)
//                    file = cache
//
//                    it.close()
//                    fos.close()
//                }
//
//            }
//        }
//
//        return file
//    }


    
}

