package mr.minecraft15.onlinetime.common;

import mr.minecraft15.onlinetime.common.TimeParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TimeParserTest {

    @Test
    public void testParsing() {
        TimeParser parser = defaultTimeParser();
        assertEquals(0L, parser.parseToSeconds("").getAsLong());
        assertEquals(0L, parser.parseToSeconds("0").getAsLong());
        assertEquals(0L, parser.parseToSeconds("+0").getAsLong());
        assertEquals(0L, parser.parseToSeconds("-0").getAsLong());

        assertEquals(1L, parser.parseToSeconds("1").getAsLong());
        assertEquals(1L, parser.parseToSeconds("+1").getAsLong());
        assertEquals(-1L, parser.parseToSeconds("-1").getAsLong());

        assertEquals(1L, parser.parseToSeconds("1s").getAsLong());
        assertEquals(1L, parser.parseToSeconds("1 s").getAsLong());
        assertEquals(1L, parser.parseToSeconds("1                        s").getAsLong());
        assertEquals(1L, parser.parseToSeconds("1sec").getAsLong());
        assertEquals(1L, parser.parseToSeconds("1 seconds").getAsLong());
        assertEquals(1L, parser.parseToSeconds("1second").getAsLong());

        assertEquals(61L, parser.parseToSeconds("1min1sec").getAsLong());
        assertEquals(61L, parser.parseToSeconds("1 min1sec").getAsLong());
        assertEquals(61L, parser.parseToSeconds("1min 1sec").getAsLong());
        assertEquals(61L, parser.parseToSeconds("1min1 sec").getAsLong());
        assertEquals(61L, parser.parseToSeconds("1 min 1sec").getAsLong());
        assertEquals(61L, parser.parseToSeconds("1 min1 sec").getAsLong());
        assertEquals(61L, parser.parseToSeconds("1min 1 sec").getAsLong());
        assertEquals(61L, parser.parseToSeconds("1 min 1 sec").getAsLong());

        assertEquals(59L, parser.parseToSeconds("1 min -1 sec").getAsLong());
    }

    private TimeParser defaultTimeParser() {
        return TimeParser.builder()
                .addUnit(1, "second", "seconds", "sec", "s")
                .addUnit(60, "minute", "minutes", "min")
                .addUnit(60 * 60, "hour", "hours", "h")
                .addUnit(60 * 60 * 24, "day", "days", "d")
                .addUnit(60 * 60 * 24 * 7, "week", "weeks", "w")
                .addUnit(60 * 60 * 24 * 30, "month", "months", "m")
                .addUnit(60 * 60 * 24 * 30 * 12, "year", "years", "y")
                .build();
    }
}