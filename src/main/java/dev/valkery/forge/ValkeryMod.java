package dev.valkery.forge;

import dev.valkery.common.ValkyrieConstants;
import dev.valkery.forge.item.ValkeryItems;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ValkyrieConstants.MOD_ID)
public final class ValkeryMod {
    public ValkeryMod() {
        ValkeryItems.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ValkeryConfig.SPEC);
    }
}
