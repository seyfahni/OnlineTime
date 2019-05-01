package mr.minecraft15.onlinetime;

public final class TimeUtil {

    public static String formatTime(long seconds, Lang lang) {
        long sec = seconds;

        long min = sec / 60;
        sec %= 60;

        long h = min / 60;
        min %= 60;

        long d = h / 24;
        h %= 24;

        long m = d / 30;
        d %= 30;

        long y = m / 12;
        m %= 12;

        long w = d / 7;
        d %= 7;

        String secStr =            sec + " " + (sec == 1 ? lang.getMessage("unit.second.singular") : lang.getMessage("unit.second.plural"))       ;
        String minStr = min != 0 ? min + " " + (min == 1 ? lang.getMessage("unit.minute.singular") : lang.getMessage("unit.minute.plural")) : null;
        String hStr   = h   != 0 ? h   + " " + (h   == 1 ? lang.getMessage("unit.hour.singular")   : lang.getMessage("unit.hour.plural"))   : null;
        String dStr   = d   != 0 ? d   + " " + (d   == 1 ? lang.getMessage("unit.day.singular")    : lang.getMessage("unit.day.plural"))    : null;
        String wStr   = w   != 0 ? w   + " " + (w   == 1 ? lang.getMessage("unit.week.singular")   : lang.getMessage("unit.week.plural"))   : null;
        String mStr   = m   != 0 ? m   + " " + (m   == 1 ? lang.getMessage("unit.month.singular")  : lang.getMessage("unit.month.plural"))  : null;
        String yStr   = y   != 0 ? y   + " " + (y   == 1 ? lang.getMessage("unit.year.singular")   : lang.getMessage("unit.year.plural"))   : null;

        String r = (yStr   == null ? "" : yStr   + " ")
                 + (mStr   == null ? "" : mStr   + " ")
                 + (wStr   == null ? "" : wStr   + " ")
                 + (dStr   == null ? "" : dStr   + " ")
                 + (hStr   == null ? "" : hStr   + " ")
                 + (minStr == null ? "" : minStr + " ")
                 + (sec == 0 && seconds != 0 ? "" : secStr + " ");
        return r.substring(0, r.length() - 1);
    }

    private TimeUtil() {}
}
