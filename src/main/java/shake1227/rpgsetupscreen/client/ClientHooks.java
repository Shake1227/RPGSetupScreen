package shake1227.rpgsetupscreen.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import shake1227.rpgsetupscreen.RPGSetupScreen;
import shake1227.rpgsetupscreen.data.ScreenData;
import shake1227.rpgsetupscreen.network.RPGNetwork;
import shake1227.rpgsetupscreen.setup.RPGCapability;
import shake1227.rpgsetupscreen.setup.RPGCommands;
import shake1227.rpgsetupscreen.util.ModernNotificationHandler;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = RPGSetupScreen.MODID, value = Dist.CLIENT)
public class ClientHooks {

    private static ClientSettingsCache.CachedData pendingData = null;
    private static boolean introPlayedSession = false;
    private static boolean pendingSetupOpen = false;

    public static final List<ScreenData.Def> screenDefs = new ArrayList<>();
    public static CompoundTag accumulatedInputs = new CompoundTag();
    public static String pendingSpawnLocationName = "";
    public static Vec3 pendingSpawnPosition = null;

    private static boolean pendingManagerOpen = false;
    private static boolean hasCompletedSetup = false;

    private static int tickCounter = 0;
    private static boolean lastHasArmor = false;
    private static boolean layerCheckFailed = false;

    public static void setPendingSetupData(int g, float w, float h, float c, float cy, float cs, float ca, boolean phys) {
        pendingData = new ClientSettingsCache.CachedData(g, w, h, c, cy, cs, ca, phys);
    }

    public static void updateScreenDefs(List<ScreenData.Def> screens) {
        screenDefs.clear();
        screenDefs.addAll(screens);

        if (pendingManagerOpen) {
            pendingManagerOpen = false;
            Minecraft.getInstance().setScreen(new ScreenManager(screenDefs));
        }
    }

    public static void requestOpenManager() {
        pendingManagerOpen = true;
        RPGNetwork.CHANNEL.sendToServer(new RPGNetwork.PacketRequestScreens());
    }

    public static void openManager() {
        requestOpenManager();
    }

    public static void openScreen(int index) {
        if (screenDefs.isEmpty()) {
            if (index == -1) openSetup();
            else if (index == 0 && pendingData != null)
                Minecraft.getInstance().setScreen(new ScreenSpawn(new ArrayList<>(), pendingData.gender, pendingData.width, pendingData.height, pendingData.chest, pendingData.chestY, pendingData.chestSep, pendingData.chestAng, pendingData.physics));
            return;
        }

        if (index >= screenDefs.size()) {
            finishSetupSequence();
            return;
        }
        if (index < 0) {
            index = 0;
        }

        ScreenData.Def def = screenDefs.get(index);
        if (def.uuid.equals("setup")) {
            openSetup();
        } else if (def.uuid.equals("spawn")) {
            if (pendingData != null) {
                RPGNetwork.CHANNEL.sendToServer(new RPGNetwork.PacketAdminGui(null));
            } else {
                openSetup();
            }
        } else {
            Minecraft.getInstance().setScreen(new ScreenCustom(def, index));
        }
    }

    private static void finishSetupSequence() {
        if (pendingData != null) {
            RPGNetwork.CHANNEL.sendToServer(new RPGNetwork.PacketFinishSetup(
                    pendingSpawnLocationName,
                    pendingData.gender, pendingData.width, pendingData.height, pendingData.chest, pendingData.chestY, pendingData.chestSep, pendingData.chestAng, pendingData.physics,
                    "", accumulatedInputs, false
            ));
        }
        Minecraft.getInstance().setScreen(null);
        pendingSetupOpen = false;
        hasCompletedSetup = true;

        IntroAnimation.start(false, null);
        introPlayedSession = true;

        String locName = pendingSpawnLocationName != null && !pendingSpawnLocationName.isEmpty() ? pendingSpawnLocationName : "???";
        IntroAnimation.setOnComplete(() -> {
            ModernNotificationHandler.showTopRightNotification(
                    "notification.rpgsetupscreen.adventure.title",
                    "notification.rpgsetupscreen.adventure.message",
                    Collections.singletonList(locName),
                    "system",
                    6
            );
        });
    }

