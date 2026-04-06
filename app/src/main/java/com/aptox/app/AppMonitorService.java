package com.aptox.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppMonitorService extends Service {

    private static final String TAG = "AppMonitor";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRunning = false;
    private Map<String, Integer> currentRestrictionMap = Collections.emptyMap();
    private long lastCheckedTime = System.currentTimeMillis();
    /** MOVE_TO_FOREGROUND/BACKGROUND 이벤트로 유지되는 현재 포그라운드 앱 */
    private String lastKnownForegroundPkg = null;
    /** 포그라운드 진입 시각(ms). 이벤트 추적용 */
    private final Map<String, Long> foregroundStartTimeMap = new HashMap<>();
    /** 카운트 미중지 알림 예약된 패키지 (복귀 시 취소용) */
    private String scheduledCountReminderPkg = null;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // startForegroundService() 호출마다 제한 시간 내 startForeground() 필요 (이미 실행 중일 때도 동일)
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildInitialNotification());

        boolean clearForegroundPkg = intent != null && intent.getBooleanExtra(EXTRA_CLEAR_FOREGROUND_PKG, false);
        String releasedPackage = intent != null ? intent.getStringExtra(EXTRA_RELEASED_PACKAGE) : null;
        Map<String, Integer> restrictionMap = intent != null && intent.hasExtra(EXTRA_RESTRICTION_MAP)
            ? parseRestrictionMap(intent.getStringExtra(EXTRA_RESTRICTION_MAP))
            : null;

        if (restrictionMap != null) {
            currentRestrictionMap = restrictionMap;
        }
        // 제한 해제 시 해당 앱 타이머 세션 즉시 종료 (SharedPreferences apply 비동기 race 방지)
        if (releasedPackage != null && !releasedPackage.isEmpty()) {
            new ManualTimerRepository(this).endSession(releasedPackage);
            Log.d(TAG, "제한 해제 수신: " + releasedPackage + " 타이머 세션 종료 후 노티 갱신");
        }
        lastCheckedTime = System.currentTimeMillis();
        Log.d(TAG, "서비스 시작/갱신 | restrictionMap=" + currentRestrictionMap + " clearFg=" + clearForegroundPkg);

        if (!isRunning) {
            isRunning = true;
            initForegroundPkg();
            scheduleEventCheck();
            scheduleNotificationUpdate();
        } else {
            handler.removeCallbacksAndMessages(null);
            if (clearForegroundPkg) {
                // 오버레이 닫기 또는 일시정지 종료 후 호출 시 포그라운드 앱 초기화
                // (제한 앱이 아직 포그라운드로 기록된 경우 즉시 차단되는 것을 방지)
                lastKnownForegroundPkg = null;
                foregroundStartTimeMap.clear();
            }
            scheduleEventCheck();
            scheduleNotificationUpdate();
            // 제한 해제 등 restriction map 변경 시 1초 대기 없이 즉시 알림 상태 반영
            updateNotificationIfCounting();
        }
        return START_STICKY;
    }

    private static final long NOTIFICATION_UPDATE_INTERVAL_MS = 1_000L;

    private void scheduleNotificationUpdate() {
        if (!isRunning) return;
        handler.postDelayed(() -> {
            updateNotificationIfCounting();
            scheduleNotificationUpdate();
        }, NOTIFICATION_UPDATE_INTERVAL_MS);
    }

    /** 카운트 미중지 알람 + 이미 띄운 리마인더 노티 정리 (ongoing 노티는 앱에서 명시 취소 필요) */
    private void cancelCountReminderAlarmAndNotification() {
        String reminderPkg = scheduledCountReminderPkg;
        CountReminderAlarmScheduler.INSTANCE.cancel(this);
        if (reminderPkg != null) {
            CountReminderNotificationHelper.INSTANCE.cancel(this, reminderPkg);
        }
        scheduledCountReminderPkg = null;
    }

    /**
     * 알림·FGS 상태 갱신.
     * - 카운트 중: 카운트 알림
     * - 시간 지정 제한 구간 중: 해당 알림
     * - 그 외: 기본 모니터링 알림(제한 앱 0개여도 상시 표시, 서비스 유지)
     */
    /**
     * 한도 소진(남은 시간 0 이하)인 세션은 노티 "진행 중" 카운트에서 제외.
     * active_* 키만으로는 소진 후 세션이 남는 경우가 있어 필터링 필요.
     */
    private java.util.List<kotlin.Pair<String, Long>> filterSessionsWithRemainingMs(
            ManualTimerRepository timerRepo,
            java.util.List<kotlin.Pair<String, Long>> sessions) {
        java.util.ArrayList<kotlin.Pair<String, Long>> out = new java.util.ArrayList<>();
        for (kotlin.Pair<String, Long> p : sessions) {
            String pkg = p.getFirst();
            long todayUsageMs = timerRepo.getTodayUsageMs(pkg);
            int limitMinutes = currentRestrictionMap.getOrDefault(pkg, 60);
            long limitMs = (long) limitMinutes * 60 * 1000;
            long remainingMs = Math.max(0, limitMs - todayUsageMs);
            if (remainingMs > 0) {
                out.add(p);
            }
        }
        return out;
    }

    private void updateNotificationIfCounting() {
        ManualTimerRepository timerRepo = new ManualTimerRepository(this);
        java.util.List<kotlin.Pair<String, Long>> sessions =
                filterSessionsWithRemainingMs(timerRepo, timerRepo.getAllActiveSessions());
        long now = System.currentTimeMillis();
        List<android.util.Pair<String, Long>> timeSpecActive = getActiveTimeSpecifiedInWindow(now);
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (!sessions.isEmpty()) {
            Notification n;
            if (sessions.size() == 1) {
                String pkg = sessions.get(0).getFirst();
                long todayUsageMs = timerRepo.getTodayUsageMs(pkg);
                int limitMinutes = currentRestrictionMap.getOrDefault(pkg, 60);
                long limitMs = (long) limitMinutes * 60 * 1000;
                long remainingMs = Math.max(0, limitMs - todayUsageMs);
                String appName = getAppNameForPackage(pkg);
                n = buildCountingNotification(appName, remainingMs, pkg);
            } else {
                n = buildCountingNotification(sessions.size());
            }
            startForeground(NOTIFICATION_ID, n);
        } else if (!timeSpecActive.isEmpty()) {
            Notification n;
            if (timeSpecActive.size() == 1) {
                String pkg = timeSpecActive.get(0).first;
                long blockUntil = timeSpecActive.get(0).second;
                String appName = getAppNameForPackage(pkg);
                n = buildTimeSpecifiedNotification(appName, blockUntil);
            } else {
                n = buildTimeSpecifiedNotification(timeSpecActive.size());
            }
            startForeground(NOTIFICATION_ID, n);
            cancelCountReminderAlarmAndNotification();
        } else {
            // 제한 앱이 없어도 앱 설치 후 상시 감지 채널 유지 (빈 map이면 차단 루프만 생략)
            startForeground(NOTIFICATION_ID, buildDefaultNotification());
            cancelCountReminderAlarmAndNotification();
        }
    }

    private Notification buildInitialNotification() {
        ManualTimerRepository timerRepo = new ManualTimerRepository(this);
        java.util.List<kotlin.Pair<String, Long>> sessions =
                filterSessionsWithRemainingMs(timerRepo, timerRepo.getAllActiveSessions());
        long now = System.currentTimeMillis();
        List<android.util.Pair<String, Long>> timeSpecActive = getActiveTimeSpecifiedInWindow(now);
        if (!sessions.isEmpty()) {
            if (sessions.size() == 1) {
                String pkg = sessions.get(0).getFirst();
                long todayUsageMs = timerRepo.getTodayUsageMs(pkg);
                int limitMinutes = currentRestrictionMap.getOrDefault(pkg, 60);
                long limitMs = (long) limitMinutes * 60 * 1000;
                long remainingMs = Math.max(0, limitMs - todayUsageMs);
                String appName = getAppNameForPackage(pkg);
                return buildCountingNotification(appName, remainingMs, pkg);
            } else {
                return buildCountingNotification(sessions.size());
            }
        }
        if (!timeSpecActive.isEmpty()) {
            if (timeSpecActive.size() == 1) {
                String pkg = timeSpecActive.get(0).first;
                long blockUntil = timeSpecActive.get(0).second;
                String appName = getAppNameForPackage(pkg);
                return buildTimeSpecifiedNotification(appName, blockUntil);
            }
            return buildTimeSpecifiedNotification(timeSpecActive.size());
        }
        return buildDefaultNotification();
    }

    /**
     * 시간 지정 제한: startTimeMs ≤ now &lt; blockUntilMs 인 항목만 (제한 구간 중).
     */
    private List<android.util.Pair<String, Long>> getActiveTimeSpecifiedInWindow(long now) {
        ArrayList<android.util.Pair<String, Long>> out = new ArrayList<>();
        AppRestrictionRepository repo = new AppRestrictionRepository(this);
        for (com.aptox.app.model.AppRestriction r : repo.getAll()) {
            if (r.getStartTimeMs() <= 0L) continue;
            long start = r.getStartTimeMs();
            long end = r.getBlockUntilMs();
            if (now >= start && now < end) {
                out.add(new android.util.Pair<>(r.getPackageName(), r.getBlockUntilMs()));
            }
        }
        return out;
    }

    /** 노티용 종료 시각 (HH:mm) */
    private static String formatRestrictionEndTimeHhMm(long epochMs) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.KOREA);
        return sdf.format(new java.util.Date(epochMs));
    }

    private static final int PI_RC_OPEN_APP = 9101;
    private static final int PI_RC_COUNT_STOP_BASE = 9200;

    private Notification buildDefaultNotification() {
        PendingIntent pi = PendingIntent.getActivity(this, PI_RC_OPEN_APP,
            getPackageManager().getLaunchIntentForPackage(getPackageName()),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("앱 사용 시간을 기록하고 있어요")
            .setContentText("")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pi)
            .build();
    }

    private String getAppNameForPackage(String pkg) {
        AppRestrictionRepository repo = new AppRestrictionRepository(this);
        for (com.aptox.app.model.AppRestriction r : repo.getAll()) {
            if (r.getPackageName().equals(pkg)) return r.getAppName();
        }
        try {
            android.content.pm.ApplicationInfo ai = getPackageManager().getApplicationInfo(pkg, 0);
            return getPackageManager().getApplicationLabel(ai).toString();
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private static String formatElapsedHhMmSs(long elapsedMs) {
        long sec = (elapsedMs / 1000) % 86400;
        int h = (int) (sec / 3600);
        int m = (int) ((sec % 3600) / 60);
        int s = (int) (sec % 60);
        return String.format(java.util.Locale.KOREAN, "%02d:%02d:%02d", h, m, s);
    }

    private Notification buildCountingNotification(String appName, long remainingMs, String packageName) {
        PendingIntent pi = PendingIntent.getActivity(this, PI_RC_OPEN_APP,
            getPackageManager().getLaunchIntentForPackage(getPackageName()),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent openSheetIntent = new Intent(this, MainActivity.class);
        openSheetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openSheetIntent.putExtra(EXTRA_OPEN_BOTTOM_SHEET, packageName);
        int stopReq = PI_RC_COUNT_STOP_BASE + (packageName.hashCode() & 0x7FFF);
        PendingIntent endPi = PendingIntent.getActivity(this, stopReq, openSheetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String contentText = appName + " 사용 중 · " + formatElapsedHhMmSs(remainingMs) + " 남음";
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("앱 사용 시간을 기록하고 있어요")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pi)
            .addAction(android.R.drawable.ic_media_pause, "카운트 중지", endPi)
            .build();
    }

    /** 복수 세션용: "앱 N개 카운트 중" */
    private Notification buildCountingNotification(int activeCount) {
        PendingIntent pi = PendingIntent.getActivity(this, PI_RC_OPEN_APP,
            getPackageManager().getLaunchIntentForPackage(getPackageName()),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent endPi = PendingIntent.getActivity(this, PI_RC_COUNT_STOP_BASE + 1, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String contentText = "앱 " + activeCount + "개 카운트 중";
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("앱 사용 시간을 기록하고 있어요")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pi)
            .addAction(android.R.drawable.ic_media_pause, "카운트 중지", endPi)
            .build();
    }

    /** 시간 지정 제한 구간 중 — 단일 앱 (한 줄 요약 + 본문 비움: 확장 화살표 최소화, 액션 없음) */
    private Notification buildTimeSpecifiedNotification(String appName, long blockUntilMs) {
        PendingIntent pi = PendingIntent.getActivity(this, PI_RC_OPEN_APP,
            getPackageManager().getLaunchIntentForPackage(getPackageName()),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String endStr = formatRestrictionEndTimeHhMm(blockUntilMs);
        String oneLine = appName + " 제한 중 · " + endStr + "까지";
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(oneLine)
            .setContentText(null)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pi)
            .build();
    }

    /** 시간 지정 제한 구간 중 — 복수 앱 (한 줄, 액션 없음) */
    private Notification buildTimeSpecifiedNotification(int activeCount) {
        PendingIntent pi = PendingIntent.getActivity(this, PI_RC_OPEN_APP,
            getPackageManager().getLaunchIntentForPackage(getPackageName()),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String oneLine = "앱 " + activeCount + "개 제한 중";
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(oneLine)
            .setContentText(null)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pi)
            .build();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "서비스 중지");
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    private void scheduleEventCheck() {
        if (!isRunning) return;
        handler.postDelayed(() -> {
            checkForegroundEvents();
            scheduleEventCheck();
        }, EVENT_CHECK_INTERVAL_MS);
    }

    /** 마지막으로 자정 리셋을 수행한 날짜 (yyyyMMdd). 서비스가 자정을 넘겨 실행 중일 때 1회만 리셋. */
    private String lastMidnightResetDate = todayDateKey();

    private static String todayDateKey() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        return String.format(java.util.Locale.KOREAN, "%04d%02d%02d",
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH));
    }

    /** 날짜가 바뀌었으면 ManualTimerRepository 자정 리셋 + 시간지정 제한 갱신 수행 */
    private void checkAndApplyMidnightResetIfNeeded() {
        String today = todayDateKey();
        if (!today.equals(lastMidnightResetDate)) {
            Log.d(TAG, "자정 경과 감지 (" + lastMidnightResetDate + " → " + today + "): 일일 사용시간 초기화");
            new ManualTimerRepository(this).resetStaleActiveSessionsAtMidnight();
            // 시간 지정 제한: 만료된 항목을 다음날 같은 시각으로 갱신 후 restriction map 업데이트
            AppRestrictionRepository repo = new AppRestrictionRepository(this);
            boolean renewed = repo.renewExpiredTimeSpecifiedRestrictions();
            if (renewed) {
                currentRestrictionMap = repo.toRestrictionMap();
                Log.d(TAG, "시간지정 제한 갱신 완료 → restrictionMap 업데이트");
            }
            TimeSpecifiedRestrictionAlarmScheduler.INSTANCE.scheduleAll(this);
            lastMidnightResetDate = today;
        }
    }

    private void checkForegroundEvents() {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return;

        checkAndApplyMidnightResetIfNeeded();

        long now = System.currentTimeMillis();
        UsageEvents events;
        try {
            events = usm.queryEvents(lastCheckedTime, now);
        } catch (SecurityException e) {
            Log.w(TAG, "queryEvents 실패(사용정보 권한 없음) — lastCheckedTime 유지", e);
            return;
        }
        lastCheckedTime = now;

        ManualTimerRepository timerRepo = new ManualTimerRepository(this);
        kotlin.Pair<String, Long> activeSession = timerRepo.getActiveSession();

        String selfPkg = getPackageName();
        final boolean mapEmpty = currentRestrictionMap.isEmpty();
        if (events != null) {
            UsageEvents.Event event = new UsageEvents.Event();
            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                String pkg = event.getPackageName();
                if (selfPkg.equals(pkg)) continue;

                if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastKnownForegroundPkg = pkg;
                    foregroundStartTimeMap.put(pkg, event.getTimeStamp());
                    if (!mapEmpty && currentRestrictionMap.containsKey(pkg)) {
                        Log.d(TAG, "FOREGROUND 감지: " + pkg);
                        checkAndBlockPackage(pkg);
                    }
                    // 카운트 미중지: 포그라운드 복귀 시 예약 취소
                    if (pkg.equals(scheduledCountReminderPkg)) {
                        CountReminderAlarmScheduler.INSTANCE.cancel(this);
                        CountReminderNotificationHelper.INSTANCE.cancel(this, pkg);
                        scheduledCountReminderPkg = null;
                    }
                } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    // PiP 등 가시 윈도우가 있으면 포그라운드와 동일하게 카운팅 유지
                    boolean hasVisibleWindow = new AppVisibilityRepository(this).hasVisibleWindow(pkg);
                    if (!hasVisibleWindow) {
                        if (pkg.equals(lastKnownForegroundPkg)) {
                            lastKnownForegroundPkg = null;
                        }
                        foregroundStartTimeMap.remove(pkg);
                        // 카운트 미중지: 활성 세션 앱이 백그라운드 된 경우 1분 후 알림 예약
                        if (activeSession != null && pkg.equals(activeSession.getFirst())) {
                            CountReminderAlarmScheduler.INSTANCE.schedule(this, pkg);
                            scheduledCountReminderPkg = pkg;
                        }
                    }
                }
            }
        }

        // 이미 포그라운드에 있는 앱에서 일시정지 만료 시 바로 감지 (앱 나갔다 오지 않아도 동작)
        if (!mapEmpty && lastKnownForegroundPkg != null && currentRestrictionMap.containsKey(lastKnownForegroundPkg)) {
            Log.d(TAG, "현재 포그라운드 체크: " + lastKnownForegroundPkg);
            checkAndBlockPackage(lastKnownForegroundPkg);
        }
    }

    /**
     * 서비스 시작 시 최근 60초 이벤트로 현재 포그라운드 앱 초기화.
     * 서비스가 새로 시작될 때 lastKnownForegroundPkg가 null이 되는 문제 방지.
     */
    private void initForegroundPkg() {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) return;
        long now = System.currentTimeMillis();
        UsageEvents events;
        try {
            events = usm.queryEvents(now - 60_000L, now);
        } catch (SecurityException e) {
            Log.w(TAG, "initForegroundPkg queryEvents 실패(사용정보 권한 없음)", e);
            return;
        }
        if (events == null) return;
        String selfPkg = getPackageName();
        Map<String, Long> foregroundMap = new HashMap<>();
        UsageEvents.Event event = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            if (selfPkg.equals(event.getPackageName())) continue;
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                foregroundMap.put(event.getPackageName(), event.getTimeStamp());
            } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                foregroundMap.remove(event.getPackageName());
            }
        }
        long latestTime = 0;
        for (Map.Entry<String, Long> e : foregroundMap.entrySet()) {
            if (e.getValue() > latestTime) {
                latestTime = e.getValue();
                lastKnownForegroundPkg = e.getKey();
            }
        }
        if (lastKnownForegroundPkg != null) {
            foregroundStartTimeMap.put(lastKnownForegroundPkg, latestTime);
        }
        Log.d(TAG, "초기 포그라운드 앱: " + lastKnownForegroundPkg);
    }

    private void checkAndBlockPackage(String pkg) {
        // 일시정지 중이면 차단 건너뜀 (타이머 알림은 PauseTimerNotificationService가 담당)
        PauseRepository pauseRepo = new PauseRepository(this);
        if (pauseRepo.isPaused(pkg)) {
            Log.d(TAG, "일시정지 중 - 차단 건너뜀: " + pkg);
            return;
        }

        AppRestrictionRepository repo = new AppRestrictionRepository(this);
        com.aptox.app.model.AppRestriction restriction = null;
        for (com.aptox.app.model.AppRestriction r : repo.getAll()) {
            if (r.getPackageName().equals(pkg)) { restriction = r; break; }
        }

        // 제한 해제된 앱(restriction 삭제됨)은 차단하지 않음
        if (restriction == null) {
            Log.d(TAG, "제한 해제된 앱 - 차단 건너뜀: " + pkg);
            return;
        }

        boolean shouldBlock = false;
        String overlayState = BlockDialogActivity.OVERLAY_STATE_USAGE_EXCEEDED;
        if (restriction != null && restriction.getBlockUntilMs() > 0) {
            // 시간 지정 차단: startTimeMs 이전이면 아직 제한 시작 전 → 차단하지 않음
            long now = System.currentTimeMillis();
            boolean beforeStart = restriction.getStartTimeMs() > 0 && now < restriction.getStartTimeMs();
            shouldBlock = !beforeStart && now < restriction.getBlockUntilMs();
            long remainingMin = (restriction.getBlockUntilMs() - now) / 60000;
            Log.d(TAG, "체크(시간지정) | " + pkg + " beforeStart=" + beforeStart + " 종료까지 " + remainingMin + "분 남음");
            if (now >= restriction.getBlockUntilMs()) {
                BadgeAutoGrant.onTimeBlockWindowEnded(this, pkg, restriction.getBlockUntilMs());
                // 메인 "진행중인 앱"에서 만료된 시간지정 앱을 "해제됨"으로 표시하려고 repo에서 삭제하지 않음
                Map<String, Integer> newMap = new HashMap<>(currentRestrictionMap);
                newMap.remove(pkg);
                currentRestrictionMap = newMap;
                Log.d(TAG, "시간 지정 차단 종료: " + pkg);
            }
        } else {
            // 일일 사용량 차단: 수동 타이머(ManualTimerRepository) 기준. 홈 카드(StubScreens)와 동일.
            // UsageStatsManager는 카운트 정지 후에도 당일 포그라운드 시간이 남아 즉시 초과 차단되는 버그가 있었음.
            int limitMinutes = currentRestrictionMap.getOrDefault(pkg, 60);
            ManualTimerRepository timerRepo = new ManualTimerRepository(this);
            long todayUsageMs = timerRepo.getTodayUsageMs(pkg);
            Log.d(TAG, "체크(일일) | " + pkg + " 오늘(수동타이머)=" + (todayUsageMs / 60000) + "분 | 제한=" + limitMinutes + "분");
            long limitMs = limitMinutes * 60L * 1000L;
            boolean sessionActive = timerRepo.isSessionActive(pkg);
            if (!sessionActive) {
                // 카운트 정지 상태: 차단 ("카운트 시작" 안내 오버레이)
                shouldBlock = true;
                overlayState = BlockDialogActivity.OVERLAY_STATE_COUNT_NOT_STARTED;
            } else if (todayUsageMs >= limitMs) {
                // 카운트 진행 중 + 사용량 초과: 차단 (timeout 이벤트 기록)
                shouldBlock = true;
                overlayState = BlockDialogActivity.OVERLAY_STATE_USAGE_EXCEEDED;
                if (restriction != null) {
                    AppLimitLogRepository.saveTimeoutEventIfNeeded(this, pkg, restriction.getAppName());
                }
                timerRepo.endSession(pkg); // 세션 종료 → 노티바 카운트 정지
            } else {
                // 카운트 진행 중 + 사용량 여유: 사용 허용
                shouldBlock = false;
            }

            // 자정 직후 2분 이내 + 카운트 진행 중인 앱은 차단 스킵
            // (DailyUsageMidnightResetReceiver가 자동 시작한 세션 보호)
            if (shouldBlock && sessionActive) {
                long nowMs = System.currentTimeMillis();
                java.util.Calendar midnightCal = java.util.Calendar.getInstance();
                midnightCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                midnightCal.set(java.util.Calendar.MINUTE, 0);
                midnightCal.set(java.util.Calendar.SECOND, 0);
                midnightCal.set(java.util.Calendar.MILLISECOND, 0);
                long todayMidnightMs = midnightCal.getTimeInMillis();
                if (nowMs - todayMidnightMs <= 2 * 60 * 1000L) {
                    Log.d(TAG, "자정 2분 이내 카운트 진행 중 - 차단 스킵: " + pkg);
                    shouldBlock = false;
                }
            }

            // 푸시 알림 (해당 앱 사용 중일 때만)
            if (pkg.equals(lastKnownForegroundPkg) && restriction != null) {
                String appName = restriction.getAppName();
                long fiveMinBeforeMs = limitMs - 5L * 60 * 1000;
                long oneMinBeforeMs = limitMs - 60 * 1000;
                if (todayUsageMs >= limitMs) {
                    if (!DailyUsageNotificationHelper.INSTANCE.hasFiredLimitReachedToday(this, pkg)) {
                        DailyUsageNotificationHelper.INSTANCE.sendLimitReachedNotification(this, appName, pkg);
                    }
                } else if (todayUsageMs >= fiveMinBeforeMs && todayUsageMs < limitMs) {
                    if (!DailyUsageNotificationHelper.INSTANCE.hasFiredFiveMinWarningToday(this, pkg)) {
                        DailyUsageNotificationHelper.INSTANCE.sendFiveMinWarningNotification(this, appName, pkg);
                    }
                }
                // 마감 임박(1분 전) 알림 (설정 토글 ON, 같은 날 동일 앱 1회)
                if (todayUsageMs >= oneMinBeforeMs && todayUsageMs < limitMs) {
                    if (!DailyUsageNotificationHelper.INSTANCE.hasFiredDeadlineImminentToday(this, pkg)) {
                        DailyUsageNotificationHelper.INSTANCE.sendDeadlineImminentNotification(this, appName, pkg);
                    }
                }
            }
        }

        if (shouldBlock) {
            Log.d(TAG, "차단! " + pkg + " state=" + overlayState);
            if (!BlockDialogActivity.isRunning) {
                long blockUntilMs = restriction != null ? restriction.getBlockUntilMs() : 0L;
                String appName = restriction != null ? restriction.getAppName() : pkg;
                BlockDialogActivity.Companion.start(this, pkg, appName, blockUntilMs, overlayState);
            }
        }
    }

    private static Map<String, Integer> parseRestrictionMap(String encoded) {
        Map<String, Integer> map = new HashMap<>();
        if (encoded == null || encoded.isEmpty()) return map;
        for (String item : encoded.split("\\" + SEP_ITEM)) {
            int colon = item.indexOf(SEP_KV);
            if (colon > 0 && colon < item.length() - 1) {
                String pkg = item.substring(0, colon);
                try {
                    int mins = Integer.parseInt(item.substring(colon + 1));
                    // limitMinutes == 0 인 시간 지정 제한도 모니터링 대상에 포함
                    if (mins >= 0) map.put(pkg, mins);
                } catch (NumberFormatException ignored) {}
            }
        }
        return map;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel monitorChannel = new NotificationChannel(
                CHANNEL_ID, "앱 모니터링", NotificationManager.IMPORTANCE_DEFAULT);
            monitorChannel.setDescription("제한 앱 감시 및 사용 시간 기록");
            nm.createNotificationChannel(monitorChannel);
        }
    }

    /** v2: 기존 LOW 채널은 상태 표시줄에 아이콘이 안 보이는 기기가 많아 새 채널로 상향 */
    private static final String CHANNEL_ID = "app_monitor_v2";
    private static final int NOTIFICATION_ID = 1001;
    private static final long EVENT_CHECK_INTERVAL_MS = 1_000L;
    private static final String SEP_ITEM = "|";
    private static final String SEP_KV = ":";
    public static final String EXTRA_RESTRICTION_MAP = "restriction_map";
    public static final String EXTRA_CLEAR_FOREGROUND_PKG = "clear_foreground_pkg";
    /** 제한 해제된 패키지. 해당 앱 타이머 세션 즉시 종료 후 노티 갱신 */
    public static final String EXTRA_RELEASED_PACKAGE = "released_package";
    /** MainActivity로 바텀시트 오픈 요청용 extra (카운트 중지/시작 버튼에서 앱 실행 후 바텀시트 열기) */
    public static final String EXTRA_OPEN_BOTTOM_SHEET = "open_bottom_sheet";

    public static void start(Context context) {
        start(context, Collections.emptyMap());
    }

    public static void start(Context context, Map<String, Integer> restrictionMap) {
        start(context, restrictionMap, false);
    }

    public static void startAndClearForeground(Context context) {
        start(context, Collections.emptyMap(), true);
    }

    public static void start(Context context, Map<String, Integer> restrictionMap, boolean clearForegroundPkg) {
        start(context, restrictionMap, clearForegroundPkg, null);
    }

    /**
     * 제한 해제 후 호출. releasedPackage 전달 시 해당 앱 타이머 세션 즉시 종료 후 노티 갱신.
     */
    public static void start(Context context, Map<String, Integer> restrictionMap, boolean clearForegroundPkg, String releasedPackage) {
        Context appCtx = context.getApplicationContext();
        if (!PermissionScreenKt.areRequiredAppPermissionsGranted(appCtx)) {
            Log.d(TAG, "필수 권한(사용정보·접근성) 미충족 — AppMonitorService.start 생략");
            return;
        }
        Intent intent = new Intent(context, AppMonitorService.class);
        if (restrictionMap != null) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> e : restrictionMap.entrySet()) {
                if (sb.length() > 0) sb.append(SEP_ITEM);
                sb.append(e.getKey()).append(SEP_KV).append(e.getValue());
            }
            intent.putExtra(EXTRA_RESTRICTION_MAP, sb.toString());
        }
        if (clearForegroundPkg) {
            intent.putExtra(EXTRA_CLEAR_FOREGROUND_PKG, true);
        }
        if (releasedPackage != null && !releasedPackage.isEmpty()) {
            intent.putExtra(EXTRA_RELEASED_PACKAGE, releasedPackage);
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (IllegalStateException e) {
            // API 31+ ForegroundServiceStartNotAllowedException 등 — 클래스명은 구버전 기기에서 로딩 이슈가 있어 캐치만 공통 처리
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Log.w(TAG, "FGS 시작 불가(백그라운드 제한). 앱을 포그라운드로 열면 재시도됩니다.", e);
            } else {
                throw e;
            }
        }
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, AppMonitorService.class));
    }
}
