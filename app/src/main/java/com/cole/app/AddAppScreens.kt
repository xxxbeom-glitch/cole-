package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.ImageView
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────
// AA-01: 제한 앱 추가 플로우
// AA-02A-03, AA-03-01 공통 화면 재사용
// ─────────────────────────────────────────────

/** 앱 제한 플로우 헤더 상단 여백 (status bar 아래) — 전체 화면 통일 */
private val AddAppHeaderTopPadding = 10.dp

/** 제한 앱 추가 플로우 단계 */
enum class AddAppStep {
    AA_01,           // 제한 방법 선택 (시간지정/일일)
    AA_02A_01,       // 제한 방식 선택 (일일/시간지정/차단) — 앱 선택 후
    // 일일사용량 제한 플로우 (5화면)
    AA_DAILY_01, AA_DAILY_02, AA_DAILY_03, AA_DAILY_04, AA_DAILY_05,
    // 시간지정제한 플로우
    AA_02A_TIME_01,  // 잠깐 확인 + 앱/제한시간 row (241-3287, 241-3341)
    AA_02A_TIME_05,  // 설정 요약 (241-3084)
    AA_02B_02,       // 앱 차단 — 설정
    AA_03_01,        // 공통: 완료 확인
}

// ─────────────────────────────────────────────
// AA-01: 제한 방법 선택 (Figma 기반)
// 두 가지 선택 → AA-02A(시간 지정) / AA-02B(일일 사용량)
// ─────────────────────────────────────────────

@Composable
fun AddAppScreenAA01(
    onTimeSpecifiedClick: () -> Unit,
    onDailyLimitClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        SelectionCardItem(
            "시간 지정 제한",
            "지정 시간만큼 사용을 제한해요",
            "",
        ),
        SelectionCardItem(
            "일일 사용량 제한",
            "하루에 정해진 만큼만 사용해요",
            "",
        ),
    )
    var selected by remember { mutableIntStateOf(-1) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = AddAppHeaderTopPadding)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        ColeHeaderSub(
            title = "제한 방법 선택",
            backIcon = painterResource(R.drawable.ic_back),
            onBackClick = onBackClick,
            showNotification = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 44.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            ColeSelectionCardGroup(
                items = options,
                selectedIndex = selected,
                onItemSelected = { index ->
                    selected = index
                    when (index) {
                        0 -> onTimeSpecifiedClick()
                        else -> onDailyLimitClick()
                    }
                },
            )
        }
    }
}

// ─────────────────────────────────────────────
// 앱 선택 화면 (AA-01 이전 또는 플로우 내)
// ─────────────────────────────────────────────

