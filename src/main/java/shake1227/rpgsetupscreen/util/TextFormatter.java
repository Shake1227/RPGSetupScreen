package shake1227.rpgsetupscreen.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextFormatter {

    public static String formatForNotification(String text) {
        if (text == null || text.isEmpty()) return "";

        StringBuilder formatted = new StringBuilder();
        int length = 0;
        boolean inBrackets = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            formatted.append(c);
            length++;

            if (c == '「') {
                inBrackets = true;
            } else if (c == '」') {
                inBrackets = false;
            }

            if (length >= 20 && (c == '、' || c == '。')) {
                if (!inBrackets) {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '」') {
                        continue;
                    }

                    formatted.append("&u");
                    length = 0;
                }
            }

            if (length >= 20 && c == '」' && !inBrackets) {
                formatted.append("&u");
                length = 0;
            }
        }

        return formatted.toString();
    }
}