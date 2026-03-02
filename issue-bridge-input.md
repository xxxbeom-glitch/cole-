# 🐛 이슈 요약

권한 안내 화면(PermissionScreen)이 Figma 디자인(861-4229)과 다르게 구현되어 있음

## 문제 설명

앱 접근 권한 안내 화면이 Figma 원본 디자인과 불일치하여 제대로 구현되지 않은 상태였음.
특히 하단 버튼 구성이 디자인과 완전히 달랐음.

## 발생 상황

- 언제: Figma 861-4229 기반으로 PermissionScreen 구현 후
- 어디서: `PermissionScreen.kt` — 앱 접근 권한 안내 화면
- 증상: Figma에는 버튼 2개인데 실제 구현은 버튼 1개만 표시됨. 안내 문구도 디자인과 상이함.

## 에러 메시지

```
(UI/디자인 불일치로 인한 이슈 — 런타임 에러 없음)
```

## 시도한 방법

- [x] Figma MCP로 get_design_context 호출하여 원본 디자인 스펙 확인
- [x] ColeTwoLineButton으로 교체 및 콜백 시그니처 수정 (onPrimaryClick, onGhostClick)
- [x] DebugMenuScreen 호출부 수정

## 관련 파일

- `app/src/main/java/com/cole/app/PermissionScreen.kt` - 권한 안내 화면 Composable
- `app/src/main/java/com/cole/app/DebugMenuScreen.kt` - PermissionScreen 호출부
- `app/src/main/java/com/cole/app/ButtonComponents.kt` - ColeTwoLineButton 사용
- `app/src/main/java/com/cole/app/AppColors.kt` - 해당 화면에서 사용하는 색상 스타일
- `app/src/main/java/com/cole/app/AppTypography.kt` - 해당 화면에서 사용하는 타이포그래피

## 예상 원인

Figma 디자인 스펙(861-4229) 확인 없이 구현 시 Button/Large Two line 컴포넌트(계속 진행 + 돌아가기)를 단일 Primary 버튼(나중에 하기)으로 잘못 대체함. 선택 안내 문구의 "①"도 Figma에는 없으나 구현에 포함됨.

## 참고 사항

- Figma URL: https://www.figma.com/design/jTxTaPrc0c2cyGeSRknNEN/cole?node-id=861-4229
- 디버그 메뉴 → 권한에서 화면 확인 가능
- 해결: ColeTwoLineButton 적용, 콜백 onPrimaryClick/onGhostClick 분리, "①" 제거

---
*생성 시각: [타임스탬프]*
*프로젝트: cole (디지털 디톡스 앱)*
*환경: Android / Kotlin + Jetpack Compose*
