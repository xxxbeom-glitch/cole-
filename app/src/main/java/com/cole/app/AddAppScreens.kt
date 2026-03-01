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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.Dp
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

/** 앱 제한 플로우 헤더 상단 여백 (status bar 아래) — 메인 화면과 동일하게 18dp */
// 회원가입 플로우와 헤더 위치 통일 (10dp)
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
    onSelectCompleteWithPackages: ((List<com.cole.app.model.SelectedAppInfo>) -> Unit)? = null,
    /** 다른 제한 방식(일일/시간지정)에서 선택된 앱 패키지 - 해당 앱은 비활성화 */
    additionalRestrictedPackages: Set<String> = emptySet(),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(initialSelected) }
    var searchQuery by remember { mutableStateOf("") }
    var appList by remember { mutableStateOf<List<AppSelectItem>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }

    // 이미 제한 중 + 다른 방식에서 선택된 패키지
    val restrictedPackages = remember(additionalRestrictedPackages) {
        AppRestrictionRepository(context).getAll().map { it.packageName }.toSet() + additionalRestrictedPackages
    }

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
        isLoadingApps = false
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
        subtitle = "앱은 최대 1개까지 선택가능 해요\n이미 제한이 진행중인 앱은 선택하실 수 없어요",
        onDismissRequest = onDismissRequest,
        onPrimaryClick = {
            if (selected.isNotEmpty()) {
                onSelectComplete(selected)
                onSelectCompleteWithPackages?.let { cb ->
                    val withPkg = selected.mapNotNull { name ->
                        appList.find { it.name == name }?.packageName?.let { pkg ->
                            com.cole.app.model.SelectedAppInfo(name, pkg)
                        }
                    }
                    cb(withPkg)
                }
            }
        },
        primaryButtonText = "다음",
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
            if (isLoadingApps) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = AppColors.TextHighlight,
                        modifier = Modifier.size(32.dp),
                    )
                }
            } else LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filteredApps) { item ->
                    val appName = item.name
                    val isSelfApp = item.packageName == context.packageName
                    val isAlreadyRestricted = item.packageName != null && item.packageName in restrictedPackages
                    val isDisabled = isSelfApp || isAlreadyRestricted || (selected.size >= MAX_APP_SELECTION && appName !in selected)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isDisabled) Modifier.alpha(0.5f) else Modifier
                            )
                            .clickable(enabled = !isSelfApp && !isAlreadyRestricted) {
                                if (!isSelfApp && !isAlreadyRestricted) tryToggleApp(appName)
                            },
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
                            onCheckedChange = { if (!isDisabled) tryToggleApp(appName) },
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
                modifier = Modifier.padding(horizontal = 16.dp),
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
// AA-02A-05: 공통 설정 요약 (Figma 241-3084)
// 시간지정/일일 공통 레이아웃, summaryContent만 플로우별 상이
// ─────────────────────────────────────────────

@Composable
fun AddAppCommonConfirmSummaryScreen(
    headerTitle: String,
    summaryContent: @Composable () -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    mainTitle: String = "잠깐! 확인하세요",
    showSubtitle: Boolean = true,
    primaryButtonText: String = "계속 진행",
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
            title = headerTitle,
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
                    text = mainTitle,
                    style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary),
                )
                if (showSubtitle) {
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
            }
            Spacer(modifier = Modifier.height(36.dp))
            summaryContent()
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 24.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ColePrimaryButton(text = primaryButtonText, onClick = onNextClick)
        }
    }
}

@Composable
fun AddAppScreenAA02ATimeSummary(
    selectedAppName: String,
    selectedTimeLimit: String,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AddAppCommonConfirmSummaryScreen(
        headerTitle = "앱 제한",
        mainTitle = "선택하신 내용을 확인해주세요",
        showSubtitle = false,
        primaryButtonText = "다음",
        summaryContent = {
            AddAppTimeSummaryBox(
                selectedAppName = selectedAppName,
                selectedTimeLimit = selectedTimeLimit,
            )
        },
        onNextClick = onNextClick,
        onBackClick = onBackClick,
        modifier = modifier,
    )
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
        AddAppSummaryRow(label = "선택된 앱", value = selectedAppName, valueMaxWidth = 120.dp)
        AddAppSummaryRow(label = "제한 시간", value = selectedTimeLimit)
    }
}

