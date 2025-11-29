package shake1227.rpgsetupscreen.setup;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shake1227.rpgsetupscreen.RPGSetupScreen;

@Mod.EventBusSubscriber(modid = RPGSetupScreen.MODID)
public class RPGCapability {
    public static final Capability<IRPGData> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {});

    public static void register(IEventBus bus) {
        bus.addListener(RPGCapability::registerCaps);
    }

    private static void registerCaps(RegisterCapabilitiesEvent event) {
        event.register(IRPGData.class);
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(INSTANCE).isPresent()) {
                event.addCapability(new ResourceLocation(RPGSetupScreen.MODID, "data"), new Provider());
            }
        }
    }

    public interface IRPGData {
        boolean isFinished();
        void setFinished(boolean finished);
        int getGender();
        void setGender(int gender);
        float getWidth();
        void setWidth(float width);
        float getHeight();
        void setHeight(float height);
        float getChest();
        void setChest(float chest);
        void copyFrom(IRPGData other);
    }

    public static class Imp implements IRPGData {
        private boolean finished = false;
        private int gender = 0;
        private float width = 1.0f;
        private float height = 1.0f;
        private float chest = 0.0f;

        @Override public boolean isFinished() { return finished; }
        @Override public void setFinished(boolean f) { this.finished = f; }
        @Override public int getGender() { return gender; }
        @Override public void setGender(int g) { this.gender = g; }
        @Override public float getWidth() { return width; }
        @Override public void setWidth(float w) { this.width = w; }
        @Override public float getHeight() { return height; }
        @Override public void setHeight(float h) { this.height = h; }
        @Override public float getChest() { return chest; }
        @Override public void setChest(float c) { this.chest = c; }
        @Override public void copyFrom(IRPGData other) {
            this.finished = other.isFinished();
            this.gender = other.getGender();
            this.width = other.getWidth();
            this.height = other.getHeight();
            this.chest = other.getChest();
        }
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final Imp backend = new Imp();
        private final LazyOptional<IRPGData> optional = LazyOptional.of(() -> backend);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return INSTANCE.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("finished", backend.finished);
            tag.putInt("gender", backend.gender);
            tag.putFloat("width", backend.width);
            tag.putFloat("height", backend.height);
            tag.putFloat("chest", backend.chest);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            backend.finished = nbt.getBoolean("finished");
            backend.gender = nbt.getInt("gender");
            backend.width = nbt.getFloat("width");
            backend.height = nbt.getFloat("height");
            backend.chest = nbt.getFloat("chest");
        }
    }
}