# Aptox (앱톡스) — 코드베이스 스펙 (SPEC)

> 본 문서는 저장소 내 실제 코드·설정 파일을 기준으로 작성했습니다. 저장소에 없는 항목은 **미확인**으로 표기합니다.

---

## 1. 프로젝트 개요

| 항목 | 값 |
|------|-----|
| 앱 이름 | 플레이버별 `resValue`: **Aptox Dev** (`dev`), 기본 `@string/app_name` (`externalTest` 등) |
| 패키지 / namespace | `com.aptox.app` (`app/build.gradle.kts`: `namespace`, `applicationId`) |
| 버전 | `versionCode` **23**, `versionName` **"1.0"** (`app/build.gradle.kts` `defaultConfig`) |

### 빌드 플레이버 (`flavorDimensions = "distribution"`)

| Flavor | 요약 |
|--------|------|
| **dev** | `SHOW_DEBUG_MENU = true`, `EXCLUDE_3MIN_OPTION = false`, 앱 표시명 "Aptox Dev" |
| **externalTest** | `SHOW_DEBUG_MENU = false`, `EXCLUDE_3MIN_OPTION = true`, `applicationId` 동일 `com.aptox.app` |

### 기술 스택 (요약)

- **언어**: Kotlin (JVM 11), 일부 XML 리소스
- **UI**: Jetpack Compose (BOM `2024.09.00`), Material 3, `core-splashscreen`
- **아키텍처**: 단일 `:app` 모듈, Activity 단일(`MainActivity`) + Compose 내비게이션 상태(`SignUpStep`, `MainFlowHost`의 `navIndex` / `settingsDetail`)
- **비동기**: Kotlin Coroutines, `kotlinx-coroutines-play-services`
- **로컬 저장**: DataStore Preferences, SharedPreferences, SQLite 직접(`UsageStatsDatabase` — Room 엔티티는 앱 코드에 없음). Gradle에 `room`/`work-runtime` 의존성은 있으나 WorkManager 내부용; 앱 도메인 DB는 `aptox_usage.db` SQLiteOpenHelper.
- **원격**: Firebase (BOM 34.0.0) — Auth, Firestore, Functions, Storage, Analytics, Crashlytics
- **결제**: Google Play Billing KTX 7.1.1
- **기타**: Credential Manager + Google ID, WorkManager, AndroidX Startup, Media(알림 스타일)

---

## 2. 아키텍처

### 전체 구조

- **진입점**: `MainActivity` → `SignUpFlowHost` 또는 (dev+DEBUG) `DebugFlowHost` → `AptoxRootContent`
- **온보딩/로그인**: `SignUpStep` enum으로 화면 전환 (`MainActivity.kt`)
- **메인 앱**: `MainFlowHost` — 하단 탭(홈/챌린지/통계/설정) + 설정 서브화면(`SettingsDetail` sealed class, `StubScreens.kt`)
- **백그라운드**: `AppMonitorService`(포그라운드 사용량 모니터링), `AptoxAccessibilityService`, 각종 `BroadcastReceiver`, WorkManager(`UsageStatsSyncWorker` 등)
- **오버레이/차단 UI**: `BlockDialogActivity`(투명 테마)

### 주요 클래스·파일 역할 (요약)

| 영역 | 파일/클래스 | 역할 |
|------|-------------|------|
| 인증 | `AuthRepository.kt` | 이메일/폰 가입·로그인(Functions 연동), 구글(Credential Manager), Firestore `users` 동기화, `getCurrentUserInfo`, `signOut`, `reauthenticateWithGoogle` |
| 탈퇴 | `AccountWithdrawalHelper.kt` | Callable `deleteAccount` → Firestore `users/{uid}` 삭제 → `terminate`/`clearPersistence` → 로그아웃·로컬 wipe |
| 제한·저장소 | `AppRestrictionRepository`, `RestrictionSettingsStore` 등 | 앱별 제한(일일/시간지정 등) 로컬 저장 |
| 사용량 통계 | `StatisticsData.kt`, `UsageStatsLocalRepository.kt`, `UsageStatsDatabase.kt` | UsageStats 기반 집계, 로컬 SQLite `daily_usage` / `category_daily` / `time_segment_daily` |
| Firestore 동기화 | `DailyUsageFirestoreRepository.kt`, `UsageStatsSyncWorker.kt`, `UsageStatsDaySyncUtils.kt` | 로그인 사용자 일별 사용량 등 업로드 |
| 통계 백업 | `StatisticsBackupFirestoreRepository.kt` | `categoryStats`, `timeSegments` 업로드/복원 |
| 구독 | `SubscriptionManager.kt`, `SubscriptionFeature`, `PremiumStatusRepository.kt`, `SubscriptionBillingController.kt` | 플래그·DataStore·BillingClient |
| AI 연동 | `ClaudeRepository.kt` | Functions `callClaude`, `classifyApps` 호출 |
| 알림 | `NotificationRepository.kt`, `NotificationHistoryScreen.kt` | Firestore `users/.../notifications` 등 |
| 뱃지 | `BadgeRepository.kt`, `BadgeAutoGrant.kt`, `ChallengeScreen.kt` | 챌린지/뱃지 |

