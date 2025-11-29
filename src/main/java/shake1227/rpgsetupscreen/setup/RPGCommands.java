package shake1227.rpgsetupscreen.setup;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import shake1227.rpgsetupscreen.network.RPGNetwork;

import java.util.ArrayList;
import java.util.List;

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
        );
    }

    private int addLoc(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
        SpawnData data = SpawnData.get(ctx.getSource().getLevel());
        data.add(name, pos.x, pos.y, pos.z);
        ctx.getSource().sendSuccess(() -> Component.literal("Added: " + name), true);
        return 1;
    }

    private int removeLoc(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        SpawnData.get(ctx.getSource().getLevel()).remove(name);
        ctx.getSource().sendSuccess(() -> Component.literal("Removed: " + name), true);
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
        public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
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