package com.cole.app

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NidOAuth
import com.navercorp.nid.oauth.util.NidOAuthCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Firebase Auth + Firestore 회원가입
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance(),
) {
    /**
     * 이메일/비밀번호로 계정 생성 후 Firestore users 컬렉션에 저장
     * @return Result에 uid 포함, 실패 시 예외
     */
    suspend fun signUpWithEmail(email: String, password: String): Result<String> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw IllegalStateException("User is null after sign up")
        val uid = user.uid

        val userData = hashMapOf(
            "uid" to uid,
            "email" to email,
            "createdAt" to FieldValue.serverTimestamp(),
        )
        firestore.collection("users")
            .document(uid)
            .set(userData, SetOptions.merge())
            .await()

        uid
    }

    /**
     * 이메일/비밀번호로 로그인
     */
    suspend fun signInWithEmail(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    /**
     * 회원가입용 인증번호 SMS 발송 (Solapi)
     */
    suspend fun sendSignUpVerificationSms(phone: String): Result<Unit> = runCatching {
        functions.getHttpsCallable("sendSignUpVerificationSms")
            .call(hashMapOf("phone" to phone))
            .await()
    }

    /**
     * 회원가입 인증번호 검증 후 Firebase 계정 생성 (Cloud Function)
     */
    suspend fun verifyAndCompleteSignUp(
        phone: String,
        code: String,
        email: String,
        password: String,
        name: String,
        birth: String,
    ): Result<Unit> = runCatching {
        functions.getHttpsCallable("verifyAndCompleteSignUp")
            .call(
                hashMapOf(
                    "phone" to phone,
                    "code" to code,
                    "email" to email,
                    "password" to password,
                    "name" to name,
                    "birth" to birth,
                )
            )
            .await()
    }

    /**
     * 비밀번호 재설정용 인증번호 SMS 발송 (Solapi, 가입된 휴대폰 번호로)
     */
    suspend fun sendPasswordResetSms(phone: String): Result<Unit> = runCatching {
        functions.getHttpsCallable("sendPasswordResetSms")
            .call(hashMapOf("phone" to phone))
            .await()
    }

    /**
     * 비밀번호 재설정 인증번호 검증 후 비밀번호 변경 (Cloud Function)
     */
    suspend fun verifyAndResetPassword(phone: String, code: String, newPassword: String): Result<Unit> = runCatching {
        functions.getHttpsCallable("verifyAndResetPassword")
            .call(
                hashMapOf(
                    "phone" to phone,
                    "code" to code,
                    "newPassword" to newPassword,
                )
            )
            .await()
    }

    /**
     * 카카오 로그인 후 Firebase Custom Token으로 인증
     * 1. 카카오톡 앱 → 실패 시 카카오 계정 웹뷰 순서로 시도
     * 2. 카카오 accessToken → Cloud Function(kakaoLogin) → Firebase Custom Token
     */
    suspend fun signInWithKakao(context: Context): Result<Unit> = runCatching {
        Log.d(TAG, "카카오 로그인 시작")
        val token = getKakaoToken(context)
        Log.d(TAG, "카카오 토큰 발급 성공: ${token.accessToken.take(10)}...")

        val result = functions.getHttpsCallable("kakaoLogin")
            .call(hashMapOf("accessToken" to token.accessToken))
            .await()
        Log.d(TAG, "kakaoLogin Cloud Function 응답: ${result.data}")

        @Suppress("UNCHECKED_CAST")
        val customToken = (result.data as? Map<String, Any>)?.get("firebaseToken") as? String
            ?: throw IllegalStateException("Firebase custom token is null")

        auth.signInWithCustomToken(customToken).await()
        Log.d(TAG, "카카오 Firebase 로그인 성공: uid=${auth.currentUser?.uid}")
    }

    /**
     * 네이버 로그인 후 Firebase Custom Token으로 인증
     * MainActivity에서 SDK를 초기화한 뒤, authenticate() 콜백을 코루틴으로 래핑
     */
    suspend fun signInWithNaver(activity: MainActivity): Result<Unit> = runCatching {
        Log.d(TAG, "네이버 로그인 시작")
        val accessToken = getNaverToken(activity)
        Log.d(TAG, "네이버 토큰 발급 성공: ${accessToken.take(10)}...")

        val result = functions.getHttpsCallable("naverLogin")
            .call(hashMapOf("accessToken" to accessToken))
            .await()
        Log.d(TAG, "naverLogin Cloud Function 응답: ${result.data}")

        @Suppress("UNCHECKED_CAST")
        val customToken = (result.data as? Map<String, Any>)?.get("firebaseToken") as? String
            ?: throw IllegalStateException("Firebase custom token is null")

        auth.signInWithCustomToken(customToken).await()
        Log.d(TAG, "네이버 Firebase 로그인 성공: uid=${auth.currentUser?.uid}")
    }

    /** 네이버 로그인 코루틴 래퍼 */
    private suspend fun getNaverToken(activity: MainActivity): String =
        suspendCancellableCoroutine { cont ->
            activity.naverLoginCallback = object : NidOAuthCallback {
                override fun onSuccess() {
                    val token = NidOAuth.getAccessToken()
                    if (token != null) cont.resume(token)
                    else cont.resumeWithException(IllegalStateException("Naver access token is null"))
                }
                override fun onFailure(errorCode: String, errorDesc: String) {
                    cont.resumeWithException(IllegalStateException("네이버 로그인 실패($errorCode): $errorDesc"))
                }
            }
            NidOAuth.requestLogin(activity, activity.naverLoginCallback!!)
        }

    /**
     * 구글 로그인 후 Firebase Auth로 인증
     * Credential Manager를 사용해 Google ID Token 획득 → Firebase GoogleAuthProvider로 로그인
     */
    suspend fun signInWithGoogle(context: Context): Result<Unit> = runCatching {
        Log.d(TAG, "구글 로그인 시작")

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("200339538980-dcns3vafkransdp86o3sd74n1ontbb1j.apps.googleusercontent.com")
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(context)
        val credentialResponse = try {
            credentialManager.getCredential(context = context, request = request)
        } catch (e: GetCredentialCancellationException) {
            throw IllegalStateException("구글 로그인이 취소되었습니다.")
        }

        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credentialResponse.credential.data)
        val idToken = googleIdTokenCredential.idToken
        Log.d(TAG, "구글 ID Token 발급 성공")

        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(firebaseCredential).await()
        val user = result.user ?: throw IllegalStateException("Firebase user is null after Google sign-in")

        // Firestore users 저장/업데이트
        val userRef = firestore.collection("users").document(user.uid)
        val userSnap = userRef.get().await()
        if (!userSnap.exists()) {
            userRef.set(
                hashMapOf<String, Any?>(
                    "uid" to user.uid,
                    "provider" to "google",
                    "email" to (user.email ?: ""),
                    "nickname" to (user.displayName ?: ""),
                    "profileImage" to (user.photoUrl?.toString() ?: ""),
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            ).await()
        } else {
            userRef.update(
                "nickname", (user.displayName ?: ""),
                "profileImage", (user.photoUrl?.toString() ?: ""),
                "lastLoginAt", FieldValue.serverTimestamp(),
            ).await()
        }

        Log.d(TAG, "구글 Firebase 로그인 성공: uid=${user.uid}")
    }

    /** 카카오 토큰 코루틴 래퍼 (카카오톡 앱 우선, 실패 시 웹뷰) */
    private suspend fun getKakaoToken(context: Context): OAuthToken =
        suspendCancellableCoroutine { cont ->
            val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
                if (error != null) cont.resumeWithException(error)
                else if (token != null) cont.resume(token)
                else cont.resumeWithException(IllegalStateException("Kakao token is null"))
            }
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                    // 카카오톡 설치 안 됨 / 취소 등 → 카카오 계정으로 fallback
                    if (error != null) {
                        if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                            cont.resumeWithException(error)
                            return@loginWithKakaoTalk
                        }
                        UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
                    } else if (token != null) {
                        cont.resume(token)
                    }
                }
            } else {
                UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
            }
        }

    companion object {
        private const val TAG = "AuthRepository"
    }

    /** 카카오 사용자 정보 조회 */
    private suspend fun getUserKakaoInfo(): Map<String, String>? =
        suspendCancellableCoroutine { cont ->
            UserApiClient.instance.me { user, error ->
                if (error != null || user == null) { cont.resume(null); return@me }
                val info = mutableMapOf<String, String>()
                user.kakaoAccount?.profile?.nickname?.let { info["nickname"] = it }
                user.kakaoAccount?.email?.let { info["email"] = it }
                cont.resume(info)
            }
        }
}
