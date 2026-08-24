/*
 * Muddykat
 * Copyright (c) 2024
 *
 * This code is licensed under "GNU LESSER GENERAL PUBLIC LICENSE"
 * Details can be found in the license file in the root folder of this project
 */

package com.igteam.immersivegeology.common.block.multiblocks.recipe.serializer;

import blusunrize.immersiveengineering.api.ApiUtils;
import com.igteam.immersivegeology.common.compat.ie.crafting.FluidTagInput;
import com.igteam.immersivegeology.common.recipe.LegacyIERecipeSerializer;
import blusunrize.immersiveengineering.common.network.PacketUtils;
import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.igteam.immersivegeology.common.block.multiblocks.recipe.GeothermalConversionRecipe;
import com.igteam.immersivegeology.common.block.multiblocks.recipe.GeothermalExchangerRecipe;
import com.igteam.immersivegeology.core.registration.IGMultiblockProvider;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.conditions.ICondition.IContext;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GeothermalConversionRecipeSerializer extends LegacyIERecipeSerializer<GeothermalConversionRecipe>
{
	@Override
	public ItemStack getIcon()
	{
		return IGMultiblockProvider.GEOTHERMAL_EXCHANGER.iconStack();
	}

	@Override
	public GeothermalConversionRecipe readFromJson(ResourceLocation resourceLocation, JsonObject json, IContext iContext)
	{

		ResourceLocation transitionBlockName = ResourceLocation.parse(json.get("transitionBlock").getAsString());
		Block transitionBlock = Preconditions.checkNotNull(BuiltInRegistries.BLOCK.get(transitionBlockName));
		int transitionBlockHeat = json.get("blockHeat").getAsInt();

		boolean hasUpper = json.has("upperBoundBlock");
		boolean hasLower = json.has("lowerBoundBlock");
		Pair<Block, Integer> upperBound = null;
		Pair<Block, Integer> lowerBound = null;

		if(hasUpper)
		{
			ResourceLocation upperBoundBlockName = ResourceLocation.parse(json.get("upperBoundBlock").getAsString());
			Block upperBlock = Preconditions.checkNotNull(BuiltInRegistries.BLOCK.get(upperBoundBlockName));
			int upperHeat = json.get("upperHeat").getAsInt();
			upperBound = Pair.of(upperBlock, upperHeat);
		}

		if(hasLower)
		{
			ResourceLocation lowerBoundBlockName = ResourceLocation.parse(json.get("lowerBoundBlock").getAsString());
			Block lowerBoundBlock = Preconditions.checkNotNull(BuiltInRegistries.BLOCK.get(lowerBoundBlockName));
			int upperHeat = json.get("lowerHeat").getAsInt();
			lowerBound = Pair.of(lowerBoundBlock, upperHeat);
		}

		return new GeothermalConversionRecipe(resourceLocation, Lazy.of(() -> transitionBlock), transitionBlockHeat, upperBound, lowerBound);
	}

	@Override
	public @Nullable GeothermalConversionRecipe fromNetwork(ResourceLocation resourceLocation, FriendlyByteBuf buffer)
	{
		List<Block> blocks = PacketUtils.readList(buffer,
				buf -> BuiltInRegistries.BLOCK.get(buf.readResourceLocation()));

		Block baseBlock = blocks.get(0);
		Block upperBlock = blocks.get(1);
		Block lowerBlock = blocks.get(2);

		int transitionBlockHeat = buffer.readInt();
		boolean hasUpper = upperBlock != Blocks.BARRIER;
		boolean hasLower = lowerBlock != Blocks.BARRIER;

		Pair<Block, Integer> upperBound = null;
		Pair<Block, Integer> lowerBound = null;

		if(hasUpper)
		{
			upperBound = Pair.of(upperBlock, buffer.readInt());
		}

		if(hasLower)
		{
			lowerBound = Pair.of(lowerBlock, buffer.readInt());
		}

		return new GeothermalConversionRecipe(resourceLocation, Lazy.of(() -> baseBlock), transitionBlockHeat, upperBound, lowerBound);
	}

	@Override
	public void toNetwork(FriendlyByteBuf buffer, GeothermalConversionRecipe recipe)
	{
		PacketUtils.writeList(buffer, recipe.getMatchingBlocks(),
				(block, buf) -> buf.writeResourceLocation(BuiltInRegistries.BLOCK.getKey(block)));
		buffer.writeInt(recipe.blockHeat);
		if(recipe.upperHeat != null) buffer.writeInt(recipe.upperHeat);
		if(recipe.lowerHeat != null) buffer.writeInt(recipe.lowerHeat);
	}
}
