package cloud.yundu.changhe;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.time.LocalTime;

/** 每日 7:30（及次日）的精准闹钟触发：先排好明天的闹钟，再在授权且未过当日结束时拉起前台服务。 */
public class NotifyAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        // 重新排好下一次闹钟，保证链路不断
        NotifyScheduler.arm(ctx);

        if (!NotifyScheduler.hasNotifyPermission(ctx)) return;
        ChangheData.Snapshot s = ChangheData.load(ctx);
        if (!s.notifyEnabled) return;
        // 当日 23:59 之后不再拉起，留给次日 7:30
        if (LocalTime.now().isAfter(LocalTime.of(23, 59))) return;

        try {
            ctx.startForegroundService(new Intent(ctx, ChangheNotifyService.class));
        } catch (Exception ignored) {
        }
    }
}
