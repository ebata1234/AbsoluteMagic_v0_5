package jp.absolute.magic.config;

import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

/** config/absolutemagic-magician.toml : settings of the "Magician" L2Hostility trait. */
public final class MagicianConfig {
    public static final ForgeConfigSpec SPEC;
    public static final int MAX_TRAIT_LEVEL = 5;

    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.IntValue COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.IntValue INSTANT_WINDUP_TICKS;
    public static final ForgeConfigSpec.DoubleValue MAX_RANGE;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_LINE_OF_SIGHT;
    public static final ForgeConfigSpec.IntValue SPELL_LEVEL_OFFSET;
    public static final ForgeConfigSpec.IntValue MAX_SPELL_LEVEL;
    public static final ForgeConfigSpec.DoubleValue POWER_PER_TRAIT_LEVEL;
    public static final ForgeConfigSpec.DoubleValue POWER_PER_MOB_LEVEL;
    public static final ForgeConfigSpec.DoubleValue MAX_POWER_BONUS;
    /** index 0 = trait level 1 ... index 4 = trait level 5 */
    public static final List<ForgeConfigSpec.ConfigValue<List<? extends String>>> LEVEL_SPELLS = new ArrayList<>();

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("magician");
        ENABLED = b.comment("Master switch of the Magician trait behaviour.")
                .define("enabled", true);
        COOLDOWN_SECONDS = b.comment("Seconds a mob waits AFTER finishing a spell before it casts again (minimum 6).",
                        "Mobs have no mana, so this cooldown is the only limit.")
                .defineInRange("cooldownSeconds", 8, 6, 3600);
        INSTANT_WINDUP_TICKS = b.comment("Standing-still wind-up (ticks, 20 = 1s) before an INSTANT spell.",
                        "LONG spells use their own cast time. The mob swings its arm during the wind-up.")
                .defineInRange("instantWindupTicks", 20, 0, 200);
        MAX_RANGE = b.comment("The mob only casts when its target is within this many blocks.")
                .defineInRange("maxRange", 24.0D, 4.0D, 128.0D);
        REQUIRE_LINE_OF_SIGHT = b.comment("Only start casting when the mob can see its target.")
                .define("requireLineOfSight", true);
        b.pop();

        b.push("power");
        SPELL_LEVEL_OFFSET = b.comment("Spell level = trait level + this offset (then limited by maxSpellLevel and the spell's own max).",
                        "A list entry like \"irons_spellbooks:fireball:3\" fixes the spell level to 3.")
                .defineInRange("spellLevelOffset", 0, -10, 20);
        MAX_SPELL_LEVEL = b.defineInRange("maxSpellLevel", 10, 1, 50);
        POWER_PER_TRAIT_LEVEL = b.comment("Bonus added to the mob's irons_spellbooks:spell_power attribute per trait level.")
                .defineInRange("spellPowerPerTraitLevel", 0.15D, 0.0D, 100.0D);
        POWER_PER_MOB_LEVEL = b.comment("Bonus added to spell_power per L2Hostility mob level.")
                .defineInRange("spellPowerPerMobLevel", 0.004D, 0.0D, 100.0D);
        MAX_POWER_BONUS = b.comment("Upper limit of the total spell_power bonus from the two values above.")
                .defineInRange("maxSpellPowerBonus", 2.0D, 0.0D, 1000.0D);
        b.pop();

        b.push("spells");
        b.comment("Spell lists per trait level. A mob with trait level N picks randomly from the lists of level 1..N (cumulative).",
                "Entry format: \"namespace:spell_id\" or \"namespace:spell_id:spellLevel\".",
                "Only INSTANT and LONG cast-type spells work. Continuous spells are skipped.");
        String[][] defaults = {
                {"irons_spellbooks:firebolt", "irons_spellbooks:icicle"},
                {"irons_spellbooks:lightning_lance", "irons_spellbooks:poison_arrow", "irons_spellbooks:blood_slash"},
                {"irons_spellbooks:fireball", "irons_spellbooks:wither_skull", "irons_spellbooks:chain_lightning"},
                {"irons_spellbooks:magma_bomb", "irons_spellbooks:lightning_bolt", "irons_spellbooks:ball_lightning"},
                {"irons_spellbooks:starfall", "irons_spellbooks:thunderstorm"}
        };
        for (int i = 0; i < MAX_TRAIT_LEVEL; i++) {
            LEVEL_SPELLS.add(b.defineList("level" + (i + 1), List.of(defaults[i]), o -> o instanceof String));
        }
        b.pop();

        SPEC = b.build();
    }

    private MagicianConfig() {}
}
