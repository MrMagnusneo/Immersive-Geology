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

public class BasicChemicalRecipe extends MultiblockRecipe
{
	public static DeferredHolder<RecipeSerializer<?>, ? extends IERecipeSerializer<BasicChemicalRecipe>> SERIALIZER;
	public static final CachedRecipeList<BasicChemicalRecipe> RECIPES = new CachedRecipeList<>(IGRecipeTypes.BASIC_CHEMICAL_REACTOR);
	public final Lazy<ItemStack> itemOutput;
	public final FluidStack fluidOutput;
	public final Set<FluidTagInput> fluidIn;
	public final IngredientWithSize itemInput;
	Lazy<Integer> totalProcessEnergy;
	Lazy<Integer> totalProcessTime;
	Lazy<Integer> damage_per_second;

	public BasicChemicalRecipe(ResourceLocation id, IngredientWithSize inputItem, Set<FluidTagInput> fluidInputSet, TagOutput itemOutput, FluidStack fluidOutput, int damage_per_second, int energy, int time)
	{
		super(itemOutput, IGRecipeTypes.BASIC_CHEMICAL_REACTOR, time, energy, () -> new RecipeMultiplier(() -> 1, () -> 1));
		this.itemOutput = Lazy.of(itemOutput::get);
		this.fluidOutput = fluidOutput;
		this.fluidIn = fluidInputSet;
		this.itemInput = inputItem;
		totalProcessEnergy = Lazy.of(() -> energy);
		totalProcessTime = Lazy.of(() -> time);
		this.damage_per_second = Lazy.of(() -> damage_per_second);
		this.outputList = new TagOutputList(itemOutput);
		this.fluidOutputList = List.of(fluidOutput);
		this.fluidInputList = fluidIn.stream().map(FluidTagInput::asSizedIngredient).toList();
		this.setInputListWithSizes(List.of(itemInput));
		if(this.fluidIn.isEmpty() || this.fluidIn.size() > 2) IGLib.IG_LOGGER.error("Basic Chemical Recipe {} has either NO or more than 2 Fluid Tag inputs in the set.", id);
	}

	public static boolean acceptableCatalyst(Level level, ItemStack stack)
	{
		for(RecipeHolder<BasicChemicalRecipe> holder : RECIPES.getRecipes(level))
		{
			BasicChemicalRecipe recipe = holder.value();
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
		list.add(this.itemOutput.get());
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

	public static RecipeHolder<BasicChemicalRecipe> findRecipe(Level level, FluidStack inputA, FluidStack inputB, ItemStack itemInput)
	{
		List<FluidStack> tankedFluids = List.of(inputA, inputB);
		RecipeHolder<BasicChemicalRecipe> bestMatch = null;
		for(RecipeHolder<BasicChemicalRecipe> holder : RECIPES.getRecipes(level))
		{
			BasicChemicalRecipe recipe = holder.value();
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

	public int getDamagePerTick()
	{
		return damage_per_second.get();
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
