package com.cole.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;
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
    /** 일시정지 1분 전 알림을 이미 보낸 패키지 (중복 방지) */
    private final java.util.Set<String> pauseWarningNotifiedPkgs = new java.util.HashSet<>();

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Map<String, Integer> restrictionMap = intent != null
            ? parseRestrictionMap(intent.getStringExtra(EXTRA_RESTRICTION_MAP))
            : Collections.emptyMap();

        currentRestrictionMap = restrictionMap;
        lastCheckedTime = System.currentTimeMillis();
        Log.d(TAG, "서비스 시작/갱신 | restrictionMap=" + currentRestrictionMap);

        if (!isRunning) {
            isRunning = true;
            createNotificationChannel();
            startForeground(NOTIFICATION_ID, createNotification());
            initForegroundPkg();
            scheduleEventCheck();
        } else {
            handler.removeCallbacksAndMessages(null);
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
                    if (currentRestrictionMap.containsKey(pkg)) {
                        Log.d(TAG, "FOREGROUND 감지: " + pkg);
                        checkAndBlockPackage(usm, pkg);
                    }
                } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    if (pkg.equals(lastKnownForegroundPkg)) {
                        lastKnownForegroundPkg = null;
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
        Log.d(TAG, "초기 포그라운드 앱: " + lastKnownForegroundPkg);
    }

    private void checkAndBlockPackage(UsageStatsManager usm, String pkg) {
        // 일시정지 중이면 차단 건너뜀 + 1분 전 알림 체크
        PauseRepository pauseRepo = new PauseRepository(this);
        if (pauseRepo.isPaused(pkg)) {
            long pauseUntilMs = pauseRepo.getPauseUntilMs(pkg);
            long remainingMs = pauseUntilMs - System.currentTimeMillis();
            // 남은 시간이 60초 이하이고 아직 알림을 보내지 않은 경우
            if (remainingMs <= 60_000L && !pauseWarningNotifiedPkgs.contains(pkg)) {
                pauseWarningNotifiedPkgs.add(pkg);
                sendPauseWarningNotification(pkg);
                Log.d(TAG, "일시정지 1분 전 알림 전송: " + pkg);
            }
            // 일시정지가 끝나면 알림 전송 기록 초기화 (다음 일시정지에 다시 알림 가능)
            Log.d(TAG, "일시정지 중 - 차단 건너뜀: " + pkg);
            return;
        }
        // 일시정지가 끝났으면 알림 전송 기록 초기화
        pauseWarningNotifiedPkgs.remove(pkg);

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
                repo.delete(pkg);
                Map<String, Integer> newMap = new HashMap<>(currentRestrictionMap);
                newMap.remove(pkg);
                currentRestrictionMap = newMap;
                Log.d(TAG, "시간 지정 차단 종료: " + pkg);
            }
        } else {
            // 일일 사용량 차단
            int limitMinutes = currentRestrictionMap.getOrDefault(pkg, 60);
            long todayUsageMs = getTodayUsageMs(usm, pkg);
            long limitMs = limitMinutes * 60L * 1000L;
            shouldBlock = todayUsageMs >= limitMs;
            Log.d(TAG, "체크(일일) | " + pkg + " 오늘=" + (todayUsageMs / 60000) + "분 | 제한=" + limitMinutes + "분");
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

    private long getTodayUsageMs(UsageStatsManager usageStatsManager, String packageName) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startTime = cal.getTimeInMillis();
        long endTime = System.currentTimeMillis();
        List<UsageStats> stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
        if (stats == null) return 0;
        long total = 0;
        for (UsageStats s : stats) {
            if (packageName.equals(s.getPackageName())) {
                total += s.getTotalTimeInForeground();
            }
        }
        return total;
    }

    private void sendPauseWarningNotification(String pkg) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        String appName = pkg;
        try {
            appName = (String) getPackageManager().getApplicationLabel(
                getPackageManager().getApplicationInfo(pkg, 0));
        } catch (Exception ignored) {}

        PendingIntent pi = PendingIntent.getActivity(this, 0,
            getPackageManager().getLaunchIntentForPackage(getPackageName()),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_PAUSE_WARNING_ID)
            .setContentTitle(appName)
            .setContentText("1분 후 다시 사용이 제한됩니다")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build();

        nm.notify(NOTIFICATION_PAUSE_WARNING_ID_BASE + pkg.hashCode(), notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel monitorChannel = new NotificationChannel(
                CHANNEL_ID, "앱 모니터링", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(monitorChannel);
            NotificationChannel warningChannel = new NotificationChannel(
                CHANNEL_PAUSE_WARNING_ID, "일시정지 종료 알림", NotificationManager.IMPORTANCE_HIGH);
            warningChannel.setDescription("일시정지 1분 전 알림");
            nm.createNotificationChannel(warningChannel);
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
    private static final String CHANNEL_PAUSE_WARNING_ID = "pause_warning";
    private static final int NOTIFICATION_ID = 1001;
    private static final int NOTIFICATION_PAUSE_WARNING_ID_BASE = 2000;
    private static final long EVENT_CHECK_INTERVAL_MS = 1_000L;
    private static final String SEP_ITEM = "|";
    private static final String SEP_KV = ":";
    public static final String EXTRA_RESTRICTION_MAP = "restriction_map";

    public static void start(Context context) {
        start(context, Collections.emptyMap());
    }

    public static void start(Context context, Map<String, Integer> restrictionMap) {
        Intent intent = new Intent(context, AppMonitorService.class);
        if (restrictionMap != null && !restrictionMap.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> e : restrictionMap.entrySet()) {
                if (sb.length() > 0) sb.append(SEP_ITEM);
                sb.append(e.getKey()).append(SEP_KV).append(e.getValue());
            }
            intent.putExtra(EXTRA_RESTRICTION_MAP, sb.toString());
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