package shake1227.rpgsetupscreen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import shake1227.rpgsetupscreen.RPGSetupScreen;
import shake1227.rpgsetupscreen.setup.RPGCapability;
import shake1227.rpgsetupscreen.setup.RPGCommands;

import java.util.List;

@Mod.EventBusSubscriber(modid = RPGSetupScreen.MODID, value = Dist.CLIENT)
public class ClientHooks {

    // --- Network Logic ---
    public static void handleSync(boolean f, int g, float w, float h, float c) {
        Player p = Minecraft.getInstance().player;
        if(p != null) {
            p.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
                cap.setFinished(f);
                cap.setGender(g);
                cap.setWidth(w);
                cap.setHeight(h);
                cap.setChest(c);
            });
        }
    }

    public static void openSetup() {
        Minecraft.getInstance().setScreen(new ScreenSetup());
    }

    public static void openAdmin(List<RPGCommands.SpawnData.Entry> list) {
        if (Minecraft.getInstance().screen instanceof ScreenSetup) {
            float w = 1f, h = 1f, c = 0f; int g = 0;
            if(Minecraft.getInstance().player != null) {
                var cap = Minecraft.getInstance().player.getCapability(RPGCapability.INSTANCE).orElse(null);
                if(cap != null) {
                    w = cap.getWidth();
                    h = cap.getHeight();
                    c = cap.getChest();
                    g = cap.getGender();
                }
            }
            Minecraft.getInstance().setScreen(new ScreenSpawn(list, g, w, h, c));
        } else {
            Minecraft.getInstance().setScreen(new ScreenAdmin(list));
        }
    }

    // --- Forge Bus Events (Pre-Render Scaling) ---
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        event.getEntity().getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
            event.getPoseStack().pushPose();
            // 身長・横幅のスケール反映
            event.getPoseStack().scale(cap.getWidth(), cap.getHeight(), cap.getWidth());
        });
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        event.getEntity().getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
            event.getPoseStack().popPose();
        });
    }

    // --- Mod Bus Events (Layer Registration) ---
    @Mod.EventBusSubscriber(modid = RPGSetupScreen.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
            addLayer(event, "default");
            addLayer(event, "slim");
        }

        private static void addLayer(EntityRenderersEvent.AddLayers event, String skin) {
            EntityRenderer<? extends Player> renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer playerRenderer) {
                playerRenderer.addLayer(new PlayerChestLayer(playerRenderer));
            }
        }
    }
}