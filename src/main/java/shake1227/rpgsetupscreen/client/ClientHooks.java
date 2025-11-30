package shake1227.rpgsetupscreen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
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

    // 同期受け取り（サーバから受け取ってローカルプレイヤーに反映）
    public static void handleSync(boolean finished, int gender, float width, float height, float chest,
                                  float chestY, float chestSep, float chestAng) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;
        player.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
            cap.setFinished(finished);
            cap.setGender(gender);
            cap.setWidth(width);
            cap.setHeight(height);
            cap.setChest(chest);
            cap.setChestY(chestY);
            cap.setChestSep(chestSep);
            cap.setChestAng(chestAng);
        });
    }

    public static void openSetup() {
        Minecraft.getInstance().setScreen(new ScreenSetup());
    }

    public static void openSetupForTarget(UUID targetId, int g, float w, float h, float c, float cy, float cs, float ca) {
        ScreenSetup screen = new ScreenSetup();
        screen.setEditTarget(targetId, g, w, h, c, cy, cs, ca);
        Minecraft.getInstance().setScreen(screen);
    }

    public static void openAdmin(List<RPGCommands.SpawnData.Entry> list) {
        var mc = Minecraft.getInstance();
        if (mc.screen instanceof ScreenSetup) {
            float w = 1f, h = 1f, c = 0f, cy = 0f, cs = 0f, ca = 0f;
            int g = 0;
            if (mc.player != null) {
                var cap = mc.player.getCapability(RPGCapability.INSTANCE).orElse(null);
                if (cap != null) {
                    w = cap.getWidth();
                    h = cap.getHeight();
                    c = cap.getChest();
                    cy = cap.getChestY();
                    cs = cap.getChestSep();
                    ca = cap.getChestAng();
                    g = cap.getGender();
                }
            }
            mc.setScreen(new ScreenSpawn(list, g, w, h, c, cy, cs, ca));
        } else {
            mc.setScreen(new ScreenAdmin(list));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // 1秒ごとにサーバ同期要求（元コードでは tickCount % 20）
        if (mc.player != null && mc.player.tickCount % 20 == 0) {
            RPGNetwork.CHANNEL.send(PacketDistributor.SERVER.noArg(), new RPGNetwork.PacketRequestSync());
        }

        // 全プレイヤーに対して胸物理を更新（女性かつ胸サイズ>0 のみ）
        for (Player pl : mc.level.players()) {
            pl.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
                if (cap.getGender() == 1 && cap.getChest() > 0.0f) {
                    updateChestPhysics(pl, cap);
                } else {
                    // 初期化等が必要ならここで行うが、元コードでは物理は放置される
                }
            });
        }
    }

    // 物理計算用の戻り値型
    private static class PhysicsState {
        float pos, vel, rot, rotVel;
        PhysicsState(float pos, float vel, float rot, float rotVel) {
            this.pos = pos; this.vel = vel; this.rot = rot; this.rotVel = rotVel;
        }
    }

    private static void updateChestPhysics(Player plr, RPGCapability.IRPGData cap) {
        // 前回値を prev フィールドに退避（元コードと等価）
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

        // --- 共通入力 ---
        float walkBob = 0f;
        if (plr.onGround() && (Math.abs(dx) > 0.01 || Math.abs(dz) > 0.01)) {
            walkBob = Mth.sin(plr.walkAnimation.position() * 1.0f) * (0.005f + size * 0.01f);
        }

        float yRotDelta = plr.yBodyRot - plr.yBodyRotO;
        float inputRotCommon = -yRotDelta * (0.02f + size * 0.03f);

        // 左胸（標準パラメータ）
        PhysicsState left = simulateSingleBreast(
                cap.getBouncePosL(), cap.getBounceVelL(), cap.getBounceRotL(), cap.getBounceRotVelL(),
                size, dy, walkBob, inputRotCommon, 1.0f
        );
        cap.setBouncePosL(left.pos); cap.setBounceVelL(left.vel);
        cap.setBounceRotL(left.rot); cap.setBounceRotVelL(left.rotVel);

        // 右胸（わずかに特性を変えて非同期化）
        PhysicsState right = simulateSingleBreast(
                cap.getBouncePosR(), cap.getBounceVelR(), cap.getBounceRotR(), cap.getBounceRotVelR(),
                size, dy, walkBob, inputRotCommon, 0.95f
        );
        cap.setBouncePosR(right.pos); cap.setBounceVelR(right.vel);
        cap.setBounceRotR(right.rot); cap.setBounceRotVelR(right.rotVel);
    }

    // 単胸の物理演算を行い、結果を PhysicsState で返す（内部は元の式と同等）
    private static PhysicsState simulateSingleBreast(
            float pos, float vel, float rot, float rotVel,
            float size, double dy, float walkBob, float inputRot,
            float variantScale) {

        // 基本係数（元コードの数式と同等）
        float stiffness = (0.15f - (size * 0.05f)) * variantScale;
        float damping = 0.1f * (2.0f - variantScale);
        float sensitivity = (0.05f + (size * 0.05f));
        float friction = (0.92f + (size * 0.03f)) * variantScale;

        float inputY = (float) dy * sensitivity + walkBob;

        // Y 方向力学（元コードと等価）
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

        // 回転側
        float rotAccel = (-stiffness * rot) - (damping * rotVel) + inputRot;
        rotVel += rotAccel;
        rotVel *= friction;
        rot += rotVel;

        float limitRot = 5.0f + (size * 5.0f);
        rot = Mth.clamp(rot, -limitRot, limitRot);

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