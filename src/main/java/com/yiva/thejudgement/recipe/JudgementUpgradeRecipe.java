package com.yiva.thejudgement.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.yiva.thejudgement.item.JudgementStaffItem;
import com.yiva.thejudgement.item.ModItems;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public final class JudgementUpgradeRecipe extends SmithingTransformRecipe {

    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient addition;
    private final JudgementStaffItem.UpgradeType upgradeType;

    public JudgementUpgradeRecipe(
            ResourceLocation id,
            Ingredient template,
            Ingredient base,
            Ingredient addition,
            JudgementStaffItem.UpgradeType upgradeType
    ) {
        super(id, template, base, addition, new ItemStack(ModItems.JUDGEMENT_STAFF.get()));
        this.template = template;
        this.base = base;
        this.addition = addition;
        this.upgradeType = upgradeType;
    }

    @Override
    public boolean matches(Container container, Level level) {
        return super.matches(container, level)
                && JudgementStaffItem.getUpgradeLevel(container.getItem(1), upgradeType)
                < JudgementStaffItem.MAX_UPGRADE_LEVEL;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        ItemStack result = container.getItem(1).copy();
        result.setCount(1);
        if (!JudgementStaffItem.applyUpgrade(result, upgradeType)) {
            return ItemStack.EMPTY;
        }
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.JUDGEMENT_UPGRADE.get();
    }

    public static final class Serializer implements RecipeSerializer<JudgementUpgradeRecipe> {

        @Override
        public JudgementUpgradeRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient template = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "template"));
            Ingredient base = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "base"));
            Ingredient addition = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "addition"));
            JudgementStaffItem.UpgradeType upgradeType = parseUpgradeType(
                    GsonHelper.getAsString(json, "upgrade")
            );
            return new JudgementUpgradeRecipe(id, template, base, addition, upgradeType);
        }

        @Nullable
        @Override
        public JudgementUpgradeRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            Ingredient template = Ingredient.fromNetwork(buffer);
            Ingredient base = Ingredient.fromNetwork(buffer);
            Ingredient addition = Ingredient.fromNetwork(buffer);
            JudgementStaffItem.UpgradeType upgradeType = buffer.readEnum(JudgementStaffItem.UpgradeType.class);
            return new JudgementUpgradeRecipe(id, template, base, addition, upgradeType);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, JudgementUpgradeRecipe recipe) {
            recipe.template.toNetwork(buffer);
            recipe.base.toNetwork(buffer);
            recipe.addition.toNetwork(buffer);
            buffer.writeEnum(recipe.upgradeType);
        }

        private static JudgementStaffItem.UpgradeType parseUpgradeType(String name) {
            return switch (name) {
                case "amplifier" -> JudgementStaffItem.UpgradeType.AMPLIFIER;
                case "stabilizer" -> JudgementStaffItem.UpgradeType.STABILIZER;
                case "phase_tuner" -> JudgementStaffItem.UpgradeType.PHASE_TUNER;
                default -> throw new JsonSyntaxException("Unknown Judgement upgrade type: " + name);
            };
        }
    }
}
