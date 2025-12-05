package shake1227.rpgsetupscreen.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import shake1227.rpgsetupscreen.RPGSetupScreen;
import shake1227.rpgsetupscreen.client.ClientConfigCache;
import shake1227.rpgsetupscreen.client.ClientHooks;
import shake1227.rpgsetupscreen.setup.RPGCapability;
import shake1227.rpgsetupscreen.setup.RPGCommands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class RPGNetwork {
    public static SimpleChannel CHANNEL;
    private static int id = 0;
    private static final String VER = "5"; // パケット追加のため更新

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(RPGSetupScreen.MODID, "net"), () -> VER, VER::equals, VER::equals);
        CHANNEL.registerMessage(id++, PacketSyncData.class, PacketSyncData::enc, PacketSyncData::dec, PacketSyncData::handle);
        CHANNEL.registerMessage(id++, PacketOpenGui.class, (a,b)->{}, buf->new PacketOpenGui(), PacketOpenGui::handle);
        CHANNEL.registerMessage(id++, PacketFinishSetup.class, PacketFinishSetup::enc, PacketFinishSetup::dec, PacketFinishSetup::handle);
        CHANNEL.registerMessage(id++, PacketAdminGui.class, PacketAdminGui::enc, PacketAdminGui::dec, PacketAdminGui::handle);
        CHANNEL.registerMessage(id++, PacketAdminAction.class, PacketAdminAction::enc, PacketAdminAction::dec, PacketAdminAction::handle);
        CHANNEL.registerMessage(id++, PacketRequestSync.class, (a,b)->{}, buf->new PacketRequestSync(), PacketRequestSync::handle);
        CHANNEL.registerMessage(id++, PacketOpenEditor.class, PacketOpenEditor::enc, PacketOpenEditor::dec, PacketOpenEditor::handle);
        CHANNEL.registerMessage(id++, PacketTogglePhysics.class, (a,b)->{}, buf->new PacketTogglePhysics(), PacketTogglePhysics::handle);
        // 新規パケット
        CHANNEL.registerMessage(id++, PacketSyncConfig.class, PacketSyncConfig::enc, PacketSyncConfig::dec, PacketSyncConfig::handle);
    }

    // --- 設定同期パケット ---
    public static class PacketSyncConfig {
        public boolean g, w, h, c, cy, cs, ca, phys;
        public PacketSyncConfig(boolean g, boolean w, boolean h, boolean c, boolean cy, boolean cs, boolean ca, boolean phys) {
            this.g=g; this.w=w; this.h=h; this.c=c; this.cy=cy; this.cs=cs; this.ca=ca; this.phys=phys;
        }
        public static void enc(PacketSyncConfig m, FriendlyByteBuf b) {
            b.writeBoolean(m.g); b.writeBoolean(m.w); b.writeBoolean(m.h); b.writeBoolean(m.c);
            b.writeBoolean(m.cy); b.writeBoolean(m.cs); b.writeBoolean(m.ca); b.writeBoolean(m.phys);
        }
        public static PacketSyncConfig dec(FriendlyByteBuf b) {
            return new PacketSyncConfig(b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readBoolean(),
                    b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readBoolean());
        }
        public static void handle(PacketSyncConfig m, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> ClientConfigCache.update(m.g, m.w, m.h, m.c, m.cy, m.cs, m.ca, m.phys));
            c.get().setPacketHandled(true);
        }
    }

    // --- 既存パケット ---
    public static class PacketSyncData {
        public int entityId;
        public boolean f; public int g; public float w, h, c, cy, cs, ca; public boolean physics;

        public PacketSyncData(int entityId, boolean f, int g, float w, float h, float c, float cy, float cs, float ca, boolean physics) {
            this.entityId = entityId;
            this.f=f; this.g=g; this.w=w; this.h=h; this.c=c; this.cy=cy; this.cs=cs; this.ca=ca; this.physics=physics;
        }
        public static void enc(PacketSyncData m, FriendlyByteBuf b) {
            b.writeInt(m.entityId);
            b.writeBoolean(m.f); b.writeInt(m.g); b.writeFloat(m.w); b.writeFloat(m.h); b.writeFloat(m.c);
            b.writeFloat(m.cy); b.writeFloat(m.cs); b.writeFloat(m.ca); b.writeBoolean(m.physics);
        }
        public static PacketSyncData dec(FriendlyByteBuf b) {
            return new PacketSyncData(b.readInt(), b.readBoolean(), b.readInt(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readBoolean());
        }
        public static void handle(PacketSyncData m, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> ClientHooks.handleSync(m.entityId, m.f, m.g, m.w, m.h, m.c, m.cy, m.cs, m.ca, m.physics));
            c.get().setPacketHandled(true);
        }
    }

    public static class PacketTogglePhysics {
        public static void handle(PacketTogglePhysics m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer p = ctx.get().getSender();
                if(p != null) {
                    p.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
                        if (cap.getGender() == 1) {
                            boolean newState = !cap.isPhysicsEnabled();
                            cap.setPhysicsEnabled(newState);
                            RPGNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->p),
                                    new PacketSyncData(p.getId(), true, cap.getGender(), cap.getWidth(), cap.getHeight(), cap.getChest(),
                                            cap.getChestY(), cap.getChestSep(), cap.getChestAng(), newState));
                        }
                    });
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class PacketOpenGui {
        public static void handle(PacketOpenGui m, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(ClientHooks::openSetup);
            c.get().setPacketHandled(true);
        }
    }

    public static class PacketOpenEditor {
        public UUID targetId; public int g; public float w, h, c, cy, cs, ca; public boolean physics;
        public PacketOpenEditor(UUID targetId, int g, float w, float h, float c, float cy, float cs, float ca, boolean physics) {
            this.targetId = targetId; this.g = g; this.w = w; this.h = h; this.c = c; this.cy=cy; this.cs=cs; this.ca=ca; this.physics=physics;
        }
        public static void enc(PacketOpenEditor m, FriendlyByteBuf b) {
            b.writeUUID(m.targetId); b.writeInt(m.g); b.writeFloat(m.w); b.writeFloat(m.h); b.writeFloat(m.c);
            b.writeFloat(m.cy); b.writeFloat(m.cs); b.writeFloat(m.ca); b.writeBoolean(m.physics);
        }
        public static PacketOpenEditor dec(FriendlyByteBuf b) {
            return new PacketOpenEditor(b.readUUID(), b.readInt(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readBoolean());
        }
        public static void handle(PacketOpenEditor m, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> ClientHooks.openSetupForTarget(m.targetId, m.g, m.w, m.h, m.c, m.cy, m.cs, m.ca, m.physics));
            c.get().setPacketHandled(true);
        }
    }

    public static class PacketFinishSetup {
        public String loc; public int g; public float w, h, c, cy, cs, ca; public boolean physics;
        public String targetUUID;

        public PacketFinishSetup(String loc, int g, float w, float h, float c, float cy, float cs, float ca, boolean physics, String targetUUID) {
            this.loc=loc; this.g=g; this.w=w; this.h=h; this.c=c;
            this.cy=cy; this.cs=cs; this.ca=ca; this.physics=physics;
            this.targetUUID = targetUUID;
        }
        public static void enc(PacketFinishSetup m, FriendlyByteBuf b) {
            b.writeUtf(m.loc); b.writeInt(m.g); b.writeFloat(m.w); b.writeFloat(m.h); b.writeFloat(m.c);
            b.writeFloat(m.cy); b.writeFloat(m.cs); b.writeFloat(m.ca); b.writeBoolean(m.physics);
            b.writeUtf(m.targetUUID);
        }
        public static PacketFinishSetup dec(FriendlyByteBuf b) {
            return new PacketFinishSetup(b.readUtf(), b.readInt(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readBoolean(), b.readUtf());
        }
        public static void handle(PacketFinishSetup m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sender = ctx.get().getSender();
                if(sender == null) return;

                ServerPlayer targetPlayer = sender;
                if(!m.targetUUID.isEmpty() && sender.hasPermissions(2)) {
                    ServerPlayer t = sender.server.getPlayerList().getPlayer(UUID.fromString(m.targetUUID));
                    if(t != null) targetPlayer = t;
                }

                final ServerPlayer p = targetPlayer;
                p.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
                    boolean wasFinished = cap.isFinished();

                    cap.setGender(m.g); cap.setWidth(m.w); cap.setHeight(m.h); cap.setChest(m.c);
                    cap.setChestY(m.cy); cap.setChestSep(m.cs); cap.setChestAng(m.ca);
                    cap.setPhysicsEnabled(m.physics);
                    cap.setFinished(true);

                    RPGNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->p),
                            new PacketSyncData(p.getId(), true, m.g, m.w, m.h, m.c, m.cy, m.cs, m.ca, m.physics));

                    if (!wasFinished && p.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                        p.setGameMode(GameType.SURVIVAL);
                    }
                });

                if(!m.loc.isEmpty()) {
                    RPGCommands.SpawnData data = RPGCommands.SpawnData.get(p.serverLevel());
                    var opt = data.list.stream().filter(e -> e.name.equals(m.loc)).findFirst();
                    if(opt.isPresent()) p.teleportTo(opt.get().x, opt.get().y, opt.get().z);
                    else if(m.loc.equals("Initial Spawn Point")) {
                        BlockPos s = p.serverLevel().getSharedSpawnPos();
                        p.teleportTo(s.getX(), s.getY(), s.getZ());
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class PacketRequestSync {
        public static void handle(PacketRequestSync m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer p = ctx.get().getSender();
                if(p != null) {
                    p.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
                        RPGNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),
                                new PacketSyncData(p.getId(), cap.isFinished(), cap.getGender(), cap.getWidth(), cap.getHeight(), cap.getChest(),
                                        cap.getChestY(), cap.getChestSep(), cap.getChestAng(), cap.isPhysicsEnabled()));
                    });
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class PacketAdminGui {
        public List<RPGCommands.SpawnData.Entry> list;
        public PacketAdminGui(List<RPGCommands.SpawnData.Entry> l) { this.list=l; }
        public static void enc(PacketAdminGui m, FriendlyByteBuf b) {
            if(m.list == null) { b.writeInt(0); return; }
            b.writeInt(m.list.size()); m.list.forEach(e->{b.writeUtf(e.name); b.writeDouble(e.x); b.writeDouble(e.y); b.writeDouble(e.z);});
        }
        public static PacketAdminGui dec(FriendlyByteBuf b) {
            List<RPGCommands.SpawnData.Entry> l = new ArrayList<>();
            int s = b.readInt(); for(int i=0; i<s; i++) l.add(new RPGCommands.SpawnData.Entry(b.readUtf(), b.readDouble(), b.readDouble(), b.readDouble()));
            return new PacketAdminGui(l);
        }
        public static void handle(PacketAdminGui m, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> {
                if (c.get().getDirection().getReceptionSide().isClient()) {
                    ClientHooks.openAdmin(m.list);
                } else {
                    ServerPlayer p = c.get().getSender();
                    if(p!=null) {
                        RPGCommands.SpawnData data = RPGCommands.SpawnData.get(p.serverLevel());
                        RPGNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->p), new PacketAdminGui(data.list));
                    }
                }
            });
            c.get().setPacketHandled(true);
        }
    }

    public static class PacketAdminAction {
        public int type; public String n;
        public PacketAdminAction(int t, String n) { this.type=t; this.n=n; }
        public static void enc(PacketAdminAction m, FriendlyByteBuf b) { b.writeInt(m.type); b.writeUtf(m.n); }
        public static PacketAdminAction dec(FriendlyByteBuf b) { return new PacketAdminAction(b.readInt(), b.readUtf()); }
        public static void handle(PacketAdminAction m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer p = ctx.get().getSender();
                if(p != null && p.hasPermissions(2)) {
                    RPGCommands.SpawnData data = RPGCommands.SpawnData.get(p.serverLevel());
                    if(m.type==0) data.add(m.n, p.getX(), p.getY(), p.getZ());
                    if(m.type==1) data.remove(m.n);
                    RPGNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->p), new PacketAdminGui(data.list));
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}