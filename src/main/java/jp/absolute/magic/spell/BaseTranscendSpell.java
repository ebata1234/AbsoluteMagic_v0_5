package jp.absolute.magic.spell;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import java.util.List;
import jp.absolute.magic.AbsoluteMagic;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Self buff shared by Fixed Strike and Percent Strike. Spell level (1-5) becomes the effect amplifier. */
public abstract class BaseTranscendSpell extends AbstractSpell {
    private final ResourceLocation spellId;
    private final DefaultConfig defaultConfig;

    protected BaseTranscendSpell(String name, ResourceLocation school) {
        this.spellId = ResourceLocation.fromNamespaceAndPath(AbsoluteMagic.MODID, name);
        this.defaultConfig = new DefaultConfig()
                .setMinRarity(SpellRarity.RARE)
                .setSchoolResource(school)
                .setMaxLevel(5)
                .setCooldownSeconds(45.0)
                .build();
        this.manaCostPerLevel = 15;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
        this.baseManaCost = 80;
    }

    protected abstract MobEffect effect();

    protected abstract MutableComponent amountText(int spellLevel);

    protected abstract int durationSeconds();

    @Override
    public ResourceLocation getSpellResource() {
        return this.spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return this.defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.SELF_CAST_ANIMATION;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                amountText(spellLevel),
                Component.translatable("ui.absolutemagic.transcend_duration", durationSeconds()));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            int ticks = durationSeconds() * 20;
            entity.addEffect(new MobEffectInstance(effect(), ticks, spellLevel - 1, false, false, true));
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