@Composable
fun AddAppDailySummaryBox(
    limitMinutes: String,
    selectedDaysText: String,
    duration: String,
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
        AddAppSummaryRow(label = "사용 시간", value = "$limitMinutes/일")
        AddAppSummaryRow(label = "적용 요일", value = selectedDaysText)
        AddAppSummaryRow(label = "적용 기간", value = duration)
    }
}

/** Figma 241-3538 AA-03-01 완료 화면용 인포박스: 선택된 앱, 일일 사용시간, 반복 요일, 적용 기간 */
@Composable
fun AddAppDailyCompleteSummaryBox(
    appName: String,
    limitMinutes: String,
    selectedDaysText: String,
    duration: String,
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
        AddAppSummaryRow(label = "선택된 앱", value = appName)
        AddAppSummaryRow(label = "일일 사용시간", value = limitMinutes)
        AddAppSummaryRow(label = "반복 요일", value = selectedDaysText)
        AddAppSummaryRow(label = "적용 기간", value = duration)
    }
}

@Composable
fun AddAppSummaryRow(
    label: String,
    value: String,
    valueMaxWidth: Dp? = null,
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
            modifier = valueMaxWidth?.let { Modifier.widthIn(max = it) } ?: Modifier,
            maxLines = if (valueMaxWidth != null) 1 else Int.MAX_VALUE,
            overflow = if (valueMaxWidth != null) TextOverflow.Ellipsis else TextOverflow.Clip,
        )
    }
}

// ─────────────────────────────────────────────
// 일일사용량 제한 플로우 (AA-02B-02)
// Figma 258-2392, 304-1760, 241-3409, 243-6318
// 스크린샷: 헤더 "일일 사용량 제한", 4개 SelectionRow, "다음" 버튼
// ─────────────────────────────────────────────

private val DAILY_TIME_STEPS = listOf("30분", "60분", "90분", "120분", "150분", "180분")
private val DAILY_DAY_LABELS = listOf("월", "화", "수", "목", "금", "토", "일")
private val DAILY_DURATION_OPTIONS = listOf("오늘 하루만", "1주", "2주", "3주", "4주")
private val DAILY_DURATION_OPTIONS_FOR_REPEAT = listOf("1주", "2주", "3주", "4주") // AA-02B-05: 반복 ON일 때만 사용

private fun formatSelectedDays(selectedDays: Set<Int>, labels: List<String>): String =
    selectedDays.sorted().joinToString(", ") { labels.getOrElse(it) { "" } }

