package com.yiva.thejudgement.recipe;

import com.yiva.thejudgement.TheJudgement;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {

    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, TheJudgement.MOD_ID);

    public static final RegistryObject<RecipeSerializer<JudgementUpgradeRecipe>> JUDGEMENT_UPGRADE =
            RECIPE_SERIALIZERS.register("judgement_upgrade", JudgementUpgradeRecipe.Serializer::new);

    private ModRecipes() {
    }

    public static void register(IEventBus modEventBus) {
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
