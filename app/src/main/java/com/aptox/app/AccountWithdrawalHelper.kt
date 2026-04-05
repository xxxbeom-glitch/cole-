package com.aptox.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.work.WorkManager
import com.aptox.app.usage.AppDatabaseProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 설정 > 계정관리 > 탈퇴: 확인 후 `deleteAccount` Functions → Firestore 정리 → 로그아웃·로컬 초기화 → 감사 다이얼로그 → 스플래시(메인) 재시작.
 */
object AccountWithdrawalHelper {

    private const val TAG = "AccountWithdrawal"

    /**
     * 탈퇴 확인 화면에서 호출. 서버에서 Auth 삭제 후 Firestore·로컬 정리, 감사 다이얼로그, 앱 재시작까지 처리한다.
     */
    fun startWithdrawalFlow(activity: ComponentActivity) {
        val app = activity.applicationContext
        runCatching { WorkManager.getInstance(app).cancelAllWork() }
        performWithdrawalThenThankYouAndRestart(activity)
    }

    private fun showThankYouDialogThenRestart(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle("이용해주셔서 감사합니다")
            .setPositiveButton("확인") { _, _ ->
                val intent = Intent(activity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                activity.startActivity(intent)
                (activity as? ComponentActivity)?.finishAffinity()
                Handler(Looper.getMainLooper()).postDelayed(
                    { Process.killProcess(Process.myPid()) },
                    250L,
                )
            }
            .setCancelable(false)
            .show()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun performWithdrawalThenThankYouAndRestart(activity: ComponentActivity) {
        val app = activity.applicationContext
        GlobalScope.launch(Dispatchers.Main.immediate) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Toast.makeText(activity, "로그인된 사용자가 없습니다.", Toast.LENGTH_LONG).show()
                return@launch
            }
            val uid = currentUser.uid
            val remoteStep = runCatching {
                withContext(Dispatchers.IO + NonCancellable) {
                    FirebaseFunctions.getInstance()
                        .getHttpsCallable("deleteAccount")
                        .withTimeout(60, TimeUnit.SECONDS)
                        .call(hashMapOf<String, Any>())
                        .await()
                    val firestore = FirebaseFirestore.getInstance()
                    deleteUserFirestoreSubtree(firestore, uid)
                    AuthRepository().signOut()
                    wipeLocalStorage(app)
                }
            }
            if (remoteStep.isFailure) {
                val err = remoteStep.exceptionOrNull()
                Log.e(TAG, "탈퇴 처리 실패", err)
                val toastText = err.toWithdrawalToastMessage()
                Toast.makeText(activity, toastText, Toast.LENGTH_LONG).show()
                return@launch
            }
            showThankYouDialogThenRestart(activity)
        }
    }

    private fun Throwable?.toWithdrawalToastMessage(): String {
        val fe = (this as? FirebaseFunctionsException)
            ?: (this?.cause as? FirebaseFunctionsException)
        if (fe != null) {
            return when (fe.code) {
                FirebaseFunctionsException.Code.UNAUTHENTICATED -> "로그인이 필요합니다. 다시 로그인 후 시도해주세요."
                FirebaseFunctionsException.Code.NOT_FOUND,
                FirebaseFunctionsException.Code.UNIMPLEMENTED,
                -> "탈퇴 서비스를 불러올 수 없습니다. 앱을 최신으로 업데이트했는지 확인해주세요."
                FirebaseFunctionsException.Code.UNAVAILABLE -> "인터넷 연결을 확인한 뒤 다시 시도해주세요."
                else -> fe.message?.takeIf { it.isNotBlank() }
                    ?: "탈퇴 처리 중 오류가 발생했습니다. 네트워크를 확인한 뒤 다시 시도해주세요."
            }
        }
        return this?.message?.takeIf { it.isNotBlank() }
            ?: "탈퇴 처리 중 오류가 발생했습니다. 네트워크를 확인한 뒤 다시 시도해주세요."
    }

