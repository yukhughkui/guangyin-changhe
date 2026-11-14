package cloud.yundu.changhe;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** 原生壳：整页复用长河.html，另把设置与刻度日期同步给系统小组件。 */
public class MainActivity extends Activity {

    private static final String BASE = "https://changhe.local/";
    private static final String PREFS_NIGHT = "changhe_night";
    private static final String KEY_NIGHT = "night";
    private WebView web;
    private boolean pageReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        // 关掉 WebView 默认画在点击元素外的橙色焦点框
        web.setDefaultFocusHighlightEnabled(false);
        setContentView(web);
        // 启动即按已存主题设置系统栏，避免夜模式下短暂闪白
        applyTheme(getSharedPreferences(PREFS_NIGHT, MODE_PRIVATE).getBoolean(KEY_NIGHT, false));

        // 常驻通知：申请权限并按设置排程 / 拉起前台服务
        requestNotifyPermission();
        applyNotify(ChangheData.load(this).notifyEnabled);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setTextZoom(100);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        s.setUseWideViewPort(false);
        s.setLoadWithOverviewMode(false);
        s.setMediaPlaybackRequiresUserGesture(true);

        web.addJavascriptInterface(new Bridge(this), "ChangheBridge");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                sync();
            }
        });

        try {
            web.loadDataWithBaseURL(BASE, readAsset(), "text/html", "UTF-8", BASE);
        } catch (Exception e) {
            web.loadUrl("file:///android_asset/changhe.html");
        }
    }

    private String readAsset() throws Exception {
        InputStream in = getAssets().open("changhe.html");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pageReady) sync();
    }

    @Override
    protected void onPause() {
        sync();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    /** 从网页内存里取设置与未来三年的刻度，交给原生侧。 */
    void sync() {
        if (web == null || !pageReady) return;
        web.evaluateJavascript(SYNC_JS, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                if (value == null || "null".equals(value)) return;
                try {
                    Object parsed = new JSONTokener(value).nextValue();
                    if (!(parsed instanceof String)) return;
                    String json = (String) parsed;
                    if (!json.startsWith("{") || json.contains("\"error\"")) return;
                    SharedPreferences.Editor e =
                            getSharedPreferences(ChangheData.PREF, Context.MODE_PRIVATE).edit();
                    e.putString(ChangheData.KEY_SNAPSHOT, json);
                    e.putLong(ChangheData.KEY_SYNC_TIME, System.currentTimeMillis());
                    e.apply();
                    ChangheWidget.refresh(MainActivity.this);
                    applyNotify(ChangheData.load(MainActivity.this).notifyEnabled);
                } catch (Exception ignored) {
                }
            }
        });
    }

    public static final class Bridge {
        private final MainActivity activity;

        Bridge(MainActivity a) {
            activity = a;
        }

        @JavascriptInterface
        public void onSettingsChanged() {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.sync();
                }
            });
        }

        /** 网页切换昼夜主题后通知原生，让系统状态栏（时间/电量/信号）随之变暗/变亮。 */
        @JavascriptInterface
        public void onTheme(final boolean night) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.applyTheme(night);
                }
            });
        }

        /** 网页请求把指定尺寸的小组件钉到桌面（Android 8+ 走系统确认框）。 */
        @JavascriptInterface
        public void requestWidget(final String size) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.requestWidget(size);
                }
            });
        }
    }

    /** 请求把指定尺寸的小组件钉到桌面。Android 8+ 由系统弹一次确认框；
     *  不支持时退化为提示用户手动添加。出于系统安全，任何 App 都不能静默往桌面塞小组件。 */
    void requestWidget(String size) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            toast("请在桌面长按空白处，从「小组件」里选择「岁月长河」");
            return;
        }
        AppWidgetManager awm = AppWidgetManager.getInstance(this);
        if (awm == null || !awm.isRequestPinAppWidgetSupported()) {
            toast("请长按桌面空白处，从「小组件」里选择「岁月长河」");
            return;
        }
        ComponentName cn = "small".equals(size)
                ? new ComponentName(this, ChangheWidgetSmall.class)
                : new ComponentName(this, ChangheWidget.class);
        try {
            awm.requestPinAppWidget(cn, null, null);
        } catch (Exception e) {
            toast("无法自动添加，请在桌面长按添加「岁月长河」小组件");
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    /** 夜模式同步系统状态栏（时间/电量/信号）：白天浅底深字，夜晚深底浅字。 */
    void applyTheme(boolean night) {
        getSharedPreferences(PREFS_NIGHT, MODE_PRIVATE).edit().putBoolean(KEY_NIGHT, night).apply();

        Window window = getWindow();
        int bg = night ? 0xFF0B110F : 0xFFF4F5F1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(bg);
            window.setNavigationBarColor(bg);
        }
        // 浅底时用深图标（不置位），深底时用浅图标（置位）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && window.getInsetsController() != null) {
            int appearance = night ? 0
                    : (WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                       | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            window.getInsetsController().setSystemBarsAppearance(appearance,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                            | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        } else {
            int flags = window.getDecorView().getSystemUiVisibility();
            if (night) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            } else {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            }
            window.getDecorView().setSystemUiVisibility(flags);
        }
    }

    /** 申请通知权限（Android 13+ 才需要）；旧版本默认授予，无需弹窗。 */
    private void requestNotifyPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1001);
            }
        }
    }

    /** 按开关排程每日 7:30 闹钟，并在授权且处于当日活动时段时拉起常驻服务。 */
    private void applyNotify(boolean enabled) {
        if (enabled) {
            NotifyScheduler.arm(this);
            if (NotifyScheduler.hasNotifyPermission(this) && inActiveWindow()) {
                try {
                    startForegroundService(new Intent(this, ChangheNotifyService.class));
                } catch (Exception ignored) {
                }
            }
        } else {
            NotifyScheduler.cancel(this);
            stopService(new Intent(this, ChangheNotifyService.class));
        }
    }

    /** 活动时段：7:30 至 23:59，贴合「每天 7:30 起常驻」的设定。 */
    private boolean inActiveWindow() {
        java.time.LocalTime now = java.time.LocalTime.now();
        return !now.isBefore(java.time.LocalTime.of(7, 30))
                && now.isBefore(java.time.LocalTime.of(23, 59));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted && inActiveWindow()) {
                try {
                    startForegroundService(new Intent(this, ChangheNotifyService.class));
                } catch (Exception ignored) {
                }
            }
        }
    }

    /** 页面内部导出的快照函数，能访问 IIFE 里的设置与节日表。 */
    private static final String SYNC_JS = "(function(){try{"
            + "return window.ChangheSnapshot?window.ChangheSnapshot():JSON.stringify({error:'no snapshot'});"
            + "}catch(e){return JSON.stringify({error:String(e)});}})()";
}
