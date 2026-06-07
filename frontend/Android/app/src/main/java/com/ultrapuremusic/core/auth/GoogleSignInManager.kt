package com.ultrapuremusic.core.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.ultrapuremusic.core.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleSignInManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Resolved from google-services.json by the Google Services Gradle plugin
    // (R.string.default_web_client_id is the OAuth 2.0 web client id, type 3).
    // Each developer's local Firebase project is used automatically.
    private val webClientId: String
        get() = context.getString(com.ultrapuremusic.R.string.default_web_client_id)

    // YouTube read-only scope — declared up front so the very first sign-in
    // already grants it. After login we can immediately fetch the access token
    // and sync the user's playlists without a second permission prompt.
    private val youtubeScope = Scope("https://www.googleapis.com/auth/youtube.readonly")

    private val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientId)
        .requestEmail()
        .requestProfile()
        .requestScopes(youtubeScope)
        .build()

    private val client: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(context, signInOptions)
    }

    fun getSignInIntent(): Intent = client.signInIntent

    suspend fun handleSignInResult(data: Intent?): GoogleSignInAccount? {
        if (data == null) return null
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            task.await()
        } catch (e: Exception) {
            null
        }
    }

    fun getLastSignedInAccount(): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)

    suspend fun signOut() {
        client.signOut().await()
    }

    // ── YouTube OAuth ────────────────────────────────────────────────────────

    /**
     * Attempts to get a YouTube OAuth access token for the currently signed-in account.
     *
     * @throws UserRecoverableAuthException if the user must grant YouTube permission.
     *   The caller should launch `exception.intent` and retry after the user approves.
     * @throws Exception for other failures.
     * @return the access token, or null if no account is signed in.
     */
    suspend fun getYoutubeToken(): String? = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null
        val androidAccount = account.account ?: return@withContext null
        GoogleAuthUtil.getToken(
            context,
            androidAccount,
            "oauth2:https://www.googleapis.com/auth/youtube.readonly",
        )
    }
}
