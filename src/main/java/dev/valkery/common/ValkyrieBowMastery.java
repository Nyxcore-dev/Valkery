package dev.valkery.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class ValkyrieBowMastery {
    private static final String DATA_KEY = "valkery_bow_mastery";
    private static final String EXPERIENCE_KEY = "experience";
    private static final String LEVEL_KEY = "divine_smite_level";
    public static final int MAX_LEVEL = 5;
    public static final int EXPERIENCE_PER_LEVEL = 25;

    private ValkyrieBowMastery() {
    }

    public static int experience(Player player) {
        return data(player).getInt(EXPERIENCE_KEY);
    }

    public static int level(Player player) {
        return data(player).getInt(LEVEL_KEY);
    }

    public static int nextUpgradeCost(Player player) {
        return EXPERIENCE_PER_LEVEL * (level(player) + 1);
    }

    public static void recordBowHit(Player player) {
        data(player).putInt(EXPERIENCE_KEY, experience(player) + 1);
    }

    public static boolean upgrade(Player player) {
        CompoundTag data = data(player);
        int level = data.getInt(LEVEL_KEY);
        int cost = EXPERIENCE_PER_LEVEL * (level + 1);
        if (level >= MAX_LEVEL || data.getInt(EXPERIENCE_KEY) < cost) {
            return false;
        }

        data.putInt(EXPERIENCE_KEY, data.getInt(EXPERIENCE_KEY) - cost);
        data.putInt(LEVEL_KEY, level + 1);
        return true;
    }

    public static double divineSmiteChanceBonus(Player player) {
        return level(player) * 0.02D;
    }

    public static double divineSmiteDamageBonus(Player player) {
        return level(player) * 0.10D;
    }

    public static boolean hasGaleArrow(Player player) {
        return level(player) >= 3;
    }

    public static CompoundTag data(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(DATA_KEY, CompoundTag.TAG_COMPOUND)) {
            persistentData.put(DATA_KEY, new CompoundTag());
        }
        return persistentData.getCompound(DATA_KEY);
    }

    public static void copy(Player original, Player replacement) {
        replacement.getPersistentData().put(DATA_KEY, data(original).copy());
    }
}
