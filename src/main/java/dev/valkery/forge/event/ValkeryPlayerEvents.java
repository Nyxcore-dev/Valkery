package dev.valkery.forge.event;

import dev.valkery.common.ValkyrieConstants;
import dev.valkery.common.ValkyrieBowMastery;
import dev.valkery.common.ValkyrieFlightMastery;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ValkyrieConstants.MOD_ID)
public final class ValkeryPlayerEvents {
    private ValkeryPlayerEvents() {
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player replacement = event.getEntity();
        ValkyrieFlightMastery.copy(original, replacement);
        ValkyrieBowMastery.copy(original, replacement);
    }
}
