package jp.absolute.magic.trait;

import dev.xkmc.l2hostility.content.capability.mob.MobTraitCap;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import jp.absolute.magic.AbsoluteMagic;
import jp.absolute.magic.config.MagicianConfig;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Server-side state machine of the Magician trait.
 * idle -> (cooldown over, target in range) -> wind-up (standing still, arm swing) -> cast -> cooldown.
 * No mana is used: the mob calls AbstractSpell.onCast directly.
 */
final class MagicianCaster {
    private static final UUID FREEZE_ID = UUID.fromString("6d1c2a10-7a52-4f0e-9b61-2f6c0a5e1a01");
    private static final UUID POWER_ID = UUID.fromString("6d1c2a10-7a52-4f0e-9b61-2f6c0a5e1a02");
    private static final int MIN_COOLDOWN_TICKS = 6 * 20;

    private static final Map<Mob, State> STATES = new WeakHashMap<>();
    private static final Set<String> BAD_ENTRIES = new HashSet<>();

    private static final class State {
        int cooldown = 40;          // short grace period after spawn / target found
        int windup = -1;            // < 0 : not casting
        AbstractSpell spell;
        int spellLevel;
        boolean powerApplied;
    }

    private record Pick(AbstractSpell spell, int fixedLevel) {}

    static void tick(Mob mob, int traitLevel) {
        State st = STATES.computeIfAbsent(mob, m -> new State());
        if (!MagicianConfig.ENABLED.get()) {
            release(mob, st);
            return;
        }
        if (!st.powerApplied) {
            applyPower(mob, traitLevel);
            st.powerApplied = true;
        }

        LivingEntity target = mob.getTarget();

        if (st.spell != null) {                       // ---- wind-up ----
            if (!valid(mob, target)) {
                abort(mob, st);
                return;
            }
            freeze(mob, true);
            mob.getLookControl().setLookAt(target, 60.0F, 60.0F);
            if (mob.tickCount % 8 == 0) {
                mob.swing(InteractionHand.MAIN_HAND);   // substitute casting motion
            }
            if (--st.windup <= 0) {
                fire(mob, st, target);
            }
            return;
        }

        if (st.cooldown > 0) {
            st.cooldown--;
            return;
        }
        if (!valid(mob, target)) {
            return;
        }
        Pick pick = pickSpell(mob, traitLevel);
        if (pick == null) {
            st.cooldown = 100;
            return;
        }
        int lvl = spellLevel(pick, traitLevel);
        st.spell = pick.spell();
        st.spellLevel = lvl;
        st.windup = windupTicks(pick.spell(), lvl, mob);
    }

    // ------------------------------------------------------------------ helpers

    private static boolean valid(Mob mob, LivingEntity target) {
        if (target == null || !target.isAlive() || mob.isDeadOrDying()) return false;
        double r = MagicianConfig.MAX_RANGE.get();
        if (mob.distanceToSqr(target) > r * r) return false;
        return !MagicianConfig.REQUIRE_LINE_OF_SIGHT.get() || mob.hasLineOfSight(target);
    }

    private static int windupTicks(AbstractSpell spell, int lvl, Mob mob) {
        if (spell.getCastType() == CastType.LONG) {
            return Math.max(1, spell.getEffectiveCastTime(lvl, mob));
        }
        return Math.max(1, MagicianConfig.INSTANT_WINDUP_TICKS.get());
    }

    private static int cooldownTicks() {
        return Math.max(MIN_COOLDOWN_TICKS, MagicianConfig.COOLDOWN_SECONDS.get() * 20);
    }

    private static void fire(Mob mob, State st, LivingEntity target) {
        AbstractSpell spell = st.spell;
        int lvl = st.spellLevel;
        freeze(mob, false);
        try {
            mob.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
            MagicData md = MagicData.getPlayerMagicData(mob);
            try {
                md.initiateCast(spell, lvl, 0, CastSource.MOB, "mob");
            } catch (Throwable t) {
                AbsoluteMagic.LOGGER.debug("[Magician] initiateCast failed (continuing): {}", t.toString());
            }
            if (spell.checkPreCastConditions(mob.level(), lvl, mob, md)) {
                spell.onCast(mob.level(), lvl, mob, CastSource.MOB, md);
                spell.onServerCastComplete(mob.level(), lvl, mob, md, false);
            }
            md.resetCastingState();
        } catch (Throwable t) {
            AbsoluteMagic.LOGGER.warn("[Magician] casting {} failed: {}", spell.getSpellId(), t.toString());
            BAD_ENTRIES.add(spell.getSpellId());
        }
        st.spell = null;
        st.windup = -1;
        st.cooldown = cooldownTicks();   // counted from the END of the cast
    }

