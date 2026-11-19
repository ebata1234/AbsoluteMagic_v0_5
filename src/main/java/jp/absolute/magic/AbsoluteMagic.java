package jp.absolute.magic;

import com.mojang.logging.LogUtils;
import jp.absolute.magic.config.MagicConfig;
import jp.absolute.magic.config.MagicianConfig;
import jp.absolute.magic.damage.TimeBombHandler;
import jp.absolute.magic.damage.TranscendHandler;
import jp.absolute.magic.registry.ModEffects;
import jp.absolute.magic.registry.ModSpells;
import jp.absolute.magic.registry.ModTraits;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(AbsoluteMagic.MODID)
public class AbsoluteMagic {
    public static final String MODID = "absolutemagic";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AbsoluteMagic() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEffects.EFFECTS.register(modBus);
        ModSpells.SPELLS.register(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MagicConfig.SPEC, "absolutemagic-common.toml");
        ModTraits.init();   // L2Hostility trait "Magician"
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MagicianConfig.SPEC, "absolutemagic-magician.toml");
        MinecraftForge.EVENT_BUS.register(TranscendHandler.class);
        MinecraftForge.EVENT_BUS.register(TimeBombHandler.class);
    }
}
