package com.yiva.thejudgement.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yiva.thejudgement.enchantment.ModEnchantments;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class ClampEnergyEnchantmentsLootModifier extends LootModifier {

    public static final Codec<ClampEnergyEnchantmentsLootModifier> CODEC = RecordCodecBuilder.create(
            instance -> codecStart(instance).apply(instance, ClampEnergyEnchantmentsLootModifier::new)
    );

    public ClampEnergyEnchantmentsLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @NotNull
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!context.getQueriedLootTableId().getPath().startsWith("chests/")) {
            return generatedLoot;
        }

        for (ItemStack stack : generatedLoot) {
            if (!stack.is(Items.ENCHANTED_BOOK)) {
                continue;
            }

            Map<Enchantment, Integer> enchantments = new HashMap<>(EnchantmentHelper.getEnchantments(stack));
            boolean changed = clampToLevelTwo(enchantments, ModEnchantments.ENERGY_CAPACITY.get());
            changed |= clampToLevelTwo(enchantments, ModEnchantments.ENERGY_REGENERATION.get());
            if (changed) {
                EnchantmentHelper.setEnchantments(enchantments, stack);
            }
        }
        return generatedLoot;
    }

    private static boolean clampToLevelTwo(Map<Enchantment, Integer> enchantments, Enchantment enchantment) {
        Integer level = enchantments.get(enchantment);
        if (level == null || level <= 2) {
            return false;
        }
        enchantments.put(enchantment, 2);
        return true;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