@Composable
fun AddAppDailyLimitScreen01(
    selectedAppNames: Set<String>,
    selectedDailyMinutes: String?,
    selectedDays: Set<Int>,
    selectedDuration: String?,
    onAppRowClick: () -> Unit,
    onTimeRowClick: () -> Unit,
    onDaysRowClick: () -> Unit,
    onDurationRowClick: () -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canProceed = selectedAppNames.isNotEmpty() &&
        selectedDailyMinutes != null &&
        selectedDuration != null &&
        (selectedDays.isNotEmpty() || selectedDuration == "오늘 하루만")

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
            title = "일일 사용량 제한",
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
                    label = "하루 사용량을 지정해주세요",
                    variant = if (selectedDailyMinutes != null) SelectionRowVariant.Selected else SelectionRowVariant.Default,
                    selectedValue = selectedDailyMinutes ?: "",
                    onClick = onTimeRowClick,
                )
                SelectionRow(
                    label = "언제 반복할까요",
                    variant = if (selectedDays.isNotEmpty() || selectedDuration == "오늘 하루만") SelectionRowVariant.Selected else SelectionRowVariant.Default,
                    selectedValue = when {
                        selectedDays.isNotEmpty() -> formatSelectedDays(selectedDays, DAILY_DAY_LABELS)
                        selectedDuration == "오늘 하루만" -> "오늘 하루만"
                        else -> ""
                    },
                    onClick = onDaysRowClick,
                )
                SelectionRow(
                    label = "언제까지 할까요",
                    variant = if (selectedDuration != null) SelectionRowVariant.Selected else SelectionRowVariant.Default,
                    selectedValue = selectedDuration ?: "",
                    onClick = onDurationRowClick,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ColePrimaryButton(
                text = "다음",
                onClick = onNextClick,
                enabled = canProceed,
            )
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
    selectedDaysText: String,
    duration: String,
    onConfirmClick: () -> Unit,
    onBackClick: () -> Unit,
    mainTitle: String = "잠깐! 확인하세요",
    showSubtitle: Boolean = true,
    primaryButtonText: String = "계속 진행",
    modifier: Modifier = Modifier,
) {
    AddAppCommonConfirmSummaryScreen(
        headerTitle = "일일 사용량 제한",
        mainTitle = mainTitle,
        showSubtitle = showSubtitle,
        primaryButtonText = primaryButtonText,
        summaryContent = {
            AddAppDailySummaryBox(
                limitMinutes = limitMinutes,
                selectedDaysText = selectedDaysText,
                duration = duration,
            )
        },
        onNextClick = onConfirmClick,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
fun AddAppDailyLimitScreen05(
    appName: String,
    limitMinutes: String,
    selectedDaysText: String,
    duration: String,
    onCompleteClick: () -> Unit,
    onAddAnotherClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AddAppCommonCompleteScreen(
        headerTitle = "일일 사용량 제한",
        summaryContent = {
            AddAppDailyCompleteSummaryBox(
                appName = appName,
                limitMinutes = limitMinutes,
                selectedDaysText = selectedDaysText,
                duration = duration,
            )
        },
        primaryButtonText = "홈으로 가기",
        secondaryButtonText = "다른 앱 추가하기",
        onPrimaryClick = onCompleteClick,
        onSecondaryClick = onAddAnotherClick,
        onBackClick = onBackClick,
        modifier = modifier,
    )
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
// AA-02B-05: 몇 주 동안 진행할까요 (Figma 243-6318, 바텀시트)
// ─────────────────────────────────────────────

@Composable
fun AddAppDailyDurationBottomSheet(
    options: List<String> = DAILY_DURATION_OPTIONS_FOR_REPEAT,
    initialIndex: Int = 0,
    selectedDaysText: String = "",
    limitMinutes: String = "30분",
    onDismissRequest: () -> Unit,
    onPrimaryClick: (String) -> Unit,
    onSecondaryClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, options.lastIndex)) }
    val selectedDuration = options.getOrElse(selectedIndex) { options.firstOrNull() ?: "1주" }

    val subtitleAnnotated = remember(selectedDaysText, limitMinutes, selectedDuration) {
        buildAnnotatedString {
            append("매주 ")
            pushStyle(SpanStyle(color = AppColors.TextHighlight))
            append(selectedDaysText.ifEmpty { "—" })
            pop()
            append(" 마다 ")
            pushStyle(SpanStyle(color = AppColors.TextHighlight))
            append("$limitMinutes/일")
            pop()
            append("로 ")
            pushStyle(SpanStyle(color = AppColors.TextHighlight))
            append(selectedDuration)
            pop()
            append("간 진행돼요")
        }
    }

    BaseBottomSheet(
        title = "몇 주 동안 진행할까요",
        subtitleContent = {
            Text(
                text = subtitleAnnotated,
                style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
            )
        },
        onDismissRequest = onDismissRequest,
        onPrimaryClick = { onPrimaryClick(options[selectedIndex]) },
        primaryButtonText = "완료",
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            ColeSelectionCardTitleOnlyGroup(
                options = options,
                selectedIndex = selectedIndex,
                onOptionSelected = { selectedIndex = it },
                titleStyle = AppTypography.BodyRegular,
                titleColor = AppColors.TextTertiary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─────────────────────────────────────────────
// AA-02B-05 (구 full screen — 디버그/레거시용)
// ─────────────────────────────────────────────

@Composable
fun AddAppDailyDurationScreen(
    options: List<String> = DAILY_DURATION_OPTIONS,
    initialIndex: Int = 0,
    onComplete: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, options.lastIndex)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = AddAppHeaderTopPadding)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        ColeHeaderSub(
            title = "일일 사용량 제한",
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "몇 주 동안 진행할까요",
                    style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary),
                )
                Text(
                    text = "처음이라면 짧게 진행하시는게 좋아요",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                )
                Text(
                    text = "오늘 하루 먼저 해보시고 천천히 늘려가세요",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            ColeSelectionCardTitleOnlyGroup(
                options = options,
                selectedIndex = selectedIndex,
                onOptionSelected = { selectedIndex = it },
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ColePrimaryButton(
                text = "완료",
                onClick = { onComplete(options[selectedIndex]) },
            )
        }
    }
}

