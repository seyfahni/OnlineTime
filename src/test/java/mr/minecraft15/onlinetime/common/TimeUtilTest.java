package mr.minecraft15.onlinetime.common;

import mr.minecraft15.onlinetime.common.Lang;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import static mr.minecraft15.onlinetime.common.TimeUtil.*;

public class TimeUtilTest {

    private static final long S = 1L;
    private static final long MIN = 60L * S;
    private static final long H = 60L * MIN;
    private static final long D = 24L * H;
    private static final long W = 7L * D;
    private static final long M = 30L * D;
    private static final long Y = 12L * M;

    private Lang getTestLang() {
        Lang lang = mock(Lang.class);
        when(lang.getMessage("unit.second.singular")).thenReturn("S_ONE");
        when(lang.getMessage("unit.second.plural")).thenReturn("S_MANY");
        when(lang.getMessage("unit.minute.singular")).thenReturn("MIN_ONE");
        when(lang.getMessage("unit.minute.plural")).thenReturn("MIN_MANY");
        when(lang.getMessage("unit.hour.singular")).thenReturn("H_ONE");
        when(lang.getMessage("unit.hour.plural")).thenReturn("H_MANY");
        when(lang.getMessage("unit.day.singular")).thenReturn("D_ONE");
        when(lang.getMessage("unit.day.plural")).thenReturn("D_MANY");
        when(lang.getMessage("unit.week.singular")).thenReturn("W_ONE");
        when(lang.getMessage("unit.week.plural")).thenReturn("W_MANY");
        when(lang.getMessage("unit.month.singular")).thenReturn("M_ONE");
        when(lang.getMessage("unit.month.plural")).thenReturn("M_MANY");
        when(lang.getMessage("unit.year.singular")).thenReturn("Y_ONE");
        when(lang.getMessage("unit.year.plural")).thenReturn("Y_MANY");
        return lang;
    }

    @Test
    public void testFormatTime() {
        Lang lang = getTestLang();
        assertEquals("0 S_MANY", formatTime(0L, lang));
        assertEquals("1 S_ONE", formatTime(S, lang));
        assertEquals("2 S_MANY", formatTime(2L * S, lang));
        assertEquals("1 MIN_ONE", formatTime(MIN, lang));
        assertEquals("2 MIN_MANY", formatTime(2L * MIN, lang));
        assertEquals("1 H_ONE", formatTime(H, lang));
        assertEquals("2 H_MANY", formatTime(2L * H, lang));
        assertEquals("1 D_ONE", formatTime(D, lang));
        assertEquals("2 D_MANY", formatTime(2L * D, lang));
        assertEquals("1 W_ONE", formatTime(W, lang));
        assertEquals("2 W_MANY", formatTime(2L * W, lang));
        assertEquals("1 M_ONE", formatTime(M, lang));
        assertEquals("2 M_MANY", formatTime(2L * M, lang));
        assertEquals("1 Y_ONE", formatTime(Y, lang));
        assertEquals("2 Y_MANY", formatTime(2L * Y, lang));

        assertEquals("4 W_MANY", formatTime(4L * W, lang));
        assertEquals("1 M_ONE 5 D_MANY", formatTime(5L * W, lang));

        assertEquals("59 S_MANY", formatTime(MIN - S, lang));
        assertEquals("59 MIN_MANY", formatTime(H - MIN, lang));
        assertEquals("59 MIN_MANY 59 S_MANY", formatTime(H - S, lang));
        assertEquals("23 H_MANY", formatTime(D - H, lang));
        assertEquals("23 H_MANY 59 MIN_MANY 59 S_MANY", formatTime(D - S, lang));
        assertEquals("6 D_MANY", formatTime(W - D, lang));
        assertEquals("6 D_MANY 23 H_MANY 59 MIN_MANY 59 S_MANY", formatTime(W - S, lang));
        assertEquals("4 W_MANY 1 D_ONE", formatTime(M - D, lang));
        assertEquals("4 W_MANY 1 D_ONE 23 H_MANY 59 MIN_MANY 59 S_MANY", formatTime(M - S, lang));
        assertEquals("11 M_MANY", formatTime(Y - M, lang));
        assertEquals("11 M_MANY 4 W_MANY 1 D_ONE 23 H_MANY 59 MIN_MANY 59 S_MANY", formatTime(Y - S, lang));

        assertEquals("-1 H_MANY -1 MIN_MANY -1 S_MANY", formatTime(- H - MIN - S, lang));
    }
}