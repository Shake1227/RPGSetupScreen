package shake1227.rpgsetupscreen.setup;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import shake1227.rpgsetupscreen.RPGSetupScreen;
import shake1227.rpgsetupscreen.network.RPGNetwork;

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
            // サーバー設定（制限）を同期
            RPGNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new RPGNetwork.PacketSyncConfig(
                            RPGConfig.ENABLE_GENDER.get(),
                            RPGConfig.ENABLE_WIDTH.get(),
                            RPGConfig.ENABLE_HEIGHT.get(),
                            RPGConfig.ENABLE_CHEST_SIZE.get(),
                            RPGConfig.ENABLE_CHEST_Y.get(),
                            RPGConfig.ENABLE_CHEST_SEP.get(),
                            RPGConfig.ENABLE_CHEST_ANG.get(),
                            RPGConfig.ENABLE_PHYSICS.get()
                    ));

            // プレイヤーデータを同期
            player.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
                RPGNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new RPGNetwork.PacketSyncData(
                                player.getId(),
                                cap.isFinished(),
                                cap.getGender(),
                                cap.getWidth(),
                                cap.getHeight(),
                                cap.getChest(),
                                cap.getChestY(),
                                cap.getChestSep(),
                                cap.getChestAng(),
                                cap.isPhysicsEnabled()
                        ));

                if (!cap.isFinished()) {
                    player.setGameMode(GameType.SPECTATOR);
                    RPGNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RPGNetwork.PacketOpenGui());
                }
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