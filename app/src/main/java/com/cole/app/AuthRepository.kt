package com.cole.app

import com.google.firebase.auth.FirebaseAuth
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
}
