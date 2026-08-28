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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CentrifugeRecipe extends MultiblockRecipe
{
	public static DeferredHolder<RecipeSerializer<?>, ? extends IERecipeSerializer<CentrifugeRecipe>> SERIALIZER;
	public static final CachedRecipeList<CentrifugeRecipe> RECIPES = new CachedRecipeList<>(IGRecipeTypes.CENTRIFUGE);
	public final Lazy<ItemStack> itemOutput;
	public final FluidTagInput fluidIn;

	public final Lazy<FluidStack> primaryFluidOutput;
	public final Lazy<FluidStack> secondaryFluidOutput;

	Lazy<Integer> totalProcessEnergy;
	Lazy<Integer> totalProcessTime;

	public CentrifugeRecipe(ResourceLocation id, FluidTagInput fluidInput, TagOutput output, Lazy<FluidStack> primaryFluidOutput, Lazy<FluidStack> secondaryFluidOutput, int energy, int time)
	{
		super(output, IGRecipeTypes.CENTRIFUGE, time, energy, () -> new RecipeMultiplier(() -> 1, () -> 1));
		this.itemOutput = Lazy.of(output::get);
		this.fluidIn = fluidInput;
		totalProcessEnergy = Lazy.of(() -> energy);
		totalProcessTime = Lazy.of(() -> time);
		this.primaryFluidOutput = primaryFluidOutput;
		this.secondaryFluidOutput = secondaryFluidOutput;

		this.outputList = new TagOutputList(output);
		this.fluidOutputList = java.util.List.of(primaryFluidOutput.get(), secondaryFluidOutput.get());
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

	public static RecipeHolder<CentrifugeRecipe> findRecipe(Level level, FluidStack input)
	{
		for(RecipeHolder<CentrifugeRecipe> holder : RECIPES.getRecipes(level))
			if(holder.value().fluidIn.test(input))
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
		return 4;
	}
}
