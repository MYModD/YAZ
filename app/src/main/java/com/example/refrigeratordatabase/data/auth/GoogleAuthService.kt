package com.example.refrigeratordatabase.data.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.calendar.CalendarScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * GoogleAuthService - Google認証サービス
 *
 * PHPでいう「OAuth認証でアクセストークンを取得する処理」に相当。
 * Google Sign-In APIを使ってGoogleアカウントでログインし、
 * Calendar APIにアクセスするためのトークンを取得する。
 *
 * 注意: Google Sign-In APIは非推奨（deprecated）ですが、
 * 現時点では引き続き動作します。将来的にはCredential Manager APIへの
 * 移行が推奨されています。
 *
 * PHPの例:
 * ```php
 * // OAuth認証URL生成
 * $authUrl = $client->createAuthUrl();
 * // コールバックでアクセストークン取得
 * $accessToken = $client->fetchAccessTokenWithAuthCode($_GET['code']);
 * $_SESSION['access_token'] = $accessToken;
 * ```
 */
@Suppress("DEPRECATION")  // Google Sign-In APIは非推奨だが現在も動作する
class GoogleAuthService(private val context: Context) {

    companion object {
        private const val TAG = "GoogleAuthService"
        // Google Cloud ConsoleのOAuth Client ID (Web Application用)
        // ※ Android用ではなくWeb用のClient IDを使用する
        @Suppress("SpellCheckingInspection")  // OAuth Client IDなのでタイポ警告を抑制
        const val WEB_CLIENT_ID = "498849439056-oaaj0ld6m50j1pabkrd1p6aod0j2bbn0.apps.googleusercontent.com"
    }

    // Google Sign-In Client（トークン取得用）
    // ※ CALENDAR（読み書き両用）スコープを要求して、イベント追加も可能にする
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(WEB_CLIENT_ID)
            .requestScopes(Scope(CalendarScopes.CALENDAR))  // 読み書き両用に変更
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    /**
     * 現在のサインイン状態を確認
     * PHPでいう: isset($_SESSION['access_token'])
     */
    fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && GoogleSignIn.hasPermissions(account, Scope(CalendarScopes.CALENDAR))
    }

    /**
     * 現在サインインしているアカウントを取得
     */
    fun getCurrentAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    /**
     * サインインIntentを取得
     * ActivityからstartActivityForResultで呼び出す
     */
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    /**
     * サインイン結果を処理
     * onActivityResultから呼び出す
     */
    fun handleSignInResult(data: Intent?): GoogleSignInResult {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            
            Log.d(TAG, "Sign-in successful: ${account.email}")
            GoogleSignInResult.Success(
                email = account.email ?: "",
                displayName = account.displayName ?: ""
            )
        } catch (e: ApiException) {
            Log.e(TAG, "Sign-in failed with code: ${e.statusCode}", e)
            val errorMessage = when (e.statusCode) {
                12500 -> "Google Play Servicesの更新が必要です"
                12501 -> "サインインがキャンセルされました"
                12502 -> "サインイン中にエラーが発生しました"
                10 -> "開発者設定エラー（SHA-1またはパッケージ名を確認）"
                else -> "サインインに失敗しました (コード: ${e.statusCode})"
            }
            GoogleSignInResult.Error(errorMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in failed", e)
            GoogleSignInResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * サイレントサインインを試行
     * すでにサインイン済みの場合に使用
     */
    suspend fun trySilentSignIn(): GoogleSignInResult = withContext(Dispatchers.IO) {
        try {
            val account = googleSignInClient.silentSignIn().await()
            Log.d(TAG, "Silent sign-in successful: ${account.email}")
            GoogleSignInResult.Success(
                email = account.email ?: "",
                displayName = account.displayName ?: ""
            )
        } catch (e: ApiException) {
            Log.w(TAG, "Silent sign-in failed, need interactive sign-in", e)
            GoogleSignInResult.NeedInteractiveSignIn
        } catch (e: Exception) {
            Log.e(TAG, "Silent sign-in failed", e)
            GoogleSignInResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * サインアウト
     * PHPでいう: unset($_SESSION['access_token']); session_destroy();
     */
    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            googleSignInClient.signOut().await()
            Log.d(TAG, "Sign-out successful")
        } catch (e: Exception) {
            Log.e(TAG, "Sign-out failed", e)
        }
    }

    /**
     * Google Sign-In Clientを取得
     */
    fun getSignInClient(): GoogleSignInClient = googleSignInClient
}

/**
 * Google Sign-Inの結果
 * PHPでいう連想配列での結果返却に相当:
 * ['success' => true, 'email' => '...'] or ['success' => false, 'error' => '...']
 */
sealed class GoogleSignInResult {
    data class Success(
        val email: String,
        val displayName: String
    ) : GoogleSignInResult()
    
    data class Error(val message: String) : GoogleSignInResult()
    
    data object Cancelled : GoogleSignInResult()
    
    /** インタラクティブなサインインが必要 */
    data object NeedInteractiveSignIn : GoogleSignInResult()
}
