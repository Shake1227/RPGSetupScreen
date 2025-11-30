package shake1227.rpgsetupscreen.setup;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RPGCapability {
    // Capability トークンと取得（公開 API は元コードと同じシンボルを維持）
    public static final Capability<IRPGData> INSTANCE = CapabilityManager.get(new CapabilityToken<IRPGData>() {});

    // 登録リスナをイベントバスに追加
    public static void register(IEventBus bus) {
        bus.addListener(RPGCapability::onRegisterCapabilities);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent ev) {
        ev.register(IRPGData.class);
    }

    // 外部から参照されるインターフェースは元と同じメソッド群を維持
    public interface IRPGData {
        boolean isFinished(); void setFinished(boolean f);
        int getGender(); void setGender(int g);
        float getWidth(); void setWidth(float w);
        float getHeight(); void setHeight(float h);
        float getChest(); void setChest(float c);

        float getChestY(); void setChestY(float y);
        float getChestSep(); void setChestSep(float s);
        float getChestAng(); void setChestAng(float a);

        // 物理（保存不要）
        float getBouncePosL(); void setBouncePosL(float p);
        float getPrevBouncePosL(); void setPrevBouncePosL(float p);
        float getBounceVelL(); void setBounceVelL(float v);

        float getBounceRotL(); void setBounceRotL(float r);
        float getPrevBounceRotL(); void setPrevBounceRotL(float r);
        float getBounceRotVelL(); void setBounceRotVelL(float v);

        float getBouncePosR(); void setBouncePosR(float p);
        float getPrevBouncePosR(); void setPrevBouncePosR(float p);
        float getBounceVelR(); void setBounceVelR(float v);

        float getBounceRotR(); void setBounceRotR(float r);
        float getPrevBounceRotR(); void setPrevBounceRotR(float r);
        float getBounceRotVelR(); void setBounceRotVelR(float v);

        double getPrevPlayerX(); void setPrevPlayerX(double x);
        double getPrevPlayerY(); void setPrevPlayerY(double y);
        double getPrevPlayerZ(); void setPrevPlayerZ(double z);

        void copyFrom(IRPGData other);
    }

    // 実装：元の動作と同等だが内部は配列とヘルパーを使って少し異なる表現にしている
    public static class Imp implements IRPGData {
        // 永続化するプロパティ
        private boolean finished = false;
        private int gender = 0;
        private float width = 1.0f;
        private float height = 1.0f;
        private float chest = 0.0f;
        private float chestY = 0.0f;
        private float chestSep = 0.0f;
        private float chestAng = 0.0f;

        // 物理系をコンパクトに管理する配列（左: index 0-2, 旋回左: 3-5, 右: 6-8, 旋回右: 9-11）
        // 各 3 要素は [pos, prevPos, vel] または [rot, prevRot, rotVel]
        private final float[] physical = new float[12];

        // プレイヤー移動量計算用
        private double prevPlayerX = Double.NaN, prevPlayerY = Double.NaN, prevPlayerZ = Double.NaN;

        // --- 永続化プロパティのアクセサ ---
        public boolean isFinished() { return finished; }
        public void setFinished(boolean f) { this.finished = f; }
        public int getGender() { return gender; }
        public void setGender(int g) { this.gender = g; }
        public float getWidth() { return width; }
        public void setWidth(float w) { this.width = w; }
        public float getHeight() { return height; }
        public void setHeight(float h) { this.height = h; }
        public float getChest() { return chest; }
        public void setChest(float c) { this.chest = c; }

        public float getChestY() { return chestY; }
        public void setChestY(float y) { this.chestY = y; }
        public float getChestSep() { return chestSep; }
        public void setChestSep(float s) { this.chestSep = s; }
        public float getChestAng() { return chestAng; }
        public void setChestAng(float a) { this.chestAng = a; }

        // --- 物理系のゲッター/セッター（配列アクセス） ---
        private static final int L_POS = 0, L_PREV_POS = 1, L_VEL = 2;
        private static final int L_ROT = 3, L_PREV_ROT = 4, L_ROTVEL = 5;
        private static final int R_POS = 6, R_PREV_POS = 7, R_VEL = 8;
        private static final int R_ROT = 9, R_PREV_ROT = 10, R_ROTVEL = 11;

        public float getBouncePosL() { return physical[L_POS]; }
        public void setBouncePosL(float p) { physical[L_POS] = p; }
        public float getPrevBouncePosL() { return physical[L_PREV_POS]; }
        public void setPrevBouncePosL(float p) { physical[L_PREV_POS] = p; }
        public float getBounceVelL() { return physical[L_VEL]; }
        public void setBounceVelL(float v) { physical[L_VEL] = v; }

        public float getBounceRotL() { return physical[L_ROT]; }
        public void setBounceRotL(float r) { physical[L_ROT] = r; }
        public float getPrevBounceRotL() { return physical[L_PREV_ROT]; }
        public void setPrevBounceRotL(float r) { physical[L_PREV_ROT] = r; }
        public float getBounceRotVelL() { return physical[L_ROTVEL]; }
        public void setBounceRotVelL(float v) { physical[L_ROTVEL] = v; }

        public float getBouncePosR() { return physical[R_POS]; }
        public void setBouncePosR(float p) { physical[R_POS] = p; }
        public float getPrevBouncePosR() { return physical[R_PREV_POS]; }
        public void setPrevBouncePosR(float p) { physical[R_PREV_POS] = p; }
        public float getBounceVelR() { return physical[R_VEL]; }
        public void setBounceVelR(float v) { physical[R_VEL] = v; }

        public float getBounceRotR() { return physical[R_ROT]; }
        public void setBounceRotR(float r) { physical[R_ROT] = r; }
        public float getPrevBounceRotR() { return physical[R_PREV_ROT]; }
        public void setPrevBounceRotR(float r) { physical[R_PREV_ROT] = r; }
        public float getBounceRotVelR() { return physical[R_ROTVEL]; }
        public void setBounceRotVelR(float v) { physical[R_ROTVEL] = v; }

        // プレイヤー位置
        public double getPrevPlayerX() { return prevPlayerX; }
        public void setPrevPlayerX(double x) { prevPlayerX = x; }
        public double getPrevPlayerY() { return prevPlayerY; }
        public void setPrevPlayerY(double y) { prevPlayerY = y; }
        public double getPrevPlayerZ() { return prevPlayerZ; }
        public void setPrevPlayerZ(double z) { prevPlayerZ = z; }

        @Override
        public void copyFrom(IRPGData other) {
            // 永続化部分のみコピー（物理は保存不要の意図に従う）
            this.finished = other.isFinished();
            this.gender = other.getGender();
            this.width = other.getWidth();
            this.height = other.getHeight();
            this.chest = other.getChest();
            this.chestY = other.getChestY();
            this.chestSep = other.getChestSep();
            this.chestAng = other.getChestAng();
        }

        // NBT 保存 / 読み込み（キーは元と同じ）
        public void saveNBT(CompoundTag tag) {
            tag.putBoolean("f", finished);
            tag.putInt("g", gender);
            tag.putFloat("w", width);
            tag.putFloat("h", height);
            tag.putFloat("c", chest);
            tag.putFloat("cy", chestY);
            tag.putFloat("cs", chestSep);
            tag.putFloat("ca", chestAng);
        }

        public void loadNBT(CompoundTag tag) {
            finished = tag.getBoolean("f");
            gender = tag.getInt("g");
            width = tag.contains("w") ? tag.getFloat("w") : 1.0f;
            height = tag.contains("h") ? tag.getFloat("h") : 1.0f;
            chest = tag.getFloat("c");
            chestY = tag.getFloat("cy");
            chestSep = tag.getFloat("cs");
            chestAng = tag.getFloat("ca");
        }
    }

    // Provider（シリアライズは CompoundTag を使う）
    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final Imp impl = new Imp();
        private final LazyOptional<IRPGData> capOptional = LazyOptional.of(() -> impl);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return INSTANCE.orEmpty(cap, capOptional);
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            impl.saveNBT(tag);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            impl.loadNBT(nbt);
        }
    }
}