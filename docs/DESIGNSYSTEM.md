# Aptox 디자인 시스템

---

## 통계 화면 컴포넌트 (Statistics Screen)

> Figma 디자인 파일 기준. 통계 화면 전체 리뉴얼용 에셋.

### 1. 날짜 네비게이션 좌/우 아이콘

**Figma:** 948-3576 (상위), 948-3627 (날짜 범위 + 좌우 아이콘)

| 에셋 | 용도 | 사양 |
|------|------|------|
| `ic_nav_left_enable` | 이전 주 이동 (활성) | 22×22dp, 검정 삼각형 #000000 |
| `ic_nav_left_disable` | 이전 주 이동 (비활성) | 22×22dp, 회색 삼각형 #BDBDBD |
| `ic_nav_right_enable` | 다음 주 이동 (활성) | 22×22dp, 검정 삼각형 #000000 |
| `ic_nav_right_disable` | 다음 주 이동 (비활성) | 22×22dp, 회색 삼각형 #BDBDBD |

**Composable API**
```kotlin
IcoNavLeft(enabled: Boolean = true, modifier, tint, size = 22.dp)
IcoNavRight(enabled: Boolean = true, modifier, tint, size = 22.dp)
```

### 2. 날짜 범위 선택 Row (948-3627)

- 좌측: `IcoNavLeft` (이전)
- 중앙: 날짜 텍스트 "12.22 ~ 12.29" — Caption2 (Bold, 13sp, #000000)
- 우측: `IcoNavRight` (다음)
- 간격: 4dp
- Enable 시 검정 아이콘·텍스트, Disable 시 회색 아이콘·텍스트

### 3. 카테고리 태그 (948-3543)

| 라벨 | 배경색 | 텍스트색 |
|------|--------|----------|
| SNS | #FFC34B | #553C0A |
| OTT | #EBCFFF | #55366B |
| 게임 | #818CFF | #FFFFFF |
| 쇼핑 | #A2A2A2 | #FFFFFF |
| 웹툰 | #88C9FF | #FFFFFF |
| 주식/코인 | #3D9E5D | #FFFFFF |

- 패딩: 4dp horizontal, 3dp vertical
- 모서리: 3dp
- 폰트: SUIT Variable ExtraBold, 9sp

### 4. 리스트 / 앱 상태 데이터 뷰 (948-3650)

- 앱 아이콘 56×56dp
- 카테고리 태그 + 상태 태그(위험 등)
- 앱 이름: BodyMedium 15sp, #1F1F1F
- 사용 시간: BodyBold 15sp, 우측 정렬
- 아이콘–정보 간격 12dp

### 5. 스택 바 차트 + 범례 (948-3576)

- 가로 스택 바: 296×48dp, 6dp 라운드
- 카테고리별 색상: OTT(#EBCFFF), SNS(#FFC34B), 게임(#818CFF), 쇼핑(#A2A2A2), 웹툰(#88C9FF), 주식/코인(#3D9E5D)
- 범례: 6×6dp 컬러 점 + Caption2 텍스트 + 퍼센트

### 6. 제한 방식 필터 칩 (948-3689)

- 선택됨: Primary50 배경, Primary300 텍스트, padding 14h×6v
- 비선택: 배경 없음, Grey500 텍스트
- 모서리: 999dp (pill)
- 폰트: 선택 Bold 12sp / 비선택 Medium 12sp

### 7. 요일별 막대 차트 — 단일 (949-3550)

- 막대: 26×n dp, 2dp 라운드, Grey350 / Primary300
- X축: 월~일, Caption1 Medium 12sp, #4D4D4D
- 그리드 배경 포함

### 8. 요일별 막대 차트 — 그룹 (948-3696)

- 각 요일당 2개 막대 (11×n dp, 4dp 간격)
- 회색 + 보라색 쌍

---

## Select / List Switch Button

**Figma:** 352-3425 (Off), 352-3527 (On)

### AptoxListSwitch
- **사양:** 52×30dp pill 형태
- **Off:** 회색 트랙(Grey250 `#E8E8E8`), 흰색 원형 thumb 좌측
- **On:** 보라색 트랙(Primary300 `#6C54DD`), 흰색 원형 thumb 우측, **체크 아이콘** 포함
- **체크 아이콘:** `ic_switch_check.xml` (16×16dp, Primary300)

### 사용처
- `AptoxSelectionCard` – 선택 카드 리스트
- `AptoxSelectionCardTitleOnly` – 제목만 있는 선택 카드

### API
```kotlin
AptoxListSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
)
```

### AptoxRadioButton
- **용도:** 원형 라디오 (SelectionCard 외 단일 선택 UI)
- **사양:** 28×28dp, 선택 시 내부 10dp 점

---

## SpeechBubble (말풍선)

**Figma:** 1084-4659

- **구성:** 좌측 꼬리(13×10dp) + 본문 박스
- **본문 박스**
  - 배경: Primary200 `#E9EBF8`
  - 패딩: 5dp horizontal, 3dp vertical
  - 모서리: 2dp
- **텍스트:** Caption1 (12sp), Primary300 `#6C54DD`
- **꼬리:** 좌측 또는 우측 방향 삼각형

### API
```kotlin
SpeechBubble(
    text: String,
    modifier: Modifier = Modifier,
    tailDirection: TailDirection = TailDirection.Start,
)

enum class TailDirection { Start, End }
```

---

## 하루 사용량 지정 — 시간 선택 칩 Row (304-1760)

**Figma:** 304-1760 (일일사용량제한_사용량지정 바텀시트), node 1164:4824

일일 사용량 제한 바텀시트에서 사용 시간을 선택하는 UI. 기존 슬라이더(AptoxStepBar) 대신 가로 칩 Row로 변경.

### 레이아웃 스펙

| 속성 | 값 | 비고 |
|------|-----|------|
| display | flex (Row) | 가로 배치 |
| width | 328px (fillMaxWidth) | 바텀시트 콘텐츠 영역 기준 (360 - 32 padding) |
| align-items | center | 세로 중앙 정렬 |
| gap | 12dp | 칩 간 간격 |

### 개별 칩(옵션) 스펙

| 속성 | 선택됨 | 비선택 |
|------|--------|--------|
| 크기 | 80×60dp | 80×60dp |
| 모서리 | 12dp | 12dp |
| 배경 | Primary200 (`#E9EBF8`) | White900 또는 Grey150 |
| 테두리 | 없음 | 1dp Grey250 (`#E8E8E8`) 선택사항 |
| 텍스트 | HeadingH3, Primary300 (`#6C54DD`) | HeadingH3, TextTertiary (`#333333`) |
| 텍스트 정렬 | 중앙 | 중앙 |

### Kotlin/Compose 매핑

```kotlin
// Row 레이아웃
Row(
    modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 328.dp),  // 필요 시
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
) { /* 칩들 */ }

// 개별 칩
Box(
    modifier = Modifier
        .size(80.dp, 60.dp)
        .clip(RoundedCornerShape(12.dp))
        .then(
            if (selected) Modifier.background(AppColors.Primary200)
            else Modifier
                .background(AppColors.White900)
                .border(1.dp, AppColors.Grey250, RoundedCornerShape(12.dp))
        )
        .clickable { onClick() },
    contentAlignment = Alignment.Center,
) {
    Text(
        text = label,  // e.g. "30분", "60분", "90분"
        style = AppTypography.HeadingH3.copy(
            color = if (selected) AppColors.TextHighlight else AppColors.TextTertiary,
            textAlign = TextAlign.Center,
        ),
    )
}
```

### 옵션 예시 (일일 사용량)

- `30분`, `50분`, `60분`, `90분`, `120분` (Figma 기준)
- 또는 `listOf("30분", "60분", "90분", "120분", "150분", "180분")` (기존 AptoxStepBar steps)

### 사용처

- `AddAppDailyLimitScreen01` — "하루 사용량을 지정해주세요" 바텀시트
- `AppLimitSetupTimeBottomSheet` → 일일 전용으로 변경 시
