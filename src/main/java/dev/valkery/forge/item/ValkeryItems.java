package dev.valkery.forge.item;

import dev.valkery.common.ValkyrieConstants;
import dev.valkery.common.item.ValkyrieSpearItem;
import dev.valkery.forge.ValkeryAdvancements;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ValkeryItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ValkyrieConstants.MOD_ID);

    public static final RegistryObject<Item> SPEAR_OF_THE_OATH = ITEMS.register(
            "spear_of_the_oath",
            () -> new ValkyrieSpearItem(
                    new Item.Properties().durability(512),
                    player -> ValkeryAdvancements.award(player, "oath_calls_thunder", "summoned_lightning")
            )
    );

    private ValkeryItems() {
    }
}
