package cloud.yundu.changhe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 开机后重新排好每日 7:30 的闹钟（前台服务本身由闹钟在 7:30 拉起，不在开机时直接启动）。 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            NotifyScheduler.arm(ctx);
        }
    }
}