@Composable
fun AddAppScreenAppSelect(
    onAddAppClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = AddAppHeaderTopPadding)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        ColeHeaderSub(
            title = "앱 제한",
            backIcon = painterResource(R.drawable.ic_back),
            onBackClick = onBackClick,
            showNotification = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = "제한할 앱을 선택해주세요",
                style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary),
            )
            ColeInfoBox(
                text = "선택한 앱에 사용 시간 제한이나 차단을 설정할 수 있습니다.",
            )
            Spacer(modifier = Modifier.height(24.dp))
            ColeAddAppButton(
                text = "앱 추가하기",
                icon = painterResource(R.drawable.ic_add),
                onClick = onAddAppClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─────────────────────────────────────────────
// AA-02A-01: 제한 방식 선택 (Figma 241-3084)
// 일일사용량 제한 | 시간지정제한 | 앱 차단
// ─────────────────────────────────────────────

@Composable
fun AddAppScreenAA02A01(
    onDailyLimitClick: () -> Unit,   // 일일사용량 제한
    onTimeSpecifiedClick: () -> Unit, // 시간지정제한
    onBlockClick: () -> Unit,         // 앱 차단
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val limitOptions = listOf(
        SelectionCardItem("일일사용량 제한", "매일 사용할 수 있는 시간을 설정합니다", "30분"),
        SelectionCardItem("시간지정제한", "특정 시간대에만 사용 가능하도록 설정합니다", "설정"),
        SelectionCardItem("앱 차단", "특정 시간대에 앱 사용을 차단합니다", "설정"),
    )
    var selected by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = AddAppHeaderTopPadding)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        ColeHeaderSub(title = "앱 제한", backIcon = painterResource(R.drawable.ic_back), onBackClick = onBackClick, showNotification = true, modifier = Modifier.fillMaxWidth())
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("제한 방식을 선택해주세요", style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary))
            ColeSelectionCardGroup(
                items = limitOptions,
                selectedIndex = selected,
                onItemSelected = { selected = it },
            )
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ColePrimaryButton(
                    text = "다음",
                    onClick = {
                        when (selected) {
                            0 -> onDailyLimitClick()
                            1 -> onTimeSpecifiedClick()
                            else -> onBlockClick()
                        }
                    },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// AA-02A-03: 앱 선택 바텀시트 (Figma 238-2311)
// 검색 필드 + 스크롤 리스트, 최대 3개 선택
// ─────────────────────────────────────────────

private const val MAX_APP_SELECTION = 1

/** 앱 선택 아이템 (name: 표시명, packageName: 아이콘 로드용, null이면 placeholder) */
private data class AppSelectItem(val name: String, val packageName: String?)

/** 앱 선택용 mock 리스트 (~20개). PackageManager 로드 실패 시 사용 */
private val APP_SELECT_MOCK_LIST = listOf(
    "인스타그램", "유튜브", "틱톡", "넷플릭스", "페이스북",
    "카카오톡", "배달의민족", "당근마켓", "쿠팡", "네이버",
    "토스", "람보르기니", "쇼핑", "게임", "뮤직",
    "지도", "메시지", "카메라", "갤러리", "설정",
).map { AppSelectItem(it, null) }

@Composable
fun AddAppSelectBottomSheet(
    initialSelected: Set<String> = emptySet(),
    onDismissRequest: () -> Unit,
    onSelectComplete: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(initialSelected) }
    var searchQuery by remember { mutableStateOf("") }
    var appList by remember { mutableStateOf(APP_SELECT_MOCK_LIST) }

    LaunchedEffect(Unit) {
        val items = withContext(Dispatchers.Default) {
            @Suppress("DEPRECATION")
            runCatching {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                pm.queryIntentActivities(intent, 0)
                    .mapNotNull { ri ->
                        runCatching {
                            val pkg = ri.activityInfo.packageName
                            val appInfo = pm.getApplicationInfo(pkg, 0)
                            val label = (pm.getApplicationLabel(appInfo) as? String)?.takeIf { it.isNotBlank() }
                            label?.let { AppSelectItem(it, pkg) }
                        }.getOrNull()
                    }
                    .distinctBy { it.name }
                    .sortedBy { it.name }
                    .takeIf { it.isNotEmpty() }
            }.getOrNull()
        }
        if (items != null) appList = items
    }

    val filteredApps = remember(searchQuery, appList) {
        if (searchQuery.isBlank()) appList
        else appList.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    fun tryToggleApp(appName: String) {
        when {
            appName in selected -> selected = selected - appName
            selected.size >= MAX_APP_SELECTION -> {
                Toast.makeText(context, "최대 1개까지만 선택 가능합니다", Toast.LENGTH_SHORT).show()
            }
            else -> selected = selected + appName
        }
    }

    BaseBottomSheet(
        title = "앱을 선택해주세요",
        subtitle = "제한할 앱 1개를 선택해주세요",
        onDismissRequest = onDismissRequest,
        onPrimaryClick = { if (selected.isNotEmpty()) onSelectComplete(selected) },
        primaryButtonText = "선택완료",
        primaryButtonEnabled = selected.isNotEmpty(),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
        ) {
            ColeTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "앱 검색",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filteredApps) { item ->
                    val appName = item.name
                    val isDisabled = selected.size >= MAX_APP_SELECTION && appName !in selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isDisabled) Modifier.alpha(0.5f) else Modifier
                            )
                            .clickable { tryToggleApp(appName) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (item.packageName != null) {
                                    AndroidView(
                                        factory = { ctx ->
                                            ImageView(ctx).apply {
                                                setImageDrawable(ctx.packageManager.getApplicationIcon(item.packageName))
                                                scaleType = ImageView.ScaleType.FIT_CENTER
                                            }
                                        },
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_app_placeholder),
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = AppColors.TextBody,
                                    )
                                }
                            }
                            Text(
                                text = appName,
                                style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
                            )
                        }
                        ColeCheckBox(
                            checked = appName in selected,
                            onCheckedChange = { tryToggleApp(appName) },
                            size = 32.dp,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// AA-02A TIME: 시간지정제한 플로우 (Figma 241-3287, 241-3341)
// 잠깐 확인 + 앱 선택 row(→02A-03) + 제한 시간 row(→02A-04) → 다음 → 02A-05
// ─────────────────────────────────────────────

@Composable
fun AddAppScreenAA02ATimeSetup(
    selectedAppNames: Set<String>,
    selectedTimeLimit: String?,
    onAppRowClick: () -> Unit,
    onTimeRowClick: () -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canProceed = selectedAppNames.isNotEmpty() && selectedTimeLimit != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = AddAppHeaderTopPadding)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ColeHeaderSub(
            title = "앱 제한",
            backIcon = painterResource(R.drawable.ic_back),
            onBackClick = onBackClick,
            showNotification = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "아래 내용을 선택해주세요",
                style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary),
            )
            Spacer(modifier = Modifier.height(36.dp))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SelectionRow(
                    label = "앱을 선택해주세요",
                    variant = if (selectedAppNames.isNotEmpty()) SelectionRowVariant.Selected else SelectionRowVariant.Default,
                    selectedValue = selectedAppNames.joinToString(", "),
                    onClick = onAppRowClick,
                )
                SelectionRow(
                    label = "제한 시간을 지정해주세요",
                    variant = if (selectedTimeLimit != null) SelectionRowVariant.Selected else SelectionRowVariant.Default,
                    selectedValue = selectedTimeLimit ?: "",
                    onClick = onTimeRowClick,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "※ 사용 시간을 1시간 이하를 선택하시면 '10분간 사용하기'를 이용하실 수 없어요",
                style = AppTypography.Caption1.copy(color = AppColors.TextDisclaimer),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    top = 0.dp,
                    end = 16.dp,
                    bottom = 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ColePrimaryButton(
                text = "계속 진행",
                onClick = onNextClick,
                enabled = canProceed,
            )
        }
    }
}

