package cloud.yundu.changhe;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
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

    /** 五段文本，顺序与 widget_changhe.xml 中的控件一一对应。 */
    public static String[] texts(Context context) {
        Snapshot s = load(context);
        LocalDate today = LocalDate.now();
        LocalDate end = safePlusYears(s.birth, s.horizon);

        Period lived = betweenNonNegative(s.birth, today);
        Period ahead = betweenNonNegative(today, end);
        long livedDays = Math.max(0, ChronoUnit.DAYS.between(s.birth, today));

        String aheadText;
        if (!today.isBefore(end)) {
            aheadText = "前方尚有 · 且看此刻";
        } else if (ahead.getYears() > 0) {
            aheadText = "前方尚有 " + ahead.getYears() + " 年";
        } else {
            aheadText = "前方尚有 " + Math.max(1, ahead.getMonths()) + " 月";
        }

        String livedNum = String.valueOf(lived.getYears());
        StringBuilder livedRest = new StringBuilder();
        if (lived.getMonths() > 0 || lived.getDays() > 0) {
            livedRest.append("零 ");
            if (lived.getMonths() > 0) livedRest.append(lived.getMonths()).append(" 个月 ");
            if (lived.getDays() > 0) livedRest.append(lived.getDays()).append(" 天");
        }

        String markName = "最近 · 打开岁月长河同步一次";
        String markDays = "—";
        if (s.synced) {
            for (Mark m : s.marks) {
                if (m.date.isBefore(today)) continue;
                long d = ChronoUnit.DAYS.between(today, m.date);
                markName = "最近 · " + m.name;
                markDays = (d == 0) ? "就在今日" : "还剩 " + d + " 天";
                break;
            }
        }

        return new String[]{aheadText, livedNum, livedRest.toString(), markName, markDays};
    }

    /** 把一段时间写成倒计时语：N 年 M 个月 D 天（不补零）。 */
    private static String durText(Period p) {
        return p.getYears() + " 年 " + p.getMonths() + " 个月 " + p.getDays() + " 天";
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
}
