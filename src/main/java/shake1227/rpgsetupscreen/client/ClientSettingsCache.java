package shake1227.rpgsetupscreen.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ClientSettingsCache {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get();
    private static final File CACHE_FILE = CONFIG_DIR.resolve("rpgsetupscreen_cache.json").toFile();

    public static class CachedData {
        public int gender;
        public float width, height, chest, chestY, chestSep, chestAng;
        public boolean physics;

        public CachedData(int g, float w, float h, float c, float cy, float cs, float ca, boolean physics) {
            this.gender=g; this.width=w; this.height=h; this.chest=c;
            this.chestY=cy; this.chestSep=cs; this.chestAng=ca;
            this.physics = physics;
        }
    }

    private static Map<String, CachedData> cacheMap = new HashMap<>();

    public static void load() {
        if (!CACHE_FILE.exists()) return;
        try (FileReader reader = new FileReader(CACHE_FILE)) {
            Map<String, CachedData> data = GSON.fromJson(reader, new com.google.gson.reflect.TypeToken<Map<String, CachedData>>(){}.getType());
            if (data != null) cacheMap = data;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save(int g, float w, float h, float c, float cy, float cs, float ca, boolean physics) {
        String key = getServerKey();
        if (key == null) return;

        cacheMap.put(key, new CachedData(g, w, h, c, cy, cs, ca, physics));

        try (FileWriter writer = new FileWriter(CACHE_FILE)) {
            GSON.toJson(cacheMap, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static CachedData getForCurrentServer() {
        String key = getServerKey();
        return (key != null) ? cacheMap.get(key) : null;
    }

    private static String getServerKey() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCurrentServer() != null) {
            return mc.getCurrentServer().ip;
        } else if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
            return "SP_" + mc.getSingleplayerServer().getWorldData().getLevelName();
        }
        return null;
    }
}