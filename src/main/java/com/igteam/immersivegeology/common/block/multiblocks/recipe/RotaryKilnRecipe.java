/*
 * Muddykat
 * Copyright (c) 2024
 *
 * This code is licensed under "GNU LESSER GENERAL PUBLIC LICENSE"
 * Details can be found in the license file in the root folder of this project
 */

package com.igteam.immersivegeology.common.block.multiblocks.recipe;

import blusunrize.immersiveengineering.api.crafting.FluidTagInput;
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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

public class RotaryKilnRecipe extends MultiblockRecipe
{
	public static DeferredHolder<RecipeSerializer<?>, ? extends IERecipeSerializer<RotaryKilnRecipe>> SERIALIZER;
	public static final CachedRecipeList<RotaryKilnRecipe> RECIPES = new CachedRecipeList<>(IGRecipeTypes.ROTARYKILN);
	public final Lazy<ItemStack> itemOutput;
	public final IngredientWithSize itemIn;
	Lazy<Integer> totalProcessEnergy;
	Lazy<Integer> heatRequired;
	Lazy<Integer> totalProcessTime;

	public RotaryKilnRecipe(ResourceLocation id, IngredientWithSize input, Lazy<ItemStack> output, int time, int heat)
	{
		super(new TagOutput(output.get()), IGRecipeTypes.ROTARYKILN, time, time, () -> new RecipeMultiplier(() -> 1, () -> 1));
		this.itemOutput = output;
		this.itemIn = input;
		// Basic upkeep
		totalProcessEnergy = Lazy.of(() -> time);
		heatRequired = Lazy.of(() -> heat);
		totalProcessTime = Lazy.of(() -> time);
		this.outputList = new TagOutputList(new TagOutput(output.get()));
		this.setInputListWithSizes(java.util.List.of(input));
	}

	@Override
	public @NotNull RecipeSerializer<?> getSerializer()
	{
		return SERIALIZER.get();
	}

	@Override
	public int getTotalProcessEnergy()
	{
		return totalProcessEnergy.get();
	}

	public int getHeatRequired()
	{
		return heatRequired.get();
	}

	@Override
	public int getTotalProcessTime()
	{
		return totalProcessTime.get();
	}

	public static RecipeHolder<RotaryKilnRecipe> findRecipe(Level level, ItemStack input)
	{
		for(RecipeHolder<RotaryKilnRecipe> holder : RECIPES.getRecipes(level))
			if(holder.value().itemIn.testIgnoringSize(input))
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