// ─────────────────────────────────────────────
// AA-02A-05: 설정 요약 (Figma 241-3084) — 선택된 앱, 제한 시간 2개만
// ─────────────────────────────────────────────

@Composable
fun AddAppScreenAA02ATimeSummary(
    selectedAppName: String,
    selectedTimeLimit: String,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = AddAppHeaderTopPadding)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ColeHeaderSub(
            title = "앱 제한",
            backIcon = painterResource(R.drawable.ic_back),
            onBackClick = onBackClick,
            showNotification = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "잠깐! 확인하세요",
                    style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary),
                )
                Text(
                    text = buildAnnotatedString {
                        append("한 번 설정하면 되돌릴 수 없고, ")
                        append("해제를 원하시면 ")
                        pushStyle(SpanStyle(color = AppColors.TextHighlight, fontSize = 14.sp))
                        append("1,900원 결제 후 앱 사용")
                        pop()
                        append("이 가능해요")
                    },
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                )
            }
            Spacer(modifier = Modifier.height(36.dp))
            AddAppTimeSummaryBox(
                selectedAppName = selectedAppName,
                selectedTimeLimit = selectedTimeLimit,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    top = 0.dp,
                    end = 16.dp,
                    bottom = 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ColePrimaryButton(
                text = "계속 진행",
                onClick = onNextClick,
            )
        }
    }
}

@Composable
fun AddAppTimeSummaryBox(
    selectedAppName: String,
    selectedTimeLimit: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.SurfaceBackgroundInfoBox, RoundedCornerShape(12.dp))
            .border(0.5.dp, AppColors.BorderInfoBox, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AddAppSummaryRow(label = "선택된 앱", value = selectedAppName)
        AddAppSummaryRow(label = "제한 시간", value = selectedTimeLimit)
    }
}

@Composable
fun AddAppSummaryRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
        )
        Text(
            text = value,
            style = AppTypography.BodyBold.copy(color = AppColors.TextHighlight),
            textAlign = TextAlign.End,
        )
    }
}

