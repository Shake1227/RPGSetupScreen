package shake1227.rpgsetupscreen.client;

public class ClientConfigCache {
    public static boolean enableGender = true;
    public static boolean enableWidth = true;
    public static boolean enableHeight = true;
    public static boolean enableChest = true;
    public static boolean enableChestY = true;
    public static boolean enableChestSep = true;
    public static boolean enableChestAng = true;
    public static boolean enablePhysics = true;

    public static void update(boolean g, boolean w, boolean h, boolean c, boolean cy, boolean cs, boolean ca, boolean phys) {
        enableGender = g;
        enableWidth = w;
        enableHeight = h;
        enableChest = c;
        enableChestY = cy;
        enableChestSep = cs;
        enableChestAng = ca;
        enablePhysics = phys;
    }
}