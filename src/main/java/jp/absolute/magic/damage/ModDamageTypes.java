package jp.absolute.magic.damage;

import jp.absolute.magic.AbsoluteMagic;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class ModDamageTypes {
    /** Extra true damage (Fixed / Percent Strike, bomb damage to mobs). */
    public static final ResourceKey<DamageType> TRANSCEND = key("transcend");
    /** Lethal bomb damage against players. Also bypasses invulnerability (so a Totem of Undying cannot cancel it). */
    public static final ResourceKey<DamageType> TIME_BOMB = key("time_bomb");

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(AbsoluteMagic.MODID, name));
    }

    public static DamageSource source(Level level, Entity causing, Entity direct) {
        return of(level, TRANSCEND, causing, direct);
    }

    public static DamageSource timeBomb(Level level, Entity causing, Entity direct) {
        return of(level, TIME_BOMB, causing, direct);
    }

    private static DamageSource of(Level level, ResourceKey<DamageType> type, Entity causing, Entity direct) {
        Holder<DamageType> holder = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(type);
        return new DamageSource(holder, direct, causing);
    }

    private ModDamageTypes() {
    }
}
