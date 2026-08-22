/*
 * Muddykat
 * Copyright (c) 2024
 *
 * This code is licensed under "GNU LESSER GENERAL PUBLIC LICENSE"
 * Details can be found in the license file in the root folder of this project
 */

package com.igteam.immersivegeology.common.block.multiblocks.recipe;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.MultiblockRecipe;
import blusunrize.immersiveengineering.api.crafting.TagOutput;
import blusunrize.immersiveengineering.api.crafting.TagOutputList;
import blusunrize.immersiveengineering.api.crafting.cache.CachedRecipeList;
import com.igteam.immersivegeology.core.registration.IGRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.registries.DeferredHolder;

public class GravitySeparatorRecipe extends MultiblockRecipe
{
	public static DeferredHolder<RecipeSerializer<?>, ? extends IERecipeSerializer<GravitySeparatorRecipe>> SERIALIZER;
	public static final CachedRecipeList<GravitySeparatorRecipe> RECIPES = new CachedRecipeList<>(IGRecipeTypes.GRAVITYSEPARATOR);
	public final Lazy<ItemStack> itemOutput;
	public final Lazy<ItemStack> itemByproduct;
	public final Ingredient itemIn;
	Lazy<Integer> totalProcessWater;
	Lazy<Integer> totalProcessTime;
	Lazy<Float> byproductChance;

	public GravitySeparatorRecipe(ResourceLocation id, Ingredient itemIn, Lazy<ItemStack> output, Lazy<ItemStack> byproduct, float chance, int water, int time)
	{
		super(new TagOutput(output.get()), IGRecipeTypes.GRAVITYSEPARATOR, time, 0, () -> new RecipeMultiplier(() -> 1, () -> 1));
		this.itemOutput = output;
		this.itemByproduct = byproduct;
		this.itemIn = itemIn;
		byproductChance = Lazy.of(() -> chance);
		totalProcessWater = Lazy.of(() -> water);
		totalProcessTime = Lazy.of(() -> time);
		this.outputList = new TagOutputList(new TagOutput(output.get()));
		this.setInputList(java.util.List.of(itemIn));
	}

	@Override
	public RecipeSerializer<?> getSerializer()
	{
		return SERIALIZER.get();
	}

	@Override
	public int getTotalProcessEnergy()
	{
		return 0;
	}

	@Override
	public int getTotalProcessTime()
	{
		return totalProcessTime.get();
	}

	public static GravitySeparatorRecipe findRecipe(Level level, ItemStack item)
	{
		for(RecipeHolder<GravitySeparatorRecipe> holder : RECIPES.getRecipes(level))
			if(holder.value().itemIn.test(item))
				return holder.value();
		return null;
	}

	@Override
	protected IERecipeSerializer<?> getIESerializer()
	{
		return SERIALIZER.get();
	}

	@Override
	public int getMultipleProcessTicks()
	{
		return 1;
	}

	public int getTotalProcessWater()
	{
		return totalProcessWater.get();
	}

	public float getChance()
	{
		return byproductChance.get();
	}
}
