/*
 * Muddykat
 * Copyright (c) 2024
 *
 * This code is licensed under "GNU LESSER GENERAL PUBLIC LICENSE"
 * Details can be found in the license file in the root folder of this project
 */

package com.igteam.immersivegeology.common.block.multiblocks.recipe;

import com.igteam.immersivegeology.common.compat.ie.crafting.FluidTagInput;
import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.api.crafting.MultiblockRecipe;
import blusunrize.immersiveengineering.api.crafting.TagOutput;
import blusunrize.immersiveengineering.api.crafting.TagOutputList;
import blusunrize.immersiveengineering.api.crafting.cache.CachedRecipeList;
import com.igteam.immersivegeology.core.registration.IGRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public class FoundryRecipe extends MultiblockRecipe
{
	public static DeferredHolder<RecipeSerializer<?>, ? extends IERecipeSerializer<FoundryRecipe>> SERIALIZER;
	public static final CachedRecipeList<FoundryRecipe> RECIPES = new CachedRecipeList<>(IGRecipeTypes.FOUNDRY);
	public final Lazy<ItemStack> itemOutput;
	public final FluidTagInput fluidIn;
	Lazy<Integer> totalProcessEnergy;
	Lazy<Integer> totalProcessTime;
	public final Item mold;

	public FoundryRecipe(ResourceLocation id, FluidTagInput fluidInput, Lazy<ItemStack> output, Item mold, int energy, int time)
	{
		super(new TagOutput(output.get()), IGRecipeTypes.FOUNDRY, time, energy, () -> new RecipeMultiplier(() -> 1, () -> 1));
		this.itemOutput = output;
		this.fluidIn = fluidInput;
		this.mold = mold;
		totalProcessEnergy = Lazy.of(() -> energy);
		totalProcessTime = Lazy.of(() -> time);
		this.outputList = new TagOutputList(new TagOutput(output.get()));
		this.fluidInputList = java.util.List.of(fluidInput.asSizedIngredient());

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

	public static RecipeHolder<FoundryRecipe> findRecipe(Level level, FluidStack input)
	{
		for(RecipeHolder<FoundryRecipe> holder : RECIPES.getRecipes(level))
			if(holder.value().fluidIn.test(input))
				return holder;
		return null;
	}

	@Override
	public ItemStack getDisplayStack(ItemStack input)
	{
		return new ItemStack(input.getItem(), input.getCount());
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
