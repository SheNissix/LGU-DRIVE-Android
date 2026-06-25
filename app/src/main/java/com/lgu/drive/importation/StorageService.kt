package com.lgu.drive.importation

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object StorageService {
    suspend fun saveSignatureLocally(context: Context, fileUri: Uri): String = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(fileUri)
            ?: throw Exception("Could not open file input stream")

        // Create a dedicated directory for signatures in internal storage
        val sigDir = File(context.filesDir, "signatures")
        if (!sigDir.exists()) {
            sigDir.mkdirs()
        }

        val uniqueFileName = "uploaded_signature_${(1000..9999).random()}.png"
        val localFile = File(sigDir, uniqueFileName)

        FileOutputStream(localFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }

        // Return the absolute local path to store in the database
        localFile.absolutePath
    }
}