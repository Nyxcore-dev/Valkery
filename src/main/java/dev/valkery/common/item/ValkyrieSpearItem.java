package dev.valkery.common.item;

import dev.valkery.common.ValkyrieBalance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.function.Consumer;

public final class ValkyrieSpearItem extends SwordItem {
    private final Consumer<ServerPlayer> lightningSummoned;

    public ValkyrieSpearItem(Item.Properties properties, Consumer<ServerPlayer> lightningSummoned) {
        super(
                Tiers.IRON,
                ValkyrieBalance.SPEAR_ATTACK_DAMAGE_BONUS,
                ValkyrieBalance.SPEAR_ATTACK_SPEED_PENALTY,
                properties
        );
        this.lightningSummoned = lightningSummoned;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (user.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            HitResult hit = user.pick(ValkyrieBalance.LIGHTNING_RANGE, 1.0F, false);
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
            if (lightning == null) {
                return InteractionResultHolder.fail(stack);
            }
            lightning.moveTo(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
            if (user instanceof ServerPlayer serverPlayer) {
                lightning.setCause(serverPlayer);
            }
            boolean summoned = level.addFreshEntity(lightning);
            if (summoned && user instanceof ServerPlayer serverPlayer) {
                lightningSummoned.accept(serverPlayer);
            }

            ServerLevel serverLevel = (ServerLevel) level;
            double x = hit.getLocation().x;
            double y = hit.getLocation().y;
            double z = hit.getLocation().z;
            serverLevel.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y + 0.5, z, 28, 0.55, 0.7, 0.55, 0.18);
            serverLevel.sendParticles(ParticleTypes.END_ROD, x, y + 0.6, z, 10, 0.3, 0.45, 0.3, 0.08);
            level.playSound(null, x, y, z, SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.2F, 0.8F);
        }

        user.getCooldowns().addCooldown(this, ValkyrieBalance.LIGHTNING_COOLDOWN_TICKS);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Level level,
            List<Component> tooltip,
            TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(stack, level, tooltip, tooltipFlag);
        tooltip.add(Component.translatable("tooltip.valkery.spear.lightning").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.valkery.spear.cooldown").withStyle(ChatFormatting.GRAY));
    }
}
