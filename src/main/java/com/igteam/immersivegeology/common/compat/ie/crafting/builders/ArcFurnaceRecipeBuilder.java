package com.igteam.immersivegeology.common.compat.ie.crafting.builders;

import blusunrize.immersiveengineering.api.crafting.ArcFurnaceRecipe;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import com.google.gson.JsonArray;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/** IE 1.20 datagen API retained for Immersive Geology recipes. */
public class ArcFurnaceRecipeBuilder extends IEFinishedRecipe<ArcFurnaceRecipeBuilder>
{
    private final JsonArray secondaryArray = new JsonArray();

    private ArcFurnaceRecipeBuilder()
    {
        super(ArcFurnaceRecipe.SERIALIZER.get());
        setMultipleResults(6);
        setUseInputArray(4, "additives");
        addWriter(json -> {
            if(!secondaryArray.isEmpty())
                json.add("secondaries", secondaryArray);
        });
    }

    public static ArcFurnaceRecipeBuilder builder(Item result)
    {
        return new ArcFurnaceRecipeBuilder().addResult(result);
    }

    public static ArcFurnaceRecipeBuilder builder(ItemStack result)
    {
        return new ArcFurnaceRecipeBuilder().addResult(result);
    }

    public static ArcFurnaceRecipeBuilder builder(TagKey<Item> result, int count)
    {
        return new ArcFurnaceRecipeBuilder().addResult(new IngredientWithSize(result, count));
    }

    public ArcFurnaceRecipeBuilder addSlag(ItemLike slag)
    {
        return addItem("slag", slag);
    }

    public ArcFurnaceRecipeBuilder addSlag(ItemStack slag)
    {
        return addItem("slag", slag);
    }

    public ArcFurnaceRecipeBuilder addSlag(TagKey<Item> slag, int count)
    {
        return addIngredient("slag", new IngredientWithSize(slag, count));
    }

    public ArcFurnaceRecipeBuilder addSecondary(TagKey<Item> secondary, float chance)
    {
        secondaryArray.add(serializeStackWithChance(new IngredientWithSize(secondary), chance));
        return this;
    }
}
