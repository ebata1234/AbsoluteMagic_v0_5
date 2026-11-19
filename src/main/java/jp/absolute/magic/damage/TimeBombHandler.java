package jp.absolute.magic.damage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jp.absolute.magic.config.MagicConfig;
import jp.absolute.magic.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Time Bomb logic.
 *  - arm():        puts the TIME_BOMB effect (= the fuse) on the target and remembers who cast it.
 *  - onFuseTick(): called by the effect every second (chat countdown to players) and on the last tick (detonation request).
 *  - detonate():   at the end of the server tick. Visual explosion only (no block damage, no splash damage).
 *                  Player -> lethal damage. Mob -> fixed true damage (config, default 20), no instant kill.
 *  - onAttack():   a bombed player who hits ANOTHER player (melee) passes the bomb on; remaining time moves along.
 */
public final class TimeBombHandler {
    /** bombed entity -> caster (for kill credit and messages). Not saved: after a restart the bomb still explodes, without credit. */
    private static final Map<UUID, UUID> CASTER = new HashMap<>();
    /** entities whose fuse ran out this tick. */
    private static final Map<UUID, LivingEntity> DETONATE = new LinkedHashMap<>();
    /** exact damage amounts of our own hits, so other mods cannot reduce them. */
    private static final Map<DamageSource, Float> EXACT = new IdentityHashMap<>();

    // ------------------------------------------------------------------ arming

    public static boolean canBomb(LivingEntity caster, LivingEntity target) {
        if (target == caster || !target.isAlive() || target.hasEffect(ModEffects.TIME_BOMB.get())) {
            return false;
        }
        if (target instanceof Player player) {
            return MagicConfig.BOMB_PLAYER_TARGETS.get() && !player.isCreative() && !player.isSpectator();
        }
        return true;
    }

    public static void arm(LivingEntity caster, LivingEntity target, int fuseTicks) {
        target.addEffect(new MobEffectInstance(ModEffects.TIME_BOMB.get(), fuseTicks, 0, false, false, true));
        CASTER.put(target.getUUID(), caster.getUUID());
        play(target, SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, 1.0F);
        if (target instanceof ServerPlayer victim) {
            victim.sendSystemMessage(Component.translatable("message.absolutemagic.time_bomb.armed",
                    caster.getDisplayName(), fuseTicks / 20).withStyle(ChatFormatting.RED));
        }
        if (caster instanceof ServerPlayer player && player != target) {
            player.sendSystemMessage(Component.translatable("message.absolutemagic.time_bomb.armed_caster",
                    target.getDisplayName()).withStyle(ChatFormatting.GOLD));
        }
    }

    // ------------------------------------------------------------------ fuse

    /** Called by TimeBombEffect on the server: every 20 ticks and on the last tick. */
    public static void onFuseTick(LivingEntity entity) {
        MobEffectInstance bomb = entity.getEffect(ModEffects.TIME_BOMB.get());
        if (bomb == null) {
            return;
        }
        int ticks = bomb.getDuration();
        if (ticks <= 1) {
            DETONATE.put(entity.getUUID(), entity);
            return;
        }
        if (entity instanceof ServerPlayer player) {
            int seconds = ticks / 20;
            player.sendSystemMessage(Component.translatable("message.absolutemagic.time_bomb.count", seconds)
                    .withStyle(seconds <= 3 ? ChatFormatting.RED : ChatFormatting.GOLD));
            play(player, SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, 0.6F);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || DETONATE.isEmpty()) {
            return;
        }
        List<LivingEntity> batch = new ArrayList<>(DETONATE.values());
        DETONATE.clear();
        for (LivingEntity target : batch) {
            if (target.isAlive() && !target.level().isClientSide) {
                detonate(target);
            }
        }
    }

