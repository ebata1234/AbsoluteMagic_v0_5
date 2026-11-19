package jp.absolute.magic.spell;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import jp.absolute.magic.config.MagicConfig;
import jp.absolute.magic.registry.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;

public class FixedDamageSpell extends BaseTranscendSpell {
    public FixedDamageSpell() {
        super("fixed_damage", SchoolRegistry.HOLY_RESOURCE);
    }

    @Override
    protected MobEffect effect() {
        return ModEffects.FIXED_DAMAGE.get();
    }

    @Override
    protected int durationSeconds() {
        return MagicConfig.FIXED_DURATION_SECONDS.get();
    }

    @Override
    protected MutableComponent amountText(int spellLevel) {
        double amount = MagicConfig.FIXED_DAMAGE_PER_LEVEL.get() * spellLevel;
        return Component.translatable("ui.absolutemagic.fixed_amount", String.format("%.1f", amount));
    }
}
