package jp.absolute.magic.trait;

import dev.xkmc.l2hostility.content.traits.base.MobTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** L2Hostility trait "Magician": the mob casts Iron's Spells spells at its target. */
public class MagicianTrait extends MobTrait {

    public MagicianTrait(ChatFormatting format) {
        super(format);
    }

    @Override
    public void tick(LivingEntity entity, int level) {
        if (entity instanceof Mob mob && !mob.level().isClientSide) {
            MagicianCaster.tick(mob, level);
        }
    }
}
