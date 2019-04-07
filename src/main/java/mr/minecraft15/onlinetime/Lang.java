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
