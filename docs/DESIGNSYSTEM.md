# Cole 디자인 시스템

## Select / List Switch Button

**Figma:** 352-3425 (Off), 352-3527 (On)

### ColeListSwitch
- **사양:** 52×30dp pill 형태
- **Off:** 회색 트랙(Grey250 `#E8E8E8`), 흰색 원형 thumb 좌측
- **On:** 보라색 트랙(Primary300 `#6C54DD`), 흰색 원형 thumb 우측, **체크 아이콘** 포함
- **체크 아이콘:** `ic_switch_check.xml` (16×16dp, Primary300)

### 사용처
- `ColeSelectionCard` – 선택 카드 리스트
- `ColeSelectionCardTitleOnly` – 제목만 있는 선택 카드

### API
```kotlin
ColeListSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
)
```

### ColeRadioButton
- **용도:** 원형 라디오 (SelectionCard 외 단일 선택 UI)
- **사양:** 28×28dp, 선택 시 내부 10dp 점
