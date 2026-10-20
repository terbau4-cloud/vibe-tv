package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.R
import com.example.model.Gender
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

data class AuthUser(
    val uid: String,
    val name: String,
    val email: String,
    val gender: Gender
)

class AuthManager(private val context: Context) {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore get() = try {
        val dbId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(dbId)
    } catch (e: Exception) {
        FirebaseFirestore.getInstance()
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("vibetv_auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "AuthManager"
        private const val KEY_UID = "user_uid"
        private const val KEY_NAME = "user_name"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_GENDER = "user_gender"
    }

    fun getPersistedSession(): AuthUser? {
        val authUid = try { auth.currentUser?.uid } catch (e: Exception) { null }
        val uid = prefs.getString(KEY_UID, null) ?: authUid ?: return null
        val name = prefs.getString(KEY_NAME, null) ?: try { auth.currentUser?.displayName } catch (e: Exception) { null } ?: "User"
        val email = prefs.getString(KEY_EMAIL, null) ?: try { auth.currentUser?.email } catch (e: Exception) { null } ?: "user@vibetv.app"
        val genderStr = prefs.getString(KEY_GENDER, Gender.MALE.name) ?: Gender.MALE.name
        val gender = try { Gender.valueOf(genderStr) } catch (e: Exception) { Gender.MALE }
        return AuthUser(uid = uid, name = name, email = email, gender = gender)
    }

    suspend fun register(
        fullName: String,
        emailOrUsername: String,
        password: String,
        gender: Gender
    ): Result<AuthUser> {
        val trimmedName = fullName.trim()
        val trimmedInput = emailOrUsername.trim()
        val trimmedPass = password.trim()

        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your full name."))
        }
        if (trimmedInput.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter an email or username."))
        }
        if (trimmedPass.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        val isEmail = trimmedInput.contains("@")
        val effectiveEmail = if (isEmail) trimmedInput.lowercase() else "${trimmedInput.lowercase()}@vibetv.app"
        val usernameKey = if (isEmail) trimmedInput.substringBefore("@").lowercase() else trimmedInput.lowercase()

        try {
            // First check if username is already taken in Firestore
            val existingUsernameDoc = db.collection("usernames").document(usernameKey).get().await()
            if (existingUsernameDoc.exists()) {
                val existingEmail = existingUsernameDoc.getString("email")
                if (existingEmail != null && existingEmail != effectiveEmail) {
                    return Result.failure(IllegalArgumentException("This username is already taken. Please choose another."))
                }
            }

            // Attempt Firebase Auth user creation
            var uid: String? = null
            try {
                val authResult = auth.createUserWithEmailAndPassword(effectiveEmail, trimmedPass).await()
                uid = authResult.user?.uid
            } catch (authEx: FirebaseAuthException) {
                Log.w(TAG, "FirebaseAuth exception: ${authEx.errorCode} - ${authEx.message}")
                if (authEx.errorCode == "ERROR_EMAIL_ALREADY_IN_USE") {
                    return Result.failure(IllegalArgumentException("This account already exists. Please log in."))
                } else if (authEx.errorCode == "ERROR_WEAK_PASSWORD") {
                    return Result.failure(IllegalArgumentException("Password is too weak. Please use at least 6 characters."))
                } else if (authEx.errorCode == "ERROR_INVALID_EMAIL") {
                    return Result.failure(IllegalArgumentException("Invalid email format."))
                } else {
                    // If cloud Firebase console has not toggled email provider yet, allow fallback Firestore storage
                    Log.i(TAG, "Falling back to Firestore user creation...")
                }
            } catch (ex: Exception) {
                Log.w(TAG, "FirebaseAuth fallback: ${ex.message}")
            }

            val finalUid = uid ?: "user_${hashString(effectiveEmail).take(16)}"
            val userProfile = hashMapOf(
                "uid" to finalUid,
                "name" to trimmedName,
                "email" to effectiveEmail,
                "gender" to gender.name,
                "passwordHash" to hashString(trimmedPass),
                "isOnline" to true,
                "createdAt" to System.currentTimeMillis()
            )

            // Save in Firestore
            db.collection("users").document(finalUid).set(userProfile).await()
            db.collection("usernames").document(usernameKey).set(
                hashMapOf("email" to effectiveEmail, "uid" to finalUid)
            ).await()

            // Save session
            persistSession(finalUid, trimmedName, effectiveEmail, gender)

            return Result.success(AuthUser(finalUid, trimmedName, effectiveEmail, gender))
        } catch (e: Exception) {
            Log.e(TAG, "Registration error", e)
            return Result.failure(e)
        }
    }

    suspend fun login(
        emailOrUsername: String,
        password: String
    ): Result<AuthUser> {
        val trimmedInput = emailOrUsername.trim()
        val trimmedPass = password.trim()

        if (trimmedInput.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your email or username."))
        }
        if (trimmedPass.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your password."))
        }

        val isEmail = trimmedInput.contains("@")
        var effectiveEmail = if (isEmail) trimmedInput.lowercase() else "${trimmedInput.lowercase()}@vibetv.app"

        try {
            // If username, check if mapped to an email in Firestore
            if (!isEmail) {
                val usernameDoc = db.collection("usernames").document(trimmedInput.lowercase()).get().await()
                if (usernameDoc.exists()) {
                    val mapped = usernameDoc.getString("email")
                    if (!mapped.isNullOrBlank()) {
                        effectiveEmail = mapped
                    }
                }
            }

            var authUid: String? = null
            var firebaseAuthSuccess = false

            try {
                val authResult = auth.signInWithEmailAndPassword(effectiveEmail, trimmedPass).await()
                authUid = authResult.user?.uid
                firebaseAuthSuccess = true
            } catch (authEx: FirebaseAuthException) {
                Log.w(TAG, "Firebase login error: ${authEx.errorCode} - ${authEx.message}")
                if (authEx.errorCode == "ERROR_WRONG_PASSWORD" || authEx.errorCode == "ERROR_INVALID_CREDENTIAL") {
                    return Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
                } else if (authEx.errorCode == "ERROR_USER_NOT_FOUND") {
                    return Result.failure(IllegalArgumentException("No account found with these details. Please register first."))
                }
                // Other errors fall through to Firestore credential verification
            } catch (ex: Exception) {
                Log.w(TAG, "Firebase login general error: ${ex.message}")
            }

            // Verify with Firestore user document
            var userDoc = if (authUid != null) {
                db.collection("users").document(authUid).get().await()
            } else {
                null
            }

            if (userDoc == null || !userDoc.exists()) {
                val query = db.collection("users")
                    .whereEqualTo("email", effectiveEmail)
                    .limit(1)
                    .get()
                    .await()
                if (!query.isEmpty) {
                    userDoc = query.documents.first()
                }
            }

            if (userDoc != null && userDoc.exists()) {
                val storedHash = userDoc.getString("passwordHash")
                if (!firebaseAuthSuccess && storedHash != null && storedHash != hashString(trimmedPass)) {
                    return Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
                }

                val finalUid = userDoc.getString("uid") ?: authUid ?: userDoc.id
                val name = userDoc.getString("name") ?: effectiveEmail.substringBefore("@")
                val genderStr = userDoc.getString("gender") ?: Gender.MALE.name
                val gender = try { Gender.valueOf(genderStr) } catch (e: Exception) { Gender.MALE }

                persistSession(finalUid, name, effectiveEmail, gender)
                return Result.success(AuthUser(finalUid, name, effectiveEmail, gender))
            } else if (firebaseAuthSuccess && authUid != null) {
                val name = effectiveEmail.substringBefore("@")
                persistSession(authUid, name, effectiveEmail, Gender.MALE)
                return Result.success(AuthUser(authUid, name, effectiveEmail, Gender.MALE))
            } else {
                return Result.failure(IllegalArgumentException("No account found for '$trimmedInput'. Please register first."))
            }

        } catch (e: IllegalArgumentException) {
            return Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Login error", e)
            return Result.failure(IllegalArgumentException("Authentication failed: ${e.message}"))
        }
    }

    fun logout() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "auth.signOut error", e)
        }
        prefs.edit().clear().apply()
    }

    private fun persistSession(uid: String, name: String, email: String, gender: Gender) {
        prefs.edit()
            .putString(KEY_UID, uid)
            .putString(KEY_NAME, name)
            .putString(KEY_EMAIL, email)
            .putString(KEY_GENDER, gender.name)
            .apply()
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
