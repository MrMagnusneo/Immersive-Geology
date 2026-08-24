package com.igteam.immersivegeology.common.compat.ie.crafting.builders;

import blusunrize.immersiveengineering.api.crafting.CrusherRecipe;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.ICondition;

/** IE 1.20 datagen API retained for Immersive Geology recipes. */
public class CrusherRecipeBuilder extends IEFinishedRecipe<CrusherRecipeBuilder>
{
    private final JsonArray secondaryArray = new JsonArray();

    private CrusherRecipeBuilder()
    {
        super(CrusherRecipe.SERIALIZER.get());
        addWriter(json -> json.add("secondaries", secondaryArray));
    }

    public static CrusherRecipeBuilder builder(Item result)
    {
        return new CrusherRecipeBuilder().addResult(result);
    }

    public static CrusherRecipeBuilder builder(ItemStack result)
    {
        return new CrusherRecipeBuilder().addResult(result);
    }

    public static CrusherRecipeBuilder builder(TagKey<Item> result, int count)
    {
        return new CrusherRecipeBuilder().addResult(new IngredientWithSize(result, count));
    }

    public static CrusherRecipeBuilder builder(IngredientWithSize result)
    {
        return new CrusherRecipeBuilder().addResult(result);
    }

    public CrusherRecipeBuilder addSecondary(ItemLike secondary, float chance)
    {
        return addSecondary(new ItemStack(secondary), chance);
    }

    public CrusherRecipeBuilder addSecondary(ItemStack secondary, float chance)
    {
        JsonObject entry = new JsonObject();
        entry.addProperty("chance", chance);
        entry.add("output", serializeItemStack(secondary));
        secondaryArray.add(entry);
        return this;
    }

    public CrusherRecipeBuilder addSecondary(TagKey<Item> secondary, float chance)
    {
        return addSecondary(new IngredientWithSize(secondary), chance);
    }

    public CrusherRecipeBuilder addSecondary(
            IngredientWithSize secondary, float chance, ICondition... conditions
    )
    {
        secondaryArray.add(serializeStackWithChance(secondary, chance, conditions));
        return this;
    }
}
