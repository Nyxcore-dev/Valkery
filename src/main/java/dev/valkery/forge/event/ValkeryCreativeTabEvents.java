package dev.valkery.forge.event;

import dev.valkery.common.ValkyrieConstants;
import dev.valkery.forge.item.ValkeryItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ValkyrieConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ValkeryCreativeTabEvents {
    private ValkeryCreativeTabEvents() {
    }

    @SubscribeEvent
    public static void addItemsToCombatTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ValkeryItems.SPEAR_OF_THE_OATH);
        }
    }
}
