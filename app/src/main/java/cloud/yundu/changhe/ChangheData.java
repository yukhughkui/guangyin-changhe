package cloud.yundu.changhe;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** 网页算好的刻度日期存成快照，原生侧只负责按今天重新取数与排版。 */
public final class ChangheData {

    public static final String PREF = "changhe_widget";
    public static final String KEY_SNAPSHOT = "snapshot";
    public static final String KEY_SYNC_TIME = "sync_time";

    private static final String DEFAULT_BIRTH = "2002-06-15";
    private static final int DEFAULT_HORIZON = 80;

    private ChangheData() {}

    public static final class Mark {
        public final String name;
        public final LocalDate date;

        Mark(String name, LocalDate date) {
            this.name = name;
            this.date = date;
        }
    }

    public static final class Snapshot {
        public LocalDate birth = LocalDate.parse(DEFAULT_BIRTH);
        public int horizon = DEFAULT_HORIZON;
        public boolean synced = false;
        public boolean notifyEnabled = true;
        public String notifyTime = "07:30";
        public final List<Mark> marks = new ArrayList<>();
    }

    public static Snapshot load(Context context) {
        Snapshot s = new Snapshot();
        SharedPreferences sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String json = sp.getString(KEY_SNAPSHOT, null);
        if (json == null) return s;
        try {
            JSONObject o = new JSONObject(json);
            s.birth = LocalDate.parse(o.optString("birth", DEFAULT_BIRTH));
            s.horizon = clampHorizon(o.optInt("horizon", DEFAULT_HORIZON));
            s.notifyEnabled = o.optBoolean("notify", true);
            s.notifyTime = o.optString("notifyTime", "07:30");
            JSONArray arr = o.optJSONArray("marks");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject m = arr.optJSONObject(i);
                    if (m == null) continue;
                    String n = m.optString("n", "");
                    String d = m.optString("d", "");
                    if (n.isEmpty() || d.isEmpty()) continue;
                    try {
                        s.marks.add(new Mark(n, LocalDate.parse(d)));
                    } catch (Exception ignored) {
                    }
                }
            }
            s.synced = true;
        } catch (Exception ignored) {
        }
        Collections.sort(s.marks, (a, b) -> a.date.compareTo(b.date));
        return s;
    }

    /**
     * 四段数值文本，顺序与 widget 布局中的数值控件一一对应：
     * [0] 已在此间数值 / [1] 前方尚有数值 / [2] 最近事项名称 / [3] 最近事项天数。
     * 标签（已在此间 / 前方尚有 / 最近）为固定文案，写在布局 XML 里并加粗着色强调。
     */
    public static String[] texts(Context context) {
        Snapshot s = load(context);
        LocalDate today = LocalDate.now();
        LocalDate end = safePlusYears(s.birth, s.horizon);

        Period lived = betweenNonNegative(s.birth, today);
        Period ahead = betweenNonNegative(today, end);

        String livedVal = durSentence(lived);
        String aheadVal;
        if (!today.isBefore(end)) {
            aheadVal = "且看此刻";
        } else {
            aheadVal = durSentence(ahead);
        }

        String markName = "未设置";
        String markDays = "—";
        if (s.synced) {
            for (Mark m : s.marks) {
                if (m.date.isBefore(today)) continue;
                long d = ChronoUnit.DAYS.between(today, m.date);
                markName = m.name;
                markDays = (d == 0) ? "就在今日" : d + " 天";
                break;
            }
        }

        return new String[]{livedVal, aheadVal, markName, markDays};
    }

    /** 分组件演示用的结构化数据，与布局中的控件一一对应。 */
    public static final class WidgetInfo {
        public String livedYears = "0";   // "24"
        public String livedUnit = "年";    // "年"
        public String livedSub = "";       // "4 个月 30 天"
        public String aheadYears = "0";    // "55"
        public String aheadRest = "";      // " 年 零 7 个月"
        public String markName = "未设置"; // "元旦"
        public String markDaysNum = "—";   // "47"
        public String markDaysUnit = "";   // "天"
    }

    /**
     * 计算小组件所需的分项数据：大数（年）、次行（月日）、前方尚有（年+余）、最近事项（名+天数）。
     * 主题（素白/夜航）由 ChangheWidget 在运行时按系统日/夜模式选择，这里只给纯数值。
     */
    public static WidgetInfo widget(Context context) {
        Snapshot s = load(context);
        LocalDate today = LocalDate.now();
        LocalDate end = safePlusYears(s.birth, s.horizon);

        Period lived = betweenNonNegative(s.birth, today);
        Period ahead = betweenNonNegative(today, end);

        WidgetInfo w = new WidgetInfo();
        w.livedYears = String.valueOf(lived.getYears());
        w.livedUnit = "年";
        w.livedSub = subSentence(lived);

        if (!today.isBefore(end)) {
            w.aheadYears = "且看";
            w.aheadRest = "此刻";
        } else {
            w.aheadYears = String.valueOf(ahead.getYears());
            w.aheadRest = " 年 零 " + ahead.getMonths() + " 个月"
                    + (ahead.getDays() > 0 ? " " + ahead.getDays() + " 天" : "");
        }

        if (s.synced) {
            for (Mark m : s.marks) {
                if (m.date.isBefore(today)) continue;
                long d = ChronoUnit.DAYS.between(today, m.date);
                w.markName = m.name;
                if (d == 0) {
                    w.markDaysNum = "今日";
                    w.markDaysUnit = "";
                } else {
                    w.markDaysNum = String.valueOf(d);
                    w.markDaysUnit = "天";
                }
                break;
            }
        }
        return w;
    }

    /** 把一段时间写成「4 个月 30 天」（零值段省略；恰好整年时为「整」）。 */
    private static String subSentence(Period p) {
        StringBuilder sb = new StringBuilder();
        if (p.getMonths() > 0) sb.append(p.getMonths()).append(" 个月 ");
        if (p.getDays() > 0) sb.append(p.getDays()).append(" 天");
        if (sb.length() == 0) sb.append("整");
        return sb.toString().trim();
    }

    /** 把一段时间写成「X 年零 X 个月 X 天」（零仅在有剩余时出现，零值段省略）。 */
    private static String durSentence(Period p) {
        StringBuilder sb = new StringBuilder();
        sb.append(p.getYears()).append(" 年");
        if (p.getMonths() > 0 || p.getDays() > 0) {
            sb.append(" 零 ");
            if (p.getMonths() > 0) sb.append(p.getMonths()).append(" 个月 ");
            if (p.getDays() > 0) sb.append(p.getDays()).append(" 天");
        }
        return sb.toString().trim();
    }

    private static int clampHorizon(int h) {
        return (h < 1 || h > 130) ? DEFAULT_HORIZON : h;
    }

    private static LocalDate safePlusYears(LocalDate b, int years) {
        try {
            return b.plusYears(years);
        } catch (Exception e) {
            return b.plusYears(DEFAULT_HORIZON);
        }
    }

    private static Period betweenNonNegative(LocalDate a, LocalDate b) {
        return b.isBefore(a) ? Period.ZERO : Period.between(a, b);
    }

    /** 最近节点的实时倒计时（月/天/小时/分）与寄语分级。 */
    public static final class Countdown {
        public boolean valid = false;
        public String name = "未设置";
        public long months = 0;
        public long days = 0;
        public long hours = 0;
        public long minutes = 0;
        public long totalDays = 0;
    }

    /** 在全部已同步的刻度里，挑出此刻之后最近的那个，算出到当天零点的倒计时。 */
    public static Countdown nearestCountdown(Context context) {
        Snapshot s = load(context);
        Countdown c = new Countdown();
        if (!s.synced) return c;
        LocalDateTime now = LocalDateTime.now();
        long bestMs = Long.MAX_VALUE;
        Mark bestMark = null;
        for (Mark m : s.marks) {
            LocalDateTime target = LocalDateTime.of(m.date, LocalTime.MIDNIGHT);
            if (!target.isAfter(now)) continue;
            long ms = java.time.Duration.between(now, target).toMillis();
            if (ms < bestMs) { bestMs = ms; bestMark = m; }
        }
        if (bestMark == null) return c;
        c.valid = true;
        c.name = bestMark.name;
        c.totalDays = bestMs / 86400000L;
        LocalDateTime target = LocalDateTime.of(bestMark.date, LocalTime.MIDNIGHT);
        long[] parts = borrow(now, target);
        c.months = parts[0];
        c.days = parts[1];
        c.hours = parts[2];
        c.minutes = parts[3];
        return c;
    }

    /** 两个时刻之间的日历借位差：返回 [月, 天, 小时, 分]（年并入月）。 */
    private static long[] borrow(LocalDateTime a, LocalDateTime b) {
        int y = b.getYear() - a.getYear();
        int mo = b.getMonthValue() - a.getMonthValue();
        int d = b.getDayOfMonth() - a.getDayOfMonth();
        int h = b.getHour() - a.getHour();
        int mi = b.getMinute() - a.getMinute();
        if (mi < 0) { mi += 60; h--; }
        if (h < 0) { h += 24; d--; }
        if (d < 0) {
            LocalDate prev = b.toLocalDate().minusDays(b.getDayOfMonth());
            d += prev.getDayOfMonth();
            mo--;
        }
        if (mo < 0) { mo += 12; y--; }
        long totalMonths = (long) y * 12 + mo;
        return new long[]{totalMonths, d, h, mi};
    }
}
