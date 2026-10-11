package dev.valkery.forge;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.advancements.Advancement;
import org.slf4j.Logger;

public final class ValkeryAdvancements {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ValkeryAdvancements() {
    }

    public static void award(ServerPlayer player, String advancementPath, String criterion) {
        if (!"root".equals(advancementPath)) {
            award(player, "root", "began_oath");
        }
        ResourceLocation id = new ResourceLocation("valkery", advancementPath);
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        if (advancement == null) {
            LOGGER.error("Could not award Valkery advancement {}: it is missing from the loaded datapack.", id);
            return;
        }
        player.getAdvancements().award(advancement, criterion);
    }
}
