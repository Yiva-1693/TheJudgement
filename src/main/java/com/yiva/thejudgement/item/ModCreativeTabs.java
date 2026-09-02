package com.yiva.thejudgement.item;

import com.yiva.thejudgement.TheJudgement;
import com.yiva.thejudgement.enchantment.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {

    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TheJudgement.MOD_ID);

    public static final RegistryObject<CreativeModeTab> THE_JUDGEMENT_TAB =
            CREATIVE_TABS.register("the_judgement", () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.thejudgement.the_judgement"))
                    .icon(() -> new ItemStack(ModItems.JUDGEMENT_STAFF.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.JUDGEMENT_STAFF.get());
                        output.accept(ModItems.ENERGY_CORE.get());
                        addEnchantedBooks(output, ModEnchantments.ENERGY_CAPACITY.get());
                        addEnchantedBooks(output, ModEnchantments.ENERGY_REGENERATION.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    private static void addEnchantedBooks(CreativeModeTab.Output output, Enchantment enchantment) {
        for (int level = 1; level <= enchantment.getMaxLevel(); level++) {
            output.accept(EnchantedBookItem.createForEnchantment(
                    new EnchantmentInstance(enchantment, level)
            ));
        }
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_TABS.register(modEventBus);
    }
}
