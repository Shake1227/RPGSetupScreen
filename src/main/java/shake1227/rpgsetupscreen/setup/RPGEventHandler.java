package shake1227.rpgsetupscreen.setup;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import shake1227.rpgsetupscreen.RPGSetupScreen;
import shake1227.rpgsetupscreen.network.RPGNetwork;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class RPGEventHandler {

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(RPGCapability.INSTANCE).isPresent()) {
                event.addCapability(new ResourceLocation(RPGSetupScreen.MODID, "rpg_data"), new RPGCapability.Provider());
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RPGNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new RPGNetwork.PacketSyncConfig(
                            RPGConfig.ENABLE_GENDER.get(), RPGConfig.ENABLE_WIDTH.get(), RPGConfig.ENABLE_HEIGHT.get(),
                            RPGConfig.ENABLE_CHEST_SIZE.get(), RPGConfig.ENABLE_CHEST_Y.get(), RPGConfig.ENABLE_CHEST_SEP.get(),
                            RPGConfig.ENABLE_CHEST_ANG.get(), RPGConfig.ENABLE_PHYSICS.get()
                    ));

            RPGScreenManager manager = RPGScreenManager.get(player.server);
            RPGNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RPGNetwork.PacketSyncScreens(manager.screens));
            try {
                File serverConfigDir = player.server.getWorldPath(new LevelResource("serverconfig")).toFile();
                File logoFile = new File(serverConfigDir, "rpgscreen_logo.png");

                if (logoFile.exists() && logoFile.isFile()) {
                    try (FileInputStream fis = new FileInputStream(logoFile)) {
                        byte[] data = fis.readAllBytes();
                        if (data.length > 0 && data.length < 10485760) {
                            RPGNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RPGNetwork.PacketSyncLogo(data));
                        } else {
                            System.out.println("[RPGSetupScreen] Logo file is too large (>10MB) or empty: " + data.length + " bytes");
                            RPGNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RPGNetwork.PacketSyncLogo(new byte[0]));
                        }
                    }
                } else {
                    RPGNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RPGNetwork.PacketSyncLogo(new byte[0]));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            RPGCommands.ResetData resetData = RPGCommands.ResetData.get(player.serverLevel());
            boolean shouldReset = resetData.remove(player.getUUID());

            player.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
                if (shouldReset) cap.setFinished(false);
                RPGNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new RPGNetwork.PacketSyncData(player.getId(), cap.isFinished(), cap.getGender(), cap.getWidth(), cap.getHeight(), cap.getChest(), cap.getChestY(), cap.getChestSep(), cap.getChestAng(), cap.isPhysicsEnabled()));

                if (!cap.isFinished()) {
                    player.setGameMode(GameType.SPECTATOR);
                    RPGNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RPGNetwork.PacketForceReset());
                }
            });
        }
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer tracker) {
            target.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
                RPGNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> tracker),
                        new RPGNetwork.PacketSyncData(target.getId(), cap.isFinished(), cap.getGender(), cap.getWidth(), cap.getHeight(), cap.getChest(), cap.getChestY(), cap.getChestSep(), cap.getChestAng(), cap.isPhysicsEnabled()));
            });
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().getCapability(RPGCapability.INSTANCE).ifPresent(oldCap ->
                event.getEntity().getCapability(RPGCapability.INSTANCE).ifPresent(newCap ->
                        newCap.copyFrom(oldCap)
                )
        );
    }
}