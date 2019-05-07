/*
 * MIT License
 *
 * Copyright (c) 2019 Niklas Seyfarth
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package mr.minecraft15.onlinetime.common;

public final class TimeUtil {

    public static String formatTime(long seconds, Localization localization) {
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

        String secStr =            sec + " " + (sec == 1 ? localization.getMessage("unit.second.singular") : localization.getMessage("unit.second.plural"))       ;
        String minStr = min != 0 ? min + " " + (min == 1 ? localization.getMessage("unit.minute.singular") : localization.getMessage("unit.minute.plural")) : null;
        String hStr   = h   != 0 ? h   + " " + (h   == 1 ? localization.getMessage("unit.hour.singular")   : localization.getMessage("unit.hour.plural"))   : null;
        String dStr   = d   != 0 ? d   + " " + (d   == 1 ? localization.getMessage("unit.day.singular")    : localization.getMessage("unit.day.plural"))    : null;
        String wStr   = w   != 0 ? w   + " " + (w   == 1 ? localization.getMessage("unit.week.singular")   : localization.getMessage("unit.week.plural"))   : null;
        String mStr   = m   != 0 ? m   + " " + (m   == 1 ? localization.getMessage("unit.month.singular")  : localization.getMessage("unit.month.plural"))  : null;
        String yStr   = y   != 0 ? y   + " " + (y   == 1 ? localization.getMessage("unit.year.singular")   : localization.getMessage("unit.year.plural"))   : null;

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
