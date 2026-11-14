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
 *
 * v1.18 起原样回退到 v1.12 的「已验证可用」实现：仅做 textViewText + 点击跳转，
 * 无主题切换、无 dark 布局、无结构化数据、无任何运行时反射。
 * v1.22 仅新增 compact 标志让 2×2 取「只到年」的短文案，RemoteViews 调用方式未变。
 */
public class ChangheWidget extends AppWidgetProvider {

    protected int layoutId() { return R.layout.widget_wide; }

    protected Class<?> providerClass() { return ChangheWidget.class; }

    /** 2×2 覆写为 true：数值只到「年」，避免窄卡里「零 X 个月 X 天」被截断。 */
    protected boolean compact() { return false; }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) updateOne(context, manager, id, layoutId(), compact());
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
            refreshClass(context, providerClass(), layoutId(), compact());
        }
    }

    /** 供 Activity 在设置变更后立即刷新全部尺寸。 */
    public static void refresh(Context context) {
        refreshClass(context, ChangheWidget.class, R.layout.widget_wide, false);
        refreshClass(context, ChangheWidgetSmall.class, R.layout.widget_small, true);
    }

    private static void refreshClass(Context context, Class<?> cls, int layout, boolean compact) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, cls));
            for (int id : ids) updateOne(context, manager, id, layout, compact);
        } catch (Exception ignored) {
        }
    }

    private static void updateOne(Context context, AppWidgetManager manager,
                                  int id, int layout, boolean compact) {
        RemoteViews views = new RemoteViews(context.getPackageName(), layout);
        String[] t = ChangheData.texts(context, compact);
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
