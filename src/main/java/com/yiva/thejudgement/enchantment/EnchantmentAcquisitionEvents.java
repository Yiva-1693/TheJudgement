package com.yiva.thejudgement.enchantment;

import com.yiva.thejudgement.TheJudgement;
import com.yiva.thejudgement.item.JudgementStaffItem;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.enchanting.EnchantmentLevelSetEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = TheJudgement.MOD_ID)
public final class EnchantmentAcquisitionEvents {

    private static final int MAX_BOOKSHELF_POWER = 15;
    private static final int HIGHEST_ENCHANTMENT_ROW = 2;
    private static final int MAX_TABLE_LEVEL = 30;
    private static final Set<ItemStack> MAX_LEVEL_TABLE_ITEMS = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>())
    );

    private EnchantmentAcquisitionEvents() {
    }

    @SubscribeEvent
    public static void restrictStaffEnchanting(EnchantmentLevelSetEvent event) {
        ItemStack stack = event.getItem();
        if (stack.is(Items.BOOK) || stack.getItem() instanceof JudgementStaffItem) {
            if (event.getPower() >= MAX_BOOKSHELF_POWER) {
                MAX_LEVEL_TABLE_ITEMS.add(stack);
            } else {
                MAX_LEVEL_TABLE_ITEMS.remove(stack);
            }
        }

        if (!(stack.getItem() instanceof JudgementStaffItem)) {
            return;
        }

        boolean isMaxLevelOption = event.getPower() >= MAX_BOOKSHELF_POWER
                && event.getEnchantRow() == HIGHEST_ENCHANTMENT_ROW
                && event.getOriginalLevel() >= MAX_TABLE_LEVEL;
        if (!isMaxLevelOption) {
            event.setEnchantLevel(0);
        }
    }

    static boolean isMaxLevelTableItem(ItemStack stack) {
        return MAX_LEVEL_TABLE_ITEMS.contains(stack);
    }

    @SubscribeEvent
    public static void addLibrarianTrades(VillagerTradesEvent event) {
        if (event.getType() != VillagerProfession.LIBRARIAN) {
            return;
        }

        event.getTrades().get(2).add(enchantedBookTrade(ModEnchantments.ENERGY_CAPACITY, 1, 16, 12, 10));
        event.getTrades().get(2).add(enchantedBookTrade(ModEnchantments.ENERGY_REGENERATION, 1, 16, 12, 10));
        event.getTrades().get(4).add(enchantedBookTrade(ModEnchantments.ENERGY_CAPACITY, 2, 32, 6, 30));
        event.getTrades().get(4).add(enchantedBookTrade(ModEnchantments.ENERGY_REGENERATION, 2, 32, 6, 30));
    }

    private static VillagerTrades.ItemListing enchantedBookTrade(
            Supplier<? extends Enchantment> enchantment,
            int level,
            int emeraldCost,
            int maxUses,
            int villagerXp
    ) {
        return (trader, random) -> {
            ItemStack result = EnchantedBookItem.createForEnchantment(
                    new EnchantmentInstance(enchantment.get(), level)
            );
            return new MerchantOffer(
                    new ItemStack(Items.EMERALD, emeraldCost),
                    new ItemStack(Items.BOOK),
                    result,
                    maxUses,
                    villagerXp,
                    0.2F
            );
        };
    }
}