// ─────────────────────────────────────────────
// 일일사용량 제한 플로우 (5화면)
// Figma 304-1507, 258-2392, 304-1760, 241-3409, 243-6318
// ─────────────────────────────────────────────

@Composable
fun AddAppDailyLimitScreen01(
    onNextClick: (minutes: String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeSteps = listOf("30분", "1시간", "2시간", "3시간", "4시간")
    var selectedIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = AddAppHeaderTopPadding)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        ColeHeaderSub(title = "앱 제한", backIcon = painterResource(R.drawable.ic_back), onBackClick = onBackClick, showNotification = true, modifier = Modifier.fillMaxWidth())
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("일일 사용 한도를 설정해주세요", style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary))
            Text("하루 동안 사용할 수 있는 최대 시간을 선택하세요", style = AppTypography.BodyMedium.copy(color = AppColors.TextBody))
            Text("사용 시간", style = AppTypography.BodyMedium.copy(color = AppColors.FormTextLabel))
            ColeStepBar(steps = timeSteps, selectedIndex = selectedIndex, onStepSelected = { selectedIndex = it })
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ColePrimaryButton(text = "다음", onClick = { onNextClick(timeSteps[selectedIndex]) })
            }
        }
    }
}

@Composable
fun AddAppDailyLimitScreen02(
    onNextClick: (warnEnabled: Boolean) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var warnAt80 by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = AddAppHeaderTopPadding)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        ColeHeaderSub(title = "앱 제한", backIcon = painterResource(R.drawable.ic_back), onBackClick = onBackClick, showNotification = true, modifier = Modifier.fillMaxWidth())
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("사용량 경고 알림", style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary))
            Text("설정한 한도의 80%에 도달하면 알림을 보냅니다.", style = AppTypography.BodyMedium.copy(color = AppColors.TextBody))
            SelectionRow(
                label = "80% 도달 시 알림",
                variant = SelectionRowVariant.Switch,
                switchChecked = warnAt80,
                onSwitchChange = { warnAt80 = it },
            )
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ColePrimaryButton(text = "다음", onClick = { onNextClick(warnAt80) })
            }
        }
    }
}

@Composable
fun AddAppDailyLimitScreen03(
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedDays by remember { mutableStateOf(emptySet<Int>()) }
    val dayLabels = listOf("월", "화", "수", "목", "금", "토", "일")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = AddAppHeaderTopPadding)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        ColeHeaderSub(title = "앱 제한", backIcon = painterResource(R.drawable.ic_back), onBackClick = onBackClick, showNotification = true, modifier = Modifier.fillMaxWidth())
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("적용 요일을 선택해주세요", style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary))
            Text("선택한 요일에만 일일 사용 한도가 적용됩니다.", style = AppTypography.BodyMedium.copy(color = AppColors.TextBody))
            ColeChipRow(labels = dayLabels, selectedIndices = selectedDays, onChipClick = { i -> selectedDays = if (i in selectedDays) selectedDays - i else selectedDays + i })
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ColePrimaryButton(text = "다음", onClick = onNextClick)
            }
        }
    }
}

@Composable
fun AddAppDailyLimitScreen04(
    limitMinutes: String,
    warnEnabled: Boolean,
    onConfirmClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = AddAppHeaderTopPadding)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        ColeHeaderSub(title = "앱 제한", backIcon = painterResource(R.drawable.ic_back), onBackClick = onBackClick, showNotification = true, modifier = Modifier.fillMaxWidth())
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("설정 내용을 확인해주세요", style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary))
            ColeInfoBox(
                text = "• 일일 사용 한도: $limitMinutes\n• 80% 도달 알림: ${if (warnEnabled) "ON" else "OFF"}",
            )
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ColePrimaryButton(text = "확인", onClick = onConfirmClick)
            }
        }
    }
}

@Composable
fun AddAppDailyLimitScreen05(
    appName: String,
    onCompleteClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = AddAppHeaderTopPadding)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        ColeHeaderSub(title = "앱 제한", backIcon = painterResource(R.drawable.ic_back), onBackClick = onBackClick, showNotification = true, modifier = Modifier.fillMaxWidth())
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("설정이 완료되었습니다", style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary))
            ColeInfoBox(text = "$appName 앱에 일일 사용량 제한이 적용되었습니다.")
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ColePrimaryButton(text = "완료", onClick = onCompleteClick)
            }
        }
    }
}

