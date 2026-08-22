/*
 * Muddykat
 * Copyright (c) 2024
 *
 * This code is licensed under "GNU LESSER GENERAL PUBLIC LICENSE"
 * Details can be found in the license file in the root folder of this project
 */

package com.igteam.immersivegeology.common.block.multiblocks.recipe;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IERecipeTypes.TypeWithClass;
import blusunrize.immersiveengineering.api.crafting.IESerializableRecipe;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.api.crafting.MultiblockRecipe;
import blusunrize.immersiveengineering.api.crafting.TagOutput;
import blusunrize.immersiveengineering.api.crafting.TagOutputList;
import blusunrize.immersiveengineering.api.crafting.cache.CachedRecipeList;
import com.igteam.immersivegeology.core.registration.IGRecipeTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.registries.DeferredHolder;

import javax.annotation.Nullable;

public class BloomeryRecipe extends MultiblockRecipe
{
	public static DeferredHolder<RecipeSerializer<?>, ? extends IERecipeSerializer<BloomeryRecipe>> SERIALIZER;
	public static final CachedRecipeList<BloomeryRecipe> RECIPES = new CachedRecipeList<>(IGRecipeTypes.BLOOMERY);
	public int time;
	public IngredientWithSize input;
	public Lazy<ItemStack> result;
	Lazy<Integer> totalProcessTime;

	public BloomeryRecipe(ResourceLocation id, IngredientWithSize input, Lazy<ItemStack> result, int time)
	{
		super(new TagOutput(result.get()), IGRecipeTypes.BLOOMERY, time, 0, () -> new RecipeMultiplier(() -> 1, () -> 1));
		this.input = input;
		this.result = result;
		this.time = time;
		totalProcessTime = Lazy.of(() -> time);
		this.outputList = new TagOutputList(new TagOutput(result.get()));
		this.setInputListWithSizes(java.util.List.of(input));
	}

	public static BloomeryRecipe findRecipe(Level level, ItemStack input)
	{
		for(RecipeHolder<BloomeryRecipe> holder : RECIPES.getRecipes(level))
			if(holder.value().input.test(input))
				return holder.value();
		return null;
	}

	public static BloomeryRecipe findRecipe(Level level, ItemStack input, @Nullable BloomeryRecipe hint)
	{
		if (input.isEmpty())
			return null;
		if (hint != null && hint.matches(input))
			return hint;
		for(RecipeHolder<BloomeryRecipe> holder : RECIPES.getRecipes(level))
			if(holder.value().input.test(input))
				return holder.value();
		return null;
	}

	private boolean matches(ItemStack input)
{
	return this.input.test(input);
}

	@Override
	public int getTotalProcessTime()
	{
		return totalProcessTime.get();
	}

	@Override
	protected IERecipeSerializer<?> getIESerializer()
	{
		return SERIALIZER.get();
	}

	@Override
	public int getMultipleProcessTicks()
	{
		return 0;
	}
}
