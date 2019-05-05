package mr.minecraft15.onlinetime.common;

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

    private Localization getTestLocalization() {
        Localization localization = mock(Localization.class);
        when(localization.getRawMessage("unit.second.singular")).thenReturn("S_ONE");
        when(localization.getRawMessage("unit.second.plural")).thenReturn("S_MANY");
        when(localization.getRawMessage("unit.minute.singular")).thenReturn("MIN_ONE");
        when(localization.getRawMessage("unit.minute.plural")).thenReturn("MIN_MANY");
        when(localization.getRawMessage("unit.hour.singular")).thenReturn("H_ONE");
        when(localization.getRawMessage("unit.hour.plural")).thenReturn("H_MANY");
        when(localization.getRawMessage("unit.day.singular")).thenReturn("D_ONE");
        when(localization.getRawMessage("unit.day.plural")).thenReturn("D_MANY");
        when(localization.getRawMessage("unit.week.singular")).thenReturn("W_ONE");
        when(localization.getRawMessage("unit.week.plural")).thenReturn("W_MANY");
        when(localization.getRawMessage("unit.month.singular")).thenReturn("M_ONE");
        when(localization.getRawMessage("unit.month.plural")).thenReturn("M_MANY");
        when(localization.getRawMessage("unit.year.singular")).thenReturn("Y_ONE");
        when(localization.getRawMessage("unit.year.plural")).thenReturn("Y_MANY");
        return localization;
    }

    @Test
    public void testFormatTime() {
        Localization localization = getTestLocalization();
        assertEquals("0 S_MANY", formatTime(0L, localization));
        assertEquals("1 S_ONE", formatTime(S, localization));
        assertEquals("2 S_MANY", formatTime(2L * S, localization));
        assertEquals("1 MIN_ONE", formatTime(MIN, localization));
        assertEquals("2 MIN_MANY", formatTime(2L * MIN, localization));
        assertEquals("1 H_ONE", formatTime(H, localization));
        assertEquals("2 H_MANY", formatTime(2L * H, localization));
        assertEquals("1 D_ONE", formatTime(D, localization));
        assertEquals("2 D_MANY", formatTime(2L * D, localization));
        assertEquals("1 W_ONE", formatTime(W, localization));
        assertEquals("2 W_MANY", formatTime(2L * W, localization));
        assertEquals("1 M_ONE", formatTime(M, localization));
        assertEquals("2 M_MANY", formatTime(2L * M, localization));
        assertEquals("1 Y_ONE", formatTime(Y, localization));
        assertEquals("2 Y_MANY", formatTime(2L * Y, localization));

        assertEquals("4 W_MANY", formatTime(4L * W, localization));
        assertEquals("1 M_ONE 5 D_MANY", formatTime(5L * W, localization));

        assertEquals("59 S_MANY", formatTime(MIN - S, localization));
        assertEquals("59 MIN_MANY", formatTime(H - MIN, localization));
        assertEquals("59 MIN_MANY 59 S_MANY", formatTime(H - S, localization));
        assertEquals("23 H_MANY", formatTime(D - H, localization));
        assertEquals("23 H_MANY 59 MIN_MANY 59 S_MANY", formatTime(D - S, localization));
        assertEquals("6 D_MANY", formatTime(W - D, localization));
        assertEquals("6 D_MANY 23 H_MANY 59 MIN_MANY 59 S_MANY", formatTime(W - S, localization));
        assertEquals("4 W_MANY 1 D_ONE", formatTime(M - D, localization));
        assertEquals("4 W_MANY 1 D_ONE 23 H_MANY 59 MIN_MANY 59 S_MANY", formatTime(M - S, localization));
        assertEquals("11 M_MANY", formatTime(Y - M, localization));
        assertEquals("11 M_MANY 4 W_MANY 1 D_ONE 23 H_MANY 59 MIN_MANY 59 S_MANY", formatTime(Y - S, localization));

        assertEquals("-1 H_MANY -1 MIN_MANY -1 S_MANY", formatTime(- H - MIN - S, localization));
    }
}