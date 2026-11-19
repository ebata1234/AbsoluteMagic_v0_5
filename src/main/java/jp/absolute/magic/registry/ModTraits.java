package jp.absolute.magic.registry;

import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.xkmc.l2hostility.content.config.TraitConfig;
import dev.xkmc.l2hostility.init.entries.LHRegistrate;
import jp.absolute.magic.AbsoluteMagic;
import jp.absolute.magic.trait.MagicianTrait;
import net.minecraft.ChatFormatting;

/** L2Hostility traits added by Absolute Magic. Call init() from the mod constructor. */
public final class ModTraits {
    /** Registrate of this mod (its constructor hooks into this mod's event bus). */
    public static final LHRegistrate REGISTRATE = new LHRegistrate(AbsoluteMagic.MODID);

    public static final RegistryEntry<MagicianTrait> MAGICIAN = REGISTRATE
            .regTrait("magician",
                    () -> new MagicianTrait(ChatFormatting.LIGHT_PURPLE),
                    // only used for data generation; the real values come from
                    // data/absolutemagic/l2hostility_config/trait/magician.json
                    rl -> new TraitConfig(rl, 40, 30, 5, 80))
            .lang("Magician")
            .register();

    public static void init() {
        // touching this class registers the trait
    }

    private ModTraits() {}
}