### 데이터 흐름 (로컬 ↔ 원격)

1. **사용량**: 시스템 `UsageStats` → 집계 로직 → **로컬 SQLite** (`AppDatabaseProvider` / `UsageStatsDatabase`).
2. **로그인 시**: `UsageStatsSyncWorker` 등으로 **Firestore `users/{uid}/dailyUsage`** 등과 동기화(관련 코드: `DailyUsageFirestoreRepository`, `UsageStatsSyncWorker`).
3. **통계 확장**: 카테고리·시간대 세그먼트 → **Firestore** `users/{uid}/categoryStats/{date}`, `users/{uid}/timeSegments/{package}/days/{date}` (`StatisticsBackupFirestoreRepository`).
4. **제한 이벤트 로그**: `AppLimitLogRepository` — 로그인 시 Firestore, 비로그인 시 로컬(`AppLimitLogLocalPreferences` 주석 기준).

---

## 3. 화면 목록 (Composable / Activity 기준)

**Activity**

| 이름 | 역할 | 진입 | 로그인 | 프리미엄 |
|------|------|------|--------|----------|
| `MainActivity` | 루트 Compose, 스플래시 테마 | 런처 | (플로우 단계에 따름) | 탭/기능별 |
| `BlockDialogActivity` | 차단 오버레이 다이얼로그 | 서비스/인텐트 | 불필요 | 불필요 |

**온보딩·인증 플로우 (`SignUpFlowHost` / `MainActivity`)**

| Composable | 역할 | 진입 | 로그인 | 프리미엄 |
|------------|------|------|--------|----------|
| `SplashScreen` | 초기 스플래시 후 다음 단계 결정 | 앱 시작 | - | - |
| `PermissionScreen` | 필수 권한 안내 | 스플래시 후 미허용 시 | - | - |
| `AppIntroOnboardingScreen` | 앱 소개(온보딩 1회 흐름) | 권한 화면 후, 온보딩 미완료 | - | - |
| `SplashLoginScreen` | 구글 로그인(스플래시 스타일) | `SignUpStep.LOGIN` | - | - |
| `SelfTestScreenVer2` | 이름 입력 + 자가테스트 Ver2 | 온보딩 | - | - |
| `SelfTestLoadingScreen` | 분석 로딩 | Ver2 완료 후 | - | - |
| `DiagnosisResultScreen` | 사용 패턴 진단 결과 | 로딩 후 | - | - |
| `OnboardingStartScreen` | 온보딩 마무리 → 메인 진입 | 진단 후 | - | - |
| `SelfTestScreen` | (구) 자가테스트 — enum에 남아 있으나 메인 백은 Ver2 위주 | `SELFTEST` 스텝 | - | - |
| `SelfTestResultScreenST10` | ST10 결과 화면 | `SELFTEST_RESULT` 스텝 | - | - |
| `AddAppFlowHost` / `TimeSpecifiedFlowHost` | 앱 추가·시간지정 플로우 | 메인에서 진입 | - | 제한 개수 등(`SubscriptionFeature`) |

**비활성화(코드상 리다이렉트만 존재)**

- 이메일 회원가입 단계: `SignUpEmailScreen` 등 `SignUpScreens.kt` — `MainActivity`에서 해당 스텝은 `LOGIN`으로 보냄.
- 비밀번호 재설정: `PasswordResetScreens.kt` — 동일하게 비활성화 분기.

**메인 탭 (`MainFlowHost`, `StubScreens.kt`)**

| Composable | 역할 | 진입 | 로그인 | 프리미엄 |
|------------|------|------|--------|----------|
| `MainScreenMA01` | 홈(제한 목록, 인사, 통계 카드 등) | 탭 홈 | 불필요 | 일부(제한 개수·통계 Pro 등) |
| `MainScreenMA02` | 빈 홈 레이아웃(코드 존재; `MainFlowHost`에서는 `MainScreenMA01` 사용) | (직접 연결 없음) | - | - |
| `ChallengeScreen` | 챌린지/뱃지 | 탭 챌린지 | **필요**(비로그인 시 탭 전환/토스트 처리) | - |
| `StatisticsScreen` | 통계 | 탭 통계 | 불필요 | Pro 통계(`SubscriptionFeature.canAccessProStats`) |
| `MyPageScreen` | 설정 허브(리스트) | 탭 설정, `settingsDetail == null` | 불필요 | 구독 배너 등 |

