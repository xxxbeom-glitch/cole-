package com.aptox.app.widget

import android.content.res.Resources
import android.util.TypedValue
import androidx.annotation.DimenRes

internal fun Resources.readDesignFloat(@DimenRes id: Int): Float {
    val tv = TypedValue()
    getValue(id, tv, true)
    return when (tv.type) {
        TypedValue.TYPE_FLOAT -> tv.float
        else -> error("Expected float item for res 0x${Integer.toHexString(id)}, type=${tv.type}")
    }
}
