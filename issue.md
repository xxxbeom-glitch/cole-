# 🐛 이슈 요약

앱바(App Bar)가 고정되지 않고 스크롤 시 함께 움직이거나 위치가 어긋나는 문제

## 문제 설명

챌린지, 통계, 설정 탭에서 상단 앱바(헤더: 타이틀 + 알림 아이콘)가 화면 상단에 고정되어 있지 않다. 스크롤할 때 앱바가 콘텐츠와 함께 움직이거나, 예상된 위치에 고정되지 않고 떠다니는 것처럼 보인다.

## 발생 상황

- 언제: 챌린지/통계/설정 화면에서 콘텐츠 스크롤 시 또는 화면 진입 시
- 어디서: MainFlowHost 내 홈/챌린지/통계/설정 탭 화면, ColeHeaderTitleWithNotification 사용 구간
- 증상: 앱바가 고정되지 않음. 스크롤 시 헤더가 콘텐츠와 함께 스크롤되거나, 헤더가 제 자리를 찾지 못하고 배치됨

## 에러 메시지

```
(에러 로그 없음 - UI 레이아웃 이슈)
```

## 시도한 방법

- [x] ColeHeaderTitleWithNotification의 align(Alignment.BottomCenter), padding(bottom) 제거하여 세로 중앙 정렬로 수정 (이전 세션)
- [ ] 헤더가 스크롤 영역 밖에 고정되도록 레이아웃 구조 검토 필요

## 관련 파일

- `app/src/main/java/com/cole/app/StubScreens.kt` - MainFlowHost 레이아웃, 헤더/탭 콘텐츠 Column 구조, nestedScroll
- `app/src/main/java/com/cole/app/NavigationComponents.kt` - ColeHeaderTitleWithNotification, ColeHeaderHome, ColeHeaderSub
- `app/src/main/java/com/cole/app/ChallengeScreen.kt` - 챌린지 화면 (verticalScroll)
- `app/src/main/java/com/cole/app/StatisticsScreen.kt` - 통계 화면 (verticalScroll)
- `app/src/main/java/com/cole/app/MyPageScreen.kt` - 설정 메인 화면
- `app/src/main/java/com/cole/app/Theme.kt` - AppColors, AppTypography (헤더 스타일 참조)

## 예상 원인

1. **헤더가 스크롤 영역 내부에 포함됨**: Column 구조에서 헤더와 탭 콘텐츠가 같은 스크롤 컨텍스트에 있어, nestedScroll 또는 자식의 verticalScroll이 헤더까지 영향을 줄 수 있음
2. **weight(1f) 사용으로 인한 레이아웃 제약**: 탭 콘텐츠에 weight(1f)를 주면서 헤더가 압축되거나 스크롤 뷰에 묶일 수 있음
3. **nestedScrollConnection 전파**: scrollToHideConnection이 하위 verticalScroll과 연동될 때 전체 Column이 스크롤되도록 해석될 가능성

## 참고 사항

- 환경: Android / Kotlin + Jetpack Compose
- 바텀바는 스크롤 시 숨김/표시 동작이 정상 동작함 (graphicsLayer translationY)
- 홈 탭(navIndex 0)은 ColeHeaderHome 사용, 챌린지/통계/설정은 ColeHeaderTitleWithNotification 사용

---
*생성 시각: 2026년 03월 08일 22:31*
*프로젝트: cole (디지털 디톡스 앱)*
*환경: Android / Kotlin + Jetpack Compose*
