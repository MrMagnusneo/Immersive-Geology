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
import com.igteam.immersivegeology.common.block.multiblocks.logic.ChemicalReactorLogic;
import com.igteam.immersivegeology.core.lib.IGLib;
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

import java.util.List;
import java.util.Set;

public class ChemicalRecipe extends MultiblockRecipe
{
	public static DeferredHolder<RecipeSerializer<?>, ? extends IERecipeSerializer<ChemicalRecipe>> SERIALIZER;
	public static final CachedRecipeList<ChemicalRecipe> RECIPES = new CachedRecipeList<>(IGRecipeTypes.CHEMICAL_REACTOR);
	public final ItemStack itemOutput;
	public final FluidStack fluidOutput;
	public final Set<FluidTagInput> fluidIn;
	public final IngredientWithSize itemInput;
	Lazy<Integer> totalProcessEnergy;
	Lazy<Integer> totalProcessTime;

	public ChemicalRecipe(ResourceLocation id, IngredientWithSize inputItem, Set<FluidTagInput> fluidInputSet, ItemStack itemOutput, FluidStack fluidOutput, int energy, int time)
	{
		super(new TagOutput(itemOutput), IGRecipeTypes.CHEMICAL_REACTOR, time, energy, () -> new RecipeMultiplier(() -> 1, () -> 1));
		this.itemOutput = itemOutput;
		this.fluidOutput = fluidOutput;
		this.fluidIn = fluidInputSet;
		this.itemInput = inputItem;
		totalProcessEnergy = Lazy.of(() -> energy);
		totalProcessTime = Lazy.of(() -> time);
		this.outputList = new TagOutputList(new TagOutput(itemOutput));
		this.fluidOutputList = List.of(fluidOutput);
		this.fluidInputList = fluidIn.stream().map(FluidTagInput::asSizedIngredient).toList();
		this.setInputListWithSizes(List.of(itemInput));
		if(this.fluidIn.isEmpty() || this.fluidIn.size() > 3) IGLib.IG_LOGGER.error("Chemical Recipe {} has either NO or more than 3 Fluid Tag inputs in the set.", id);
	}

	public static boolean acceptableCatalyst(Level level, ItemStack stack)
	{
		for(RecipeHolder<ChemicalRecipe> holder : RECIPES.getRecipes(level))
		{
			ChemicalRecipe recipe = holder.value();
			if(recipe.itemInput.testIgnoringSize(stack)){
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean shouldCheckItemAvailability()
	{
		return !itemInput.hasNoMatchingItems();
	}

	@Override
	public NonNullList<ItemStack> getActualItemOutputs()
	{
		NonNullList<ItemStack> list = NonNullList.create();
		list.add(this.itemOutput);
		return list;
	}

	// Required for normal IE Multiblock Processor to access recipe info. Used in Server Tick.
	@Override
	public List<FluidStack> getActualFluidOutputs()
	{
		return List.of(fluidOutput);
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

	public static RecipeHolder<ChemicalRecipe> findRecipe(Level level, FluidStack inputA, FluidStack inputB, FluidStack inputC, ItemStack itemInput)
	{
		// TODO: Ensure this is tested a LOT.
		List<FluidStack> tankedFluids = List.of(inputA, inputB, inputC);
		RecipeHolder<ChemicalRecipe> bestMatch = null;
		for(RecipeHolder<ChemicalRecipe> holder : RECIPES.getRecipes(level))
		{
			ChemicalRecipe recipe = holder.value();
			if(!recipe.itemInput.test(itemInput)) continue;

			Set<FluidTagInput> recipeFluids = recipe.fluidIn;

			boolean allFluidsAvailable = recipeFluids.stream().allMatch(rf -> tankedFluids.stream().anyMatch(rf));

			if(allFluidsAvailable)
			{
				if(bestMatch == null || recipeFluids.size() > bestMatch.value().fluidIn.size())
				{
					bestMatch = holder;
				}
			}
		}

		return bestMatch;
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
