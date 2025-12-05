package shake1227.rpgsetupscreen;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import shake1227.rpgsetupscreen.client.KeyInit;
import shake1227.rpgsetupscreen.network.RPGNetwork;
import shake1227.rpgsetupscreen.setup.RPGCapability;
import shake1227.rpgsetupscreen.setup.RPGCommands;
import shake1227.rpgsetupscreen.setup.RPGConfig;
import shake1227.rpgsetupscreen.setup.RPGEventHandler;

@Mod(RPGSetupScreen.MODID)
public class RPGSetupScreen {
    public static final String MODID = "rpgsetupscreen";

    public RPGSetupScreen() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // コンフィグ登録
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, RPGConfig.SPEC);

        RPGCapability.register(modEventBus);
        modEventBus.addListener(KeyInit::register);

        RPGNetwork.register();

        MinecraftForge.EVENT_BUS.register(new RPGEventHandler());
        MinecraftForge.EVENT_BUS.register(new RPGCommands());
    }
}