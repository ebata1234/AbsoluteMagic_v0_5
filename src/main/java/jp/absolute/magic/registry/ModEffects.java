package jp.absolute.magic.registry;

import jp.absolute.magic.AbsoluteMagic;
import jp.absolute.magic.effect.TimeBombEffect;
import jp.absolute.magic.effect.TranscendEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, AbsoluteMagic.MODID);

    public static final RegistryObject<MobEffect> FIXED_DAMAGE = EFFECTS.register(
            "fixed_damage", () -> new TranscendEffect(MobEffectCategory.BENEFICIAL, 0xFFD54F));

    public static final RegistryObject<MobEffect> PERCENT_DAMAGE = EFFECTS.register(
            "percent_damage", () -> new TranscendEffect(MobEffectCategory.BENEFICIAL, 0xB388FF));

    public static final RegistryObject<MobEffect> TIME_BOMB = EFFECTS.register(
            "time_bomb", () -> new TimeBombEffect(MobEffectCategory.HARMFUL, 0xFF5722));

    private ModEffects() {
    }
}
