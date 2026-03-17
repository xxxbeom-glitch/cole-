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

class HomeViewModel(
    private val userPrefs: UserPreferencesRepository,
) : ViewModel() {

    private val _greeting = MutableStateFlow(computeGreeting(userName = null))
    val greeting: StateFlow<HomeGreeting> = _greeting.asStateFlow()

    init {
        _greeting.value = computeGreeting(userName = userPrefs.userName)
    }

    fun refreshGreeting() {
        val name = userPrefs.userName
        _greeting.value = computeGreeting(userName = name)
    }

    private fun computeGreeting(userName: String?): HomeGreeting {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val namePart = userName?.takeIf { it.isNotBlank() }
        return when {
            hour in 6..11 -> HomeGreeting(
                title = if (namePart != null) "${namePart}님 좋은 아침이에요" else "좋은 아침이에요",
                subtext = "오늘 하루도 스마트하게 시작해봐요",
            )
            hour in 12..17 -> HomeGreeting(
                title = if (namePart != null) "${namePart}님 안녕하세요" else "안녕하세요",
                subtext = "오후 사용 패턴을 확인해보세요",
            )
            hour in 18..21 -> HomeGreeting(
                title = if (namePart != null) "오늘 하루도 수고했어요, ${namePart}님" else "오늘 하루도 수고했어요",
                subtext = "오늘 하루 사용량을 확인해볼까요",
            )
            else -> HomeGreeting(
                title = if (namePart != null) "늦은 시간이네요, ${namePart}님" else "늦은 시간이네요",
                subtext = "내일을 위해 슬슬 쉬어볼까요",
            )
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(UserPreferencesRepository(context)) as T
        }
    }
}
