package com.igteam.immersivegeology.common.compat.ie.crafting.builders;

import blusunrize.immersiveengineering.api.crafting.BlastFurnaceRecipe;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/** IE 1.20 datagen API retained for Immersive Geology recipes. */
public class BlastFurnaceRecipeBuilder extends IEFinishedRecipe<BlastFurnaceRecipeBuilder>
{
    private BlastFurnaceRecipeBuilder()
    {
        super(BlastFurnaceRecipe.SERIALIZER.get());
    }

    public static BlastFurnaceRecipeBuilder builder(Item result)
    {
        return new BlastFurnaceRecipeBuilder().addResult(result);
    }

    public static BlastFurnaceRecipeBuilder builder(ItemStack result)
    {
        return new BlastFurnaceRecipeBuilder().addResult(result);
    }

    public static BlastFurnaceRecipeBuilder builder(TagKey<Item> result, int count)
    {
        return new BlastFurnaceRecipeBuilder().addResult(new IngredientWithSize(result, count));
    }

    public BlastFurnaceRecipeBuilder addSlag(ItemLike slag)
    {
        return addItem("slag", slag);
    }

    public BlastFurnaceRecipeBuilder addSlag(ItemStack slag)
    {
        return addItem("slag", slag);
    }

    public BlastFurnaceRecipeBuilder addSlag(TagKey<Item> slag, int count)
    {
        return addIngredient("slag", new IngredientWithSize(slag, count));
    }
}
