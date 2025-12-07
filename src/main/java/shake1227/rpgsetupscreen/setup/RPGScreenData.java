package shake1227.rpgsetupscreen.setup;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.saveddata.SavedData;
import shake1227.rpgsetupscreen.data.ScreenData;
import java.util.ArrayList;
import java.util.List;

public class RPGScreenData extends SavedData {
    public List<ScreenData.Def> screens = new ArrayList<>();

    public static RPGScreenData get(net.minecraft.server.level.ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(RPGScreenData::new, RPGScreenData::new, "rpg_screen_config");
    }

    public RPGScreenData() {
        initDefaults();
    }

    public RPGScreenData(CompoundTag tag) {
        if (tag.contains("screens")) {
            ListTag list = tag.getList("screens", 10);
            for (int i = 0; i < list.size(); i++) {
                screens.add(ScreenData.Def.load(list.getCompound(i)));
            }
        }
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
    }

    private void initDefaults() {
        ScreenData.Def setup = new ScreenData.Def("Character Setup", true);
        setup.uuid = "setup";
        screens.add(setup);

        ScreenData.Def spawn = new ScreenData.Def("Spawn Select", true);
        spawn.uuid = "spawn";
        screens.add(spawn);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (ScreenData.Def s : screens) list.add(s.save());
        tag.put("screens", list);
        return tag;
    }

    public void setScreens(List<ScreenData.Def> newScreens) {
        this.screens = newScreens;
        setDirty();
    }
}