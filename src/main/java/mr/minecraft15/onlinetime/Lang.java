/*
 * MIT License
 *
 * Copyright (c) 2018 Marvin Klar
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

import net.md_5.bungee.config.Configuration;

import java.util.HashMap;
import java.util.Map;

public class Lang {

    private final Map<String, Map<String, String>> messages;

    private final Configuration translations;
    private String defaultLanguage;

    public Lang(Configuration translationConfiguration, String defaultLanguage) {
        this.messages = new HashMap<>();
        this.translations = translationConfiguration;
        this.defaultLanguage = defaultLanguage;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public String getMessage(String messageKey) {
        return getMessage(messageKey, defaultLanguage);

    }

    public String getMessage(String messageKey, String language) {
        return this.messages
                .computeIfAbsent(language, key -> new HashMap<>())
                .computeIfAbsent(messageKey, this::loadMessage);

    }

    private String loadMessage(String messageKey) {
        return this.translations.getString(this.defaultLanguage + "." + messageKey);
    }
}
