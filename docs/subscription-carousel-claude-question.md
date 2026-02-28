# 구독 플랜 카드 캐러셀 문제 - Claude 질문용

## 1. 문제점

Android Jetpack Compose 앱에서 **구독 플랜 카드 캐러셀**(HorizontalPager + SubscriptionPlanCard)에 다음 문제가 있다.

### 1-1. 뱃지 관련
- **뱃지 잘림**: "7일 무료체험 + 약 40% 할인" 뱃지를 `offset(y = -14.dp)`로 카드 상단 테두리 위로 올렸으나, 상위 레이아웃에 여유 공간이 없어 뱃지 상단이 잘린다.
- **뱃지·텍스트 겹침**: 카드 내부 상단 패딩이 부족해 뱃지와 "연간 구독" 텍스트가 겹친다.

### 1-2. 월간 구독 카드 노출
- **우측 카드 찌그러짐/잘림**: HorizontalPager가 화면 전체 너비를 차지해, 우측에 살짝 보여야 할 "월간 구독" 카드가 비정상적으로 압축되거나 잘려 보인다. Figma 원본처럼 Peeking(옆 카드가 살짝 보이는) 형태가 아니다.

### 1-3. 텍스트 균형
- **가격 행**: "₩46,800 ₩28,000/연간"이 카드 안에서 가로로 답답하고 균형이 맞지 않다. 원가·할인가·단위 간 간격과 정렬이 부자연스럽다.

---

## 2. 해본 시도

| 시도 | 내용 | 결과 |
|------|------|------|
| 1 | `contentPadding = PaddingValues(start = 0.dp, end = 48~56.dp)` 적용 | 우측 카드 일부 노출되나, 여전히 찌그러짐/잘림 |
| 2 | `pageSpacing = 10~16.dp`, `beyondViewportPageCount = 0~1` 조정 | 큰 개선 없음 |
| 3 | Box 상단 `padding(top = 16.dp)`로 뱃지 공간 확보 | 일부 완화, 여전히 잘림 |
| 4 | 카드 Row 내부 `padding(top = 32.dp)`로 뱃지·텍스트 겹침 방지 | 일부 완화 |
| 5 | 뱃지 `offset(y = -14.dp ~ -16.dp)` 조정 | 위치는 조정되나 상단 잘림 지속 |
| 6 | 캐러셀 Box `height(200~220.dp)` 조정 | 높이만 바뀌고 구조적 해결은 아님 |
| 7 | 가격 행 `Arrangement.spacedBy(12.dp)`, `verticalAlignment = Alignment.Bottom` 등 레이아웃 수정 | 텍스트 간격은 나아졌으나 전체 균형은 여전히 부족 |
| 8 | HorizontalPager를 `verticalScroll` Column 내부에 배치 | 제스처/스크롤 동작은 되나 Peeking·카드 비율 문제 지속 |

---

## 3. 현재 구현 구조 (요약)

- **부모**: `Column` > `verticalScroll` > `Box(height = 200.dp)` > `HorizontalPager`
- **HorizontalPager**: `contentPadding(end = 56.dp)`, `pageSpacing = 12.dp`, `beyondViewportPageCount = 1`
- **SubscriptionPlanCard**: 외부 Box `padding(top = 16.dp)`, 내부 Row `padding(top = 32.dp, bottom = 20.dp, horizontal = 16.dp)`, 뱃지 `offset(-14.dp)`
- **디자인 기준**: Figma MA-01 구독 유도 화면 (연간 카드가 화면 대부분 차지, 월간 카드가 우측에 살짝 보이는 Peeking 캐러셀)

---

## 4. 같이 첨부할 파일

아래 파일들을 첨부하여 질문한다:

```
1. app/src/main/java/com/cole/app/SubscriptionGuideScreen.kt
   - 구독 화면 전체 및 SubscriptionPlanCard 구현

2. app/src/main/java/com/cole/app/AppTypography.kt
   - 폰트 스타일 참고용 (SuitVariable, AppTypography)

3. app/src/main/java/com/cole/app/AppColors.kt
   - 색상 정의 (Primary300, TextInvert 등)

4. (선택) Figma MA-01 구독 화면 스크린샷 또는 디자인 링크
```

---

## 5. Claude에게 요청할 핵심 질문

1. **HorizontalPager에서 Peeking 캐러셀**: 한 카드가 화면 대부분을 차지하고, 다음 카드가 우측에 자연스럽게 살짝 보이도록 하려면 contentPadding·pageSpacing·카드 modifier를 어떻게 설정해야 하는가?  
   `verticalScroll` 안에 HorizontalPager를 두는 구조가 문제인가?

2. **뱃지 잘림/겹침**: 뱃지가 카드 상단 테두리 위로 돌출되면서 잘리지 않고, 카드 내부 텍스트와 겹치지 않게 하려면 상위 레이아웃과 카드 내부 패딩을 어떻게 구성해야 하는가?

3. **카드 내부 텍스트 균형**: 원가(취소선)·할인가·단위(`/연간`)가 Figma와 비슷한 비율·간격으로 보이도록 하는 레이아웃 패턴 제안
