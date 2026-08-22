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

public class CrystallizerRecipe extends MultiblockRecipe
{
	public static DeferredHolder<RecipeSerializer<?>, ? extends IERecipeSerializer<CrystallizerRecipe>> SERIALIZER;
	public static final CachedRecipeList<CrystallizerRecipe> RECIPES = new CachedRecipeList<>(IGRecipeTypes.CRYSTALLIZER);
	public final Lazy<ItemStack> itemOutput;
	public final Lazy<FluidStack> fluidOutput;
	public final FluidTagInput fluidIn;
	Lazy<Integer> totalProcessEnergy;
	Lazy<Integer> totalProcessTime;

	public CrystallizerRecipe(ResourceLocation id, FluidTagInput fluidInput, Lazy<ItemStack> output, Lazy<FluidStack> fluid_output, int energy, int time)
	{
		super(new TagOutput(output.get()), IGRecipeTypes.CRYSTALLIZER, time, energy, () -> new RecipeMultiplier(() -> 1, () -> 1));
		this.itemOutput = output;
		this.fluidIn = fluidInput;
		totalProcessEnergy = Lazy.of(() -> energy);
		totalProcessTime = Lazy.of(() -> time);
		this.fluidOutput = fluid_output;
		this.outputList = new TagOutputList(new TagOutput(output.get()));
		this.fluidOutputList = java.util.List.of(fluid_output.get());
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

	public static RecipeHolder<CrystallizerRecipe> findRecipe(Level level, FluidStack input)
	{
		for(RecipeHolder<CrystallizerRecipe> holder : RECIPES.getRecipes(level))
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
		return 1;
	}
}
