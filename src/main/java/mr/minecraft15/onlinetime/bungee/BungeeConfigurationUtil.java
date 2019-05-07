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

package mr.minecraft15.onlinetime.bungee;

import net.md_5.bungee.config.Configuration;

import java.util.HashMap;
import java.util.Map;

public final class BungeeConfigurationUtil {

    public static Map<String, ?> readAllRecursive(Configuration storage, String basePath) {
        String subPathPrefix = basePath == null || basePath.isEmpty() ? "" : basePath + ".";
        Map<String, Object> data = new HashMap<>();
        for (String key : storage.getKeys()) {
            Object part = storage.get(key, null);
            if (part instanceof Configuration) {
                data.putAll(readAllRecursive((Configuration) part,subPathPrefix + key));
            } else  {
                data.put(subPathPrefix + key, part);
            }
        }
        return data;
    }

    private BungeeConfigurationUtil() {}
}
