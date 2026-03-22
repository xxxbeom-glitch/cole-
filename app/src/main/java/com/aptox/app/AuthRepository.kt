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
import kotlinx.coroutines.tasks.await

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
            throw IllegalStateException("구글 로그인이 취소되었습니다.", e)
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

    companion object {
        private const val TAG = "AuthRepository"
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

    /** 로그아웃 */
    suspend fun signOut() = runCatching {
        auth.signOut()
        Log.d(TAG, "로그아웃 완료")
    }
}
