package dev.valkery.forge.event;

import dev.valkery.common.ValkyrieBowMastery;
import dev.valkery.common.ValkyrieBalance;
import dev.valkery.common.ValkyrieConstants;
import dev.valkery.common.ValkyrieFlightMastery;
import dev.valkery.forge.ValkeryAdvancements;
import dev.valkery.forge.ValkeryConfig;
import dev.valkery.forge.item.ValkeryItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.ArrowNockEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

@Mod.EventBusSubscriber(modid = ValkyrieConstants.MOD_ID)
public final class ValkeryGameplayEvents {
    private static final String FLIGHT_TICKS = "flight_ticks";
    private static final String RECOVERY_TICKS = "recovery_ticks";
    private static final String EXHAUSTION_TICKS = "exhaustion_ticks";
    private static final String STATUS_TICKS = "status_ticks";
    private static final String PREVIOUS_X = "previous_x";
    private static final String PREVIOUS_Y = "previous_y";
    private static final String PREVIOUS_Z = "previous_z";
    private static final String HAS_PREVIOUS_POSITION = "has_previous_position";
    private static final String PREVIOUS_FALL_FLYING = "previous_fall_flying";
    private static final String SAFE_DESCENT = "safe_descent";
    private static final String LOW_FOOD_MESSAGE_SENT = "low_food_message_sent";
    private static final String VALKYRIE_MARKER = "valkyrie_origin";
    private static final String WING_SELECTION_KEY = "wings";
    private static final ResourceLocation DEFAULT_WINGS_ID =
            new ResourceLocation("icarus", "white_feathered_wings");
    private static final String DIVINE_SMITE_TAG = "valkery_divine_smite";
    private static final String SMITE_FALLOFF_TAG = "valkery_smite_falloff";
    private static final String SMITE_HITS_TAG = "valkery_smite_hits";
    private static final String BOW_REFUND_TICK = "valkery_bow_refund_tick";
    private static final String BOW_REFUND_STACK = "valkery_bow_refund_stack";

