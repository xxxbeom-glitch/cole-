# 앱 제한 플로우 — 구현 현황 문서

> 작성일: 2026-03-25  
> 대상 플로우: 일일 사용량 제한 / 시간 지정 제한

---

## 목차

1. [데이터 모델 및 저장소](#1-데이터-모델-및-저장소)
2. [일일 사용량 제한 플로우](#2-일일-사용량-제한-플로우)
3. [시간 지정 제한 플로우](#3-시간-지정-제한-플로우)
4. [공통 차단 로직](#4-공통-차단-로직)
5. [자정 리셋 및 자동 갱신](#5-자정-리셋-및-자동-갱신)
6. [제한 해제 로직](#6-제한-해제-로직)
7. [홈 화면 카드 상태 표시](#7-홈-화면-카드-상태-표시)
8. [파일 구조 요약](#8-파일-구조-요약)

---

## 1. 데이터 모델 및 저장소

### AppRestriction (model/AppRestriction.kt)

모든 제한 설정은 하나의 데이터 클래스로 표현됩니다.

```kotlin
data class AppRestriction(
    val packageName: String,     // 앱 패키지명
    val appName: String,         // 앱 이름 (표시용)
    val limitMinutes: Int,       // 제한 시간(분)
    val blockUntilMs: Long,      // 시간지정: 차단 종료 시각(ms). 0이면 일일사용량 방식
    val baselineTimeMs: Long,    // 일일사용량: 카운트 시작 기준 시각(ms)
    val repeatDays: String,      // 일일사용량: 반복 요일 ("0,1,2,3,4,5,6" = 매일)
    val durationWeeks: Int,      // 일일사용량: 적용 기간(주). 0 = 무제한
    val startTimeMs: Long,       // 시간지정: 차단 시작 시각(ms). 0이면 즉시 시작
)
```

**제한 방식 구분 기준**: `blockUntilMs > 0` → 시간지정 제한 / `blockUntilMs == 0` → 일일사용량 제한

### 저장소: AppRestrictionRepository (AppRestrictionRepository.kt)

- **저장 방식**: `SharedPreferences` (`aptox_app_restrictions`)
- **직렬화 포맷**: 항목 구분 `\n`, 필드 구분 `|`
  ```
  com.instagram.android|인스타그램|60|0|1711234567000|0,1,2,3,4,5,6|0|0
  ```
- **주요 메서드**:
  - `save(restriction)` — 저장 (동일 패키지 있으면 덮어쓰기)
  - `getAll()` — 전체 조회
  - `delete(packageName)` — 삭제
  - `toRestrictionMap()` — `Map<packageName, limitMinutes>` 반환 (AppMonitorService 전달용)
  - `renewExpiredTimeSpecifiedRestrictions()` — 만료된 시간지정 제한을 다음날 같은 시각으로 갱신

---

## 2. 일일 사용량 제한 플로우

### 2-1. UI 진입 흐름

```
홈 화면 "사용제한 앱 추가"
  └─ RestrictionTypeSelectBottomSheet (제한 방식 선택)
       └─ "일일 사용량 지정" 선택
            └─ AddAppFlowHost → SignUpStep.ADD_APP
```

### 2-2. 화면 단계 (AddAppFlowHost — AddAppScreens.kt)

| 단계 | 화면 | 내용 |
|---|---|---|
| `AA_DAILY_01` | `AddAppDailyLimitScreen01` | 앱 선택 + 하루 사용량 선택 |
| `AA_DAILY_05` | `AddAppDailyLimitScreen05` | 설정 완료 확인 + 저장 |

- **앱 선택**: `AddAppSelectBottomSheet` — 설치된 앱 목록에서 검색/선택
- **시간 선택**: `AppLimitSetupTimeBottomSheet` — 30분 / 60분 / 120분 / 180분 / 240분 / 360분

### 2-3. 저장 로직 (AA_DAILY_05 완료 버튼 탭 시)

```kotlin
AppRestriction(
    packageName = app.packageName,
    appName = app.appName,
    limitMinutes = mins,           // 선택한 분 (30~360)
    blockUntilMs = 0L,             // 일일사용량 방식
    baselineTimeMs = System.currentTimeMillis(),
    repeatDays = "0,1,2,3,4,5,6", // 매일 반복
    durationWeeks = 0,             // 무제한
)
```

저장 후 `AppMonitorService.start(context, map)` 호출로 모니터링 시작.

### 2-4. 사용량 측정: ManualTimerRepository (ManualTimerRepository.kt)

`UsageStatsManager` 대신 **수동 타이머** 방식 사용 (카운트 정지 후에도 UsageStats가 계속 누적되는 버그 방지).

- **저장소**: `SharedPreferences` (`aptox_manual_timer`)
- **키 구조**:
  - `active_{packageName}` — 카운트 시작 시각(ms). 없으면 정지 상태
  - `accum_{packageName}_{yyyyMMdd}` — 오늘 누적 사용시간(ms)

**주요 동작**:

| 메서드 | 동작 |
|---|---|
| `startSession(pkg)` | `active_` 키에 현재 시각 저장 |
| `endSession(pkg)` | 경과시간 계산 → `accum_` 에 누적, `active_` 삭제 |
| `getTodayUsageMs(pkg)` | `accum_` + 진행 중 세션 경과시간 합산 |
| `isSessionActive(pkg)` | `active_` 키 존재 여부 |
| `resetStaleActiveSessionsAtMidnight()` | 자정 이전 시작된 활성 세션 정리 |

**자정 처리**: 자정을 넘긴 활성 세션은 오늘 00:00 이후 구간만 카운트.

### 2-5. 차단 판단 (일일사용량)

`AppMonitorService.checkAndBlockPackage()` 내 분기:

```
blockUntilMs == 0 (일일사용량)
  ├─ 카운트 미시작 (isSessionActive = false)
  │    → 차단 (OVERLAY_STATE_COUNT_NOT_STARTED)
  │    → 다이얼로그: "카운트 시작" / "닫기"
  └─ 카운트 진행 중
       ├─ todayUsageMs >= limitMs
       │    → 차단 (OVERLAY_STATE_USAGE_EXCEEDED)
       │    → 다이얼로그: "사용 시간을 모두 소진했어요" / "닫기"
       └─ todayUsageMs < limitMs
            → 사용 허용
```

### 2-6. 알림

| 조건 | 알림 |
|---|---|
| 남은 시간 5분 전 | "5분 남았어요" 푸시 알림 |
| 남은 시간 1분 전 | "1분 남았어요" 푸시 알림 |
| 사용량 초과 | "사용 시간 소진" 푸시 알림 |

---

## 3. 시간 지정 제한 플로우

### 3-1. UI 진입 흐름 (두 가지 경로)

**경로 A — 기존 AddApp 플로우 (시간 단위 차단)**
```
홈 화면 "사용제한 앱 추가"
  └─ RestrictionTypeSelectBottomSheet
       └─ (내부적으로 AddAppFlowHost → AA_02A_TIME_01)
```

**경로 B — 신규 TimeSpecified 플로우 (시작~종료 시각 지정)**
```
홈 화면 "사용제한 앱 추가"
  └─ RestrictionTypeSelectBottomSheet
       └─ "지정 시간 제한" 선택
            └─ TimeSpecifiedFlowHost → SignUpStep.TIME_SPECIFIED
```

### 3-2. 경로 A: 기존 시간지정 플로우 (AddAppFlowHost)

| 단계 | 화면 | 내용 |
|---|---|---|
| `AA_02A_TIME_01` | `AddAppScreenAA02ATimeSetup` | 앱 선택 + 차단 시간(분) 선택 |
| `AA_02A_TIME_05` | `AddAppScreenAA02ATimeSummary` | 설정 요약 확인 |
| `AA_03_01` | `AddAppCommonCompleteScreen` | 완료 + 저장 |

**저장 로직** (`AA_03_01` 완료 버튼 탭 시):

```kotlin
val blockUntilMs = System.currentTimeMillis() + mins * 60L * 1000L
AppRestriction(
    packageName = app.packageName,
    appName = app.appName,
    limitMinutes = mins,
    blockUntilMs = blockUntilMs,   // 지금으로부터 N분 후
    baselineTimeMs = 0L,           // 사용량 카운트 안 함
    startTimeMs = 0L,              // 즉시 시작
)
```

### 3-3. 경로 B: 신규 시간지정 플로우 (TimeSpecifiedFlowHost — AddAppTimeSpecifiedScreens.kt)

| 단계 | 화면 | 내용 |
|---|---|---|
| `SETUP` | `TimeSpecifiedScreen01` | 앱 선택 + 시작 시간 + 종료 시간 선택 |
| `COMPLETE` | `TimeSpecifiedScreen03` | 설정 완료 요약 + 저장 |

- **시간 선택**: `DrumrollTimePickerBottomSheet` — 시(1~12) / 분(00~59) / AM·PM 드럼롤 피커
- **유효성 검사**: 시작 시간 == 종료 시간이거나 종료 < 시작이면 다음 진행 불가 + `IcoDisclaimerInfo` 에러 메시지 표시

**저장 로직** ("앱 추가하기" 버튼 탭 시):

```kotlin
AppRestriction(
    packageName = selectedPackageName,
    appName = selectedAppName,
    limitMinutes = 0,
    blockUntilMs = endMs,          // 오늘 날짜 기준 종료 시각(ms)
    baselineTimeMs = System.currentTimeMillis(),
    startTimeMs = startMs,         // 오늘 날짜 기준 시작 시각(ms)
)
```

저장 후 `AppMonitorService.start(context, map)` 호출.

### 3-4. 차단 판단 (시간지정)

`AppMonitorService.checkAndBlockPackage()` 내 분기:

```
blockUntilMs > 0 (시간지정)
  ├─ startTimeMs > 0 && now < startTimeMs
  │    → 차단 안 함 (제한 예정 상태)
  ├─ now >= startTimeMs && now < blockUntilMs
  │    → 차단
  │    → 다이얼로그: "지금은 사용하실 수 없어요" / "{time} 후에 사용 가능해요" / "닫기"
  └─ now >= blockUntilMs
       → 차단 안 함 (만료 → 자동 갱신 처리)
```

`AptoxAccessibilityService`에서도 동일한 로직으로 보완 감지.

### 3-5. 카드 상태 표시 (홈 화면)

| 상태 | 조건 | 표시 텍스트 | 색상 |
|---|---|---|---|
| 제한 중 | `startTimeMs ≤ now < blockUntilMs` | `PM 9:00 까지 제한` | 시간: TextHighlight(보라) |
| 제한 예정 | `now < startTimeMs` | `AM 9:00 제한 예정` | 시간: TextHighlight(보라) |
| 사용 가능 | 갱신 후 엣지케이스 | `사용 가능` | TextSecondary |
| 일시정지 중 | `isPaused == true` | `00:00:00 일시정지 중` | Red300 |

---

## 4. 공통 차단 로직

### 4-1. AppMonitorService (AppMonitorService.java)

- **방식**: Foreground Service + `UsageStatsManager` 이벤트 폴링 (1초 간격)
- **트리거**: `MOVE_TO_FOREGROUND` 이벤트 또는 이미 포그라운드 앱 주기적 체크
- **차단 실행**: `BlockDialogActivity.start()` 호출 (투명 Activity + AlertDialog)

**서비스 시작**:
```java
AppMonitorService.start(context, restrictionMap)
// restrictionMap: Map<packageName, limitMinutes>
```

### 4-2. AptoxAccessibilityService (AptoxAccessibilityService.kt)

- `TYPE_WINDOW_STATE_CHANGED` 이벤트로 AppMonitorService 보완
- PiP(Picture-in-Picture) 등 화면 가시 윈도우 감지 (`AppVisibilityRepository`)
- 동일한 차단 판단 로직 적용

### 4-3. BlockDialogActivity (BlockDialogActivity.kt)

투명 Activity로 AlertDialog를 띄워 앱 사용을 차단.

| 케이스 | 조건 | 타이틀 | 버튼 |
|---|---|---|---|
| 카운트 미시작 | `OVERLAY_STATE_COUNT_NOT_STARTED` | 앱을 사용하려면 카운트 시작 버튼을 눌러주세요 | 카운트 시작 / 닫기 |
| 사용량 초과 | `OVERLAY_STATE_USAGE_EXCEEDED` | 사용 시간을 모두 소진했어요 | 닫기 |
| 시간지정 차단 | `blockUntilMs > 0` | 지금은 사용하실 수 없어요 | 닫기 |

---

## 5. 자정 리셋 및 자동 갱신

### 5-1. 트리거 경로 (두 가지)

**경로 1 — AlarmManager (DailyUsageMidnightResetScheduler)**
```
매일 00:00 AlarmManager 발동
  └─ DailyUsageMidnightResetReceiver.onReceive()
       ├─ ManualTimerRepository.resetStaleActiveSessionsAtMidnight()
       ├─ AppRestrictionRepository.renewExpiredTimeSpecifiedRestrictions()
       │    └─ 만료된 시간지정 제한 → +24시간 갱신
       ├─ AppMonitorService.start() (갱신된 경우)
       └─ DailyUsageMidnightResetScheduler.scheduleNextMidnight() (다음날 예약)
```

**경로 2 — AppMonitorService 내부 감지**
```
checkForegroundEvents() 매 1초 호출
  └─ checkAndApplyMidnightResetIfNeeded()
       ├─ todayDateKey() 변경 감지
       ├─ ManualTimerRepository.resetStaleActiveSessionsAtMidnight()
       └─ AppRestrictionRepository.renewExpiredTimeSpecifiedRestrictions()
            └─ currentRestrictionMap 즉시 업데이트
```

### 5-2. renewExpiredTimeSpecifiedRestrictions() 동작

```
blockUntilMs <= now 인 시간지정 제한 항목 탐색
  └─ while (newEnd <= now):
       newStart += 24시간
       newEnd += 24시간
  └─ SharedPreferences 갱신 저장
```

여러 날 건너뛴 경우(앱 미실행 기간 등)도 미래 시각이 될 때까지 반복 전진.

---

## 6. 제한 해제 로직

`RestrictionDeleteHelper.deleteRestrictedApp()` — 단일 진입점으로 모든 정리 처리.

```
1. PauseRepository.clearForPackage()       — 일시정지 데이터 삭제
2. ManualTimerRepository.endSession()      — 카운트 세션 종료
3. AppRestrictionRepository.delete()       — 제한 설정 삭제
4. BlockDialogActivity.dismissIfPackage()  — 차단 다이얼로그 즉시 닫기
5. PauseTimerNotificationService (cancel) — 일시정지 알림 제거
6. AppMonitorService.start(newMap)         — restriction map 갱신 + 재시작
7. DailyUsageAlarmScheduler 갱신
```

---

## 7. 홈 화면 카드 상태 표시

`loadRestrictionItems()` (StubScreens.kt) — 1초 폴링으로 실시간 갱신.

### 일일 사용량 카드

| 상태 | 조건 |
|---|---|
| 카운트 진행 중 | `isSessionActive == true` |
| 카운트 정지 | `isSessionActive == false` |
| 사용량 초과 | `todayUsageMs >= limitMs` |
| 일시정지 중 | `isPaused == true` |

### 시간지정 제한 카드

| 상태 | 조건 | 표시 |
|---|---|---|
| 제한 중 | `startTimeMs ≤ now < blockUntilMs` | `PM HH:mm 까지 제한` |
| 제한 예정 | `now < startTimeMs` | `AM HH:mm 제한 예정` |
| 사용 가능 | 갱신 후 엣지케이스 | `사용 가능` |
| 일시정지 중 | `isPaused == true` | `00:00:00 일시정지 중` |

---

## 8. 파일 구조 요약

| 파일 | 역할 |
|---|---|
| `model/AppRestriction.kt` | 제한 설정 데이터 모델 |
| `AppRestrictionRepository.kt` | 제한 설정 저장/조회/갱신 (SharedPreferences) |
| `ManualTimerRepository.kt` | 일일사용량 수동 타이머 (SharedPreferences) |
| `PauseRepository.kt` | 일시정지 상태 저장 (SharedPreferences) |
| `AppMonitorService.java` | 포그라운드 서비스 — 앱 감시 + 차단 판단 (1초 폴링) |
| `AptoxAccessibilityService.kt` | 접근성 서비스 — 차단 보완 감지 |
| `BlockDialogActivity.kt` | 차단 다이얼로그 표시 (투명 Activity) |
| `RestrictionDeleteHelper.kt` | 제한 해제 단일 진입점 |
| `DailyUsageMidnightResetReceiver.kt` | 자정 AlarmManager 수신 — 리셋 + 갱신 |
| `DailyUsageMidnightResetScheduler.kt` | 자정 AlarmManager 예약 |
| `AddAppScreens.kt` | 일일사용량/시간지정 제한 추가 UI 플로우 |
| `AddAppTimeSpecifiedScreens.kt` | 신규 시간지정 제한 UI 플로우 (시작~종료 시각) |
| `StubScreens.kt` | 홈 화면 카드 상태 표시 로직 (`loadRestrictionItems`) |
