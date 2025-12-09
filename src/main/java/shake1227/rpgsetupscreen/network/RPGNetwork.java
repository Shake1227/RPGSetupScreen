package shake1227.rpgsetupscreen.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import shake1227.rpgsetupscreen.RPGSetupScreen;
import shake1227.rpgsetupscreen.client.ClientConfigCache;
import shake1227.rpgsetupscreen.client.ClientHooks;
import shake1227.rpgsetupscreen.client.LogoRenderer;
import shake1227.rpgsetupscreen.data.ScreenData;
import shake1227.rpgsetupscreen.item.ItemInit;
import shake1227.rpgsetupscreen.setup.RPGCapability;
import shake1227.rpgsetupscreen.setup.RPGCommands;
import shake1227.rpgsetupscreen.setup.RPGScreenManager;
import shake1227.rpgsetupscreen.util.ModernNotificationHandler;
import shake1227.rpgsetupscreen.util.TextFormatter;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class RPGNetwork {
    public static SimpleChannel CHANNEL;
    private static int id = 0;
    private static final String VER = "10";

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
        CHANNEL.registerMessage(id++, PacketSyncConfig.class, PacketSyncConfig::enc, PacketSyncConfig::dec, PacketSyncConfig::handle);
        CHANNEL.registerMessage(id++, PacketForceReset.class, (a,b)->{}, buf->new PacketForceReset(), PacketForceReset::handle);
        CHANNEL.registerMessage(id++, PacketSyncScreens.class, PacketSyncScreens::enc, PacketSyncScreens::dec, PacketSyncScreens::handle);
        CHANNEL.registerMessage(id++, PacketSaveScreens.class, PacketSaveScreens::enc, PacketSaveScreens::dec, PacketSaveScreens::handle);
        CHANNEL.registerMessage(id++, PacketOpenManager.class, (a,b)->{}, buf->new PacketOpenManager(), PacketOpenManager::handle);
        CHANNEL.registerMessage(id++, PacketShowNotification.class, PacketShowNotification::enc, PacketShowNotification::dec, PacketShowNotification::handle);
        CHANNEL.registerMessage(id++, PacketSyncLogo.class, PacketSyncLogo::enc, PacketSyncLogo::dec, PacketSyncLogo::handle);
        CHANNEL.registerMessage(id++, PacketRequestScreens.class, (a,b)->{}, buf->new PacketRequestScreens(), PacketRequestScreens::handle);
    }

    public static class PacketRequestScreens {
        public static void handle(PacketRequestScreens m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer p = ctx.get().getSender();
                if (p != null) {
                    RPGScreenManager manager = RPGScreenManager.get(p.server);
                    RPGNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new PacketSyncScreens(manager.screens));
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class PacketFinishSetup {
        public String loc; public int g; public float w, h, c, cy, cs, ca; public boolean physics;
        public String targetUUID;
        public CompoundTag customInputs;
        public boolean consumeItem;

        public PacketFinishSetup(String loc, int g, float w, float h, float c, float cy, float cs, float ca, boolean physics, String targetUUID, CompoundTag customInputs, boolean consumeItem) {
            this.loc=loc; this.g=g; this.w=w; this.h=h; this.c=c;
            this.cy=cy; this.cs=cs; this.ca=ca; this.physics=physics;
            this.targetUUID = targetUUID;
            this.customInputs = customInputs;
            this.consumeItem = consumeItem;
        }
        public PacketFinishSetup(String loc, int g, float w, float h, float c, float cy, float cs, float ca, boolean physics, String targetUUID) { this(loc, g, w, h, c, cy, cs, ca, physics, targetUUID, new CompoundTag(), false); }
        public PacketFinishSetup(String loc, int g, float w, float h, float c, float cy, float cs, float ca, boolean physics, String targetUUID, CompoundTag customInputs) { this(loc, g, w, h, c, cy, cs, ca, physics, targetUUID, customInputs, false); }

        public static void enc(PacketFinishSetup m, FriendlyByteBuf b) {
            b.writeUtf(m.loc); b.writeInt(m.g); b.writeFloat(m.w); b.writeFloat(m.h); b.writeFloat(m.c);
            b.writeFloat(m.cy); b.writeFloat(m.cs); b.writeFloat(m.ca); b.writeBoolean(m.physics);
            b.writeUtf(m.targetUUID);
            b.writeNbt(m.customInputs);
            b.writeBoolean(m.consumeItem);
        }
        public static PacketFinishSetup dec(FriendlyByteBuf b) {
            return new PacketFinishSetup(b.readUtf(), b.readInt(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readBoolean(), b.readUtf(), b.readNbt(), b.readBoolean());
        }
        public static void handle(PacketFinishSetup m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer p = ctx.get().getSender();
                if(p == null) return;

                System.out.println("[RPGSetupScreen] Received PacketFinishSetup (Repair/Update) from " + p.getName().getString());

                if (m.consumeItem) {
                    int slot = -1;
                    for(int i=0; i<p.getInventory().getContainerSize(); i++) { if (p.getInventory().getItem(i).getItem() == ItemInit.FLOPPY_DISK.get()) { slot = i; break; } }
                    if (slot != -1) { p.getInventory().removeItem(slot, 1); ModernNotificationHandler.sendServerNotification(p, "message.rpgsetupscreen.floppy_used", new ArrayList<>(), "success"); }
                }

                ServerPlayer targetPlayer = p;
                if(!m.targetUUID.isEmpty() && p.hasPermissions(2)) {
                    UUID uuid = null;
                    try {
                        uuid = UUID.fromString(m.targetUUID);
                    } catch (IllegalArgumentException e) {
                        if (m.targetUUID.length() == 32) {
                            try {
                                String formatted = m.targetUUID.replaceFirst(
                                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
                                uuid = UUID.fromString(formatted);
                            } catch (Exception ignored) {}
                        }
                    }

                    if (uuid != null) {
                        ServerPlayer t = p.server.getPlayerList().getPlayer(uuid);
                        if(t != null) targetPlayer = t;
                    }
                }
                final ServerPlayer fp = targetPlayer;

                fp.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
                    boolean wasFinished = cap.isFinished();
                    cap.setGender(m.g); cap.setWidth(m.w); cap.setHeight(m.h); cap.setChest(m.c); cap.setChestY(m.cy); cap.setChestSep(m.cs); cap.setChestAng(m.ca); cap.setPhysicsEnabled(m.physics); cap.setFinished(true);

                    RPGNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->fp), new PacketSyncData(fp.getId(), true, m.g, m.w, m.h, m.c, m.cy, m.cs, m.ca, m.physics));

                    if (!wasFinished && fp.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) { fp.setGameMode(GameType.SURVIVAL); }
                });

                if(!m.loc.isEmpty()) {
                    RPGCommands.SpawnData data = RPGCommands.SpawnData.get(fp.serverLevel());
                    var opt = data.list.stream().filter(e -> e.name.equals(m.loc)).findFirst();
                    if(opt.isPresent()) fp.teleportTo(opt.get().x, opt.get().y, opt.get().z);
                    else if(m.loc.equals("Initial Spawn Point")) { BlockPos s = fp.serverLevel().getSharedSpawnPos(); fp.teleportTo(s.getX(), s.getY(), s.getZ()); }
                }


                boolean isReEdit = m.consumeItem || !m.targetUUID.isEmpty();
                if (!isReEdit) {
                    RPGScreenManager manager = RPGScreenManager.get(fp.server);
                    for(ScreenData.Def def : manager.screens) {
                        for (String rawCmd : def.executeCommands) {
                            String cmd = rawCmd.replace("%player%", fp.getName().getString());
                            if (m.customInputs != null) {
                                for(String key : m.customInputs.getAllKeys()) {
                                    cmd = cmd.replace("%value-" + key + "%", m.customInputs.getString(key));
                                }
                            }
                            fp.server.getCommands().performPrefixedCommand(fp.server.createCommandSourceStack().withPermission(4).withSuppressedOutput(), cmd);
                        }
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class PacketSyncLogo {
        public byte[] data;
        public PacketSyncLogo(byte[] data) { this.data = data; }
        public static void enc(PacketSyncLogo m, FriendlyByteBuf b) { b.writeByteArray(m.data); }
        public static PacketSyncLogo dec(FriendlyByteBuf b) { return new PacketSyncLogo(b.readByteArray()); }
        public static void handle(PacketSyncLogo m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> LogoRenderer.setLogoData(m.data)));
            ctx.get().setPacketHandled(true);
        }
    }

    public static class PacketShowNotification {
        public String translationKey; public List<String> args; public String category;
        public PacketShowNotification(String key, List<String> args, String category) { this.translationKey = key; this.args = args; this.category = category; }
        public static void enc(PacketShowNotification m, FriendlyByteBuf b) { b.writeUtf(m.translationKey); b.writeCollection(m.args, FriendlyByteBuf::writeUtf); b.writeUtf(m.category); }
        public static PacketShowNotification dec(FriendlyByteBuf b) { return new PacketShowNotification(b.readUtf(), b.readList(FriendlyByteBuf::readUtf), b.readUtf()); }
        public static void handle(PacketShowNotification m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.showClientNotification(m.translationKey, m.category)));
            ctx.get().setPacketHandled(true);
        }
    }

    public static class PacketForceReset { public static void handle(PacketForceReset m, Supplier<NetworkEvent.Context> c) { c.get().enqueueWork(ClientHooks::handleForceReset); c.get().setPacketHandled(true); }}
    public static class PacketSyncConfig { public boolean g, w, h, c, cy, cs, ca, phys; public PacketSyncConfig(boolean g, boolean w, boolean h, boolean c, boolean cy, boolean cs, boolean ca, boolean phys) { this.g=g; this.w=w; this.h=h; this.c=c; this.cy=cy; this.cs=cs; this.ca=ca; this.phys=phys; } public static void enc(PacketSyncConfig m, FriendlyByteBuf b) { b.writeBoolean(m.g); b.writeBoolean(m.w); b.writeBoolean(m.h); b.writeBoolean(m.c); b.writeBoolean(m.cy); b.writeBoolean(m.cs); b.writeBoolean(m.ca); b.writeBoolean(m.phys); } public static PacketSyncConfig dec(FriendlyByteBuf b) { return new PacketSyncConfig(b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readBoolean()); } public static void handle(PacketSyncConfig m, Supplier<NetworkEvent.Context> c) { c.get().enqueueWork(() -> ClientConfigCache.update(m.g, m.w, m.h, m.c, m.cy, m.cs, m.ca, m.phys)); c.get().setPacketHandled(true); } }
    public static class PacketSyncData { public int entityId; public boolean f; public int g; public float w, h, c, cy, cs, ca; public boolean physics; public PacketSyncData(int entityId, boolean f, int g, float w, float h, float c, float cy, float cs, float ca, boolean physics) { this.entityId = entityId; this.f=f; this.g=g; this.w=w; this.h=h; this.c=c; this.cy=cy; this.cs=cs; this.ca=ca; this.physics=physics; } public static void enc(PacketSyncData m, FriendlyByteBuf b) { b.writeInt(m.entityId); b.writeBoolean(m.f); b.writeInt(m.g); b.writeFloat(m.w); b.writeFloat(m.h); b.writeFloat(m.c); b.writeFloat(m.cy); b.writeFloat(m.cs); b.writeFloat(m.ca); b.writeBoolean(m.physics); } public static PacketSyncData dec(FriendlyByteBuf b) { return new PacketSyncData(b.readInt(), b.readBoolean(), b.readInt(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readBoolean()); } public static void handle(PacketSyncData m, Supplier<NetworkEvent.Context> c) { c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.handleSync(m.entityId, m.f, m.g, m.w, m.h, m.c, m.cy, m.cs, m.ca, m.physics))); c.get().setPacketHandled(true); } }
    public static class PacketTogglePhysics { public static void handle(PacketTogglePhysics m, Supplier<NetworkEvent.Context> ctx) { ctx.get().enqueueWork(() -> { ServerPlayer p = ctx.get().getSender(); if(p != null) { p.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> { if (cap.getGender() == 1) { boolean newState = !cap.isPhysicsEnabled(); cap.setPhysicsEnabled(newState); RPGNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->p), new PacketSyncData(p.getId(), true, cap.getGender(), cap.getWidth(), cap.getHeight(), cap.getChest(), cap.getChestY(), cap.getChestSep(), cap.getChestAng(), newState)); } }); } }); ctx.get().setPacketHandled(true); } }
    public static class PacketOpenGui { public static void handle(PacketOpenGui m, Supplier<NetworkEvent.Context> c) { c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientHooks::openSetup)); c.get().setPacketHandled(true); } }
    public static class PacketOpenEditor { public UUID targetId; public int g; public float w, h, c, cy, cs, ca; public boolean physics; public PacketOpenEditor(UUID targetId, int g, float w, float h, float c, float cy, float cs, float ca, boolean physics) { this.targetId = targetId; this.g = g; this.w = w; this.h = h; this.c = c; this.cy=cy; this.cs=cs; this.ca=ca; this.physics=physics; } public static void enc(PacketOpenEditor m, FriendlyByteBuf b) { b.writeUUID(m.targetId); b.writeInt(m.g); b.writeFloat(m.w); b.writeFloat(m.h); b.writeFloat(m.c); b.writeFloat(m.cy); b.writeFloat(m.cs); b.writeFloat(m.ca); b.writeBoolean(m.physics); } public static PacketOpenEditor dec(FriendlyByteBuf b) { return new PacketOpenEditor(b.readUUID(), b.readInt(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readBoolean()); } public static void handle(PacketOpenEditor m, Supplier<NetworkEvent.Context> c) { c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openSetupForTarget(m.targetId, m.g, m.w, m.h, m.c, m.cy, m.cs, m.ca, m.physics))); c.get().setPacketHandled(true); } }
    public static class PacketRequestSync { public static void handle(PacketRequestSync m, Supplier<NetworkEvent.Context> ctx) { ctx.get().enqueueWork(() -> { ServerPlayer p = ctx.get().getSender(); if(p != null) { p.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> { RPGNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->p), new PacketSyncData(p.getId(), cap.isFinished(), cap.getGender(), cap.getWidth(), cap.getHeight(), cap.getChest(), cap.getChestY(), cap.getChestSep(), cap.getChestAng(), cap.isPhysicsEnabled())); }); } }); ctx.get().setPacketHandled(true); } }
    public static class PacketAdminGui { public List<RPGCommands.SpawnData.Entry> list; public PacketAdminGui(List<RPGCommands.SpawnData.Entry> l) { this.list=l; } public static void enc(PacketAdminGui m, FriendlyByteBuf b) { if(m.list == null) { b.writeInt(0); return; } b.writeInt(m.list.size()); m.list.forEach(e->{b.writeUtf(e.name); b.writeDouble(e.x); b.writeDouble(e.y); b.writeDouble(e.z);}); } public static PacketAdminGui dec(FriendlyByteBuf b) { List<RPGCommands.SpawnData.Entry> l = new ArrayList<>(); int s = b.readInt(); for(int i=0; i<s; i++) l.add(new RPGCommands.SpawnData.Entry(b.readUtf(), b.readDouble(), b.readDouble(), b.readDouble())); return new PacketAdminGui(l); } public static void handle(PacketAdminGui m, Supplier<NetworkEvent.Context> c) { c.get().enqueueWork(() -> { if (c.get().getDirection().getReceptionSide().isClient()) { DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openAdmin(m.list)); } else { ServerPlayer p = c.get().getSender(); if(p!=null) { RPGCommands.SpawnData data = RPGCommands.SpawnData.get(p.serverLevel()); RPGNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->p), new PacketAdminGui(data.list)); } } }); c.get().setPacketHandled(true); } }
    public static class PacketAdminAction { public int type; public String n; public PacketAdminAction(int t, String n) { this.type=t; this.n=n; } public static void enc(PacketAdminAction m, FriendlyByteBuf b) { b.writeInt(m.type); b.writeUtf(m.n); } public static PacketAdminAction dec(FriendlyByteBuf b) { return new PacketAdminAction(b.readInt(), b.readUtf()); } public static void handle(PacketAdminAction m, Supplier<NetworkEvent.Context> ctx) { ctx.get().enqueueWork(() -> { ServerPlayer p = ctx.get().getSender(); if(p != null && p.hasPermissions(2)) { RPGCommands.SpawnData data = RPGCommands.SpawnData.get(p.serverLevel()); if(m.type==0) data.add(m.n, p.getX(), p.getY(), p.getZ()); if(m.type==1) data.remove(m.n); RPGNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->p), new PacketAdminGui(data.list)); } }); ctx.get().setPacketHandled(true); } }
    public static class PacketSyncScreens { public List<ScreenData.Def> screens; public PacketSyncScreens(List<ScreenData.Def> s) { this.screens = s; } public static void enc(PacketSyncScreens m, FriendlyByteBuf b) { CompoundTag tag = new CompoundTag(); ListTag list = new ListTag(); for(ScreenData.Def d : m.screens) list.add(d.save()); tag.put("list", list); b.writeNbt(tag); } public static PacketSyncScreens dec(FriendlyByteBuf b) { CompoundTag tag = b.readNbt(); List<ScreenData.Def> screens = new ArrayList<>(); if(tag != null) { ListTag list = tag.getList("list", 10); for(int i=0; i<list.size(); i++) screens.add(ScreenData.Def.load(list.getCompound(i))); } return new PacketSyncScreens(screens); } public static void handle(PacketSyncScreens m, Supplier<NetworkEvent.Context> c) { c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.updateScreenDefs(m.screens))); c.get().setPacketHandled(true); } }
    public static class PacketSaveScreens { public List<ScreenData.Def> screens; public PacketSaveScreens(List<ScreenData.Def> s) { this.screens = s; } public static void enc(PacketSaveScreens m, FriendlyByteBuf b) { new PacketSyncScreens(m.screens).enc(new PacketSyncScreens(m.screens), b); } public static PacketSaveScreens dec(FriendlyByteBuf b) { return new PacketSaveScreens(PacketSyncScreens.dec(b).screens); } public static void handle(PacketSaveScreens m, Supplier<NetworkEvent.Context> c) { c.get().enqueueWork(() -> { ServerPlayer p = c.get().getSender(); if(p != null && p.hasPermissions(2)) { RPGScreenManager manager = RPGScreenManager.get(p.server); manager.updateScreens(p.server, m.screens); RPGNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new PacketSyncScreens(manager.screens)); } }); c.get().setPacketHandled(true); } }
    public static class PacketOpenManager { public static void handle(PacketOpenManager m, Supplier<NetworkEvent.Context> c) { c.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientHooks::openManager)); c.get().setPacketHandled(true); } }
}