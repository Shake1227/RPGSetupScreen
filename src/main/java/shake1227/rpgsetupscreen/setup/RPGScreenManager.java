package shake1227.rpgsetupscreen.setup;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import shake1227.rpgsetupscreen.data.ScreenData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RPGScreenManager {
    private static final String FILE_NAME = "rpgsetupscreen_layouts.nbt";
    private static RPGScreenManager INSTANCE;

    public List<ScreenData.Def> screens = new ArrayList<>();

    public static RPGScreenManager get(MinecraftServer server) {
        if (INSTANCE == null) {
            INSTANCE = new RPGScreenManager();
            INSTANCE.load(server);
        }
        return INSTANCE;
    }

    public static void reload(MinecraftServer server) {
        INSTANCE = new RPGScreenManager();
        INSTANCE.load(server);
    }

    private RPGScreenManager() {}

    public void load(MinecraftServer server) {
        try {
            File file = server.getWorldPath(new LevelResource("serverconfig")).resolve(FILE_NAME).toFile();

            if (file.exists()) {
                CompoundTag tag = NbtIo.readCompressed(file);
                if (tag.contains("screens")) {
                    ListTag list = tag.getList("screens", 10);
                    screens.clear();
                    for (int i = 0; i < list.size(); i++) {
                        screens.add(ScreenData.Def.load(list.getCompound(i)));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        validateAndSortScreens();
    }

    public void save(MinecraftServer server) {
        validateAndSortScreens();
        try {
            File dir = server.getWorldPath(new LevelResource("serverconfig")).toFile();
            if (!dir.exists()) dir.mkdirs();

            File file = dir.toPath().resolve(FILE_NAME).toFile();

            CompoundTag tag = new CompoundTag();
            ListTag list = new ListTag();
            for (ScreenData.Def s : screens) list.add(s.save());
            tag.put("screens", list);

            NbtIo.writeCompressed(tag, file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateScreens(MinecraftServer server, List<ScreenData.Def> newScreens) {
        this.screens = newScreens;
        save(server);
    }

    private void validateAndSortScreens() {
        if (screens.stream().noneMatch(s -> s.uuid.equals("setup"))) {
            ScreenData.Def d = new ScreenData.Def("Character Setup", true);
            d.uuid = "setup";
            screens.add(0, d);
        }
        if (screens.stream().noneMatch(s -> s.uuid.equals("spawn"))) {
            ScreenData.Def d = new ScreenData.Def("Spawn Select", true);
            d.uuid = "spawn";
            screens.add(d);
        }

        ScreenData.Def spawn = null;
        List<ScreenData.Def> others = new ArrayList<>();

        for (ScreenData.Def s : screens) {
            if (s.uuid.equals("spawn")) {
                spawn = s;
            } else {
                others.add(s);
            }
        }

        screens.clear();
        screens.addAll(others);
        if (spawn != null) {
            screens.add(spawn);
        }
    }
}