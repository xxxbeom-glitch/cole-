package com.aptox.app

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
import com.kakao.sdk.auth.model.Prompt
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

    /**
     * 네이버 SDK 지연 초기화. 로그인 시도 시점에만 호출해 KeyStoreException 크래시 방지.
     * init 실패 시(손상된 캐시 등) 크래시 대신 명시적 예외로 사용자 안내.
     */
    private fun ensureNaverSdkInitialized(activity: MainActivity) {
        when (naverInitState) {
            NAVER_INIT_OK -> return
            NAVER_INIT_FAILED -> throw IllegalStateException(
                "네이버 로그인을 사용할 수 없어요. 설정 > 앱 > aptox > 저장공간 > 캐시 삭제 후 다시 시도해 주세요."
            )
            else -> {
                try {
                    NidOAuth.initialize(activity, "BQa59cheqz4qQQ2H9Xen", "Ujdrf2_Czv", "aptox.")
                    naverInitState = NAVER_INIT_OK
                } catch (e: Throwable) {
                    naverInitState = NAVER_INIT_FAILED
                    Log.e(TAG, "NidOAuth init 실패 (KeyStoreException 등)", e)
                    throw IllegalStateException(
                        "네이버 로그인을 사용할 수 없어요. 설정 > 앱 > aptox > 저장공간 > 캐시 삭제 후 다시 시도해 주세요."
                    )
                }
            }
        }
    }

    /** 네이버 로그인 코루틴 래퍼 (동의 화면 스킵 방지: 로그인 전 기존 토큰 완전 초기화) */
    private suspend fun getNaverToken(activity: MainActivity): String {
        ensureNaverSdkInitialized(activity)
        // disconnect()로 클라이언트+서버 토큰 모두 삭제 → 동의 화면 노출 보장
        runCatching {
            suspendCancellableCoroutine<Unit> { cont ->
                NidOAuth.disconnect(object : NidOAuthCallback {
                    override fun onSuccess() { cont.resume(Unit) }
                    override fun onFailure(errorCode: String, errorDesc: String) {
                        Log.w(TAG, "네이버 disconnect 선행 호출 실패, logout 시도: $errorCode - $errorDesc")
                        NidOAuth.logout(object : NidOAuthCallback {
                            override fun onSuccess() { cont.resume(Unit) }
                            override fun onFailure(e2: String, e2d: String) { cont.resume(Unit) }
                        })
                    }
                })
            }
        }
        return suspendCancellableCoroutine { cont ->
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

    /** 카카오 토큰 코루틴 래퍼 (카카오톡 앱 우선, 실패 시 카카오 계정 웹 fallback) */
    private suspend fun getKakaoToken(context: Context): OAuthToken =
        suspendCancellableCoroutine { cont ->
            val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
                if (error != null) {
                    Log.e(TAG, "카카오 계정 로그인 실패", error)
                    cont.resumeWithException(error)
                } else if (token != null) {
                    cont.resume(token)
                } else {
                    cont.resumeWithException(IllegalStateException("Kakao token is null"))
                }
            }
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                    if (error != null) {
                        Log.w(TAG, "카카오톡 앱 로그인 실패, 카카오 계정 fallback: ${error.message}", error)
                        if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                            cont.resumeWithException(error)
                            return@loginWithKakaoTalk
                        }
                        UserApiClient.instance.loginWithKakaoAccount(
                            context,
                            prompts = listOf(Prompt.LOGIN),
                            callback = callback,
                        )
                    } else if (token != null) {
                        cont.resume(token)
                    } else {
                        Log.w(TAG, "카카오톡 앱 로그인 token/error 둘 다 null, 카카오 계정 fallback")
                        UserApiClient.instance.loginWithKakaoAccount(
                            context,
                            prompts = listOf(Prompt.LOGIN),
                            callback = callback,
                        )
                    }
                }
            } else {
                Log.d(TAG, "카카오톡 미설치, 카카오 계정 웹 로그인으로 진행")
                UserApiClient.instance.loginWithKakaoAccount(
                    context,
                    prompts = listOf(Prompt.LOGIN),
                    callback = callback,
                )
            }
        }

    companion object {
        private const val TAG = "AuthRepository"
        private const val NAVER_INIT_PENDING = 0
        private const val NAVER_INIT_OK = 1
        private const val NAVER_INIT_FAILED = 2
        @Volatile
        private var naverInitState: Int = NAVER_INIT_PENDING
    }

    /** 현재 로그인된 사용자 표시용 정보 */
    data class CurrentUserInfo(
        val uid: String,
        val displayText: String,
        val providerLabel: String,
    )

    /** 현재 로그인된 사용자 정보 (Firestore users에서 provider, nickname, email 조회) */
    suspend fun getCurrentUserInfo(): CurrentUserInfo? = runCatching {
        val user = auth.currentUser ?: return@runCatching null
        val doc = runCatching { firestore.collection("users").document(user.uid).get().await() }.getOrNull()
        val provider = doc?.getString("provider") ?: "계정"
        val nickname = doc?.getString("nickname")?.takeIf { it.isNotBlank() }
        val email = doc?.getString("email")?.takeIf { it.isNotBlank() } ?: user.email
        val displayText = (nickname ?: email ?: user.displayName)?.takeIf { it.isNotBlank() } ?: "로그인됨"
        val providerLabel = when (provider) {
            "kakao" -> "카카오"
            "naver" -> "네이버"
            "google" -> "구글"
            else -> provider
        }
        CurrentUserInfo(uid = user.uid, displayText = displayText, providerLabel = providerLabel)
    }.getOrNull()

    /** 로그아웃 (Firebase Auth + 카카오 세션) */
    suspend fun signOut() = runCatching {
        try {
            UserApiClient.instance.logout {}
        } catch (_: Exception) { }
        auth.signOut()
        Log.d(TAG, "로그아웃 완료")
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
