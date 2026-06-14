package com.lgu.drive.importation

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object StorageService {
    /**
     * Uploads a local file Uri to Firebase Storage bucket and returns its secure network download URL string.
     */
    suspend fun uploadSignature(fileUri: Uri): String = suspendCancellableCoroutine { continuation ->
        try {
            // MOVED INSIDE: This prevents the app from crashing on startup if Firebase isn't configured yet!
            val storageRef = Firebase.storage("gs://lgu-drive.firebasestorage.app").reference
            val uniqueFileName = "signatures/uploaded_signature_${UUID.randomUUID()}.png"
            val fileRef = storageRef.child(uniqueFileName)

            val uploadTask = fileRef.putFile(fileUri)

            uploadTask.addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    continuation.resume(downloadUri.toString())
                }.addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
            }.addOnFailureListener { exception ->
                if (continuation.isActive) {
                    continuation.resumeWithException(exception)
                }
            }

            continuation.invokeOnCancellation {
                uploadTask.cancel()
            }
        } catch (t: Throwable) {
            // CRASH PREVENTION: Catches fatal configuration errors (like missing google-services.json)
            if (continuation.isActive) {
                continuation.resumeWithException(Exception("Firebase is not connected to your app yet. Please connect it in Android Studio. Details: ${t.message}"))
            }
        }
    }
}
