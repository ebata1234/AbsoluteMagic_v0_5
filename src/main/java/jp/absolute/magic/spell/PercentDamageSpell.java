package jp.absolute.magic.spell;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import jp.absolute.magic.config.MagicConfig;
import jp.absolute.magic.registry.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;

public class PercentDamageSpell extends BaseTranscendSpell {
    public PercentDamageSpell() {
        super("percent_damage", SchoolRegistry.ENDER_RESOURCE);
    }

    @Override
    protected MobEffect effect() {
        return ModEffects.PERCENT_DAMAGE.get();
    }

    @Override
    protected int durationSeconds() {
        return MagicConfig.PERCENT_DURATION_SECONDS.get();
    }

    @Override
    protected MutableComponent amountText(int spellLevel) {
        double percent = MagicConfig.PERCENT_PER_LEVEL.get() * spellLevel * 100.0D;
        String basis = MagicConfig.PERCENT_BASIS.get() == MagicConfig.HpBasis.MAX ? "max" : "current";
        return Component.translatable("ui.absolutemagic.percent_amount." + basis, String.format("%.2f", percent));
    }
}
