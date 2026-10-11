package dev.valkery.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class ValkyrieFlightMastery {
    private static final String DATA_KEY = "valkery_flight";
    private static final String EXPERIENCE_KEY = "experience";
    private static final String MOVING_TICKS_KEY = "moving_ticks";

    public enum Rank {
        INITIATE("message.valkery.mastery.initiate"),
        WINDBORNE("message.valkery.mastery.windborne"),
        SKYWARDEN("message.valkery.mastery.skywarden");

        private final String translationKey;

        Rank(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }
    }

    private ValkyrieFlightMastery() {
    }

    public static int experience(Player player) {
        return data(player).getInt(EXPERIENCE_KEY);
    }

    public static Rank rank(Player player, int windborneThreshold, int skywardenThreshold) {
        int experience = experience(player);
        if (experience >= skywardenThreshold) {
            return Rank.SKYWARDEN;
        }
        if (experience >= windborneThreshold) {
            return Rank.WINDBORNE;
        }
        return Rank.INITIATE;
    }

    public static int nextThreshold(Player player, int windborneThreshold, int skywardenThreshold) {
        return switch (rank(player, windborneThreshold, skywardenThreshold)) {
            case INITIATE -> windborneThreshold;
            case WINDBORNE -> skywardenThreshold;
            case SKYWARDEN -> skywardenThreshold;
        };
    }

    public static boolean recordMovingFlightTick(Player player, int maxExperience) {
        CompoundTag data = data(player);
        int movingTicks = data.getInt(MOVING_TICKS_KEY) + 1;
        if (movingTicks < ValkyrieBalance.FLIGHT_XP_MOVING_TICKS_PER_POINT) {
            data.putInt(MOVING_TICKS_KEY, movingTicks);
            return false;
        }

        data.putInt(MOVING_TICKS_KEY, movingTicks - ValkyrieBalance.FLIGHT_XP_MOVING_TICKS_PER_POINT);
        int experience = data.getInt(EXPERIENCE_KEY);
        if (experience >= maxExperience) {
            return false;
        }
        data.putInt(EXPERIENCE_KEY, experience + 1);
        return true;
    }

    public static CompoundTag data(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(DATA_KEY, CompoundTag.TAG_COMPOUND)) {
            CompoundTag data = new CompoundTag();
            if (persistentData.contains("valkery_flight_ticks")) {
                data.putInt("flight_ticks", persistentData.getInt("valkery_flight_ticks"));
            }
            if (persistentData.contains("valkery_flight_recovery_ticks")) {
                data.putInt("recovery_ticks", persistentData.getInt("valkery_flight_recovery_ticks"));
            }
            persistentData.put(DATA_KEY, data);
        }
        return persistentData.getCompound(DATA_KEY);
    }

    public static void copy(Player original, Player replacement) {
        replacement.getPersistentData().put(DATA_KEY, data(original).copy());
    }
}
