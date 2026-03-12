package com.cole.app;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AppMonitorService extends Service {

    private static final String TAG = "AppMonitor";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRunning = false;
    private Map<String, Integer> currentRestrictionMap = Collections.emptyMap();
    private long lastCheckedTime = System.currentTimeMillis();
    /** MOVE_TO_FOREGROUND/BACKGROUND 이벤트로 유지되는 현재 포그라운드 앱 */
    private String lastKnownForegroundPkg = null;
    /** 포그라운드 진입 시각(ms). UsageStats 지연 보정용 */
    private final Map<String, Long> foregroundStartTimeMap = new HashMap<>();

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean clearForegroundPkg = intent != null && intent.getBooleanExtra(EXTRA_CLEAR_FOREGROUND_PKG, false);
        Map<String, Integer> restrictionMap = intent != null
            ? parseRestrictionMap(intent.getStringExtra(EXTRA_RESTRICTION_MAP))
            : Collections.emptyMap();

        if (!restrictionMap.isEmpty()) {
            currentRestrictionMap = restrictionMap;
        }
        lastCheckedTime = System.currentTimeMillis();
        Log.d(TAG, "서비스 시작/갱신 | restrictionMap=" + currentRestrictionMap + " clearFg=" + clearForegroundPkg);

        if (!isRunning) {
            isRunning = true;
            createNotificationChannel();
            startForeground(NOTIFICATION_ID, createNotification());
            initForegroundPkg();
            scheduleEventCheck();
        } else {
            handler.removeCallbacksAndMessages(null);
            if (clearForegroundPkg) {
                // 오버레이 닫기 또는 일시정지 종료 후 호출 시 포그라운드 앱 초기화
                // (제한 앱이 아직 포그라운드로 기록된 경우 즉시 차단되는 것을 방지)
                lastKnownForegroundPkg = null;
                foregroundStartTimeMap.clear();
            }
            scheduleEventCheck();
        }
        return START_STICKY;
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

    private void checkForegroundEvents() {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null || currentRestrictionMap.isEmpty()) return;

        long now = System.currentTimeMillis();
        UsageEvents events = usm.queryEvents(lastCheckedTime, now);
        lastCheckedTime = now;

        String selfPkg = getPackageName();
        if (events != null) {
            UsageEvents.Event event = new UsageEvents.Event();
            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                String pkg = event.getPackageName();
                if (selfPkg.equals(pkg)) continue;

                if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastKnownForegroundPkg = pkg;
                    foregroundStartTimeMap.put(pkg, event.getTimeStamp());
                    if (currentRestrictionMap.containsKey(pkg)) {
                        Log.d(TAG, "FOREGROUND 감지: " + pkg);
                        checkAndBlockPackage(usm, pkg);
                    }
                } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    // PiP 등 가시 윈도우가 있으면 포그라운드와 동일하게 카운팅 유지
                    boolean hasVisibleWindow = new AppVisibilityRepository(this).hasVisibleWindow(pkg);
                    if (!hasVisibleWindow) {
                        if (pkg.equals(lastKnownForegroundPkg)) {
                            lastKnownForegroundPkg = null;
                        }
                        foregroundStartTimeMap.remove(pkg);
                    }
                }
            }
        }

        // 이미 포그라운드에 있는 앱에서 일시정지 만료 시 바로 감지 (앱 나갔다 오지 않아도 동작)
        if (lastKnownForegroundPkg != null && currentRestrictionMap.containsKey(lastKnownForegroundPkg)) {
            Log.d(TAG, "현재 포그라운드 체크: " + lastKnownForegroundPkg);
            checkAndBlockPackage(usm, lastKnownForegroundPkg);
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
        UsageEvents events = usm.queryEvents(now - 60_000L, now);
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

    private void checkAndBlockPackage(UsageStatsManager usm, String pkg) {
        long now = System.currentTimeMillis();
        // 일시정지 중이면 차단 건너뜀 (타이머 알림은 PauseTimerNotificationService가 담당)
        PauseRepository pauseRepo = new PauseRepository(this);
        if (pauseRepo.isPaused(pkg)) {
            Log.d(TAG, "일시정지 중 - 차단 건너뜀: " + pkg);
            return;
        }

        AppRestrictionRepository repo = new AppRestrictionRepository(this);
        com.cole.app.model.AppRestriction restriction = null;
        for (com.cole.app.model.AppRestriction r : repo.getAll()) {
            if (r.getPackageName().equals(pkg)) { restriction = r; break; }
        }

        boolean shouldBlock;
        if (restriction != null && restriction.getBlockUntilMs() > 0) {
            // 시간 지정 차단
            shouldBlock = System.currentTimeMillis() < restriction.getBlockUntilMs();
            long remainingMin = (restriction.getBlockUntilMs() - System.currentTimeMillis()) / 60000;
            Log.d(TAG, "체크(시간지정) | " + pkg + " 종료까지 " + remainingMin + "분 남음");
            if (!shouldBlock) {
                // 메인 "진행중인 앱"에서 만료된 시간지정 앱을 "해제됨"으로 표시하려고 repo에서 삭제하지 않음
                Map<String, Integer> newMap = new HashMap<>(currentRestrictionMap);
                newMap.remove(pkg);
                currentRestrictionMap = newMap;
                Log.d(TAG, "시간 지정 차단 종료: " + pkg);
            }
        } else {
            // 일일 사용량 차단
            int limitMinutes = currentRestrictionMap.getOrDefault(pkg, 60);
            long baselineMs = restriction != null ? restriction.getBaselineTimeMs() : 0L;
            java.util.Set<String> visiblePkgs = new AppVisibilityRepository(this).getPackagesWithVisibleWindows();
            long todayUsageMs = UsageStatsUtils.getUsageSinceBaselineMs(usm, pkg, baselineMs, visiblePkgs);
            // UsageStats는 배치 처리되어 1~5분 지연. 현재 포그라운드 세션 시간을 보정에 추가
            Long startMs = foregroundStartTimeMap.get(pkg);
            if (pkg.equals(lastKnownForegroundPkg) && startMs != null) {
                long sessionMs = now - startMs;
                long bufferMs = Math.min(sessionMs, USAGE_STATS_LAG_BUFFER_MS);
                todayUsageMs += bufferMs;
                Log.d(TAG, "체크(일일) | " + pkg + " 오늘=" + (todayUsageMs / 60000) + "분(보정+" + (bufferMs / 60000) + "분) | 제한=" + limitMinutes + "분");
            } else {
                Log.d(TAG, "체크(일일) | " + pkg + " 오늘=" + (todayUsageMs / 60000) + "분 | 제한=" + limitMinutes + "분");
            }
            long limitMs = limitMinutes * 60L * 1000L;
            shouldBlock = todayUsageMs >= limitMs;
        }

        if (shouldBlock) {
            Log.d(TAG, "차단! " + pkg);
            if (!BlockOverlayService.isRunning) {
                Intent i = new Intent(this, BlockOverlayService.class);
                i.putExtra(BlockOverlayService.EXTRA_PACKAGE_NAME, pkg);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(i);
                } else {
                    startService(i);
                }
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
                    if (mins > 0) map.put(pkg, mins);
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
                CHANNEL_ID, "앱 모니터링", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(monitorChannel);
        }
    }

    private Notification createNotification() {
        PendingIntent pi = PendingIntent.getActivity(this, 0,
            getPackageManager().getLaunchIntentForPackage(getPackageName()),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("앱 모니터링 중")
            .setContentText("제한 앱 사용 감시")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pi)
            .build();
    }

    private static final String CHANNEL_ID = "app_monitor";
    private static final int NOTIFICATION_ID = 1001;
    private static final long EVENT_CHECK_INTERVAL_MS = 1_000L;
    /** UsageStats 배치 지연 보정용 (1~5분). 현재 포그라운드 세션에서 추가할 최대 ms */
    private static final long USAGE_STATS_LAG_BUFFER_MS = 5 * 60 * 1000L;
    private static final String SEP_ITEM = "|";
    private static final String SEP_KV = ":";
    public static final String EXTRA_RESTRICTION_MAP = "restriction_map";
    public static final String EXTRA_CLEAR_FOREGROUND_PKG = "clear_foreground_pkg";

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
        Intent intent = new Intent(context, AppMonitorService.class);
        if (restrictionMap != null && !restrictionMap.isEmpty()) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, AppMonitorService.class));
    }
}