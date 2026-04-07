package com.aptox.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeGreeting(
    val title: String,
    val subtext: String,
)

class HomeViewModel : ViewModel() {

    private val _greeting = MutableStateFlow(computeGreeting())
    val greeting: StateFlow<HomeGreeting> = _greeting.asStateFlow()

    init {
        _greeting.value = computeGreeting()
    }

    fun refreshGreeting() {
        _greeting.value = computeGreeting()
    }

    private fun computeGreeting(): HomeGreeting {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            hour in 6..11 -> HomeGreeting(
                title = "좋은 아침이에요",
                subtext = "오늘 하루도 스마트하게 시작해봐요",
            )
            hour in 12..17 -> HomeGreeting(
                title = "안녕하세요",
                subtext = "오후 사용 패턴을 확인해보세요",
            )
            hour in 18..21 -> HomeGreeting(
                title = "오늘 하루도 수고했어요",
                subtext = "오늘 하루 사용량을 확인해볼까요",
            )
            else -> HomeGreeting(
                title = "늦은 시간이네요",
                subtext = "내일을 위해 슬슬 쉬어볼까요",
            )
        }
    }

    class Factory(
        @Suppress("UNUSED_PARAMETER") private val context: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel() as T
        }
    }
}
