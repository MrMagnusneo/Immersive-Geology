/*
 * Muddykat
 * Copyright (c) 2024
 *
 * This code is licensed under "GNU LESSER GENERAL PUBLIC LICENSE"
 * Details can be found in the license file in the root folder of this project
 */

package com.igteam.immersivegeology.common.data.helper;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.igteam.immersivegeology.client.helper.IGRecipeBuilder;
import net.minecraft.world.item.crafting.Ingredient;

public class TFCCollapseRecipeBuilder extends IGRecipeBuilder<TFCCollapseRecipeBuilder>
{
	protected TFCCollapseRecipeBuilder()
	{
		super(TFCDatagenCompat.invokeCollapseRecipe());
	}

	public static TFCCollapseRecipeBuilder builder(Ingredient result)
	{
		return (TFCCollapseRecipeBuilder) new TFCCollapseRecipeBuilder().addWriter((jsonObject) -> {
			JsonArray conditions = new JsonArray();
			JsonObject tfcLoaded = new JsonObject();
			tfcLoaded.addProperty("type", "neoforge:mod_loaded");
			tfcLoaded.addProperty("modid", "tfc");
			conditions.add(tfcLoaded);
			jsonObject.add("neoforge:conditions", conditions);
			jsonObject.addProperty("type", "tfc:collapse");
			jsonObject.addProperty("copy_input", true);
			jsonObject.addProperty("ingredient", Ingredient.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, result).getOrThrow().getAsJsonObject().get("item").getAsString());
		});
	}
}
