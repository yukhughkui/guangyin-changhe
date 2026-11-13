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

    /** 四段文本，顺序与 widget 布局中的控件一一对应：已在此间 / 前方尚有 / 最近名称 / 最近天数。 */
    public static String[] texts(Context context) {
        return texts(context, false);
    }

    /** compact=true 时只取「年数」，用于 2×2 小尺寸，避免内容撑爆单元格而底部被裁掉。 */
    public static String[] texts(Context context, boolean compact) {
        Snapshot s = load(context);
        LocalDate today = LocalDate.now();
        LocalDate end = safePlusYears(s.birth, s.horizon);

        Period lived = betweenNonNegative(s.birth, today);
        Period ahead = betweenNonNegative(today, end);

        String livedText = "已在此间 " + (compact ? yearsOnly(lived) : durSentence(lived));
        String aheadText;
        if (!today.isBefore(end)) {
            aheadText = "前方尚有 · 且看此刻";
        } else {
            aheadText = "前方尚有 " + (compact ? yearsOnly(ahead) : durSentence(ahead));
        }

        String markName = "最近 · 打开光阴长河同步一次";
        String markDays = "—";
        if (s.synced) {
            for (Mark m : s.marks) {
                if (m.date.isBefore(today)) continue;
                long d = ChronoUnit.DAYS.between(today, m.date);
                markName = "最近 · " + m.name;
                markDays = (d == 0) ? "就在今日" : d + " 天";
                break;
            }
        }

        return new String[]{livedText, aheadText, markName, markDays};
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

    /** 只保留年数，如「24 年」，给 2×2 小部件用。 */
    private static String yearsOnly(Period p) {
        return p.getYears() + " 年";
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