**설정 서브화면 (`settingsDetail`)**

| Composable | 역할 | 진입 | 로그인 | 프리미엄 |
|------------|------|------|--------|----------|
| `AppCategoryEditScreen` | 앱 카테고리 수정 | 설정 | - | - |
| `AccountManageScreen` | 계정/구글 로그인/탈퇴 진입 | 설정 | (연동 시) | - |
| `WithdrawConfirmScreen` | 탈퇴 사유 + 탈퇴 실행 | 계정관리 → 탈퇴 | 권장(탈퇴 Functions) | - |
| `AppRestrictionHistoryScreen` / `AppRestrictionHistoryDetailScreen` | 제한 기록 | 설정 | Firestore 연동 시 uid 사용 | - |
| `SubscriptionManageScreen` | 구독 관리 | 설정 | - | 스토어 구독 상태 표시 |
| `NotificationSettingsScreen` | 알림 설정 | 설정 | - | - |
| `PermissionSettingsScreen` | 권한 설정 | 설정 | - | - |
| `BugReportScreen` | 버그 신고 | 설정 | Storage 업로드 시 Auth | - |
| `AppInfoScreen` / `OpenSourceScreen` | 앱 정보·오픈소스 | 설정 하위 | - | - |

**앱 추가·시간지정 (일부)**

- `AddAppScreens.kt`: `AddAppScreenAA01`, `AddAppScreenAppSelect`, 일일한도/시간 스케줄/완료 등 다단계.
- `AddAppTimeSpecifiedScreens.kt`: `TimeSpecifiedScreen01`, `TimeSpecifiedScreen03` 등.

**기타 UI**

- `SubscriptionBottomSheet` — 프리미엄 구독 바텀시트.
- `NotificationHistoryScreen` — 알림 목록(오버레이 등에서 사용).
- `AiAppCategoryClassificationScreen` — AI 카테고리 분류 UI.
- `SubscriptionGuideScreen` — 구독 안내.
- **디버그 전용** (`DebugMenuScreen.kt` 등, `SHOW_DEBUG_MENU && DEBUG`): `DebugFlowHost`, `DebugHomeLayoutPreviewScreen`, `UsageStatsTestScreen`, `AppMonitorTestScreen`, 각종 Test/Debug 화면.

---

## 4. 핵심 기능 목록 (구현 기준)

| 기능 | 설명 | 주요 파일 |
|------|------|-----------|
| 스마트폰 사용량 수집·통계 | UsageStats 기반 일/주/앱별 집계, 그래프 | `StatisticsData.kt`, `StatisticsScreen.kt`, `UsageStatsLocalRepository.kt` |
| 로컬 통계 DB | 일별 앱 사용량, 카테고리 일합, 12슬롯 시간대 | `UsageStatsDatabase.kt`, `DailyUsageEntity.kt`, `DailyCategoryStatEntity.kt`, `DailyTimeSegmentEntity.kt` |
| Firestore 일별 동기화 | 로그인 사용자 `dailyUsage` 등 | `DailyUsageFirestoreRepository.kt`, `UsageStatsSyncWorker.kt` |
| 통계 백업/복원 | `categoryStats`, `timeSegments` | `StatisticsBackupFirestoreRepository.kt` |
| 앱 사용 제한 | 일일 한도, 시간 지정 등, 오버레이 | `AppMonitorService`, `BlockDialogActivity`, `AppRestrictionRepository`, `AddAppScreens.kt`, `AddAppTimeSpecifiedScreens.kt` |
| 접근성 연동 | 제한 감지/조작 | `AptoxAccessibilityService` |
| 일시정지 제안 플로우 | 오버레이에서 일시정지 → 앱 내 바텀시트 | `AppLimitPauseBottomSheet.kt`, `MainActivity` pending pause 인텐트 |
| 로그인 | 구글(Credential Manager), 카카오/네이버(Functions 커스텀 토큰 경로는 `AuthRepository`/Functions에 존재) | `AuthRepository.kt`, `LoginScreen.kt` |
| 계정 탈퇴 | Functions `deleteAccount` + Firestore 정리 + 로컬 wipe | `AccountWithdrawalHelper.kt`, `functions/index.js` |
| 챌린지·뱃지 | Firestore 뱃지, 화면 `ChallengeScreen` | `BadgeRepository.kt`, `BadgeAutoGrant.kt`, `ChallengeScreen.kt` |
| 알림 | 푸시/로컬 알림, Firestore 알림 문서 | `NotificationRepository.kt`, 여러 `*AlarmReceiver`, `BriefDailyAlarmScheduler` 등 |
| 버그 신고 | 이미지 업로드 Storage + Callable `submitBugReport` | `BugReportScreen.kt`, `BugReportRepository.kt` |
| 위젯 | 일일 한도 위젯 | `AptoxDailyLimitWidgetProvider` |
| 크래시 로그(디버그성) | 파일 공유 등 | `CrashLogRepository.kt`, `CrashLogScreen.kt` |
| 앱 카테고리 AI 분류 | Functions `classifyApps` | `ClaudeRepository.kt`, `AddAppScreens.kt`, `AppDataPreloadRepository.kt`, `AppCategoryCacheRepository.kt` |

