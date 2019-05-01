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
