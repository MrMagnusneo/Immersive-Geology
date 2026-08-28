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
import blusunrize.immersiveengineering.api.crafting.TagOutput;
import com.google.gson.JsonObject;
import com.igteam.immersivegeology.common.block.multiblocks.recipe.ChemicalRecipe;
import com.igteam.immersivegeology.core.lib.IGLib;
import com.igteam.immersivegeology.core.registration.IGMultiblockProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.conditions.ICondition.IContext;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import javax.print.attribute.SetOfIntegerSyntax;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ChemicalRecipeSerializer extends LegacyIERecipeSerializer<ChemicalRecipe>
{
	@Override
	public ItemStack getIcon()
	{
		return IGMultiblockProvider.CHEMICAL_REACTOR.iconStack();
	}

	@Override
	public ChemicalRecipe readFromJson(ResourceLocation resourceLocation, JsonObject json, IContext iContext)
	{
		TagOutput output = readOutput(json.get("result"));
		FluidStack fluidOut = FluidStack.OPTIONAL_CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json.get("fluidResult")).getOrThrow();
		IngredientWithSize itemInput = IngredientWithSize.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json.get("itemInput")).getOrThrow();
		Set<FluidTagInput> fluidSet = new HashSet<>();

		if(GsonHelper.isValidNode(json, "fluidInputA"))
			fluidSet.add(FluidTagInput.deserialize(GsonHelper.getAsJsonObject(json, "fluidInputA")));
		if(GsonHelper.isValidNode(json, "fluidInputB"))
			fluidSet.add(FluidTagInput.deserialize(GsonHelper.getAsJsonObject(json, "fluidInputB")));
		if(GsonHelper.isValidNode(json, "fluidInputC"))
			fluidSet.add(FluidTagInput.deserialize(GsonHelper.getAsJsonObject(json, "fluidInputC")));

		int energy = GsonHelper.getAsInt(json, "energy");
		int time = GsonHelper.getAsInt(json, "time");

		return new ChemicalRecipe(resourceLocation, itemInput, fluidSet, output, fluidOut, energy, time);
	}

	@Override
	public @Nullable ChemicalRecipe fromNetwork(ResourceLocation resourceLocation, FriendlyByteBuf buffer)
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
		return new ChemicalRecipe(resourceLocation, itemInput, fluidSet, new TagOutput(output), fluidOut, energy, time);
	}

	@Override
	public void toNetwork(FriendlyByteBuf buffer, ChemicalRecipe recipe)
	{
		
		ItemStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buffer, recipe.itemOutput.get());
		FluidStack.OPTIONAL_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buffer, recipe.fluidOutput);
		IngredientWithSize.STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf)buffer, recipe.itemInput);
		buffer.writeInt(recipe.fluidIn.size());
		recipe.fluidIn.forEach(f -> f.write(buffer));
		buffer.writeInt(recipe.getTotalProcessEnergy());
		int time = recipe.getTotalProcessTime();
		buffer.writeInt(time);
	}
}
