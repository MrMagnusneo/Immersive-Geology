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
import com.google.gson.JsonObject;
import com.igteam.immersivegeology.common.block.multiblocks.recipe.CrystallizerRecipe;
import com.igteam.immersivegeology.common.block.multiblocks.recipe.GeothermalExchangerRecipe;
import com.igteam.immersivegeology.core.registration.IGMultiblockProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.conditions.ICondition.IContext;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public class GeothermalExchangerRecipeSerializer extends LegacyIERecipeSerializer<GeothermalExchangerRecipe>
{
	@Override
	public ItemStack getIcon()
	{
		return IGMultiblockProvider.GEOTHERMAL_EXCHANGER.iconStack();
	}

	@Override
	public GeothermalExchangerRecipe readFromJson(ResourceLocation resourceLocation, JsonObject json, IContext iContext)
	{
		FluidStack fluid_output = FluidStack.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json.get("fluidResult")).getOrThrow();
		FluidTagInput input = FluidTagInput.deserialize(GsonHelper.getAsJsonObject(json, "input"));
		int energy = GsonHelper.getAsInt(json, "energy");
		int time = GsonHelper.getAsInt(json, "time");
		return new GeothermalExchangerRecipe(resourceLocation, input, Lazy.of(()->fluid_output), energy, time);
	}

	@Override
	public @Nullable GeothermalExchangerRecipe fromNetwork(ResourceLocation resourceLocation, FriendlyByteBuf buffer)
	{
		FluidStack fluid_output = FluidStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf)buffer);
		FluidTagInput input = FluidTagInput.read(buffer);
		int energy = buffer.readInt();
		int time = buffer.readInt();
		return new GeothermalExchangerRecipe(resourceLocation, input, Lazy.of(()->fluid_output), energy, time);
	}

	@Override
	public void toNetwork(FriendlyByteBuf buffer, GeothermalExchangerRecipe recipe)
	{
		FluidStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf)buffer, recipe.fluidOutput.get());
		recipe.fluidIn.write(buffer);
		buffer.writeInt(recipe.getTotalProcessEnergy());
		buffer.writeInt(recipe.getTotalProcessTime());
	}
}