// ─────────────────────────────────────────────
// AA-02A-03 / AA-03-01 공통: 사용 시간 + 요일 설정
// Figma 241-3341 — AA-02A-03, AA-03-01 재사용
// ─────────────────────────────────────────────

@Composable
fun AddAppCommonTimeScheduleScreen(
    title: String,
    timeSteps: List<String> = listOf("30분", "1시간", "2시간", "3시간", "4시간"),
    initialTimeIndex: Int = 0,
    dayLabels: List<String> = listOf("월", "화", "수", "목", "금", "토", "일"),
    primaryButtonText: String = "다음",
    onPrimaryClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var timeIndex by remember { mutableIntStateOf(initialTimeIndex) }
    var selectedDays by remember { mutableStateOf(emptySet<Int>()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = AddAppHeaderTopPadding)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        ColeHeaderSub(title = "앱 제한", backIcon = painterResource(R.drawable.ic_back), onBackClick = onBackClick, showNotification = true, modifier = Modifier.fillMaxWidth())
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(title, style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary))
            Text("사용 시간", style = AppTypography.BodyMedium.copy(color = AppColors.FormTextLabel))
            ColeStepBar(steps = timeSteps, selectedIndex = timeIndex, onStepSelected = { timeIndex = it })
            Text("적용 요일", style = AppTypography.BodyMedium.copy(color = AppColors.FormTextLabel))
            ColeChipRow(labels = dayLabels, selectedIndices = selectedDays, onChipClick = { i -> selectedDays = if (i in selectedDays) selectedDays - i else selectedDays + i })
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ColePrimaryButton(text = primaryButtonText, onClick = onPrimaryClick)
            }
        }
    }
}

// ─────────────────────────────────────────────
// AA-03-01 공통: 완료 확인 화면 (Figma 238-2311, 304-1974)
// ─────────────────────────────────────────────

@Composable
fun AddAppCommonConfirmScreen(
    appName: String,
    limitDescription: String,
    onCompleteClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = AddAppHeaderTopPadding)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        ColeHeaderSub(title = "앱 제한", backIcon = painterResource(R.drawable.ic_back), onBackClick = onBackClick, showNotification = true, modifier = Modifier.fillMaxWidth())
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("설정이 완료되었습니다", style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary))
            ColeInfoBox(text = "$appName 앱에 $limitDescription 가 적용되었습니다.")
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ColePrimaryButton(text = "완료", onClick = onCompleteClick)
            }
        }
    }
}

