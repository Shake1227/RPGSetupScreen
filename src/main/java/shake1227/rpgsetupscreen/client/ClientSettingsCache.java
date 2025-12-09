package shake1227.rpgsetupscreen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ClientSettingsCache {
    private static final Map<String, CachedData> cacheMap = new HashMap<>();
    private static File saveFile;

    public static class CachedData {
        public int gender;
        public float width, height, chest, chestY, chestSep, chestAng;
        public boolean physics;

        public CachedData(int g, float w, float h, float c, float cy, float cs, float ca, boolean phys) {
            this.gender = g; this.width = w; this.height = h; this.chest = c;
            this.chestY = cy; this.chestSep = cs; this.chestAng = ca; this.physics = phys;
        }

        public static CachedData fromNbt(CompoundTag tag) {
            return new CachedData(
                    tag.getInt("g"), tag.getFloat("w"), tag.getFloat("h"), tag.getFloat("c"),
                    tag.getFloat("cy"), tag.getFloat("cs"), tag.getFloat("ca"), tag.getBoolean("p")
            );
        }

        public CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("g", gender); tag.putFloat("w", width); tag.putFloat("h", height);
            tag.putFloat("c", chest); tag.putFloat("cy", chestY); tag.putFloat("cs", chestSep);
            tag.putFloat("ca", chestAng); tag.putBoolean("p", physics);
            return tag;
        }
    }

    private static String getServerId() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            return "SP_" + mc.getSingleplayerServer().getWorldData().getLevelName();
        } else if (mc.getCurrentServer() != null) {
            return "MP_" + mc.getCurrentServer().ip;
        }
        return "UNKNOWN";
    }

    public static void load() {
        if (saveFile == null) {
            saveFile = new File(Minecraft.getInstance().gameDirectory, "config/rpgsetupscreen_client_data.dat");
        }
        cacheMap.clear();
        if (saveFile.exists()) {
            try {
                CompoundTag root = NbtIo.read(saveFile);
                if (root != null) {
                    for (String key : root.getAllKeys()) {
                        cacheMap.put(key, CachedData.fromNbt(root.getCompound(key)));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void save(int g, float w, float h, float c, float cy, float cs, float ca, boolean phys) {
        String id = getServerId();
        if (id.equals("UNKNOWN")) return;

        cacheMap.put(id, new CachedData(g, w, h, c, cy, cs, ca, phys));
        saveDisk();
    }

    private static void saveDisk() {
        if (saveFile == null) return;
        try {
            CompoundTag root = new CompoundTag();
            for (Map.Entry<String, CachedData> entry : cacheMap.entrySet()) {
                root.put(entry.getKey(), entry.getValue().toNbt());
            }
            NbtIo.write(root, saveFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static CachedData getForCurrentServer() {
        return cacheMap.get(getServerId());
    }

    public static void clearCurrentServer() {
        String id = getServerId();
        if (cacheMap.containsKey(id)) {
            cacheMap.remove(id);
            saveDisk();
        }
    }
}