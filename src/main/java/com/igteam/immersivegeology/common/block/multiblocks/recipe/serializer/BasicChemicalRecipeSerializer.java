/*
 * Muddykat
 * Copyright (c) 2024
 *
 * This code is licensed under "GNU LESSER GENERAL PUBLIC LICENSE"
 * Details can be found in the license file in the root folder of this project
 */

package com.igteam.immersivegeology.common.block.multiblocks.recipe.serializer;

import blusunrize.immersiveengineering.api.ApiUtils;
import blusunrize.immersiveengineering.api.IEApi;
import com.igteam.immersivegeology.common.compat.ie.crafting.FluidTagInput;
import com.igteam.immersivegeology.common.recipe.LegacyIERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import com.google.gson.JsonObject;
import com.igteam.immersivegeology.common.block.multiblocks.recipe.BasicChemicalRecipe;
import com.igteam.immersivegeology.common.block.multiblocks.recipe.ChemicalRecipe;
import com.igteam.immersivegeology.core.registration.IGMultiblockProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.conditions.ICondition.IContext;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class BasicChemicalRecipeSerializer extends LegacyIERecipeSerializer<BasicChemicalRecipe>
{
	@Override
	public ItemStack getIcon()
	{
		return IGMultiblockProvider.SMALL_CHEMICAL_REACTOR.iconStack();
	}

	@Override
	public BasicChemicalRecipe readFromJson(ResourceLocation resourceLocation, JsonObject json, IContext iContext)
	{
		Lazy<ItemStack> output = readOutput(GsonHelper.getAsJsonObject(json, "result"));
		ItemStack itemOut;
		try
		{
			itemOut = output.get();
		}  catch(Exception e)
		{
			itemOut = ItemStack.EMPTY;
		}

		FluidStack fluidOut = FluidStack.OPTIONAL_CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json.get("fluidResult")).getOrThrow();
		IngredientWithSize itemInput = IngredientWithSize.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json.get("itemInput")).getOrThrow();
		Set<FluidTagInput> fluidSet = new HashSet<>();

		if(GsonHelper.isValidNode(json, "fluidInputA")) fluidSet.add(FluidTagInput.deserialize(GsonHelper.getAsJsonObject(json, "fluidInputA")));
		if(GsonHelper.isValidNode(json, "fluidInputB")) fluidSet.add(FluidTagInput.deserialize(GsonHelper.getAsJsonObject(json, "fluidInputB")));

		int energy = GsonHelper.getAsInt(json, "energy");
		int time = GsonHelper.getAsInt(json, "time");
		int damage_per_second = GsonHelper.getAsInt(json, "damage_per_second");

		return new BasicChemicalRecipe(resourceLocation, itemInput, fluidSet, itemOut, fluidOut, damage_per_second, energy, time);
	}

	@Override
	public @Nullable BasicChemicalRecipe fromNetwork(ResourceLocation resourceLocation, FriendlyByteBuf buffer)
	{
		
		ItemStack output = ItemStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buffer);
		FluidStack fluidOut = FluidStack.OPTIONAL_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buffer);
		IngredientWithSize itemInput = IngredientWithSize.STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf)buffer);
		HashSet<FluidTagInput> fluidSet = new HashSet<>();
		int fluid_input_size = buffer.readInt();
		for(int i = 0; i < fluid_input_size; i++) {
			FluidTagInput fluid = FluidTagInput.read(buffer);
			fluidSet.add(fluid);
		}

		int energy = buffer.readInt();
		int time = buffer.readInt();
		int damage_per_second = buffer.readInt();
		return new BasicChemicalRecipe(resourceLocation, itemInput, fluidSet, output, fluidOut, damage_per_second, energy, time);
	}

	@Override
	public void toNetwork(FriendlyByteBuf buffer, BasicChemicalRecipe recipe)
	{
		ItemStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buffer, recipe.itemOutput);
		FluidStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buffer, recipe.fluidOutput);
		IngredientWithSize.STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf)buffer, recipe.itemInput);
		buffer.writeInt(recipe.fluidIn.size());
		recipe.fluidIn.forEach(f -> f.write(buffer));
		buffer.writeInt(recipe.getTotalProcessEnergy());
		int time = recipe.getTotalProcessTime();
		buffer.writeInt(time);
		int damage_per_second = recipe.getDamagePerTick();
		buffer.writeInt(damage_per_second);
	}
}
