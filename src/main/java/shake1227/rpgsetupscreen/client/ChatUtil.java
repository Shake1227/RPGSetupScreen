package shake1227.rpgsetupscreen.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public class ChatUtil {

    public static MutableComponent translate(String key, Object... args) {
        return parse(I18n.get(key, args));
    }

    public static MutableComponent parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.literal("");
        }

        MutableComponent root = Component.literal("");
        Style currentStyle = Style.EMPTY;
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '&' && i + 1 < text.length()) {
                if (buffer.length() > 0) {
                    root.append(Component.literal(buffer.toString()).withStyle(currentStyle));
                    buffer.setLength(0);
                }

                char code = text.charAt(i + 1);
                ChatFormatting format = ChatFormatting.getByCode(code);

                if (format != null) {
                    if (format.isColor()) {
                        currentStyle = Style.EMPTY.withColor(format);
                    } else if (format == ChatFormatting.RESET) {
                        currentStyle = Style.EMPTY;
                    } else {
                        currentStyle = currentStyle.applyFormat(format);
                    }
                } else {
                    buffer.append("&");
                    buffer.append(code);
                }

                i++;
            } else {
                buffer.append(c);
            }
        }

        if (buffer.length() > 0) {
            root.append(Component.literal(buffer.toString()).withStyle(currentStyle));
        }

        return root;
    }
}