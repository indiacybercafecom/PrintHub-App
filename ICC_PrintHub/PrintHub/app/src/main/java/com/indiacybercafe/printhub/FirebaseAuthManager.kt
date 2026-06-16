package com.indiacybercafe.printhub

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class FirebaseAuthManager {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "FirebaseAuthManager"
    }

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    /**
     * Sends OTP to the provided phone number.
     * Optimizes for silent verification (Play Integrity) where possible.
     */
    fun sendOtp(
        phoneNumber: String,
        activity: Activity,
        forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null,
        onCodeSent: (String, PhoneAuthProvider.ForceResendingToken) -> Unit,
        onVerificationCompleted: (PhoneAuthCredential) -> Unit,
        onVerificationFailed: (Exception) -> Unit
    ) {
        Log.d(TAG, "Attempting to send OTP to $phoneNumber. Ensuring Play Integrity flow...")
        
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // This callback is triggered in two cases:
                // 1. Instant verification: The phone number is verified without an SMS.
                // 2. Auto-retrieval: The SMS is received and automatically read.
                Log.d(TAG, "onVerificationCompleted: Instant/Auto verification successful.")
                onVerificationCompleted(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // This is where we detect why the silent flow failed and why it might fallback to browser.
                val errorMessage = e.message ?: "Unknown error"
                Log.e(TAG, "onVerificationFailed: $errorMessage", e)
                
                when {
                    errorMessage.contains("SafetyNet", ignoreCase = true) -> {
                        Log.e(TAG, "Fallback Alert: SafetyNet/Play Integrity check failed. Check SHA-256 and Play Integrity API status in Google Cloud Console.")
                    }
                    errorMessage.contains("reCAPTCHA", ignoreCase = true) -> {
                        Log.e(TAG, "Fallback Alert: Browser reCAPTCHA triggered. This usually happens if Play Integrity fails or the device is suspicious.")
                    }
                    errorMessage.contains("app identifier", ignoreCase = true) -> {
                        Log.e(TAG, "Fallback Alert: Project configuration mismatch. Ensure SHA-1/SHA-256 and Package Name match Firebase Console exactly.")
                    }
                }
                
                onVerificationFailed(e)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d(TAG, "onCodeSent: SMS sent successfully. ID: $verificationId")
                onCodeSent(verificationId, token)
            }
        }

        try {
            val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)       // Phone number to verify
                .setTimeout(60L, TimeUnit.SECONDS) // Timeout for auto-retrieval
                .setActivity(activity)             // Activity (for callback binding and reCAPTCHA fallback)
                .setCallbacks(callbacks)           // OnVerificationStateChangedCallbacks

            // Attach resend token if this is a resend request
            forceResendingToken?.let {
                optionsBuilder.setForceResendingToken(it)
            }

            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Error in verifyPhoneNumber call", e)
            onVerificationFailed(e)
        }
    }

    /**
     * Verifies the OTP code entered by the user.
     */
    fun verifyOtp(
        verificationId: String,
        otp: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        signInWithCredential(credential, onSuccess, onFailure)
    }

    /**
     * Signs in the user with a PhoneAuthCredential (can come from manual OTP or auto-verification).
     */
    fun signInWithCredential(
        credential: PhoneAuthCredential,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Log.d(TAG, "Signing in with credential...")
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential: Success")
                    onSuccess()
                } else {
                    Log.e(TAG, "signInWithCredential: Failure", task.exception)
                    onFailure(task.exception ?: Exception("Verification failed"))
                }
            }
    }

    fun checkUserExists(uid: String, onResult: (Boolean) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document -> 
                onResult(document.exists()) 
            }
            .addOnFailureListener { e -> 
                Log.e(TAG, "Firestore check failed", e)
                onResult(false) 
            }
    }

    fun logout() {
        auth.signOut()
    }

    fun getCurrentUserUid(): String? = auth.currentUser?.uid
}
