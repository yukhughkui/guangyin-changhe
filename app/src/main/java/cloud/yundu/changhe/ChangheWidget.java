package cloud.yundu.changhe;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * 桌面小组件的共用逻辑，同时承担 4×2 宽尺寸。
 * 沿用初版的组件名 .ChangheWidget，保证升级后桌面上已添加的旧实例不失效；
 * 2×2 由 ChangheWidgetSmall 单独注册，选择器里才有两种尺寸。
 */
public class ChangheWidget extends AppWidgetProvider {

    protected int layoutId() { return R.layout.widget_wide; }

    protected Class<?> providerClass() { return ChangheWidget.class; }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) updateOne(context, manager, id, layoutId());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // APPWIDGET_UPDATE 由 super 转给 onUpdate，这里只补跨天、换时区与开机
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (action == null) return;
        if (action.equals(Intent.ACTION_DATE_CHANGED)
                || action.equals(Intent.ACTION_TIME_CHANGED)
                || action.equals(Intent.ACTION_TIMEZONE_CHANGED)
                || action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            refreshClass(context, providerClass(), layoutId());
        }
    }

    /** 供 Activity 在设置变更后立即刷新全部尺寸。 */
    public static void refresh(Context context) {
        refreshClass(context, ChangheWidget.class, R.layout.widget_wide);
        refreshClass(context, ChangheWidgetSmall.class, R.layout.widget_small);
    }

    private static void refreshClass(Context context, Class<?> cls, int layout) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, cls));
            for (int id : ids) updateOne(context, manager, id, layout);
        } catch (Exception ignored) {
        }
    }

    private static void updateOne(Context context, AppWidgetManager manager, int id, int layout) {
        RemoteViews views = new RemoteViews(context.getPackageName(), layout);
        String[] t = ChangheData.texts(context);
        views.setTextViewText(R.id.w_lived_val, t[0]);
        views.setTextViewText(R.id.w_ahead_val, t[1]);
        views.setTextViewText(R.id.w_mark_name, t[2]);
        views.setTextViewText(R.id.w_mark_days, t[3]);

        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.w_root, pi);

        manager.updateAppWidget(id, views);
    }
}
