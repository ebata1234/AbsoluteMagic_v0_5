package jp.absolute.magic.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MagicConfig {
    public static final ForgeConfigSpec SPEC;

    // ---- Fixed Strike ----
    public static final ForgeConfigSpec.DoubleValue FIXED_DAMAGE_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue FIXED_DURATION_SECONDS;

    // ---- Percent Strike ----
    public static final ForgeConfigSpec.DoubleValue PERCENT_PER_LEVEL;
    public static final ForgeConfigSpec.EnumValue<HpBasis> PERCENT_BASIS;
    public static final ForgeConfigSpec.DoubleValue PERCENT_MAX_DAMAGE;
    public static final ForgeConfigSpec.IntValue PERCENT_DURATION_SECONDS;

    // ---- Common (Fixed / Percent Strike) ----
    public static final ForgeConfigSpec.BooleanValue TRIGGER_MELEE;
    public static final ForgeConfigSpec.BooleanValue TRIGGER_PROJECTILES;
    public static final ForgeConfigSpec.BooleanValue TRIGGER_SPELLS;
    public static final ForgeConfigSpec.IntValue INTERNAL_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.BooleanValue AFFECT_PLAYERS;

    // ---- Time Bomb ----
    public static final ForgeConfigSpec.IntValue BOMB_FUSE_SECONDS;
    public static final ForgeConfigSpec.DoubleValue BOMB_MOB_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue BOMB_TRANSFER_RESETS_TIMER;
    public static final ForgeConfigSpec.BooleanValue BOMB_FORCE_KILL_PLAYERS;
    public static final ForgeConfigSpec.BooleanValue BOMB_PLAYER_TARGETS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("fixed_damage");
        FIXED_DAMAGE_PER_LEVEL = b
                .comment("Flat extra damage per spell level (level 5 = 5x this value).",
                        "Not affected by enchantments, relics, skill tree or spell power.")
                .defineInRange("damagePerLevel", 3.0D, 0.0D, 1000000.0D);
        FIXED_DURATION_SECONDS = b
                .comment("Buff duration in seconds.")
                .defineInRange("durationSeconds", 30, 1, 3600);
        b.pop();

        b.push("percent_damage");
        PERCENT_PER_LEVEL = b
                .comment("Extra damage per spell level as a ratio of the target's HP. 0.0004 = 0.04% per level (level 5 = 0.2%).")
                .defineInRange("percentPerLevel", 0.0004D, 0.0D, 1.0D);
        PERCENT_BASIS = b
                .comment("MAX: ratio of the target's max HP. CURRENT: ratio of the target's current HP (weakens as it gets hurt).")
                .defineEnum("basis", HpBasis.MAX);
        PERCENT_MAX_DAMAGE = b
                .comment("Cap for one percent-damage hit. 0 means no limit.")
                .defineInRange("maxDamagePerHit", 0.0D, 0.0D, 1.0E9D);
        PERCENT_DURATION_SECONDS = b
                .comment("Buff duration in seconds.")
                .defineInRange("durationSeconds", 30, 1, 3600);
        b.pop();

        b.push("common");
        TRIGGER_MELEE = b
                .comment("Melee attacks (tools / weapons) trigger the extra hit.")
                .define("triggerMelee", true);
        TRIGGER_PROJECTILES = b
                .comment("Projectiles (arrows, tridents) fired by the caster trigger the extra hit.")
                .define("triggerProjectiles", false);
        TRIGGER_SPELLS = b
                .comment("Iron's Spells 'n Spellbooks spell damage triggers the extra hit.")
                .define("triggerSpells", true);
        INTERNAL_COOLDOWN_TICKS = b
                .comment("Minimum ticks between extra hits from the same attacker on the same target.",
                        "Prevents multi-hit / damage-over-time spells from stacking the extra damage every tick. 0 = none.")
                .defineInRange("internalCooldownTicks", 20, 0, 1200);
        AFFECT_PLAYERS = b
                .comment("If true, the extra damage can also hurt players (PvP).")
                .define("affectPlayers", false);
        b.pop();

        b.push("time_bomb");
        BOMB_FUSE_SECONDS = b
                .comment("Seconds until the bomb explodes.")
                .defineInRange("fuseSeconds", 10, 1, 120);
        BOMB_MOB_DAMAGE = b
                .comment("Fixed true damage dealt to a mob when its bomb explodes (the mob is NOT instantly killed).")
                .defineInRange("mobDamage", 20.0D, 0.0D, 1000000.0D);
        BOMB_PLAYER_TARGETS = b
                .comment("If false, the spell cannot target players at all (mobs only).")
                .define("canTargetPlayers", true);
        BOMB_TRANSFER_RESETS_TIMER = b
                .comment("false: when a bombed player hits another player, the remaining time moves with the bomb.",
                        "true: the new holder gets the full fuse time again.")
                .define("transferResetsTimer", false);
        BOMB_FORCE_KILL_PLAYERS = b
                .comment("If a player survives the explosion damage (some other mod blocks it), kill them anyway.")
                .define("forceKillPlayers", true);
        b.pop();

        SPEC = b.build();
    }

    public enum HpBasis {
        MAX,
        CURRENT
    }

    private MagicConfig() {
    }
}
