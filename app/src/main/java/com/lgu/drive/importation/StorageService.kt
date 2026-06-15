package com.lgu.drive.importation

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object StorageService {
    suspend fun uploadSignature(fileUri: Uri): String = suspendCancellableCoroutine { continuation ->
        try {
            val storageRef = Firebase.storage("gs://lgu-drive.firebasestorage.app").reference
            // Matches your requested format: signatures/uploaded_signature_1464.png
            val uniqueFileName = "signatures/uploaded_signature_${(1000..9999).random()}.png"
            val fileRef = storageRef.child(uniqueFileName)

            val uploadTask = fileRef.putFile(fileUri)

            uploadTask.addOnSuccessListener {
                // Immediately return the relative path instead of fetching the download URL
                continuation.resume(uniqueFileName)
            }.addOnFailureListener { exception ->
                if (continuation.isActive) {
                    continuation.resumeWithException(exception)
                }
            }

            continuation.invokeOnCancellation {
                uploadTask.cancel()
            }
        } catch (t: Throwable) {
            if (continuation.isActive) {
                continuation.resumeWithException(Exception("Firebase is not connected to your app yet. Please connect it in Android Studio. Details: ${t.message}"))
            }
        }
    }
}


