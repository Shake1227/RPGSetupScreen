package shake1227.rpgsetupscreen.setup;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import shake1227.rpgsetupscreen.network.RPGNetwork;

public class RPGEventHandler {
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
                RPGNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new RPGNetwork.PacketSyncData(cap.isFinished(), cap.getGender(), cap.getWidth(), cap.getHeight(), cap.getChest()));

                if (!cap.isFinished()) {
                    player.setGameMode(GameType.SPECTATOR);
                    RPGNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RPGNetwork.PacketOpenGui());
                }
            });
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        // Forge 1.20.1: getOriginal() から Capability を取得する正しい方法
        event.getOriginal().getCapability(RPGCapability.INSTANCE).ifPresent(oldCap ->
                event.getEntity().getCapability(RPGCapability.INSTANCE).ifPresent(newCap ->
                        newCap.copyFrom(oldCap)
                )
        );
    }
}