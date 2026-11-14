package cloud.yundu.changhe;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.time.LocalTime;

/** 前台服务：状态栏常驻「最近的刻度」倒计时，每 30 秒刷新一次，当天 23:59 后自动收起。 */
public class ChangheNotifyService extends Service {

    public static final int NOTIFY_ID = 2001;
    public static final String CHANNEL_ID = "changhe_countdown";
    private static final long TICK = 30_000L;

    private Handler handler;
    private Runnable ticker;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createChannel();
        postNotification();
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
            ticker = new Runnable() {
                @Override
                public void run() {
                    // 一天结束（23:59 后）自动收起，次日 7:30 的闹钟会重新拉起
                    if (LocalTime.now().isAfter(LocalTime.of(23, 59))) {
                        stopSelf();
                        return;
                    }
                    postNotification();
                    handler.postDelayed(this, TICK);
                }
            };
            handler.postDelayed(ticker, TICK);
        }
        return START_STICKY;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "光阴长河 · 倒计时", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("最近的刻度常驻倒计时");
            ch.setSound(null, null);
            ch.enableLights(false);
            ch.enableVibration(false);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
    }

    private void postNotification() {
        try {
            ChangheData.Snapshot s = ChangheData.load(this);
            if (!s.notifyEnabled) {
                stopSelf();
                return;
            }
            ChangheData.Countdown c = ChangheData.nearestCountdown(this);
            startForeground(NOTIFY_ID, buildNotification(c));
        } catch (Exception e) {
            // 出错也要保留一个空通知，保证前台服务不崩
            try {
                startForeground(NOTIFY_ID, buildNotification(new ChangheData.Countdown()));
            } catch (Exception ignored) {
            }
        }
    }

    private Notification buildNotification(ChangheData.Countdown c) {
        Context ctx = this;
        PendingIntent pi = PendingIntent.getActivity(ctx, 0,
                new Intent(ctx, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | flagImmutable());
        String title = "最近 · " + (c.valid ? c.name : "未设置");
        String countdown = c.valid ? formatCountdown(c) : "还没有前方的刻度";
        String verse = c.valid ? verseFor(c.totalDays) : "把时间，留在看得见的地方。";
        Notification.Builder b = new Notification.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(title)
                .setContentText(countdown)
                .setStyle(new Notification.BigTextStyle().bigText(countdown + "\n" + verse))
                .setContentIntent(pi)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSound(null)
                .setColor(0xFF3C6A60)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_STATUS);
        return b.build();
    }

    /** 「还剩 X 个月 X 天 X 小时 X 分」 */
    private static String formatCountdown(ChangheData.Countdown c) {
        StringBuilder sb = new StringBuilder("还剩 ");
        if (c.months > 0) sb.append(c.months).append(" 个月 ");
        if (c.days > 0) sb.append(c.days).append(" 天 ");
        if (c.hours > 0) sb.append(c.hours).append(" 小时 ");
        sb.append(c.minutes).append(" 分");
        return sb.toString().trim();
    }

    /** 按剩余时长给一句寄语：紧迫 / 适中 / 宽裕 */
    private static String verseFor(long days) {
        if (days <= 14) {
            return "为期已近，宜尽早着手，莫待临期方匆匆。";
        } else if (days <= 90) {
            return "尚需些时日，可从容谋划，稳步向前。";
        } else {
            return "来日方长，愿汝妥善安排，把每段光阴用在紧要处。";
        }
    }

    private static int flagImmutable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (handler != null && ticker != null) handler.removeCallbacks(ticker);
        super.onDestroy();
    }
}
