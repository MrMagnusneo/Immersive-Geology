/*
 * Muddykat
 * Copyright (c) 2024
 *
 * This code is licensed under "GNU LESSER GENERAL PUBLIC LICENSE"
 * Details can be found in the license file in the root folder of this project
 */

package com.igteam.immersivegeology.common.block.multiblocks.recipe;

import blusunrize.immersiveengineering.api.crafting.CrusherRecipe;
import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.api.crafting.MultiblockRecipe;
import blusunrize.immersiveengineering.api.crafting.TagOutput;
import blusunrize.immersiveengineering.api.crafting.TagOutputList;
import blusunrize.immersiveengineering.api.crafting.cache.CachedRecipeList;
import com.igteam.immersivegeology.core.registration.IGRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public class BallmillRecipe extends MultiblockRecipe
{
	public static DeferredHolder<RecipeSerializer<?>, ? extends IERecipeSerializer<BallmillRecipe>> SERIALIZER;
	public static final CachedRecipeList<BallmillRecipe> RECIPES = new CachedRecipeList<>(IGRecipeTypes.BALLMILL);
	public final IngredientWithSize itemOutput;
	public final IngredientWithSize itemIn;
	Lazy<Integer> totalProcessEnergy;
	Lazy<Integer> totalProcessTime;

	public BallmillRecipe(ResourceLocation id, IngredientWithSize input, IngredientWithSize output, int energy, int time)
	{
		super(new TagOutput(output), IGRecipeTypes.BALLMILL, time, energy, () -> new RecipeMultiplier(() -> 1, () -> 1));
		this.itemOutput = output;
		this.itemIn = input;
		totalProcessEnergy = Lazy.of(() -> energy);
		totalProcessTime = Lazy.of(() -> time);
		this.outputList = new TagOutputList(new TagOutput(output));
		this.setInputListWithSizes(List.of(input));
	}

	@Override
	public RecipeSerializer<?> getSerializer()
	{
		return SERIALIZER.get();
	}

	@Override
	public int getTotalProcessEnergy()
	{
		return totalProcessEnergy.get();
	}

	@Override
	public int getTotalProcessTime()
	{
		return totalProcessTime.get();
	}

	public static RecipeHolder<BallmillRecipe> findRecipe(Level level, ItemStack input)
	{
		for(RecipeHolder<BallmillRecipe> holder : RECIPES.getRecipes(level))
			if(holder.value().itemIn.test(input))
				return holder;
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
}