    private static void abort(Mob mob, State st) {
        freeze(mob, false);
        st.spell = null;
        st.windup = -1;
        st.cooldown = 40;
    }

    private static void release(Mob mob, State st) {
        if (st.spell != null) abort(mob, st);
    }

    /** "Stand still while casting": movement speed -100 % (transient) and stop pathfinding. */
    private static void freeze(Mob mob, boolean on) {
        for (Attribute attr : new Attribute[]{Attributes.MOVEMENT_SPEED, Attributes.FLYING_SPEED}) {
            AttributeInstance inst = mob.getAttribute(attr);
            if (inst == null) continue;
            inst.removeModifier(FREEZE_ID);
            if (on) {
                inst.addTransientModifier(new AttributeModifier(FREEZE_ID, "absolutemagic_casting",
                        -1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
        if (on) mob.getNavigation().stop();
    }

    /** spell_power bonus from trait level and L2Hostility mob level (transient, re-applied after reload). */
    private static void applyPower(Mob mob, int traitLevel) {
        AttributeInstance inst = mob.getAttribute(AttributeRegistry.SPELL_POWER.get());
        if (inst == null) {
            AbsoluteMagic.LOGGER.debug("[Magician] {} has no spell_power attribute", mob.getType());
            return;
        }
        int mobLevel = 0;
        MobTraitCap cap = MobTraitCap.HOLDER.get(mob);
        if (cap != null) mobLevel = cap.getLevel();
        double bonus = traitLevel * MagicianConfig.POWER_PER_TRAIT_LEVEL.get()
                + mobLevel * MagicianConfig.POWER_PER_MOB_LEVEL.get();
        bonus = Math.min(bonus, MagicianConfig.MAX_POWER_BONUS.get());
        inst.removeModifier(POWER_ID);
        if (bonus > 0) {
            inst.addTransientModifier(new AttributeModifier(POWER_ID, "absolutemagic_magician_power",
                    bonus, AttributeModifier.Operation.ADDITION));
        }
    }

    private static int spellLevel(Pick pick, int traitLevel) {
        int cap = Math.min(MagicianConfig.MAX_SPELL_LEVEL.get(), pick.spell().getMaxLevel());
        int lvl = pick.fixedLevel() > 0 ? pick.fixedLevel() : traitLevel + MagicianConfig.SPELL_LEVEL_OFFSET.get();
        return Math.max(1, Math.min(cap, lvl));
    }

    // ------------------------------------------------------------------ spell list

    private static Pick pickSpell(Mob mob, int traitLevel) {
        List<Pick> all = new ArrayList<>();
        int max = Math.min(traitLevel, MagicianConfig.MAX_TRAIT_LEVEL);
        for (int i = 0; i < max; i++) {
            for (String entry : MagicianConfig.LEVEL_SPELLS.get(i).get()) {
                Pick p = parse(entry);
                if (p != null) all.add(p);
            }
        }
        if (all.isEmpty()) return null;
        return all.get(mob.getRandom().nextInt(all.size()));
    }

    private static Pick parse(String entry) {
        if (BAD_ENTRIES.contains(entry)) return null;
        try {
            String[] parts = entry.split(":");
            if (parts.length < 2 || parts.length > 3) throw new IllegalArgumentException("bad format");
            ResourceLocation id = new ResourceLocation(parts[0], parts[1]);
            int fixed = parts.length == 3 ? Integer.parseInt(parts[2]) : 0;
            AbstractSpell spell = SpellRegistry.getSpell(id);
            if (spell == null || spell == SpellRegistry.none()) throw new IllegalArgumentException("unknown spell");
            if (!spell.isEnabled()) return null;
            CastType type = spell.getCastType();
            if (type != CastType.INSTANT && type != CastType.LONG) {
                throw new IllegalArgumentException("cast type " + type + " is not supported");
            }
            return new Pick(spell, fixed);
        } catch (Exception e) {
            if (BAD_ENTRIES.add(entry)) {
                AbsoluteMagic.LOGGER.warn("[Magician] ignoring spell entry '{}': {}", entry, e.getMessage());
            }
            return null;
        }
    }

    private MagicianCaster() {}
}
