package dev.valkery.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.valkery.common.ValkyrieBowMastery;
import dev.valkery.common.ValkyrieFlightMastery;
import dev.valkery.forge.event.ValkeryGameplayEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = "valkery")
public final class ValkeryCommands {
    private static final List<String> WING_TYPES = List.of(
            "feathered",
            "dragon",
            "mechanical_feathered",
            "mechanical_leather",
            "light"
    );
    private static final List<String> WING_COLORS = List.of(
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    );
    private static final List<String> UNIQUE_WINGS = List.of("flandres", "discords", "zanzas");

    private ValkeryCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("valkery")
                .then(Commands.literal("mastery")
                        .executes(context -> showMastery(context.getSource().getPlayerOrException())))
                .then(Commands.literal("dash")
                        .executes(context -> ValkeryGameplayEvents.activateAerialDash(
                                context.getSource().getPlayerOrException()
                        ) ? 1 : 0))
                .then(Commands.literal("bow")
                        .executes(context -> showBowMastery(context.getSource().getPlayerOrException()))
                        .then(Commands.literal("upgrade")
                                .executes(context -> upgradeBowMastery(
                                        context.getSource().getPlayerOrException()
                                ))))
                .then(Commands.literal("wings")
                        .executes(context -> showWings(context.getSource().getPlayerOrException()))
                        .then(Commands.literal("set")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((context, builder) ->
                                                SharedSuggestionProvider.suggest(WING_TYPES, builder))
                                        .then(Commands.argument("color", StringArgumentType.word())
                                                .suggests((context, builder) ->
                                                        SharedSuggestionProvider.suggest(WING_COLORS, builder))
                                                .executes(context -> setWingStyle(
                                                        context.getSource().getPlayerOrException(),
                                                        StringArgumentType.getString(context, "type"),
                                                        StringArgumentType.getString(context, "color")
                                                )))))
                        .then(Commands.literal("unique")
                                .then(Commands.argument("style", StringArgumentType.word())
                                        .suggests((context, builder) ->
                                                SharedSuggestionProvider.suggest(UNIQUE_WINGS, builder))
                                        .executes(context -> setUniqueWings(
                                                context.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(context, "style")
                                        ))))));
    }

    private static int showMastery(ServerPlayer player) {
        int experience = ValkyrieFlightMastery.experience(player);
        int windborneThreshold = ValkeryConfig.WINDBORNE_EXPERIENCE.get();
        int skywardenThreshold = Math.max(ValkeryConfig.SKYWARDEN_EXPERIENCE.get(), windborneThreshold + 1);
        ValkyrieFlightMastery.Rank rank = ValkyrieFlightMastery.rank(
                player,
                windborneThreshold,
                skywardenThreshold
        );
        int nextThreshold = ValkyrieFlightMastery.nextThreshold(
                player,
                windborneThreshold,
                skywardenThreshold
        );
        if (rank == ValkyrieFlightMastery.Rank.SKYWARDEN) {
            player.sendSystemMessage(Component.translatable(
                    "message.valkery.mastery.max",
                    Component.translatable(rank.translationKey()),
                    experience
            ));
        } else {
            player.sendSystemMessage(Component.translatable(
                    "message.valkery.mastery.progress",
                    Component.translatable(rank.translationKey()),
                    experience,
                    nextThreshold,
                    nextThreshold - experience
            ));
        }
        return 1;
    }

    private static int showBowMastery(ServerPlayer player) {
        int level = ValkyrieBowMastery.level(player);
        int experience = ValkyrieBowMastery.experience(player);
        if (level >= ValkyrieBowMastery.MAX_LEVEL) {
            player.sendSystemMessage(Component.translatable(
                    "message.valkery.bow_mastery.max",
                    level,
                    experience
            ));
        } else {
            int cost = ValkyrieBowMastery.nextUpgradeCost(player);
            player.sendSystemMessage(Component.translatable(
                    "message.valkery.bow_mastery.progress",
                    level,
                    experience,
                    cost,
                    Math.max(0, cost - experience)
            ));
        }
        return 1;
    }

    private static int upgradeBowMastery(ServerPlayer player) {
        if (!ValkyrieBowMastery.upgrade(player)) {
            player.sendSystemMessage(Component.translatable(
                    ValkyrieBowMastery.level(player) >= ValkyrieBowMastery.MAX_LEVEL
                            ? "message.valkery.bow_mastery.max_level"
                            : "message.valkery.bow_mastery.need_experience",
                    ValkyrieBowMastery.nextUpgradeCost(player)
            ));
            return 0;
        }

        player.sendSystemMessage(Component.translatable(
                "message.valkery.bow_mastery.upgraded",
                ValkyrieBowMastery.level(player)
        ));
        if (ValkyrieBowMastery.level(player) == ValkyrieBowMastery.MAX_LEVEL) {
            ValkeryAdvancements.award(player, "master_archer", "mastered_bow_magic");
        }
        return 1;
    }

    private static int showWings(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable(
                "message.valkery.wings.current",
                ValkeryGameplayEvents.selectedWings(player)
        ));
        player.sendSystemMessage(Component.translatable("message.valkery.wings.usage"));
        return 1;
    }

    private static int setWingStyle(ServerPlayer player, String type, String color) {
        if (!WING_TYPES.contains(type) || !WING_COLORS.contains(color)) {
            player.sendSystemMessage(Component.translatable("message.valkery.wings.invalid_style"));
            return 0;
        }

        String path = switch (type) {
            case "feathered" -> color + "_feathered_wings";
            case "dragon" -> color + "_dragon_wings";
            case "mechanical_feathered" -> color + "_mechanical_feathered_wings";
            case "mechanical_leather" -> color + "_mechanical_leather_wings";
            case "light" -> color + "_light_wings";
            default -> throw new IllegalStateException("Unsupported Icarus wing type: " + type);
        };
        return applyWingSelection(player, "icarus:" + path);
    }

    private static int setUniqueWings(ServerPlayer player, String style) {
        if (!UNIQUE_WINGS.contains(style)) {
            player.sendSystemMessage(Component.translatable("message.valkery.wings.invalid_style"));
            return 0;
        }
        return applyWingSelection(player, "icarus:" + style + "_wings");
    }

    private static int applyWingSelection(ServerPlayer player, String wingId) {
        if (!ValkeryGameplayEvents.isValkyrie(player)) {
            player.sendSystemMessage(Component.translatable("message.valkery.wings.origin_required"));
            return 0;
        }
        if (!ValkeryGameplayEvents.selectWings(player, wingId)) {
            player.sendSystemMessage(Component.translatable("message.valkery.wings.unavailable", wingId));
            return 0;
        }
        player.sendSystemMessage(Component.translatable("message.valkery.wings.selected", wingId));
        return 1;
    }
}
