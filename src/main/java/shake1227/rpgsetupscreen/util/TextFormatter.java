package shake1227.rpgsetupscreen.util;

public class TextFormatter {
    public static String formatForNotification(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        int lineLength = 0;
        boolean inQuote = false;
        String prohibited = "」』）)]}、。";

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            sb.append(c);
            lineLength++;

            if (c == '「') inQuote = true;
            if (c == '」') inQuote = false;

            boolean shouldBreak = false;

            if (lineLength >= 20) shouldBreak = true;
            else if (c == '。' || c == '、') shouldBreak = true;

            if (shouldBreak) {
                if (i + 1 < text.length()) {
                    char next = text.charAt(i + 1);
                    if (prohibited.indexOf(next) >= 0) continue;
                    if (inQuote && lineLength >= 20 && c != '。' && c != '、') continue;

                    sb.append("&u");
                    lineLength = 0;
                }
            }
        }
        return sb.toString();
    }
}