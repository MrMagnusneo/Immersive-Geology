/*
 * Muddykat
 * Copyright (c) 2024
 *
 * This code is licensed under "GNU LESSER GENERAL PUBLIC LICENSE"
 * Details can be found in the license file in the root folder of this project
 */

package com.igteam.immersivegeology.common.block.multiblocks.recipe.serializer;

import blusunrize.immersiveengineering.api.ApiUtils;
import blusunrize.immersiveengineering.api.crafting.TagOutput;
import com.igteam.immersivegeology.common.compat.ie.crafting.FluidTagInput;
import com.igteam.immersivegeology.common.recipe.LegacyIERecipeSerializer;
import com.google.gson.JsonObject;
import com.igteam.immersivegeology.common.block.multiblocks.recipe.CentrifugeRecipe;
import com.igteam.immersivegeology.common.block.multiblocks.recipe.CrystallizerRecipe;
import com.igteam.immersivegeology.core.lib.IGLib;
import com.igteam.immersivegeology.core.material.data.enums.ChemicalEnum;
import com.igteam.immersivegeology.core.material.data.enums.MetalEnum;
import com.igteam.immersivegeology.core.registration.IGMultiblockProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.conditions.ICondition.IContext;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public class CentrifugeRecipeSerializer extends LegacyIERecipeSerializer<CentrifugeRecipe>
{
	@Override
	public ItemStack getIcon()
	{
		return IGMultiblockProvider.CENTRIFUGE.iconStack();
	}

	@Override
	public CentrifugeRecipe readFromJson(ResourceLocation resourceLocation, JsonObject json, IContext iContext)
	{
		FluidTagInput input = FluidTagInput.deserialize(GsonHelper.getAsJsonObject(json, "fluid_input"));
		TagOutput output = readOutput(json.get("item_output"));
		FluidStack primary_fluid_output = FluidStack.OPTIONAL_CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json.get("primary_fluid_out")).getOrThrow();
		FluidStack secondary_fluid_output = FluidStack.OPTIONAL_CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json.get("secondary_fluid_out")).getOrThrow();
		int energy = GsonHelper.getAsInt(json, "energy");
		int time = GsonHelper.getAsInt(json, "time");

		return new CentrifugeRecipe(resourceLocation, input, output, Lazy.of(() -> primary_fluid_output), Lazy.of(() -> secondary_fluid_output), energy, time);
	}

	@Override
	public @Nullable CentrifugeRecipe fromNetwork(ResourceLocation resourceLocation, FriendlyByteBuf buffer)
	{
		FluidTagInput input = FluidTagInput.read(buffer);
		TagOutput output = readLazyStack(buffer);
		FluidStack primaryFluidOutput = FluidStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf)buffer);
		FluidStack secondaryFluidOutput = FluidStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf)buffer);
		int energy = buffer.readInt();
		int time = buffer.readInt();
		return new CentrifugeRecipe(resourceLocation, input, output, Lazy.of(() -> primaryFluidOutput), Lazy.of(() -> secondaryFluidOutput), energy, time);
	}

	@Override
	public void toNetwork(FriendlyByteBuf buffer, CentrifugeRecipe recipe)
	{
		recipe.fluidIn.write(buffer);
		writeLazyStack(buffer, recipe.itemOutput);
		FluidStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf)buffer, recipe.primaryFluidOutput.get());
		FluidStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf)buffer, recipe.secondaryFluidOutput.get());
		buffer.writeInt(recipe.getTotalProcessEnergy());
		buffer.writeInt(recipe.getTotalProcessTime());
	}
}