    public static void showClientNotification(String translationKey, String category) {
        ModernNotificationHandler.showClientNotification(translationKey, category);
    }

    public static void showTopRightNotification(String titleKey, String messageKey, List<String> args, String category, int duration) {
        ModernNotificationHandler.showTopRightNotification(titleKey, messageKey, args, category, duration);
    }

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        introPlayedSession = false;
        hasCompletedSetup = false;
        ClientSettingsCache.load();
        accumulatedInputs = new CompoundTag();
        pendingManagerOpen = false;
        pendingSetupOpen = false;
        layerCheckFailed = false;
        IntroAnimation.stop();

        if (Minecraft.getInstance().player != null) {
            lastHasArmor = !Minecraft.getInstance().player.getItemBySlot(EquipmentSlot.CHEST).isEmpty();
        }

        RPGNetwork.CHANNEL.sendToServer(new RPGNetwork.PacketRequestSync());
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        introPlayedSession = false;
        hasCompletedSetup = false;
        screenDefs.clear();
        accumulatedInputs = new CompoundTag();
        pendingData = null;
        pendingSpawnLocationName = "";
        pendingSpawnPosition = null;
        pendingManagerOpen = false;
        pendingSetupOpen = false;
        layerCheckFailed = false;
        IntroAnimation.stop();
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("reloadmydata")
                        .executes(ctx -> {
                            try {
                                Minecraft mc = Minecraft.getInstance();
                                if (mc.player != null) {
                                    ClientSettingsCache.CachedData cached = ClientSettingsCache.getForCurrentServer();
                                    if (cached != null) {
                                        RPGNetwork.CHANNEL.sendToServer(new RPGNetwork.PacketFinishSetup(
                                                "", cached.gender, cached.width, cached.height, cached.chest,
                                                cached.chestY, cached.chestSep, cached.chestAng, cached.physics,
                                                mc.player.getStringUUID(), new CompoundTag(), false
                                        ));
                                    } else {
                                        mc.player.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
                                            RPGNetwork.CHANNEL.sendToServer(new RPGNetwork.PacketFinishSetup(
                                                    "",
                                                    cap.getGender(), cap.getWidth(), cap.getHeight(), cap.getChest(),
                                                    cap.getChestY(), cap.getChestSep(), cap.getChestAng(), cap.isPhysicsEnabled(),
                                                    mc.player.getStringUUID(),
                                                    new CompoundTag(),
                                                    false
                                            ));
                                        });
                                    }

                                    String key = "command.rpgsetupscreen.reload_success";
                                    if (ModernNotificationHandler.IS_LOADED) {
                                        ModernNotificationHandler.showClientNotification(key, "success");
                                    } else {
                                        mc.player.sendSystemMessage(ChatUtil.translate(key));
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                String key = "command.rpgsetupscreen.reload_fail";
                                if (ModernNotificationHandler.IS_LOADED) {
                                    ModernNotificationHandler.showClientNotification(key, "failure");
                                } else {
                                    if (Minecraft.getInstance().player != null) {
                                        Minecraft.getInstance().player.sendSystemMessage(ChatUtil.translate(key));
                                    }
                                }
                            }
                            return 1;
                        })
        );
    }

    public static void handleSync(int entityId, boolean finished, int gender, float width, float height, float chest, float chestY, float chestSep, float chestAng, boolean physics) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity target = mc.level.getEntity(entityId);
        if (target instanceof Player player) {
            player.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
                cap.setFinished(finished); cap.setGender(gender); cap.setWidth(width); cap.setHeight(height); cap.setChest(chest); cap.setChestY(chestY); cap.setChestSep(chestSep); cap.setChestAng(chestAng); cap.setPhysicsEnabled(physics);
                if (player == mc.player) {
                    if (finished) {
                        pendingSetupOpen = false;
                        hasCompletedSetup = true;
                        if (!introPlayedSession && !IntroAnimation.isActive()) {
                            introPlayedSession = true;
                            IntroAnimation.start(true, null);
                        }
                    } else {
                        ClientSettingsCache.CachedData cached = ClientSettingsCache.getForCurrentServer();
                        if (cached != null) {
                            RPGNetwork.CHANNEL.sendToServer(new RPGNetwork.PacketFinishSetup(
                                    "", cached.gender, cached.width, cached.height, cached.chest,
                                    cached.chestY, cached.chestSep, cached.chestAng, cached.physics,
                                    mc.player.getStringUUID(), new CompoundTag(), false
                            ));
                            pendingSetupOpen = false;
                            hasCompletedSetup = true;
                        } else {
                            if (!hasCompletedSetup) {
                                pendingSetupOpen = true;
                            }
                        }
                    }
                }
            });
        }
    }

    public static void handleForceReset() {
        ClientSettingsCache.clearCurrentServer();
        introPlayedSession = false;
        hasCompletedSetup = false;
        accumulatedInputs = new CompoundTag();
        pendingSpawnLocationName = "";
        pendingSpawnPosition = null;
        pendingSetupOpen = true;
        IntroAnimation.stop();
    }

    public static void openSetup() {
        if (Minecraft.getInstance().screen instanceof ScreenSetup) return;
        ScreenSetup screen = new ScreenSetup();
        ClientSettingsCache.load();
        var data = ClientSettingsCache.getForCurrentServer();
        if (data != null) screen.loadDefaults(data.gender, data.width, data.height, data.chest, data.chestY, data.chestSep, data.chestAng, data.physics);
        Minecraft.getInstance().setScreen(screen);
    }

    public static void openSetupFromItem() {
        ScreenSetup screen = new ScreenSetup();
        screen.setFromFloppy(true);
        Minecraft.getInstance().player.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
            screen.loadDefaults(cap.getGender(), cap.getWidth(), cap.getHeight(), cap.getChest(), cap.getChestY(), cap.getChestSep(), cap.getChestAng(), cap.isPhysicsEnabled());
        });
        Minecraft.getInstance().setScreen(screen);
    }

    public static void openSetupForTarget(UUID targetId, int g, float w, float h, float c, float cy, float cs, float ca, boolean physics) {
        ScreenSetup screen = new ScreenSetup();
        screen.setEditTarget(targetId, g, w, h, c, cy, cs, ca, physics);
        Minecraft.getInstance().setScreen(screen);
    }

    public static void openAdmin(List<RPGCommands.SpawnData.Entry> list) {
        var mc = Minecraft.getInstance();
        ClientSettingsCache.CachedData d = ClientSettingsCache.getForCurrentServer();
        float w=1f,h=1f,c=0f,cy=0f,cs=0f,ca=0f; int g=0; boolean phys=true;
        if(d!=null){ w=d.width; h=d.height; c=d.chest; cy=d.chestY; cs=d.chestSep; ca=d.chestAng; g=d.gender; phys=d.physics; }
        else if(mc.player!=null) { var cap=mc.player.getCapability(RPGCapability.INSTANCE).orElse(null); if(cap!=null){ w=cap.getWidth(); h=cap.getHeight(); c=cap.getChest(); cy=cap.getChestY(); cs=cap.getChestSep(); ca=cap.getChestAng(); g=cap.getGender(); phys=cap.isPhysicsEnabled(); } }
        mc.setScreen(new ScreenSpawn(list, g, w, h, c, cy, cs, ca, phys));
    }

    @SubscribeEvent public static void onRenderTick(TickEvent.RenderTickEvent event) { if (event.phase == TickEvent.Phase.START) IntroAnimation.update(); }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        tickCounter++;
        if (tickCounter >= 20) {
            tickCounter = 0;
            checkSkinAndArmor();
        }

        if (pendingSetupOpen && mc.getOverlay() == null && mc.screen == null) {
            openScreen(0);
        }

        if (IntroAnimation.isActive() && !IntroAnimation.isPaused()) {
            mc.options.hideGui = false;
            for (KeyMapping key : mc.options.keyMappings) {
                if (key == mc.options.keyChat || key == mc.options.keyCommand || key == mc.options.keyScreenshot || key == mc.options.keyFullscreen) {
                    continue;
                }
                while (key.consumeClick()) { }
            }
        }

        if (KeyInit.TOGGLE_PHYSICS.consumeClick()) {
            if (mc.player != null) {
                mc.player.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
                    if (cap.getGender() == 1) {
                        RPGNetwork.CHANNEL.sendToServer(new RPGNetwork.PacketTogglePhysics());
                        boolean newState = !cap.isPhysicsEnabled();
                        String key = newState ? "message.rpgsetupscreen.physics_on" : "message.rpgsetupscreen.physics_off";
                        showClientNotification(key, "success");
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

    private static void checkSkinAndArmor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
            if (cap.getGender() != 1 || cap.getChest() <= 0.0f) return;

            if (!layerCheckFailed) {
                var dispatcher = mc.getEntityRenderDispatcher();
                for (String skinType : new String[]{"default", "slim"}) {
                    var renderer = dispatcher.getSkinMap().get(skinType);
                    if (renderer instanceof PlayerRenderer pr) {
                        boolean hasLayer = false;
                        try {
                            Field layersField = net.minecraft.client.renderer.entity.LivingEntityRenderer.class.getDeclaredField("layers");
                            layersField.setAccessible(true);
                            List<?> layers = (List<?>) layersField.get(pr);
                            hasLayer = layers.stream().anyMatch(l -> l instanceof PlayerChestLayer);
                        } catch (Exception e) {
                            layerCheckFailed = true;
                            return;
                        }

                        if (!hasLayer) {
                            pr.addLayer(new PlayerChestLayer(pr));
                        }
                    }
                }
            }

            boolean hasArmor = !mc.player.getItemBySlot(EquipmentSlot.CHEST).isEmpty();
            if (hasArmor != lastHasArmor) {
                lastHasArmor = hasArmor;
            }
        });
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        if (IntroAnimation.isActive()) {
            if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()) {
                IntroAnimation.render(event.getGuiGraphics(), event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight());
            }
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Pre event) {
        if (IntroAnimation.isActive() && !IntroAnimation.isPaused() && Minecraft.getInstance().screen == null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (IntroAnimation.isActive()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null) return;

            int k = event.getKey();

            if (k == mc.options.keyChat.getKey().getValue() ||
                    k == mc.options.keyCommand.getKey().getValue()) {
                return;
            }

            if (k == InputConstants.KEY_ESCAPE && event.getAction() == InputConstants.PRESS) {
                IntroAnimation.skip();
            }
        }
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (IntroAnimation.isActive() && !IntroAnimation.isPaused()) {
            if (event.getNewScreen() instanceof PauseScreen) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (IntroAnimation.isActive() && !IntroAnimation.isPaused()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player == Minecraft.getInstance().player && IntroAnimation.shouldHideHead()) {
            PlayerRenderer renderer = event.getRenderer();
            PlayerModel<net.minecraft.client.player.AbstractClientPlayer> model = renderer.getModel();
            model.head.visible = false;
            model.hat.visible = false;
            model.leftArm.visible = false;
            model.leftSleeve.visible = false;
            model.rightArm.visible = false;
            model.rightSleeve.visible = false;
        }

        player.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
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
        PlayerRenderer renderer = event.getRenderer();
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        model.head.visible = true;
        model.hat.visible = true;
        model.leftArm.visible = true;
        model.leftSleeve.visible = true;
        model.rightArm.visible = true;
        model.rightSleeve.visible = true;

        event.getEntity().getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
            event.getPoseStack().popPose();
        });
    }

    private static class PhysicsState { float pos, vel, rot, rotVel; PhysicsState(float pos, float vel, float rot, float rotVel) { this.pos = pos; this.vel = vel; this.rot = rot; this.rotVel = rotVel; } }
    private static void updateChestPhysics(Player plr, RPGCapability.IRPGData cap) { if (!cap.isPhysicsEnabled()) { cap.setBouncePosL(0); cap.setBounceVelL(0); cap.setBounceRotL(0); cap.setBounceRotVelL(0); cap.setBouncePosR(0); cap.setBounceVelR(0); cap.setBounceRotR(0); cap.setBounceRotVelR(0); cap.setPrevBouncePosL(0); cap.setPrevBounceRotL(0); cap.setPrevBouncePosR(0); cap.setPrevBounceRotR(0); return; } cap.setPrevBouncePosL(cap.getBouncePosL()); cap.setPrevBounceRotL(cap.getBounceRotL()); cap.setPrevBouncePosR(cap.getBouncePosR()); cap.setPrevBounceRotR(cap.getBounceRotR()); if (Double.isNaN(cap.getPrevPlayerX())) { cap.setPrevPlayerX(plr.getX()); cap.setPrevPlayerY(plr.getY()); cap.setPrevPlayerZ(plr.getZ()); return; } double dx = plr.getX() - cap.getPrevPlayerX(); double dy = plr.getY() - cap.getPrevPlayerY(); double dz = plr.getZ() - cap.getPrevPlayerZ(); cap.setPrevPlayerX(plr.getX()); cap.setPrevPlayerY(plr.getY()); cap.setPrevPlayerZ(plr.getZ()); float size = cap.getChest(); float walkBob = 0f; if (plr.onGround() && (Math.abs(dx) > 0.01 || Math.abs(dz) > 0.01)) { walkBob = net.minecraft.util.Mth.sin(plr.walkAnimation.position() * 1.0f) * (0.005f + size * 0.01f); } float yRotDelta = plr.yBodyRot - plr.yBodyRotO; float inputRotCommon = -yRotDelta * (0.02f + size * 0.03f); PhysicsState left = simulateSingleBreast( cap.getBouncePosL(), cap.getBounceVelL(), cap.getBounceRotL(), cap.getBounceRotVelL(), size, dy, walkBob, inputRotCommon, 1.0f ); cap.setBouncePosL(left.pos); cap.setBounceVelL(left.vel); cap.setBounceRotL(left.rot); cap.setBounceRotVelL(left.rotVel); PhysicsState right = simulateSingleBreast( cap.getBouncePosR(), cap.getBounceVelR(), cap.getBounceRotR(), cap.getBounceRotVelR(), size, dy, walkBob, inputRotCommon, 0.95f ); cap.setBouncePosR(right.pos); cap.setBounceVelR(right.vel); cap.setBounceRotR(right.rot); cap.setBounceRotVelR(right.rotVel); }
    private static PhysicsState simulateSingleBreast( float pos, float vel, float rot, float rotVel, float size, double dy, float walkBob, float inputRot, float variantScale) { float stiffness = (0.15f - (size * 0.05f)) * variantScale; float damping = 0.1f * (2.0f - variantScale); float sensitivity = (0.05f + (size * 0.05f)); float friction = (0.92f + (size * 0.03f)) * variantScale; float inputY = (float) dy * sensitivity + walkBob; float accel = (-stiffness * pos) - (damping * vel) + inputY; vel += accel; vel *= friction; pos += vel; float limitDown = 0.05f + (size * 0.05f); float limitUp = -0.05f - (size * 0.03f); if (pos > limitDown) { pos = limitDown; vel *= -0.4f; } else if (pos < limitUp) { pos = limitUp; vel *= -0.4f; } float rotAccel = (-stiffness * rot) - (damping * rotVel) + inputRot; rotVel += rotAccel; rotVel *= friction; rot += rotVel; float limitRot = 5.0f + (size * 5.0f); rot = net.minecraft.util.Mth.clamp(rot, -limitRot, limitRot); return new PhysicsState(pos, vel, rot, rotVel); }
    @Mod.EventBusSubscriber(modid = RPGSetupScreen.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD) public static class ModBusEvents { @SubscribeEvent public static void onAddLayers(EntityRenderersEvent.AddLayers event) { addLayer(event, "default"); addLayer(event, "slim"); } private static void addLayer(EntityRenderersEvent.AddLayers event, String skin) { var renderer = event.getSkin(skin); if (renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer pr) { pr.addLayer(new PlayerChestLayer(pr)); } } }
}