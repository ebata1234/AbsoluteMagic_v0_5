package jp.absolute.magic.effect;

import java.util.ArrayList;
import java.util.List;
import jp.absolute.magic.damage.TimeBombHandler;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * The fuse. The remaining duration IS the countdown, so the vanilla effect icon shows the time left and the
 * state survives saving / reloading. Nothing explodes here directly: the tick only reports to TimeBombHandler,
 * which detonates at the end of the server tick (never while the entity is iterating its own effects).
 */
public class TimeBombEffect extends MobEffect {
    public TimeBombEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0 || duration == 1;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            TimeBombHandler.onFuseTick(entity);
        }
    }

    /** Milk and other cures must not defuse the bomb. */
    @Override
    public List<ItemStack> getCurativeItems() {
        return new ArrayList<>();
    }
}
