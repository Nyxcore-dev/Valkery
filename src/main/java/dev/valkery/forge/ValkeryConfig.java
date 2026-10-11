package dev.valkery.forge;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ValkeryConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue FLIGHT_EXHAUSTION_PER_SECOND;
    public static final ForgeConfigSpec.IntValue MINIMUM_FLIGHT_FOOD;
    public static final ForgeConfigSpec.IntValue INITIATE_FLIGHT_SECONDS;
    public static final ForgeConfigSpec.IntValue WINDBORNE_FLIGHT_SECONDS;
    public static final ForgeConfigSpec.IntValue SKYWARDEN_FLIGHT_SECONDS;
    public static final ForgeConfigSpec.IntValue INITIATE_RECOVERY_SECONDS;
    public static final ForgeConfigSpec.IntValue WINDBORNE_RECOVERY_SECONDS;
    public static final ForgeConfigSpec.IntValue SKYWARDEN_RECOVERY_SECONDS;
    public static final ForgeConfigSpec.DoubleValue WINDBORNE_HUNGER_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SKYWARDEN_HUNGER_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue WINDBORNE_EXPERIENCE;
    public static final ForgeConfigSpec.IntValue SKYWARDEN_EXPERIENCE;
    public static final ForgeConfigSpec.DoubleValue DIVINE_SMITE_CHANCE;
    public static final ForgeConfigSpec.DoubleValue DIVINE_SMITE_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue DIVINE_SMITE_PIERCE_TARGETS;
    public static final ForgeConfigSpec.DoubleValue DIVINE_SMITE_FALLOFF;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("flight");
        FLIGHT_EXHAUSTION_PER_SECOND = builder
                .comment("Hunger exhaustion added per second of Valkyrie flight; lower values allow longer travel.")
                .defineInRange("exhaustionPerSecond", 0.025D, 0.001D, 1.0D);
        MINIMUM_FLIGHT_FOOD = builder
                .comment("Minimum food level required to start or continue flight. 6 is three full hunger icons.")
                .defineInRange("minimumFoodLevel", 6, 6, 20);
        INITIATE_FLIGHT_SECONDS = builder.defineInRange("initiateFlightSeconds", 60, 10, 600);
        WINDBORNE_FLIGHT_SECONDS = builder.defineInRange("windborneFlightSeconds", 90, 10, 600);
        SKYWARDEN_FLIGHT_SECONDS = builder.defineInRange("skywardenFlightSeconds", 120, 10, 600);
        INITIATE_RECOVERY_SECONDS = builder.defineInRange("initiateRecoverySeconds", 30, 1, 300);
        WINDBORNE_RECOVERY_SECONDS = builder.defineInRange("windborneRecoverySeconds", 24, 1, 300);
        SKYWARDEN_RECOVERY_SECONDS = builder.defineInRange("skywardenRecoverySeconds", 18, 1, 300);
        WINDBORNE_HUNGER_MULTIPLIER = builder.defineInRange("windborneHungerMultiplier", 0.75D, 0.1D, 1.0D);
        SKYWARDEN_HUNGER_MULTIPLIER = builder.defineInRange("skywardenHungerMultiplier", 0.55D, 0.1D, 1.0D);
        WINDBORNE_EXPERIENCE = builder.defineInRange("windborneExperience", 600, 1, 100000);
        SKYWARDEN_EXPERIENCE = builder.defineInRange("skywardenExperience", 1800, 2, 100000);
        builder.pop();

        builder.push("archery");
        DIVINE_SMITE_CHANCE = builder
                .comment("Chance that a fully charged Valkyrie bow shot becomes a piercing Divine Smite. Tune after gameplay testing.")
                .defineInRange("divineSmiteChance", 0.10D, 0.0D, 1.0D);
        DIVINE_SMITE_DAMAGE_MULTIPLIER = builder.defineInRange("divineSmiteDamageMultiplier", 1.5D, 1.0D, 10.0D);
        DIVINE_SMITE_PIERCE_TARGETS = builder.defineInRange("divineSmitePierceTargets", 3, 1, 10);
        DIVINE_SMITE_FALLOFF = builder.defineInRange("divineSmiteDamageFalloff", 0.65D, 0.1D, 1.0D);
        builder.pop();
        SPEC = builder.build();
    }

    private ValkeryConfig() {
    }
}