---

## 5. AI 엔진 구성

| 항목 | 내용 |
|------|------|
| **서비스** | 클라이언트는 **Firebase Callable Functions**만 호출. 서버(`functions/index.js`)에서 **Anthropic Claude** SDK(`@anthropic-ai/sdk`)로 `classifyApps`, `callClaude` 처리. |
| **API 키** | Functions Config / 환경변수: `functions.config().anthropic` 또는 `ANTHROPIC_API_KEY` (주석·코드 기준). 앱 APK에 Claude 키 직삽입 없음. |
| **기능별 사용** | **classifyApps**: 앱 패키지·이름 목록 → 카테고리 JSON (`AppDataPreloadRepository`, `AddAppScreens`, `AiAppCategoryClassificationScreen`). **callClaude**: 범용 프롬프트 → `reply` (`ClaudeRepository.chat`) — **앱 코드 내 다른 화면에서 `chat` 호출은 검색되지 않음**(Repository만 존재). |
| **프롬프트** | `classifyApps` / `callClaude`는 **서버 `index.js`에 문자열 프롬프트**로 구성. |
| **로컬 템플릿** | `DailyBriefGenerator.kt` 주석: **Claude 미사용**, 통계 상단 Daily Brief는 로컬 템플릿. |

---

## 6. Firebase 구성

| 항목 | 값 |
|------|-----|
| **프로젝트 ID** | `cole-c3f96` (`app/google-services.json` `project_id`) |
| **사용 서비스(코드/플러그인 기준)** | Authentication, Firestore, Functions, Storage, Analytics, Crashlytics |

### Firestore — 코드에서 참조하는 주요 경로(비전부)

- `users/{uid}` — 프로필, `dailyUsage`, `categoryStats`, `timeSegments`, `notifications`, `badges`, `appLimitLogs`, 등 (`AccountWithdrawalHelper`, `StatisticsBackupFirestoreRepository`, `NotificationRepository`, `BadgeRepository`, …)
- `verificationCodes` — SMS 인증(Functions와 연동)
- `bugReports` — Callable `submitBugReport`가 문서 추가

### Firestore 보안 규칙

- 저장소 루트에 **`firestore.rules` 파일 없음** → 배포 규칙 내용은 **미확인**(Firebase 콘솔 또는 별도 저장소일 수 있음).

### Firebase Storage 규칙

- `storage.rules`: `bug-reports/{userId}/**` — `request.auth.uid == userId` 또는 `userId == 'anonymous'` 쓰기 허용.

### Cloud Functions (`functions/index.js` exports)

| 함수 | 역할 |
|------|------|
| `sendSignUpVerificationSms` | 회원가입 SMS 인증번호 |
| `verifyAndCompleteSignUp` | 인증 후 Auth 계정 생성 + Firestore user |
| `sendPasswordResetSms` / `verifyAndResetPassword` | 비번 재설정 SMS 플로우 |
| `kakaoLogin` / `naverLogin` | 소셜 토큰 → Firestore + Custom Token |
| `classifyApps` | Claude로 앱 카테고리 분류 |
| `callClaude` | Claude 단발 프롬프트 |
| `submitBugReport` | 버그 신고 Firestore 저장 |
| `deleteAccount` | `admin.auth().deleteUser(uid)` (Callable 인증 필요) |

---

## 7. 프리미엄 / 수익 모델

