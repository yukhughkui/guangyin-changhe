package cloud.yundu.changhe;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.RemoteViews;

/**
 * 桌面小组件的共用逻辑，同时承担 4×2 宽尺寸。
 * 沿用初版的组件名 .ChangheWidget，保证升级后桌面上已添加的旧实例不失效；
 * 2×2 由 ChangheWidgetSmall 单独注册，选择器里才有两种尺寸。
 *
 * 主题（素白 / 夜航）按系统日/夜模式自动切换：浅色壁纸走象牙卡片 + 水纹，
 * 深色壁纸走墨绿卡片 + 细分隔线——与 App 自身的日、夜主题同源。
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

    private static boolean isNight(Context c) {
        return (c.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private static void updateOne(Context context, AppWidgetManager manager, int id, int layout) {
        RemoteViews views = new RemoteViews(context.getPackageName(), layout);
        ChangheData.WidgetInfo w = ChangheData.widget(context);
        boolean small = (layout == R.layout.widget_small);
        boolean night = isNight(context);

        int bg = night ? R.drawable.widget_bg_dark : R.drawable.widget_bg_light;
        int cLabel = night ? R.color.w_label_dark : R.color.w_label_light;
        int cHero  = night ? R.color.w_hero_dark  : R.color.w_hero_light;
        int cSub   = night ? R.color.w_sub_dark   : R.color.w_sub_light;
        int cMark  = night ? R.color.w_mark_dark  : R.color.w_mark_light;
        int cDays  = night ? R.color.w_days_dark  : R.color.w_days_light;
        int cLine  = R.color.w_line_dark;

        views.setInt(R.id.w_root, "setBackgroundResource", bg);

        // 已在此间（标签 + 大数「24 年」+ 次行「4 个月 30 天」）
        views.setTextColor(R.id.w_lbl_lived, context.getColor(cLabel));
        views.setTextViewText(R.id.w_hero_num, w.livedYears);
        views.setTextColor(R.id.w_hero_num, context.getColor(cHero));
        views.setTextViewText(R.id.w_hero_unit, w.livedUnit);
        views.setTextColor(R.id.w_hero_unit, context.getColor(cSub));
        views.setTextViewText(R.id.w_sub, w.livedSub);
        views.setTextColor(R.id.w_sub, context.getColor(cSub));

        // 前方尚有（仅 4×2 显示，年数加粗）
        if (small) {
            views.setViewVisibility(R.id.w_ahead, View.GONE);
        } else {
            SpannableString ahead = new SpannableString("前方尚有 " + w.aheadYears + w.aheadRest);
            int st = "前方尚有 ".length();
            int en = st + w.aheadYears.length();
            ahead.setSpan(new StyleSpan(Typeface.BOLD), st, en, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            views.setTextViewText(R.id.w_ahead, ahead);
            views.setTextColor(R.id.w_ahead, context.getColor(cMark));
            views.setViewVisibility(R.id.w_ahead, View.VISIBLE);
        }

        // 最近 + 天数
        views.setTextColor(R.id.w_mark_lbl, context.getColor(cLabel));
        views.setTextViewText(R.id.w_mark_name, w.markName);
        views.setTextColor(R.id.w_mark_name, context.getColor(cMark));
        views.setTextViewText(R.id.w_days_num, w.markDaysNum);
        views.setTextColor(R.id.w_days_num, context.getColor(cDays));
        views.setTextViewText(R.id.w_days_unit, w.markDaysUnit);
        views.setTextColor(R.id.w_days_unit, context.getColor(cSub));

        // 水纹（浅色）/ 细分隔线（深色）
        if (night) {
            views.setViewVisibility(R.id.w_ripple, View.GONE);
            views.setViewVisibility(R.id.w_divider, View.VISIBLE);
            views.setInt(R.id.w_divider, "setBackgroundColor", context.getColor(cLine));
        } else {
            views.setViewVisibility(R.id.w_divider, View.GONE);
            views.setViewVisibility(R.id.w_ripple, View.VISIBLE);
            views.setImageViewResource(R.id.w_ripple, R.drawable.widget_ripple);
        }

        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.w_root, pi);

        manager.updateAppWidget(id, views);
    }
}
