package shake1227.rpgsetupscreen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import shake1227.rpgsetupscreen.RPGSetupScreen;
import shake1227.rpgsetupscreen.network.RPGNetwork;
import shake1227.rpgsetupscreen.setup.RPGCapability;
import shake1227.rpgsetupscreen.setup.RPGCommands;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = RPGSetupScreen.MODID, value = Dist.CLIENT)
public class ClientHooks {

    private static ClientSettingsCache.CachedData pendingData = null;
    private static boolean introPlayedSession = false;
    private static boolean isLoginPhase = true;

    public static void setPendingSetupData(int g, float w, float h, float c, float cy, float cs, float ca, boolean phys) {
        pendingData = new ClientSettingsCache.CachedData(g, w, h, c, cy, cs, ca, phys);
    }

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        introPlayedSession = false;
        isLoginPhase = true;
        ClientSettingsCache.load();
    }

    public static void handleSync(int entityId, boolean finished, int gender, float width, float height, float chest,
                                  float chestY, float chestSep, float chestAng, boolean physics) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity target = mc.level.getEntity(entityId);
        if (target instanceof Player player) {
            player.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
                cap.setFinished(finished);
                cap.setGender(gender);
                cap.setWidth(width);
                cap.setHeight(height);
                cap.setChest(chest);
                cap.setChestY(chestY);
                cap.setChestSep(chestSep);
                cap.setChestAng(chestAng);
                cap.setPhysicsEnabled(physics);

                if (player == mc.player) {
                    if (finished) {
                        if (isLoginPhase && !introPlayedSession) {
                            playIntro();
                        }
                    } else {
                        openSetup();
                    }
                    isLoginPhase = false;
                }
            });
        }
    }

    public static void playIntro() {
        if (!introPlayedSession) {
            introPlayedSession = true;
            IntroAnimation.start();
        }
    }

    public static void openSetup() {
        if (Minecraft.getInstance().screen instanceof ScreenSetup) return;

        ScreenSetup screen = new ScreenSetup();
        ClientSettingsCache.load();
        var data = ClientSettingsCache.getForCurrentServer();
        if (data != null) {
            screen.loadDefaults(data.gender, data.width, data.height,
                    data.chest, data.chestY, data.chestSep, data.chestAng, data.physics);
        }
        Minecraft.getInstance().setScreen(screen);
    }

    public static void openSetupForTarget(UUID targetId, int g, float w, float h, float c, float cy, float cs, float ca, boolean physics) {
        ScreenSetup screen = new ScreenSetup();
        screen.setEditTarget(targetId, g, w, h, c, cy, cs, ca, physics);
        Minecraft.getInstance().setScreen(screen);
    }

    public static void openAdmin(List<RPGCommands.SpawnData.Entry> list) {
        var mc = Minecraft.getInstance();
        if (mc.screen instanceof ScreenSetup) {
            float w = 1f, h = 1f, c = 0f, cy = 0f, cs = 0f, ca = 0f;
            int g = 0;
            boolean phys = true;

            if (pendingData != null) {
                w = pendingData.width;
                h = pendingData.height;
                c = pendingData.chest;
                cy = pendingData.chestY;
                cs = pendingData.chestSep;
                ca = pendingData.chestAng;
                g = pendingData.gender;
                phys = pendingData.physics;
                pendingData = null;
            } else if (mc.player != null) {
                var cap = mc.player.getCapability(RPGCapability.INSTANCE).orElse(null);
                if (cap != null) {
                    w = cap.getWidth();
                    h = cap.getHeight();
                    c = cap.getChest();
                    cy = cap.getChestY();
                    cs = cap.getChestSep();
                    ca = cap.getChestAng();
                    g = cap.getGender();
                    phys = cap.isPhysicsEnabled();
                }
            }
            mc.setScreen(new ScreenSpawn(list, g, w, h, c, cy, cs, ca, phys));
        } else {
            mc.setScreen(new ScreenAdmin(list));
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            IntroAnimation.update();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // アニメーション中の操作無効化
        if (IntroAnimation.isActive()) {
            while (mc.options.keyTogglePerspective.consumeClick()) { }
            // keyForward -> keyUp, keyBackward -> keyDown に修正
            while (mc.options.keyUp.consumeClick()) { }
            while (mc.options.keyDown.consumeClick()) { }
            while (mc.options.keyLeft.consumeClick()) { }
            while (mc.options.keyRight.consumeClick()) { }
            while (mc.options.keyJump.consumeClick()) { }
            while (mc.options.keySprint.consumeClick()) { }
            while (mc.options.keyShift.consumeClick()) { }
        }

        if (KeyInit.TOGGLE_PHYSICS.consumeClick()) {
            if (mc.player != null) {
                mc.player.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
                    if (cap.getGender() == 1) {
                        RPGNetwork.CHANNEL.sendToServer(new RPGNetwork.PacketTogglePhysics());
                        boolean newState = !cap.isPhysicsEnabled();
                        mc.player.displayClientMessage(Component.translatable(
                                newState ? "message.rpgsetupscreen.physics_on" : "message.rpgsetupscreen.physics_off"), true);
                    }
                });
            }
        }

        if (mc.player != null && mc.player.tickCount % 100 == 0) {
            RPGNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(), new RPGNetwork.PacketRequestSync());
        }

        for (Player pl : mc.level.players()) {
            pl.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
                if (cap.getGender() == 1 && cap.getChest() > 0.0f) {
                    updateChestPhysics(pl, cap);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        if (IntroAnimation.isActive()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (IntroAnimation.isActive() && event.getOverlay() == net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.HOTBAR.type()) {
            IntroAnimation.render(event.getGuiGraphics(), event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight());
        }
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton event) {
        if (IntroAnimation.isActive()) {
            event.setCanceled(true);
        }
    }

    // --- 物理演算 ---
    private static class PhysicsState {
        float pos, vel, rot, rotVel;
        PhysicsState(float pos, float vel, float rot, float rotVel) {
            this.pos = pos; this.vel = vel; this.rot = rot; this.rotVel = rotVel;
        }
    }

    private static void updateChestPhysics(Player plr, RPGCapability.IRPGData cap) {
        if (!cap.isPhysicsEnabled()) {
            cap.setBouncePosL(0); cap.setBounceVelL(0); cap.setBounceRotL(0); cap.setBounceRotVelL(0);
            cap.setBouncePosR(0); cap.setBounceVelR(0); cap.setBounceRotR(0); cap.setBounceRotVelR(0);
            cap.setPrevBouncePosL(0); cap.setPrevBounceRotL(0);
            cap.setPrevBouncePosR(0); cap.setPrevBounceRotR(0);
            return;
        }

        cap.setPrevBouncePosL(cap.getBouncePosL());
        cap.setPrevBounceRotL(cap.getBounceRotL());
        cap.setPrevBouncePosR(cap.getBouncePosR());
        cap.setPrevBounceRotR(cap.getBounceRotR());

        if (Double.isNaN(cap.getPrevPlayerX())) {
            cap.setPrevPlayerX(plr.getX());
            cap.setPrevPlayerY(plr.getY());
            cap.setPrevPlayerZ(plr.getZ());
            return;
        }

        double dx = plr.getX() - cap.getPrevPlayerX();
        double dy = plr.getY() - cap.getPrevPlayerY();
        double dz = plr.getZ() - cap.getPrevPlayerZ();

        cap.setPrevPlayerX(plr.getX());
        cap.setPrevPlayerY(plr.getY());
        cap.setPrevPlayerZ(plr.getZ());

        float size = cap.getChest();

        float walkBob = 0f;
        if (plr.onGround() && (Math.abs(dx) > 0.01 || Math.abs(dz) > 0.01)) {
            walkBob = net.minecraft.util.Mth.sin(plr.walkAnimation.position() * 1.0f) * (0.005f + size * 0.01f);
        }

        float yRotDelta = plr.yBodyRot - plr.yBodyRotO;
        float inputRotCommon = -yRotDelta * (0.02f + size * 0.03f);

        PhysicsState left = simulateSingleBreast(
                cap.getBouncePosL(), cap.getBounceVelL(), cap.getBounceRotL(), cap.getBounceRotVelL(),
                size, dy, walkBob, inputRotCommon, 1.0f
        );
        cap.setBouncePosL(left.pos); cap.setBounceVelL(left.vel);
        cap.setBounceRotL(left.rot); cap.setBounceRotVelL(left.rotVel);

        PhysicsState right = simulateSingleBreast(
                cap.getBouncePosR(), cap.getBounceVelR(), cap.getBounceRotR(), cap.getBounceRotVelR(),
                size, dy, walkBob, inputRotCommon, 0.95f
        );
        cap.setBouncePosR(right.pos); cap.setBounceVelR(right.vel);
        cap.setBounceRotR(right.rot); cap.setBounceRotVelR(right.rotVel);
    }

    private static PhysicsState simulateSingleBreast(
            float pos, float vel, float rot, float rotVel,
            float size, double dy, float walkBob, float inputRot,
            float variantScale) {

        float stiffness = (0.15f - (size * 0.05f)) * variantScale;
        float damping = 0.1f * (2.0f - variantScale);
        float sensitivity = (0.05f + (size * 0.05f));
        float friction = (0.92f + (size * 0.03f)) * variantScale;

        float inputY = (float) dy * sensitivity + walkBob;

        float accel = (-stiffness * pos) - (damping * vel) + inputY;
        vel += accel;
        vel *= friction;
        pos += vel;

        float limitDown = 0.05f + (size * 0.05f);
        float limitUp = -0.05f - (size * 0.03f);
        if (pos > limitDown) {
            pos = limitDown;
            vel *= -0.4f;
        } else if (pos < limitUp) {
            pos = limitUp;
            vel *= -0.4f;
        }

        float rotAccel = (-stiffness * rot) - (damping * rotVel) + inputRot;
        rotVel += rotAccel;
        rotVel *= friction;
        rot += rotVel;

        float limitRot = 5.0f + (size * 5.0f);
        rot = net.minecraft.util.Mth.clamp(rot, -limitRot, limitRot);

        return new PhysicsState(pos, vel, rot, rotVel);
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        event.getEntity().getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
            event.getPoseStack().pushPose();
            float w = cap.getWidth();
            float h = cap.getHeight();
            if (w <= 0.1f) w = 1.0f;
            if (h <= 0.1f) h = 1.0f;
            event.getPoseStack().scale(w, h, w);
        });
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        event.getEntity().getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
            event.getPoseStack().popPose();
        });
    }

    @Mod.EventBusSubscriber(modid = RPGSetupScreen.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
            addLayer(event, "default");
            addLayer(event, "slim");
        }

        private static void addLayer(EntityRenderersEvent.AddLayers event, String skin) {
            var renderer = event.getSkin(skin);
            if (renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer pr) {
                pr.addLayer(new PlayerChestLayer(pr));
            }
        }
    }
}