@Composable
fun AddAppFlowHost(
    onComplete: () -> Unit,
    onBackFromFirst: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf(AddAppStep.AA_01) }
    var previousStepBeforeConfirm by remember { mutableStateOf(AddAppStep.AA_02A_TIME_05) }
    var selectedAppNames by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedTimeLimit by remember { mutableStateOf<String?>(null) }
    var showAppSelectSheet by remember { mutableStateOf(false) }
    var showTimeLimitSheet by remember { mutableStateOf(false) }
    val timeSteps = listOf("30분", "60분", "90분", "120분", "150분", "180분")
    // 일일사용량 제한 플로우 상태
    var dailyLimitMinutes by remember { mutableStateOf("30분") }
    var dailyWarnEnabled by remember { mutableStateOf(true) }

    when (step) {
        AddAppStep.AA_01 -> AddAppScreenAA01(
            onTimeSpecifiedClick = { step = AddAppStep.AA_02A_TIME_01 },
            onDailyLimitClick = { step = AddAppStep.AA_DAILY_01 },
            onBackClick = onBackFromFirst,
        )
        AddAppStep.AA_02A_01 -> AddAppScreenAA02A01(
            onDailyLimitClick = { step = AddAppStep.AA_DAILY_01 },
            onTimeSpecifiedClick = { step = AddAppStep.AA_02A_TIME_01 },
            onBlockClick = { step = AddAppStep.AA_02B_02 },
            onBackClick = { step = AddAppStep.AA_01 },
        )
        // 일일사용량 제한 플로우 (5화면)
        AddAppStep.AA_DAILY_01 -> AddAppDailyLimitScreen01(
            onNextClick = { mins -> dailyLimitMinutes = mins; step = AddAppStep.AA_DAILY_02 },
            onBackClick = { step = AddAppStep.AA_02A_01 },
        )
        AddAppStep.AA_DAILY_02 -> AddAppDailyLimitScreen02(
            onNextClick = { enabled -> dailyWarnEnabled = enabled; step = AddAppStep.AA_DAILY_03 },
            onBackClick = { step = AddAppStep.AA_DAILY_01 },
        )
        AddAppStep.AA_DAILY_03 -> AddAppDailyLimitScreen03(
            onNextClick = { step = AddAppStep.AA_DAILY_04 },
            onBackClick = { step = AddAppStep.AA_DAILY_02 },
        )
        AddAppStep.AA_DAILY_04 -> AddAppDailyLimitScreen04(
            limitMinutes = dailyLimitMinutes,
            warnEnabled = dailyWarnEnabled,
            onConfirmClick = { step = AddAppStep.AA_DAILY_05 },
            onBackClick = { step = AddAppStep.AA_DAILY_03 },
        )
        AddAppStep.AA_DAILY_05 -> AddAppDailyLimitScreen05(
            appName = selectedAppNames.joinToString(", ").ifEmpty { "앱" },
            onCompleteClick = onComplete,
            onBackClick = { step = AddAppStep.AA_DAILY_04 },
        )
        // 시간지정제한 플로우
        AddAppStep.AA_02A_TIME_01 -> {
            AddAppScreenAA02ATimeSetup(
                selectedAppNames = selectedAppNames,
                selectedTimeLimit = selectedTimeLimit,
                onAppRowClick = { showAppSelectSheet = true },
                onTimeRowClick = { showTimeLimitSheet = true },
                onNextClick = { step = AddAppStep.AA_02A_TIME_05 },
                onBackClick = { step = AddAppStep.AA_01 },
            )
            if (showAppSelectSheet) {
                AddAppSelectBottomSheet(
                    initialSelected = selectedAppNames,
                    onDismissRequest = { showAppSelectSheet = false },
                    onSelectComplete = { names ->
                        selectedAppNames = names
                        showAppSelectSheet = false
                    },
                )
            }
            if (showTimeLimitSheet) {
                AppLimitSetupTimeBottomSheet(
                    title = "제한 시간을 지정해주세요",
                    subtitle = "본인의 사용패턴을 생각해 시간을 선택해주세요",
                    steps = timeSteps,
                    initialIndex = selectedTimeLimit?.let { timeSteps.indexOf(it) }?.takeIf { it >= 0 } ?: 0,
                    onDismissRequest = { showTimeLimitSheet = false },
                    onPrimaryClick = { _, time ->
                        selectedTimeLimit = time
                        showTimeLimitSheet = false
                    },
                )
            }
        }
        AddAppStep.AA_02A_TIME_05 -> AddAppScreenAA02ATimeSummary(
            selectedAppName = selectedAppNames.joinToString(", "),
            selectedTimeLimit = selectedTimeLimit ?: "",
            onNextClick = { previousStepBeforeConfirm = AddAppStep.AA_02A_TIME_05; step = AddAppStep.AA_03_01 },
            onBackClick = { step = AddAppStep.AA_02A_TIME_01 },
        )
        AddAppStep.AA_02B_02 -> AddAppCommonTimeScheduleScreen(
            title = "차단 시간을 설정해주세요",
            timeSteps = listOf("09:00~12:00", "12:00~18:00", "18:00~22:00", "전체"),
            primaryButtonText = "완료",
            onPrimaryClick = { previousStepBeforeConfirm = AddAppStep.AA_02B_02; step = AddAppStep.AA_03_01 },
            onBackClick = { step = AddAppStep.AA_02A_01 },
        )
        AddAppStep.AA_03_01 -> AddAppCommonConfirmScreen(
            appName = selectedAppNames.joinToString(", ").ifEmpty { "앱" },
            limitDescription = "시간지정제한",
            onCompleteClick = onComplete,
            onBackClick = { step = previousStepBeforeConfirm },
        )
    }
}
