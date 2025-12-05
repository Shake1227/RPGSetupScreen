package shake1227.rpgsetupscreen.setup;

import net.minecraftforge.common.ForgeConfigSpec;

public class RPGConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_GENDER;
    public static final ForgeConfigSpec.BooleanValue ENABLE_WIDTH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_HEIGHT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CHEST_SIZE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CHEST_Y;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CHEST_SEP;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CHEST_ANG;
    public static final ForgeConfigSpec.BooleanValue ENABLE_PHYSICS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("RPG Setup Screen Configuration").push("restrictions");

        ENABLE_GENDER = builder.comment("Allow changing gender").define("allow_gender_change", true);
        ENABLE_WIDTH = builder.comment("Allow changing body width").define("allow_width_change", true);
        ENABLE_HEIGHT = builder.comment("Allow changing body height").define("allow_height_change", true);
        ENABLE_CHEST_SIZE = builder.comment("Allow changing chest size").define("allow_chest_size_change", true);
        ENABLE_CHEST_Y = builder.comment("Allow changing chest Y position").define("allow_chest_y_change", true);
        ENABLE_CHEST_SEP = builder.comment("Allow changing chest separation").define("allow_chest_sep_change", true);
        ENABLE_CHEST_ANG = builder.comment("Allow changing chest angle").define("allow_chest_ang_change", true);
        ENABLE_PHYSICS = builder.comment("Allow toggling physics").define("allow_physics_toggle", true);

        builder.pop();
        SPEC = builder.build();
    }
}