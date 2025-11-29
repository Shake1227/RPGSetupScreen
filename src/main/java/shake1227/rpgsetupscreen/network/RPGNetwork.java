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
import shake1227.rpgsetupscreen.client.ClientHooks;
import shake1227.rpgsetupscreen.setup.RPGCapability;
import shake1227.rpgsetupscreen.setup.RPGCommands;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class RPGNetwork {
    public static SimpleChannel CHANNEL;
    private static int id = 0;
    private static final String VER = "1";

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(RPGSetupScreen.MODID, "net"), () -> VER, VER::equals, VER::equals);
        CHANNEL.registerMessage(id++, PacketSyncData.class, PacketSyncData::enc, PacketSyncData::dec, PacketSyncData::handle);
        CHANNEL.registerMessage(id++, PacketOpenGui.class, (a,b)->{}, buf->new PacketOpenGui(), PacketOpenGui::handle);
        CHANNEL.registerMessage(id++, PacketFinishSetup.class, PacketFinishSetup::enc, PacketFinishSetup::dec, PacketFinishSetup::handle);
        CHANNEL.registerMessage(id++, PacketAdminGui.class, PacketAdminGui::enc, PacketAdminGui::dec, PacketAdminGui::handle);
        CHANNEL.registerMessage(id++, PacketAdminAction.class, PacketAdminAction::enc, PacketAdminAction::dec, PacketAdminAction::handle);
    }

    public static class PacketSyncData {
        public boolean f; public int g; public float w, h, c;
        public PacketSyncData(boolean f, int g, float w, float h, float c) { this.f=f; this.g=g; this.w=w; this.h=h; this.c=c; }
        public static void enc(PacketSyncData m, FriendlyByteBuf b) { b.writeBoolean(m.f); b.writeInt(m.g); b.writeFloat(m.w); b.writeFloat(m.h); b.writeFloat(m.c); }
        public static PacketSyncData dec(FriendlyByteBuf b) { return new PacketSyncData(b.readBoolean(), b.readInt(), b.readFloat(), b.readFloat(), b.readFloat()); }
        public static void handle(PacketSyncData m, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() -> ClientHooks.handleSync(m.f, m.g, m.w, m.h, m.c));
            c.get().setPacketHandled(true);
        }
    }

    public static class PacketOpenGui {
        public static void handle(PacketOpenGui m, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(ClientHooks::openSetup);
            c.get().setPacketHandled(true);
        }
    }

    public static class PacketFinishSetup {
        public String loc; public int g; public float w, h, c;
        public PacketFinishSetup(String loc, int g, float w, float h, float c) { this.loc=loc; this.g=g; this.w=w; this.h=h; this.c=c; }
        public static void enc(PacketFinishSetup m, FriendlyByteBuf b) { b.writeUtf(m.loc); b.writeInt(m.g); b.writeFloat(m.w); b.writeFloat(m.h); b.writeFloat(m.c); }
        public static PacketFinishSetup dec(FriendlyByteBuf b) { return new PacketFinishSetup(b.readUtf(), b.readInt(), b.readFloat(), b.readFloat(), b.readFloat()); }
        public static void handle(PacketFinishSetup m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer p = ctx.get().getSender();
                if(p == null) return;
                p.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
                    cap.setGender(m.g); cap.setWidth(m.w); cap.setHeight(m.h); cap.setChest(m.c); cap.setFinished(true);
                    RPGNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->p), new PacketSyncData(true, m.g, m.w, m.h, m.c));
                });
                p.setGameMode(GameType.SURVIVAL);
                RPGCommands.SpawnData data = RPGCommands.SpawnData.get(p.serverLevel());
                var opt = data.list.stream().filter(e -> e.name.equals(m.loc)).findFirst();
                if(opt.isPresent()) p.teleportTo(opt.get().x, opt.get().y, opt.get().z);
                else if(m.loc.equals("Initial Spawn Point")) {
                    BlockPos s = p.serverLevel().getSharedSpawnPos();
                    p.teleportTo(s.getX(), s.getY(), s.getZ());
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