// ─────────────────────────────────────────────
// AA-03-01 공통: 설정 완료 화면 (Figma 241-3538)
// 하단 버튼 2단: Primary + Ghost, 12dp 간격
// ─────────────────────────────────────────────

@Composable
fun AddAppCommonCompleteScreen(
    headerTitle: String,
    summaryContent: @Composable () -> Unit,
    primaryButtonText: String,
    secondaryButtonText: String,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
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
            title = headerTitle,
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
            Text("설정이 완료되었습니다", style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary))
            Spacer(modifier = Modifier.height(24.dp))
            summaryContent()
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 24.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ColePrimaryButton(text = primaryButtonText, onClick = onPrimaryClick)
            ColeGhostButton(text = secondaryButtonText, onClick = onSecondaryClick)
        }
    }
}

@Composable
fun AddAppFlowHost(
    onComplete: () -> Unit,
    onBackFromFirst: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(AddAppStep.AA_01) }
    var previousStepBeforeConfirm by remember { mutableStateOf(AddAppStep.AA_02A_TIME_05) }
    var selectedAppNames by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedAppsForTime by remember { mutableStateOf<List<com.cole.app.model.SelectedAppInfo>>(emptyList()) }
    var selectedAppsForDaily by remember { mutableStateOf<List<com.cole.app.model.SelectedAppInfo>>(emptyList()) }
    var selectedTimeLimit by remember { mutableStateOf<String?>(null) }
    var showAppSelectSheet by remember { mutableStateOf(false) }
    var showTimeLimitSheet by remember { mutableStateOf(false) }
    var showDailyAppSelectSheet by remember { mutableStateOf(false) }
    var showDailyTimeSheet by remember { mutableStateOf(false) }
    var showDailyDaysSheet by remember { mutableStateOf(false) }
    var showDailyDurationSheet by remember { mutableStateOf(false) }
    val timeSteps = listOf("30분", "60분", "120분", "180분", "240분", "360분")
    // 일일사용량 제한 플로우 상태
    var dailyLimitMinutes by remember { mutableStateOf<String?>(null) }
    var dailySelectedDays by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var dailySelectedDuration by remember { mutableStateOf<String?>(null) }

    when (step) {
        AddAppStep.AA_01 -> AddAppScreenAA01(
            onTimeSpecifiedClick = {
                selectedAppNames = emptySet()
                selectedAppsForTime = emptyList()
                selectedTimeLimit = null
                step = AddAppStep.AA_02A_TIME_01
            },
            onDailyLimitClick = {
                selectedAppNames = emptySet()
                selectedAppsForDaily = emptyList()
                dailyLimitMinutes = null
                dailySelectedDays = emptySet()
                dailySelectedDuration = null
                step = AddAppStep.AA_DAILY_01
            },
            onBackClick = onBackFromFirst,
        )
        AddAppStep.AA_02A_01 -> AddAppScreenAA02A01(
            onDailyLimitClick = {
                selectedAppNames = emptySet()
                selectedAppsForDaily = emptyList()
                dailyLimitMinutes = null
                dailySelectedDays = emptySet()
                dailySelectedDuration = null
                step = AddAppStep.AA_DAILY_01
            },
            onTimeSpecifiedClick = {
                selectedAppNames = emptySet()
                selectedAppsForTime = emptyList()
                selectedTimeLimit = null
                step = AddAppStep.AA_02A_TIME_01
            },
            onBlockClick = { step = AddAppStep.AA_02B_02 },
            onBackClick = { step = AddAppStep.AA_01 },
        )
        // 일일사용량 제한 플로우 (5화면)
        AddAppStep.AA_DAILY_01 -> {
            AddAppDailyLimitScreen01(
                selectedAppNames = selectedAppNames,
                selectedDailyMinutes = dailyLimitMinutes,
                selectedDays = dailySelectedDays,
                selectedDuration = dailySelectedDuration,
                onAppRowClick = { showDailyAppSelectSheet = true },
                onTimeRowClick = { showDailyTimeSheet = true },
                onDaysRowClick = { showDailyDaysSheet = true },
                onDurationRowClick = { if (dailySelectedDays.isNotEmpty()) showDailyDurationSheet = true },
                onNextClick = { step = AddAppStep.AA_DAILY_04 },
                onBackClick = { step = AddAppStep.AA_01 },
            )
            if (showDailyAppSelectSheet) {
                AddAppSelectBottomSheet(
                    initialSelected = selectedAppNames,
                    onDismissRequest = { showDailyAppSelectSheet = false },
                    onSelectComplete = { names ->
                        selectedAppNames = names
                        showDailyAppSelectSheet = false
                        showDailyTimeSheet = true
                    },
                    onSelectCompleteWithPackages = { selectedAppsForDaily = it },
                    additionalRestrictedPackages = selectedAppsForTime.map { it.packageName }.toSet(),
                )
            }
            if (showDailyTimeSheet) {
                AppLimitSetupTimeBottomSheet(
                    title = "하루 사용량을 지정해주세요",
                    subtitle = "본인의 사용패턴을 생각해 시간을 선택해주세요",
                    steps = DAILY_TIME_STEPS,
                    feedbackMessages = listOf(
                        "초보자가 시작하기에 딱 좋은 시간이에요!",
                        "아주 적당한 선택이에요!",
                        "충분한 여유가 있어요!",
                        "적절한 시간이에요!",
                        "넉넉한 시간이에요!",
                        "충분한 시간이에요!",
                    ),
                    initialIndex = dailyLimitMinutes?.let { DAILY_TIME_STEPS.indexOf(it) }?.takeIf { it >= 0 } ?: 0,
                    onDismissRequest = { showDailyTimeSheet = false },
                    onPrimaryClick = { _, mins ->
                        dailyLimitMinutes = mins
                        showDailyTimeSheet = false
                        showDailyDaysSheet = true
                    },
                    primaryButtonText = "다음",
                )
            }
            if (showDailyDaysSheet) {
                AppLimitSetupDayBottomSheet(
                    title = "언제 반복할까요",
                    subtitle = "반복 설정 안 하면 오늘 하루만 적용돼요",
                    dayLabels = DAILY_DAY_LABELS,
                    initialSelected = dailySelectedDays,
                    initialRepeatEnabled = dailySelectedDays.isNotEmpty(),
                    onDismissRequest = { showDailyDaysSheet = false },
                    onPrimaryClick = { days ->
                        dailySelectedDays = days
                        showDailyDaysSheet = false
                        if (days.isNotEmpty()) {
                            showDailyDurationSheet = true
                        } else {
                            dailySelectedDuration = "오늘 하루만"
                        }
                    },
                    primaryButtonText = "다음",
                )
            }
            if (showDailyDurationSheet) {
                AddAppDailyDurationBottomSheet(
                    options = DAILY_DURATION_OPTIONS_FOR_REPEAT,
                    initialIndex = dailySelectedDuration?.let { DAILY_DURATION_OPTIONS_FOR_REPEAT.indexOf(it) }?.takeIf { it >= 0 } ?: 0,
                    selectedDaysText = formatSelectedDays(dailySelectedDays, DAILY_DAY_LABELS),
                    limitMinutes = dailyLimitMinutes ?: "30분",
                    onDismissRequest = { showDailyDurationSheet = false },
                    onPrimaryClick = { duration ->
                        dailySelectedDuration = duration
                        showDailyDurationSheet = false
                    },
                    onSecondaryClick = { showDailyDurationSheet = false },
                )
            }
        }
        AddAppStep.AA_DAILY_02, AddAppStep.AA_DAILY_03 -> {
            LaunchedEffect(Unit) { step = AddAppStep.AA_DAILY_01 }
        }
        AddAppStep.AA_DAILY_04 ->         AddAppDailyLimitScreen04(
            limitMinutes = dailyLimitMinutes ?: "30분",
            selectedDaysText = if (dailySelectedDays.isNotEmpty()) formatSelectedDays(dailySelectedDays, DAILY_DAY_LABELS) else "오늘 하루만",
            duration = dailySelectedDuration ?: "",
            mainTitle = "선택하신 내용을 확인해주세요",
            showSubtitle = false,
            primaryButtonText = "다음",
            onConfirmClick = { step = AddAppStep.AA_DAILY_05 },
            onBackClick = { step = AddAppStep.AA_DAILY_01 },
        )
        AddAppStep.AA_DAILY_05 -> {
            val repo = remember { AppRestrictionRepository(context) }
            LaunchedEffect(Unit) {
                val mins = parseLimitMinutes(dailyLimitMinutes ?: "30분")
                selectedAppsForDaily.filter { it.packageName.isNotBlank() }.forEach { app ->
                    repo.save(com.cole.app.model.AppRestriction(
                        packageName = app.packageName,
                        appName = app.appName,
                        limitMinutes = mins,
                        blockUntilMs = 0L,
                    ))
                }
                val map = repo.toRestrictionMap()
                if (map.isNotEmpty()) {
                    AppMonitorService.stop(context)
                    AppMonitorService.start(context, map)
                }
            }
            AddAppDailyLimitScreen05(
                appName = selectedAppNames.joinToString(", ").ifEmpty { "앱" },
                limitMinutes = dailyLimitMinutes ?: "30분",
                selectedDaysText = if (dailySelectedDays.isNotEmpty()) formatSelectedDays(dailySelectedDays, DAILY_DAY_LABELS) else "오늘 하루만",
                duration = dailySelectedDuration ?: "",
                onCompleteClick = onComplete,
                onAddAnotherClick = { step = AddAppStep.AA_01 },
                onBackClick = { step = AddAppStep.AA_DAILY_04 },
            )
        }
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
                        showTimeLimitSheet = true
                    },
                    onSelectCompleteWithPackages = { selectedAppsForTime = it },
                    additionalRestrictedPackages = selectedAppsForDaily.map { it.packageName }.toSet(),
                )
            }
            if (showTimeLimitSheet) {
                AppLimitSetupTimeBottomSheet(
                    title = "제한 시간을 지정해주세요",
                    subtitle = "60분 이하는 일시정지 기능이 제공되지 않아요",
                    steps = timeSteps,
                    feedbackMessages = listOf(
                        "초보자가 시작하기에 딱 좋은 시간이에요!",
                        "아주 적당한 선택이에요!",
                        "적절한 시간이에요!",
                        "넉넉한 시간이에요!",
                        "충분한 시간이에요!",
                        "여유로운 시간이에요!",
                    ),
                    initialIndex = selectedTimeLimit?.let { timeSteps.indexOf(it) }?.takeIf { it >= 0 } ?: 0,
                    onDismissRequest = { showTimeLimitSheet = false },
                    onPrimaryClick = { _, time ->
                        selectedTimeLimit = time
                        showTimeLimitSheet = false
                    },
                    primaryButtonText = "완료",
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
        AddAppStep.AA_03_01 -> {
            val repo = remember { AppRestrictionRepository(context) }
            AddAppCommonCompleteScreen(
                headerTitle = "앱 제한",
                summaryContent = {
                    AddAppTimeSummaryBox(
                        selectedAppName = selectedAppNames.joinToString(", ").ifEmpty { "앱" },
                        selectedTimeLimit = selectedTimeLimit ?: "",
                    )
                },
                primaryButtonText = "홈으로 가기",
                secondaryButtonText = "다른 앱 추가하기",
                onPrimaryClick = {
    if (previousStepBeforeConfirm == AddAppStep.AA_02A_TIME_05) {
        val mins = parseLimitMinutes(selectedTimeLimit ?: "60분")
        val blockUntilMs = System.currentTimeMillis() + mins * 60L * 1000L
        selectedAppsForTime.filter { it.packageName.isNotBlank() }.forEach { app ->
            repo.save(com.cole.app.model.AppRestriction(
                packageName = app.packageName,
                appName = app.appName,
                limitMinutes = mins,
                blockUntilMs = blockUntilMs,
            ))
        }
        val map = repo.toRestrictionMap()
        if (map.isNotEmpty()) {
            AppMonitorService.stop(context)
            AppMonitorService.start(context, map)
        }
    }
    onComplete()
},
                onSecondaryClick = { step = AddAppStep.AA_01 },
                onBackClick = { step = previousStepBeforeConfirm },
            )
        }
    }
}

private fun parseLimitMinutes(limitStr: String): Int {
    return limitStr.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 60
}
