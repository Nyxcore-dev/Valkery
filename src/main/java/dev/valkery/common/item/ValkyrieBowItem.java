package dev.valkery.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public final class ValkyrieBowItem extends BowItem {
    public ValkyrieBowItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.valkery.bow.oath").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.valkery.bow.desc").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public int getDefaultProjectileRange() {
        return 20;
    }
}