    private ValkeryGameplayEvents() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        Entity directAttacker = event.getSource().getDirectEntity();
        if (attacker instanceof Player player && directAttacker == player && isValkyrie(player)) {
            event.setAmount(event.getAmount() * ValkyrieBalance.VALKYRIE_MELEE_DAMAGE_MULTIPLIER);
        } else if (attacker instanceof Player player
                && directAttacker instanceof Arrow arrow
                && arrow.getOwner() == player
                && isValkyrie(player)) {
            float multiplier = ValkyrieBalance.VALKYRIE_BOW_DAMAGE_MULTIPLIER;
            if (arrow.getPersistentData().getBoolean(DIVINE_SMITE_TAG)) {
                CompoundTag arrowData = arrow.getPersistentData();
                int previousHits = arrowData.getInt(SMITE_HITS_TAG);
                multiplier *= (float) Math.pow(arrowData.getDouble(SMITE_FALLOFF_TAG), previousHits);
                arrowData.putInt(SMITE_HITS_TAG, previousHits + 1);
            }
            event.setAmount(event.getAmount() * multiplier);
            ValkyrieBowMastery.recordBowHit(player);
        }

    }

    @SubscribeEvent
    public static void onValkyrieBowDraw(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide
                || !(event.getItemStack().getItem() instanceof BowItem)
                || !(event.getEntity() instanceof ServerPlayer player)
                || !isValkyrie(player)) {
            return;
        }

        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                0.7F,
                1.35F
        );
    }

    @SubscribeEvent
    public static void onValkyrieBowNock(ArrowNockEvent event) {
        Player player = event.getEntity();
        if (event.hasAmmo() || !isValkyrie(player)) {
            return;
        }

        ItemStack arrow = new ItemStack(Items.ARROW);
        if (!player.getInventory().add(arrow)) {
            return;
        }
        player.startUsingItem(event.getHand());
        event.setAction(InteractionResultHolder.consume(event.getBow()));
    }

    @SubscribeEvent
    public static void onValkyrieBowRelease(ArrowLooseEvent event) {
        Player player = event.getEntity();
        ItemStack bow = event.getBow();
        if (player.level().isClientSide
                || !isValkyrie(player)
                || player.getAbilities().instabuild
                || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, bow) > 0) {
            return;
        }

        ItemStack projectile = player.getProjectile(bow);
        if (!projectile.isEmpty()) {
            CompoundTag playerData = player.getPersistentData();
            playerData.putLong(BOW_REFUND_TICK, player.level().getGameTime());
            playerData.put(BOW_REFUND_STACK, projectile.copyWithCount(1).save(new CompoundTag()));
        }
    }

    @SubscribeEvent
    public static void onValkyrieArrowSpawn(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide
                && event.getEntity() instanceof AbstractArrow arrow
                && arrow.getOwner() instanceof ServerPlayer player
                && isValkyrie(player)
                && player.getPersistentData().contains(BOW_REFUND_TICK)
                && player.getPersistentData().getLong(BOW_REFUND_TICK) == event.getLevel().getGameTime()) {
            player.getPersistentData().remove(BOW_REFUND_TICK);
            CompoundTag playerData = player.getPersistentData();
            ItemStack refund = ItemStack.of(playerData.getCompound(BOW_REFUND_STACK));
            playerData.remove(BOW_REFUND_STACK);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            if (!refund.isEmpty() && !player.getInventory().add(refund)) {
                player.drop(refund, false);
            }
        }

        if (event.getLevel().isClientSide
                || !(event.getEntity() instanceof Arrow arrow)
                || !(arrow.getOwner() instanceof ServerPlayer player)
                || !isValkyrie(player)) {
            return;
        }

        double smiteChance = Math.min(
                0.75D,
                ValkeryConfig.DIVINE_SMITE_CHANCE.get()
                        + ValkyrieBowMastery.divineSmiteChanceBonus(player)
        );
        boolean divineSmite = arrow.isCritArrow() && player.getRandom().nextDouble() < smiteChance;
        if (divineSmite) {
            arrow.setBaseDamage(arrow.getBaseDamage()
                    * (ValkeryConfig.DIVINE_SMITE_DAMAGE_MULTIPLIER.get()
                    + ValkyrieBowMastery.divineSmiteDamageBonus(player)));
            arrow.setPierceLevel((byte) Math.max(0, ValkeryConfig.DIVINE_SMITE_PIERCE_TARGETS.get() - 1));
            arrow.getPersistentData().putBoolean(DIVINE_SMITE_TAG, true);
            arrow.getPersistentData().putDouble(
                    SMITE_FALLOFF_TAG,
                    ValkeryConfig.DIVINE_SMITE_FALLOFF.get()
            );
            player.displayClientMessage(Component.translatable("message.valkery.divine_smite"), true);
            ValkeryAdvancements.award(player, "divine_archer", "divine_smite");
        }

        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                divineSmite ? SoundEvents.BEACON_ACTIVATE : SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                divineSmite ? 1.1F : 0.7F,
                divineSmite ? 0.9F : 1.1F
        );
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof Player player
                && !player.level().isClientSide
                && isValkyrie(player)
                && player.isFallFlying()
                && event.getAmount() >= ValkyrieBalance.HEAVY_HIT_FLIGHT_DISRUPTION_DAMAGE) {
            interruptFlight(player, ValkyrieBalance.HEAVY_HIT_RECOVERY_TICKS);
            player.displayClientMessage(Component.translatable("message.valkery.wings_disrupted"), true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (event.phase != TickEvent.Phase.END || player.level().isClientSide) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            enforceWings(serverPlayer);
        }
        if (!isValkyrie(player)) {
            return;
        }

        CompoundTag data = ValkyrieFlightMastery.data(player);
        boolean movedThisTick = movedThisTick(player, data);
        updatePreviousPosition(player, data);
        int recoveryTicks = data.getInt(RECOVERY_TICKS);

        if (recoveryTicks > 0) {
            if (player.onGround()) {
                recoveryTicks--;
                data.putInt(RECOVERY_TICKS, recoveryTicks);
            } else if (player.isFallFlying()) {
                player.stopFallFlying();
                data.putBoolean(SAFE_DESCENT, true);
            }
        }

        if (player.onGround() && data.getBoolean(SAFE_DESCENT)) {
            data.putBoolean(SAFE_DESCENT, false);
            player.removeEffect(MobEffects.SLOW_FALLING);
        } else if (data.getBoolean(SAFE_DESCENT) && !player.isFallFlying()) {
            grantSafeDescent(player);
        }

        if (player.isFallFlying() && data.getInt(RECOVERY_TICKS) == 0) {
            tickFlight(player, data, movedThisTick);
        } else if (!player.isFallFlying() && data.getInt(RECOVERY_TICKS) == 0
                && data.getInt(FLIGHT_TICKS) > 0
                && player.getFoodData().getFoodLevel() <= ValkeryConfig.MINIMUM_FLIGHT_FOOD.get()) {
            interruptForLowFood(player, data);
        }

        if (player.onGround()) {
            data.putInt(FLIGHT_TICKS, 0);
            data.putInt(EXHAUSTION_TICKS, 0);
            data.putInt(STATUS_TICKS, 0);
            data.putBoolean(LOW_FOOD_MESSAGE_SENT, false);
        }
    }

    private static void tickFlight(Player player, CompoundTag data, boolean movedThisTick) {
        int windborneThreshold = ValkeryConfig.WINDBORNE_EXPERIENCE.get();
        int skywardenThreshold = Math.max(
                ValkeryConfig.SKYWARDEN_EXPERIENCE.get(),
                windborneThreshold + 1
        );
        int maxExperience = skywardenThreshold;
        ValkyrieFlightMastery.Rank rank = ValkyrieFlightMastery.rank(
                player,
                windborneThreshold,
                skywardenThreshold
        );
        int flightTicks = data.getInt(FLIGHT_TICKS) + 1;
        data.putInt(FLIGHT_TICKS, flightTicks);
        if (flightTicks == 1 && player instanceof ServerPlayer serverPlayer) {
            ValkeryAdvancements.award(serverPlayer, "first_flight", "flew");
        }

        if (player.getFoodData().getFoodLevel() <= ValkeryConfig.MINIMUM_FLIGHT_FOOD.get()) {
            interruptForLowFood(player, data);
            return;
        }

        int exhaustionTicks = data.getInt(EXHAUSTION_TICKS) + 1;
        if (exhaustionTicks >= ValkyrieBalance.FLIGHT_TICKS_PER_SECOND) {
            double efficiency = switch (rank) {
                case INITIATE -> 1.0D;
                case WINDBORNE -> ValkeryConfig.WINDBORNE_HUNGER_MULTIPLIER.get();
                case SKYWARDEN -> ValkeryConfig.SKYWARDEN_HUNGER_MULTIPLIER.get();
            };
            player.causeFoodExhaustion((float) (
                    ValkeryConfig.FLIGHT_EXHAUSTION_PER_SECOND.get() * efficiency
            ));
            exhaustionTicks = 0;
        }
        data.putInt(EXHAUSTION_TICKS, exhaustionTicks);

        if (movedThisTick) {
            if (ValkyrieFlightMastery.recordMovingFlightTick(player, maxExperience)) {
                ValkyrieFlightMastery.Rank newRank = ValkyrieFlightMastery.rank(
                        player,
                        windborneThreshold,
                        skywardenThreshold
                );
                if (newRank != rank) {
                    player.displayClientMessage(
                            Component.translatable(
                                    "message.valkery.mastery.rank_up",
                                    Component.translatable(newRank.translationKey())
                            ),
                            true
                    );
                    if (newRank == ValkyrieFlightMastery.Rank.SKYWARDEN
                            && player instanceof ServerPlayer serverPlayer) {
                        ValkeryAdvancements.award(serverPlayer, "master_of_skies", "reached_skywarden");
                    }
                }
            }
        }

        int statusTicks = data.getInt(STATUS_TICKS) + 1;
        if (flightTicks == 1 || statusTicks >= ValkyrieBalance.MASTERY_STATUS_INTERVAL_TICKS) {
            data.putInt(STATUS_TICKS, 0);
            showMasteryProgress(player, windborneThreshold, skywardenThreshold);
        } else {
            data.putInt(STATUS_TICKS, statusTicks);
        }

        int enduranceSeconds = switch (rank) {
            case INITIATE -> ValkeryConfig.INITIATE_FLIGHT_SECONDS.get();
            case WINDBORNE -> ValkeryConfig.WINDBORNE_FLIGHT_SECONDS.get();
            case SKYWARDEN -> ValkeryConfig.SKYWARDEN_FLIGHT_SECONDS.get();
        };
        if (flightTicks >= enduranceSeconds * ValkyrieBalance.FLIGHT_TICKS_PER_SECOND) {
            if (player instanceof ServerPlayer serverPlayer) {
                ValkeryAdvancements.award(serverPlayer, "wings_of_endurance", "completed_endurance");
            }
            interruptFlight(player, recoveryTicksFor(rank));
            player.displayClientMessage(Component.translatable("message.valkery.wings_tired"), true);
        }
    }

    private static void showMasteryProgress(Player player, int windborneThreshold, int skywardenThreshold) {
        ValkyrieFlightMastery.Rank rank = ValkyrieFlightMastery.rank(
                player,
                windborneThreshold,
                skywardenThreshold
        );
        int experience = ValkyrieFlightMastery.experience(player);
        int nextThreshold = ValkyrieFlightMastery.nextThreshold(
                player,
                windborneThreshold,
                skywardenThreshold
        );
        if (rank == ValkyrieFlightMastery.Rank.SKYWARDEN) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.valkery.mastery.max",
                            Component.translatable(rank.translationKey()),
                            experience
                    ),
                    true
            );
        } else {
            player.displayClientMessage(
                    Component.translatable(
                            "message.valkery.mastery.progress",
                            Component.translatable(rank.translationKey()),
                            experience,
                            nextThreshold,
                            nextThreshold - experience
                    ),
                    true
            );
        }
    }

    private static int recoveryTicksFor(ValkyrieFlightMastery.Rank rank) {
        int seconds = switch (rank) {
            case INITIATE -> ValkeryConfig.INITIATE_RECOVERY_SECONDS.get();
            case WINDBORNE -> ValkeryConfig.WINDBORNE_RECOVERY_SECONDS.get();
            case SKYWARDEN -> ValkeryConfig.SKYWARDEN_RECOVERY_SECONDS.get();
        };
        return seconds * ValkyrieBalance.FLIGHT_TICKS_PER_SECOND;
    }

    private static void interruptForLowFood(Player player, CompoundTag data) {
        interruptFlight(player, 0);
        if (!data.getBoolean(LOW_FOOD_MESSAGE_SENT)) {
            player.displayClientMessage(
                    Component.translatable("message.valkery.wings_hungry"),
                    true
            );
            data.putBoolean(LOW_FOOD_MESSAGE_SENT, true);
        }
    }

    private static void updatePreviousPosition(Player player, CompoundTag data) {
        Vec3 position = player.position();
        data.putBoolean(HAS_PREVIOUS_POSITION, true);
        data.putBoolean(PREVIOUS_FALL_FLYING, player.isFallFlying());
        data.putDouble(PREVIOUS_X, position.x);
        data.putDouble(PREVIOUS_Y, position.y);
        data.putDouble(PREVIOUS_Z, position.z);
    }

    private static boolean movedThisTick(Player player, CompoundTag data) {
        if (!data.getBoolean(HAS_PREVIOUS_POSITION)
                || !data.getBoolean(PREVIOUS_FALL_FLYING)
                || !player.isFallFlying()) {
            return false;
        }
        Vec3 position = player.position();
        double dx = position.x - data.getDouble(PREVIOUS_X);
        double dy = position.y - data.getDouble(PREVIOUS_Y);
        double dz = position.z - data.getDouble(PREVIOUS_Z);
        double minimumMovement = ValkyrieBalance.MINIMUM_FLIGHT_MOVEMENT_PER_TICK;
        return dx * dx + dy * dy + dz * dz >= minimumMovement * minimumMovement;
    }

    private static boolean hasIcarusWings(Player player) {
        boolean equippedInBackSlot = CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.getStacksHandler("back")
                        .map(backSlot -> {
                            for (int index = 0; index < backSlot.getSlots(); index++) {
                                if (isIcarusWing(backSlot.getStacks().getStackInSlot(index))) {
                                    return true;
                                }
                            }
                            return false;
                        })
                        .orElse(false))
                .orElse(false);
        if (equippedInBackSlot) {
            return true;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (isIcarusWing(stack)) {
                return true;
            }
        }
        return false;
    }

    private static void enforceWings(ServerPlayer player) {
        if (!isValkyrie(player) && !hasIcarusWings(player)) {
            return;
        }

        CompoundTag data = ValkyrieFlightMastery.data(player);
        ResourceLocation selectedId = selectedWingsId(data);
        net.minecraft.world.item.Item selectedItem = selectedId == null
                ? null
                : ForgeRegistries.ITEMS.getValue(selectedId);
        if (selectedItem == null || selectedItem == net.minecraft.world.item.Items.AIR) {
            ItemStack existingWings = equippedBackWings(player);
            if (existingWings.isEmpty()) {
                existingWings = findInventoryWings(player);
            }
            selectedId = existingWings.isEmpty()
                    ? DEFAULT_WINGS_ID
                    : ForgeRegistries.ITEMS.getKey(existingWings.getItem());
            selectedItem = selectedId == null ? null : ForgeRegistries.ITEMS.getValue(selectedId);
            if (selectedId != null) {
                data.putString(WING_SELECTION_KEY, selectedId.toString());
            }
        }
        if (selectedItem == null || selectedItem == net.minecraft.world.item.Items.AIR) {
            return;
        }

        net.minecraft.world.item.Item wings = selectedItem;
        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.getStacksHandler("back").ifPresent(backSlot -> {
                    if (backSlot.getSlots() == 0) {
                        return;
                    }

                    ItemStack equipped = backSlot.getStacks().getStackInSlot(0);
                    if (!equipped.is(wings)) {
                        ItemStack wingStack = takeWingsFromInventory(player, wings);
                        if (!equipped.isEmpty()) {
                            ItemStack displaced = equipped.copy();
                            if (!player.getInventory().add(displaced)) {
                                player.drop(displaced, true);
                            }
                        }
                        if (wingStack.isEmpty()) {
                            wingStack = new ItemStack(wings);
                        }
                        handler.setEquippedCurio("back", 0, wingStack);
                    }
                })
        );
        data.putBoolean(VALKYRIE_MARKER, true);
    }

    private static ItemStack takeWingsFromInventory(Player player, net.minecraft.world.item.Item wings) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(wings)) {
                return stack.split(1);
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack findInventoryWings(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (isIcarusWing(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack equippedBackWings(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.getStacksHandler("back")
                        .map(backSlot -> {
                            for (int index = 0; index < backSlot.getSlots(); index++) {
                                ItemStack stack = backSlot.getStacks().getStackInSlot(index);
                                if (isIcarusWing(stack)) {
                                    return stack;
                                }
                            }
                            return ItemStack.EMPTY;
                        })
                        .orElse(ItemStack.EMPTY))
                .orElse(ItemStack.EMPTY);
    }

    private static boolean isIcarusWing(ItemStack stack) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return !stack.isEmpty()
                && itemId != null
                && "icarus".equals(itemId.getNamespace())
                && itemId.getPath().endsWith("_wings");
    }

    private static ResourceLocation selectedWingsId(CompoundTag data) {
        String selected = data.getString(WING_SELECTION_KEY);
        return selected.isEmpty() ? null : ResourceLocation.tryParse(selected);
    }

    public static String selectedWings(Player player) {
        ResourceLocation selected = selectedWingsId(ValkyrieFlightMastery.data(player));
        if (selected != null) {
            return selected.toString();
        }
        ItemStack equipped = equippedBackWings(player);
        if (equipped.isEmpty()) {
            equipped = findInventoryWings(player);
        }
        ResourceLocation equippedId = equipped.isEmpty()
                ? DEFAULT_WINGS_ID
                : ForgeRegistries.ITEMS.getKey(equipped.getItem());
        return equippedId == null ? DEFAULT_WINGS_ID.toString() : equippedId.toString();
    }

    public static boolean selectWings(ServerPlayer player, String wingId) {
        if (!isValkyrie(player)) {
            return false;
        }

        ResourceLocation id = ResourceLocation.tryParse(wingId);
        if (id == null || !"icarus".equals(id.getNamespace())) {
            return false;
        }

        net.minecraft.world.item.Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null || item == net.minecraft.world.item.Items.AIR
                || !id.getPath().endsWith("_wings")) {
            return false;
        }

        CompoundTag data = ValkyrieFlightMastery.data(player);
        data.putString(WING_SELECTION_KEY, id.toString());
        data.putBoolean(VALKYRIE_MARKER, true);
        enforceWings(player);
        return true;
    }

    public static boolean isValkyrie(Player player) {
        if (hasIcarusWings(player)) {
            ValkyrieFlightMastery.data(player).putBoolean(VALKYRIE_MARKER, true);
            return true;
        }
        CompoundTag persistentData = player.getPersistentData();
        return persistentData.contains("valkery_flight", CompoundTag.TAG_COMPOUND)
                && persistentData.getCompound("valkery_flight").getBoolean(VALKYRIE_MARKER);
    }

    private static void interruptFlight(Player player, int recoveryTicks) {
        player.stopFallFlying();
        CompoundTag data = ValkyrieFlightMastery.data(player);
        data.putInt(RECOVERY_TICKS, Math.max(data.getInt(RECOVERY_TICKS), recoveryTicks));
        data.putBoolean(SAFE_DESCENT, true);
        grantSafeDescent(player);
    }

    private static void grantSafeDescent(Player player) {
        player.addEffect(new MobEffectInstance(
                MobEffects.SLOW_FALLING,
                ValkyrieBalance.SAFE_DESCENT_EFFECT_TICKS,
                0,
                true,
                false,
                false
        ));
    }
}
