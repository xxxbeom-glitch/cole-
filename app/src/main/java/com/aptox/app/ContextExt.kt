package com.aptox.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Context에서 Activity를 추출.
 * Compose의 LocalContext가 ContextThemeWrapper 등을 반환해도
 * baseContext 체인을 따라 실제 Activity를 찾는다.
 */
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
