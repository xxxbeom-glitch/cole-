# 🐛 이슈 요약

앱 리스트를 불러오지 못하는 문제

## 문제 설명

사용 제한 앱 추가, 통계 앱 사용량, AI 카테고리 분류 등에서 설치된 앱 목록(앱 리스트)이 비어 있거나 로드되지 않는 현상이 발생합니다.

## 발생 상황

- **언제**: 앱 선택 바텀시트 열기, 통계 화면 진입, 앱 카테고리 분류 테스트 시
- **어디서**: 
  - 사용 제한 앱 추가 → 앱 선택 화면 (AddAppScreenAppSelect)
  - 통계 → 기간별 사용량 / 월간·연간 탭 앱 사용량 카드
  - 디버그 메뉴 → AI 앱 카테고리 분류 화면
- **증상**: 앱 목록이 비어 있음, 로딩 후에도 빈 리스트, "제한 중인 앱이 없어요" 등 빈 상태 UI만 표시

## 에러 메시지

```
(명시적 에러 로그 없을 수 있음 - emptyList() 반환으로 빈 UI만 표시)
```

## 시도한 방법

- [ ] 권한 설정 확인 (PACKAGE_USAGE_STATS, QUERY_ALL_PACKAGES)
- [ ] 사용량 접근 권한 허용 여부 확인
- [ ] 앱 재설치 후 재시도
- [ ] 기기별 차이 확인 (OEM, Android 버전)

## 관련 파일

- `app/src/main/java/com/cole/app/AddAppScreens.kt` - 앱 선택 바텀시트, queryIntentActivities로 런처 앱 목록 조회
- `app/src/main/java/com/cole/app/StatisticsData.kt` - loadAppUsage, getUserInstalledPackages, hasUsageAccess 의존
- `app/src/main/java/com/cole/app/usage/UsageStatsLocalRepository.kt` - getAppUsageForRangeBlocking, DB 기반 앱별 사용량
- `app/src/main/java/com/cole/app/AiAppCategoryClassificationScreen.kt` - getInstalledApplications, queryIntentActivities
- `app/src/main/AndroidManifest.xml` - queries, QUERY_ALL_PACKAGES 권한 (Android 11+ 패키지 가시성)
- `app/src/main/java/com/cole/app/usage/UsageStatsSyncWorker.kt` - 일별 사용량 DB 동기화 (미동기화 시 통계 앱 목록 비어 있음)

## 예상 원인

1. **Android 11+ 패키지 가시성**: `queryIntentActivities(MAIN/LAUNCHER)`가 일부 기기에서 빈 목록 반환. QUERY_ALL_PACKAGES는 Play Store 배포 시 제한될 수 있음.
2. **사용량 접근 권한 미허용**: `hasUsageAccess(context) == false`이면 loadAppUsage가 항상 emptyList() 반환.
3. **DB 미동기화**: UsageStatsSyncWorker가 실행되지 않거나 초기 동기화 전이라 UsageStatsLocalRepository에 데이터가 없을 수 있음.
4. **queryIntentActivities vs getInstalledApplications**: 앱 선택 화면은 LAUNCHER intent 기반이라 시스템 앱·숨김 앱이 제외될 수 있음.

## 참고 사항

- 기술 스택: Android Kotlin, Jetpack Compose
- 앱 선택: PackageManager.queryIntentActivities(ACTION_MAIN, CATEGORY_LAUNCHER)
- 통계 앱 목록: UsageStatsLocalRepository + UsageStatsManager.queryEvents (오늘 분)
- Android 11(API 30) 이상에서 패키지 가시성 정책이 엄격해짐

---
*생성 시각: 2026년 03월 11일 21:50*
*프로젝트: cole (디지털 디톡스 앱)*
*환경: Android / Kotlin + Jetpack Compose*
