package shake1227.rpgsetupscreen.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ClientSettingsCache {
    private static final Map<String, CachedData> cache = new HashMap<>();
    private static File file;

    public static boolean enableGender = true;
    public static boolean enableWidth = true;
    public static boolean enableHeight = true;
    public static boolean enableChest = true;
    public static boolean enableChestY = true;
    public static boolean enableChestSep = true;
    public static boolean enableChestAng = true;
    public static boolean enablePhysics = true;

    private static File getFile() {
        if (file == null) {
            file = FMLPaths.CONFIGDIR.get().resolve("rpgsetupscreen_client_cache.dat").toFile();
        }
        return file;
    }

    public static void init() {
        getFile();
        load();
    }

    public static void update(boolean g, boolean w, boolean h, boolean c, boolean cy, boolean cs, boolean ca, boolean phys) {
        enableGender = g; enableWidth = w; enableHeight = h; enableChest = c;
        enableChestY = cy; enableChestSep = cs; enableChestAng = ca; enablePhysics = phys;
    }

    @SuppressWarnings("unchecked")
    public static void load() {
        File f = getFile();
        if (f.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                Map<String, CachedData> data = (Map<String, CachedData>) ois.readObject();
                cache.clear();
                cache.putAll(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void save(int g, float w, float h, float c, float cy, float cs, float ca, boolean phys) {
        String ip = getCurrentServerIP();
        cache.put(ip, new CachedData(g, w, h, c, cy, cs, ca, phys));
        saveToFile();
    }

    private static void saveToFile() {
        File f = getFile();
        try {
            if (f.getParentFile() != null && !f.getParentFile().exists()) {
                f.getParentFile().mkdirs();
            }
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
                oos.writeObject(cache);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clear() {
        String ip = getCurrentServerIP();
        if (cache.containsKey(ip)) {
            cache.remove(ip);
            saveToFile();
        }
    }

    public static CachedData getForCurrentServer() {
        return cache.get(getCurrentServerIP());
    }

    private static String getCurrentServerIP() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCurrentServer() != null) {
            return mc.getCurrentServer().ip;
        }
        return "singleplayer";
    }

    public static class CachedData implements Serializable {
        private static final long serialVersionUID = 1L;
        public int gender;
        public float width, height, chest, chestY, chestSep, chestAng;
        public boolean physics;

        public CachedData(int g, float w, float h, float c, float cy, float cs, float ca, boolean phys) {
            this.gender = g; this.width = w; this.height = h; this.chest = c;
            this.chestY = cy; this.chestSep = cs; this.chestAng = ca; this.physics = phys;
        }
    }
}