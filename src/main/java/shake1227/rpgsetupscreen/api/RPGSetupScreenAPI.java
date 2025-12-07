package shake1227.rpgsetupscreen.api;

import net.minecraft.world.entity.player.Player;
import shake1227.rpgsetupscreen.setup.RPGCapability;

public class RPGSetupScreenAPI {

    public static boolean isFinished(Player player) {
        return getCap(player).map(RPGCapability.IRPGData::isFinished).orElse(false);
    }

    public static int getGender(Player player) {
        return getCap(player).map(RPGCapability.IRPGData::getGender).orElse(0);
    }

    public static boolean isFemale(Player player) {
        return getGender(player) == 1;
    }


    public static float getBodyWidth(Player player) {
        return getCap(player).map(RPGCapability.IRPGData::getWidth).orElse(1.0f);
    }

    public static float getBodyHeight(Player player) {
        return getCap(player).map(RPGCapability.IRPGData::getHeight).orElse(1.0f);
    }

    public static float getChestSize(Player player) {
        return getCap(player).map(RPGCapability.IRPGData::getChest).orElse(0.0f);
    }

    public static float getChestY(Player player) {
        return getCap(player).map(RPGCapability.IRPGData::getChestY).orElse(0.0f);
    }

    public static float getChestSeparation(Player player) {
        return getCap(player).map(RPGCapability.IRPGData::getChestSep).orElse(0.0f);
    }

    public static float getChestAngle(Player player) {
        return getCap(player).map(RPGCapability.IRPGData::getChestAng).orElse(0.0f);
    }

    public static boolean isPhysicsEnabled(Player player) {
        return getCap(player).map(RPGCapability.IRPGData::isPhysicsEnabled).orElse(true);
    }

    private static net.minecraftforge.common.util.LazyOptional<RPGCapability.IRPGData> getCap(Player player) {
        if (player == null) return net.minecraftforge.common.util.LazyOptional.empty();
        return player.getCapability(RPGCapability.INSTANCE);
    }
}