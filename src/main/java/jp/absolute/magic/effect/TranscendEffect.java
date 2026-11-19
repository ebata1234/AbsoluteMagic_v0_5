package jp.absolute.magic.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Marker effect. The amplifier (spell level - 1) decides the strength. */
public class TranscendEffect extends MobEffect {
    public TranscendEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
