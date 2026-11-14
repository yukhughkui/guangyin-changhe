package cloud.yundu.changhe;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.Calendar;

/** 常驻通知的调度工具：每日 7:30 精准闹钟、权限判定。 */
public final class NotifyScheduler {

    public static final String ACTION_NOTIFY = "cloud.yundu.changhe.action.NOTIFY_START";
    private static final int REQ = 2001;

    /** 排好下一次 7:30 的精准闹钟（若今天已过则排明天）。 */
    public static void arm(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(ctx, NotifyAlarmReceiver.class).setAction(ACTION_NOTIFY);
        android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(
                ctx, REQ, i, android.app.PendingIntent.FLAG_UPDATE_CURRENT | flagImmutable());
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 7);
        c.set(Calendar.MINUTE, 30);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= System.currentTimeMillis()) {
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
        } catch (Exception ignored) {
        }
    }

    /** 取消已排的闹钟（关闭通知时调用）。 */
    public static void cancel(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(ctx, NotifyAlarmReceiver.class).setAction(ACTION_NOTIFY);
        android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(
                ctx, REQ, i, android.app.PendingIntent.FLAG_UPDATE_CURRENT | flagImmutable());
        am.cancel(pi);
    }

    /** Android 13+ 需要用户授予通知权限；旧版本默认授予。 */
    public static boolean hasNotifyPermission(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private static int flagImmutable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? android.app.PendingIntent.FLAG_IMMUTABLE : 0;
    }
}
