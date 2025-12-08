package shake1227.rpgsetupscreen.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.PacketDistributor;
import shake1227.modernnotification.core.NotificationCategory;
import shake1227.modernnotification.core.NotificationType;
import shake1227.modernnotification.log.LogManager;
import shake1227.modernnotification.log.NotificationData;
import shake1227.modernnotification.network.PacketHandler;
import shake1227.modernnotification.network.S2CNotificationPacket;
import shake1227.modernnotification.notification.Notification;
import shake1227.modernnotification.notification.NotificationManager;
import shake1227.modernnotification.util.TextFormattingUtils;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class ModernNotificationHandler {
    public static final boolean IS_LOADED = ModList.get().isLoaded("modernnotification");

    public static void sendServerNotification(ServerPlayer player, String translationKey, List<String> args, String category) {
        if (!IS_LOADED) {
            player.displayClientMessage(Component.translatable(translationKey, args.toArray()), true);
            return;
        }

        try {
            NotificationCategory cat = NotificationCategory.SYSTEM;
            if ("success".equalsIgnoreCase(category)) cat = NotificationCategory.SUCCESS;
            else if ("warning".equalsIgnoreCase(category)) cat = NotificationCategory.WARNING;
            else if ("failure".equalsIgnoreCase(category)) cat = NotificationCategory.FAILURE;

            String messageToSend;
            if (args != null && !args.isEmpty()) {
                messageToSend = Component.translatable(translationKey, args.toArray()).getString();
            } else {
                messageToSend = translationKey;
            }

            List<Component> messageComponents = TextFormattingUtils.parseLegacyText(messageToSend);

            S2CNotificationPacket packet = new S2CNotificationPacket(
                    NotificationType.LEFT,
                    cat,
                    null,
                    messageComponents,
                    0
            );

            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);

        } catch (Exception e) {
            e.printStackTrace();
            player.displayClientMessage(Component.translatable(translationKey, args.toArray()), true);
        }
    }

    public static void showClientNotification(String translationKey, String category) {
        if (!IS_LOADED) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(Component.translatable(translationKey), true);
            }
            return;
        }

        try {
            String translated = Component.translatable(translationKey).getString();
            String formatted = TextFormatter.formatForNotification(translated);

            NotificationCategory cat = NotificationCategory.SYSTEM;
            if ("success".equalsIgnoreCase(category)) cat = NotificationCategory.SUCCESS;
            else if ("warning".equalsIgnoreCase(category)) cat = NotificationCategory.WARNING;
            else if ("failure".equalsIgnoreCase(category)) cat = NotificationCategory.FAILURE;

            Notification notification = new Notification(
                    NotificationType.LEFT,
                    cat,
                    null,
                    TextFormattingUtils.parseLegacyText(formatted),
                    -1
            );

            NotificationManager.getInstance().getRenderer().calculateDynamicWidth(notification);
            NotificationManager.getInstance().addNotification(notification);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showTopRightNotification(String titleKey, String messageKey, List<String> args, String category, int duration) {
        if (!IS_LOADED) {
            Minecraft mc = Minecraft.getInstance();
            if(mc.player != null) {
                mc.player.displayClientMessage(Component.translatable(titleKey).withStyle(ChatFormatting.BOLD), false);
                mc.player.displayClientMessage(Component.translatable(messageKey, args.toArray()), false);
            }
            return;
        }

        try {
            NotificationCategory cat = NotificationCategory.SYSTEM;
            if ("success".equalsIgnoreCase(category)) cat = NotificationCategory.SUCCESS;
            else if ("warning".equalsIgnoreCase(category)) cat = NotificationCategory.WARNING;
            else if ("failure".equalsIgnoreCase(category)) cat = NotificationCategory.FAILURE;

            Component titleComp = Component.translatable(titleKey).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD);

            Object[] fmtArgs = args.stream().map(s -> Component.literal(s).withStyle(ChatFormatting.GOLD)).toArray();
            Component messageComp = Component.translatable(messageKey, fmtArgs).withStyle(ChatFormatting.YELLOW);

            List<Component> titleList = Collections.singletonList(titleComp);
            List<Component> messageList = Collections.singletonList(messageComp);

            Notification notification = new Notification(
                    NotificationType.TOP_RIGHT,
                    cat,
                    titleList,
                    messageList,
                    duration
            );

            LogManager.getInstance().addLog(new NotificationData(notification));

            NotificationManager.getInstance().getRenderer().calculateDynamicWidth(notification);
            NotificationManager.getInstance().addNotification(notification);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}