package shake1227.rpgsetupscreen.setup;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import shake1227.rpgsetupscreen.network.RPGNetwork;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RPGCommands {
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rpgsetupscreen")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("locate")
                        .then(Commands.literal("add")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                                .executes(this::addLoc))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .executes(this::removeLoc)))
                        .then(Commands.literal("gui")
                                .executes(this::openGui))
                )
                .then(Commands.literal("resetup")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("viewer", EntityArgument.player())
                                        .executes(this::resetupPlayer)))
                )
                .then(Commands.literal("resetdata")
                        .then(Commands.argument("targetName", StringArgumentType.string())
                                .executes(this::resetData)))
        );
    }

    private int resetData(CommandContext<CommandSourceStack> ctx) {
        String targetName = StringArgumentType.getString(ctx, "targetName");
        var server = ctx.getSource().getServer();
        var profile = server.getProfileCache().get(targetName).orElse(null);

        if (profile == null) {
            ctx.getSource().sendFailure(Component.translatable("command.rpgsetupscreen.reset_fail", targetName));
            return 0;
        }

        UUID id = profile.getId();
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayer(id);

        if (onlinePlayer != null) {
            onlinePlayer.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
                cap.setFinished(false);
                // 修正: onlinePlayer.getId() を追加
                RPGNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> onlinePlayer),
                        new RPGNetwork.PacketSyncData(onlinePlayer.getId(), false, 0, 1f, 1f, 0f, 0f, 0f, 0f, true));
            });
        } else {
            try {
                File playerDataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
                File playerFile = new File(playerDataDir, id.toString() + ".dat");

                if (playerFile.exists()) {
                    CompoundTag tag = NbtIo.readCompressed(playerFile);
                    if (tag.contains("ForgeCaps")) {
                        CompoundTag caps = tag.getCompound("ForgeCaps");
                        caps.remove("rpgsetupscreen:rpg_data");
                        tag.put("ForgeCaps", caps);
                        NbtIo.writeCompressed(tag, playerFile);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                ctx.getSource().sendFailure(Component.literal("Error modifying player data: " + e.getMessage()));
                return 0;
            }
        }

        ctx.getSource().sendSuccess(() -> Component.translatable("command.rpgsetupscreen.reset_success", targetName), true);
        return 1;
    }

    private int resetupPlayer(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
            ServerPlayer viewer = EntityArgument.getPlayer(ctx, "viewer");

            target.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
                RPGNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer),
                        new RPGNetwork.PacketOpenEditor(
                                target.getUUID(),
                                cap.getGender(),
                                cap.getWidth(),
                                cap.getHeight(),
                                cap.getChest(),
                                cap.getChestY(),
                                cap.getChestSep(),
                                cap.getChestAng(),
                                cap.isPhysicsEnabled()
                        ));
            });

            ctx.getSource().sendSuccess(() -> Component.translatable("gui.rpgsetupscreen.editing_target", target.getName().getString()), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private int addLoc(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
        SpawnData data = SpawnData.get(ctx.getSource().getLevel());
        data.add(name, pos.x, pos.y, pos.z);
        ctx.getSource().sendSuccess(() -> Component.translatable("gui.rpgsetupscreen.add_loc"), true);
        return 1;
    }

    private int removeLoc(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        SpawnData.get(ctx.getSource().getLevel()).remove(name);
        ctx.getSource().sendSuccess(() -> Component.translatable("gui.rpgsetupscreen.remove", name), true);
        return 1;
    }

    private int openGui(CommandContext<CommandSourceStack> ctx) {
        if(ctx.getSource().getEntity() instanceof ServerPlayer p) {
            SpawnData data = SpawnData.get(p.serverLevel());
            RPGNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new RPGNetwork.PacketAdminGui(data.list));
        }
        return 1;
    }

    public static class SpawnData extends SavedData {
        public List<Entry> list = new ArrayList<>();

        public static SpawnData get(net.minecraft.server.level.ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(SpawnData::new, SpawnData::new, "rpg_spawns");
        }

        public SpawnData() {}
        public SpawnData(CompoundTag tag) {
            ListTag l = tag.getList("locs", 10);
            for(int i=0; i<l.size(); i++) {
                CompoundTag t = l.getCompound(i);
                list.add(new Entry(t.getString("n"), t.getDouble("x"), t.getDouble("y"), t.getDouble("z")));
            }
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag l = new ListTag();
            for(Entry e : list) {
                CompoundTag t = new CompoundTag();
                t.putString("n", e.name); t.putDouble("x", e.x); t.putDouble("y", e.y); t.putDouble("z", e.z);
                l.add(t);
            }
            tag.put("locs", l);
            return tag;
        }

        public void add(String n, double x, double y, double z) {
            list.removeIf(e -> e.name.equals(n));
            list.add(new Entry(n, x, y, z));
            setDirty();
        }

        public void remove(String n) {
            if(list.removeIf(e -> e.name.equals(n))) setDirty();
        }

        public static class Entry {
            public String name; public double x, y, z;
            public Entry(String n, double x, double y, double z) { this.name=n; this.x=x; this.y=y; this.z=z; }
        }
    }
}