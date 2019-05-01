package mr.minecraft15.onlinetime;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class TimeParser {

    private static final String LONG_REGEX = "(\\+|-)?[0-9]+";
    private static final Pattern LONG_PATTERN = Pattern.compile(LONG_REGEX);
    private static final Pattern UNIT_PATTERN = Pattern.compile("[a-z]+");

    public static Builder builder() {
        return new Builder();
    }

    private final Map<String, Long> units;
    private final Pattern globalPattern;


    private TimeParser(Map<String, Long> units) {
        this.units = units;
        String unitRegex = units.keySet().stream().collect(Collectors.joining("|", "(", ")"));
        String globalRegex = "(" + LONG_REGEX + "\\s*" + unitRegex + "\\s*)*(" + LONG_REGEX + ")?";
        this.globalPattern = Pattern.compile(globalRegex);
    }

    public OptionalLong parseToSeconds(String representation) {
        String raw = representation.trim().toLowerCase(Locale.ROOT);
        if (!globalPattern.matcher(raw).matches()) {
            return OptionalLong.empty();
        }
        int position = 0;
        long time = 0L;

        Matcher longMatcher = LONG_PATTERN.matcher(raw);
        Matcher unitMatcher = UNIT_PATTERN.matcher(raw);
        while (position < raw.length()) {
            longMatcher.find(position);
            position = longMatcher.end();
            long unitTime = Long.parseLong(longMatcher.group());
            if (position < raw.length()) {
                unitMatcher.find(position);
                position = unitMatcher.end();
                long unitFactor = units.get(unitMatcher.group());
                time += unitTime * unitFactor;
            } else {
                time += unitTime;
            }
        }
        return OptionalLong.of(time);
    }

    public static class Builder {


        private final Map<String, Long> units;

        private Builder() {
            this.units = new HashMap<>();
        }

        public Builder addUnit(long inSeconds, String... unitSymbols) {
            if (unitSymbols != null) {
                for (String symbol : unitSymbols) {
                    if (!UNIT_PATTERN.matcher(symbol).matches()) {
                        throw new IllegalArgumentException("invalid unit symbol: " + symbol);
                    }
                    if (units.keySet().contains(symbol.toLowerCase(Locale.ROOT))) {
                        throw new IllegalArgumentException("duplicate unit symbol: " + symbol);
                    }
                }
                for (String symbol : unitSymbols) {
                    units.put(symbol.toLowerCase(Locale.ROOT), inSeconds);
                }
            }
            return this;
        }

        public TimeParser build() {
            return new TimeParser(units);
        }
    }
}
