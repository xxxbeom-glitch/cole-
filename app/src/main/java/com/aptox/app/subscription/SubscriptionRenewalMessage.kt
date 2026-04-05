package com.aptox.app.subscription

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object SubscriptionRenewalMessage {

    private val koreanDateTime: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy년 M월 d일 a h시 m분", Locale.KOREAN)

    private val koreanDateOnly: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREAN)

    /**
     * @param expiryEpochMillis Play purchase JSON의 `expiryTimeMillis` 등. 0이면 날짜 미확인.
     * @param autoRenewing [com.android.billingclient.api.Purchase.isAutoRenewing] 와 동일.
     */
    fun renewalOrExpiryLine(expiryEpochMillis: Long, autoRenewing: Boolean): String {
        if (expiryEpochMillis <= 0L) {
            return if (autoRenewing) {
                "다음 갱신 일시는 Play 스토어 구독에서 확인할 수 있어요."
            } else {
                "구독 만료 일시는 Play 스토어 구독에서 확인할 수 있어요."
            }
        }
        val formatted = Instant.ofEpochMilli(expiryEpochMillis)
            .atZone(ZoneId.systemDefault())
            .format(koreanDateTime)
        return if (autoRenewing) {
            "${formatted}에 갱신됩니다"
        } else {
            "${formatted}에 구독이 만료됩니다"
        }
    }

    /** 구독관리 화면「다음 결제일」표시용 (날짜만). 만료 시각을 알 수 없으면 안내 문구. */
    fun nextBillingDateText(expiryEpochMillis: Long): String {
        if (expiryEpochMillis <= 0L) {
            return "Play 스토어에서 확인"
        }
        return Instant.ofEpochMilli(expiryEpochMillis)
            .atZone(ZoneId.systemDefault())
            .format(koreanDateOnly)
    }
}