| 항목 | 내용 |
|------|------|
| **게이트** | `SubscriptionManager.PREMIUM_OFFERING_LIVE` — 코드상 **`false`면 스토어와 무관하게 무료만**(디버그 `debugForceSubscribed` 예외). |
| **기능 제한** | `SubscriptionFeature`: 무료 앱 차단 **최대 3개**, Pro 통계·커스텀 스케줄 등 (`FREE_BLOCK_APP_LIMIT` 등). |
| **가격(UI 문자열)** | `SubscriptionBottomSheet.kt`: 연간 표시 **₩37,000/연간**(정가 취소선 ₩46,800), 월 환산 문구 **₩3,083**; 월간 **₩3,900/월간**; 헤드라인 월 **₩3,083** 또는 **₩3,900**. |
| **Play 구독 ID** | `SubscriptionBillingController`: `PRODUCT_ID = "aptox_premium"`, base plans `monthly` / `yearly`. |
| **상태 저장** | `PremiumStatusRepository` — DataStore `aptox_premium_status`. |
| **결제 플로우** | `BillingClient` 연결, `launchBillingFlow`, 구매 확인 후 DataStore 갱신 (`SubscriptionBillingController.kt`). |

---

## 8. 디자인 토큰 & UI

| 구분 | 위치 |
|------|------|
| **시맨틱 컬러** | `AppColors.kt` — Grey/Primary/Text/Surface 등 |
| **Material 기본 샘플 색** | `ui/theme/Color.kt` — `Purple80` 등(템플릿 잔존 가능) |
| **타이포** | `AppTypography.kt` — `SuitVariable` = `R.font.suit_variable` 가변 폰트, `FontVariation`으로 weight 매핑, `AppTypography` 객체에 스타일 정의 |
| **테마** | `AptoxTheme` — `AppTheme.kt` |

---

## 9. 개발 환경 & 인프라

| 항목 | 내용 |
|------|------|
| **Gradle 태스크** | 루트 `build.gradle.kts`: `aptox` → `:app:bundleDevRelease`; `aptoxDebug` → `:app:assembleDevDebug`; `aptoxTest` → `:app:assembleExternalTestDebug` + APK 복사 |
| **릴리즈 서명** | `app/build.gradle.kts` `signingConfigs.release` — 경로·비밀번호는 **`gradle.properties`의 `APTOX_KEYSTORE_PATH`, `APTOX_KEYSTORE_PASSWORD`, `APTOX_KEY_ALIAS`, `APTOX_KEY_PASSWORD`** (민감 정보는 버전 관리 제외 권장) |
| **이슈 브릿지** | `.cursor/rules/issue-bridge.mdc`, `issue-bridge.ps1` — PowerShell로 이슈 기록 자동화 |
| **Functions** | `functions/` Node 프로젝트, `firebase.json`에 `functions` 소스 등록 |

**PowerShell “단축 명령어”**: 저장소에 별도 프로필 별칭 파일은 확인되지 않음. Windows에서는 일반적으로 `.\gradlew.bat aptox`, `.\gradlew.bat aptoxDebug` 등으로 실행.

---

## 10. 알려진 TODO / 비활성 / 엣지 메모 (코드 주석 기준)

| 설명 | 조건/맥락 | 파일 | 임시 대응 |
|------|-----------|------|-----------|
| 회원가입 이메일 플로우 비활성화 | 해당 `SignUpStep` 진입 시 즉시 `LOGIN`으로 전환 | `MainActivity.kt` | 의도적 비활성화 |
| 비밀번호 찾기 플로우 비활성화 | 동일 패턴 | `MainActivity.kt` | 의도적 비활성화 |
| 프리미엄 기능 확장 TODO | 기획 확정 후 추가 | `SubscriptionManager.kt` (`SubscriptionFeature` 주변) | 미구현 placeholder |
| 비로그인 시 제한 기록 임시 로컬 저장 | 로그인 전 이벤트 | `AppLimitLogLocalPreferences.kt` | 로컬 prefs |
| 비로그인 뱃지 임시 저장 | 동기화 전 | `PendingBadgesPreferences.kt` | 로컬 prefs |
| Dev에서 Crashlytics 수집 끔 | `SHOW_DEBUG_MENU` flavor | `AptoxApplication.kt` | 빌드 설정 분기 |
| SQLite 직접 사용 이유 주석 | KSP/Room 호환 이슈 회피 | `UsageStatsDatabase.kt` | 아키텍처 선택 |

**Firestore 보안 규칙 파일 부재**, **Claude `callClaude`의 앱 내 호출처 부재**(Repository만 존재) 등은 스펙상 **미확인/미연결**로 남습니다.

---

*문서 생성 시점: 저장소 스냅샷 기준. 버전·플로우는 `app/build.gradle.kts`, `MainActivity.kt`, `StubScreens.kt`를 우선 참고.*
