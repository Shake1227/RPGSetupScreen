package shake1227.rpgsetupscreen;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import shake1227.rpgsetupscreen.network.RPGNetwork;
import shake1227.rpgsetupscreen.setup.RPGCapability;
import shake1227.rpgsetupscreen.setup.RPGCommands;
import shake1227.rpgsetupscreen.setup.RPGEventHandler;

@Mod(RPGSetupScreen.MODID)
public class RPGSetupScreen {
    public static final String MODID = "rpgsetupscreen";

    public RPGSetupScreen() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 1. Capabilityの登録
        RPGCapability.register(modEventBus);

        // 2. ネットワークと共通セットアップの登録
        modEventBus.addListener(this::commonSetup);

        // 3. サーバーサイドイベント(ログインなど)の登録
        // ここが重要！これがないとログインイベントが発火しません
        MinecraftForge.EVENT_BUS.register(new RPGEventHandler());

        // 4. コマンドの登録
        MinecraftForge.EVENT_BUS.register(new RPGCommands());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // ネットワークの初期化
        RPGNetwork.register();
    }
}