    private static void detonate(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        UUID casterId = CASTER.remove(target.getUUID());
        ServerPlayer caster = casterId == null ? null : level.getServer().getPlayerList().getPlayer(casterId);
        target.removeEffect(ModEffects.TIME_BOMB.get());

        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            player.sendSystemMessage(Component.translatable("message.absolutemagic.time_bomb.defused")
                    .withStyle(ChatFormatting.GREEN));
            return;
        }

        // Visual / audio explosion only: no blocks are touched and nobody else is hurt.
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, target.getX(), target.getY(0.5D), target.getZ(),
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        play(target, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 4.0F);

        target.invulnerableTime = 0;
        if (target instanceof Player) {
            float amount = target.getMaxHealth() + target.getAbsorptionAmount() + 1024.0F;
            hurtExact(target, ModDamageTypes.timeBomb(level, caster, caster), amount);
            if (target.isAlive() && MagicConfig.BOMB_FORCE_KILL_PLAYERS.get()) {
                target.kill();
            }
        } else {
            hurtExact(target, ModDamageTypes.source(level, caster, caster), MagicConfig.BOMB_MOB_DAMAGE.get().floatValue());
        }
    }

    private static void hurtExact(LivingEntity target, DamageSource source, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        EXACT.put(source, amount);
        try {
            target.hurt(source, amount);
        } finally {
            EXACT.remove(source);
        }
    }

    // ------------------------------------------------------------------ passing the bomb on

    /** A bombed player punches another player: the bomb jumps to the victim. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) {
            return;
        }
        DamageSource source = event.getSource();
        // PLAYER_ATTACK only: this also rules out our own / Fixed-Percent Strike extra hits and spells.
        if (!source.is(DamageTypes.PLAYER_ATTACK)
                || !(source.getEntity() instanceof ServerPlayer holder)
                || source.getDirectEntity() != holder
                || holder == victim) {
            return;
        }
        MobEffectInstance bomb = holder.getEffect(ModEffects.TIME_BOMB.get());
        if (bomb == null
                || !victim.isAlive() || victim.isCreative() || victim.isSpectator()
                || victim.hasEffect(ModEffects.TIME_BOMB.get())) {
            return;
        }
        int remaining = MagicConfig.BOMB_TRANSFER_RESETS_TIMER.get()
                ? MagicConfig.BOMB_FUSE_SECONDS.get() * 20
                : Math.max(1, bomb.getDuration());

        holder.removeEffect(ModEffects.TIME_BOMB.get());
        DETONATE.remove(holder.getUUID());
        UUID casterId = CASTER.remove(holder.getUUID());

        victim.addEffect(new MobEffectInstance(ModEffects.TIME_BOMB.get(), remaining, 0, false, false, true));
        if (casterId != null) {
            CASTER.put(victim.getUUID(), casterId);
        }
        play(victim, SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, 1.0F);
        int seconds = (remaining + 19) / 20;
        victim.sendSystemMessage(Component.translatable("message.absolutemagic.time_bomb.received",
                holder.getDisplayName(), seconds).withStyle(ChatFormatting.RED));
        holder.sendSystemMessage(Component.translatable("message.absolutemagic.time_bomb.passed",
                victim.getDisplayName()).withStyle(ChatFormatting.GREEN));
    }

    // ------------------------------------------------------------------ exact damage / cleanup

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void pinHurt(LivingHurtEvent event) {
        Float exact = EXACT.get(event.getSource());
        if (exact != null) {
            event.setAmount(exact);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void pinDamage(LivingDamageEvent event) {
        Float exact = EXACT.get(event.getSource());
        if (exact != null) {
            event.setAmount(exact);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        UUID id = event.getEntity().getUUID();
        CASTER.remove(id);
        DETONATE.remove(id);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        CASTER.clear();
        DETONATE.clear();
        EXACT.clear();
    }

    private static void play(LivingEntity at, SoundEvent sound, SoundSource category, float volume) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), sound, category, volume, 1.0F);
    }

    private TimeBombHandler() {
    }
}