    /**
     * 클라이언트는 하위 컬렉션 목록 조회가 불가하므로, 앱에서 사용하는 users/{uid} 하위 경로를 모두 비운 뒤 루트 문서 삭제.
     */
    private suspend fun deleteUserFirestoreSubtree(firestore: FirebaseFirestore, userId: String) {
        val userRef = firestore.collection("users").document(userId)
        firestore.deleteNestedAppLimitLogs(userRef)
        firestore.deleteNestedTimeSegments(userRef)
        firestore.deleteAllDocumentsInCollection(userRef.collection("dailyUsage"))
        firestore.deleteAllDocumentsInCollection(userRef.collection("categoryStats"))
        firestore.deleteAllDocumentsInCollection(userRef.collection("notifications"))
        firestore.deleteAllDocumentsInCollection(userRef.collection("badges"))
        userRef.delete().await()
    }

    private suspend fun FirebaseFirestore.deleteAllDocumentsInCollection(coll: CollectionReference) {
        while (true) {
            val snap = coll.limit(BATCH_DELETE_LIMIT).get().await()
            if (snap.isEmpty) break
            val batch = batch()
            for (doc in snap.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
        }
    }

    /** users/{uid}/appLimitLogs/{package}/events/{eventId} */
    private suspend fun FirebaseFirestore.deleteNestedAppLimitLogs(userRef: DocumentReference) {
        val coll = userRef.collection("appLimitLogs")
        while (true) {
            val pkgSnap = coll.limit(NESTED_PARENT_PAGE).get().await()
            if (pkgSnap.isEmpty) break
            for (pkgDoc in pkgSnap.documents) {
                deleteAllDocumentsInCollection(pkgDoc.reference.collection("events"))
                pkgDoc.reference.delete().await()
            }
        }
    }

    /** users/{uid}/timeSegments/{package}/days/{yyyy-MM-dd} */
    private suspend fun FirebaseFirestore.deleteNestedTimeSegments(userRef: DocumentReference) {
        val coll = userRef.collection("timeSegments")
        while (true) {
            val pkgSnap = coll.limit(NESTED_PARENT_PAGE).get().await()
            if (pkgSnap.isEmpty) break
            for (pkgDoc in pkgSnap.documents) {
                deleteAllDocumentsInCollection(pkgDoc.reference.collection("days"))
                pkgDoc.reference.delete().await()
            }
        }
    }

    private const val BATCH_DELETE_LIMIT = 450L
    private const val NESTED_PARENT_PAGE = 40L

    private fun wipeLocalStorage(context: Context) {
        val app = context.applicationContext
        AppDatabaseProvider.clearAndClose(app)
        val dataDir = File(app.applicationInfo.dataDir)
        deleteDirContents(File(dataDir, "shared_prefs"))
        deleteDirContents(File(dataDir, "datastore"))
        deleteDirContents(File(dataDir, "databases"))
        deleteDirContents(File(dataDir, "no_backup"))
        app.filesDir.listFiles()?.forEach { it.deleteRecursivelyIgnoringErrors() }
        app.cacheDir.listFiles()?.forEach { it.deleteRecursivelyIgnoringErrors() }
        app.externalCacheDir?.listFiles()?.forEach { it.deleteRecursivelyIgnoringErrors() }
        app.getExternalFilesDirs(null).filterNotNull().forEach { root ->
            root.listFiles()?.forEach { it.deleteRecursivelyIgnoringErrors() }
        }
    }

    private fun deleteDirContents(dir: File) {
        if (!dir.isDirectory) return
        dir.listFiles()?.forEach { it.deleteRecursivelyIgnoringErrors() }
    }

    private fun File.deleteRecursivelyIgnoringErrors() {
        runCatching {
            if (isDirectory) {
                listFiles()?.forEach { it.deleteRecursivelyIgnoringErrors() }
            }
            delete()
        }
    }
}
