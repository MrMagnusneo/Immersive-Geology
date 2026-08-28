/*
 * Muddykat
 * Copyright (c) 2024
 *
 * This code is licensed under "GNU LESSER GENERAL PUBLIC LICENSE"
 * Details can be found in the license file in the root folder of this project
 */

package com.igteam.immersivegeology.common.block.multiblocks.recipe.serializer;

import com.igteam.immersivegeology.common.compat.ie.crafting.FluidTagInput;
import com.igteam.immersivegeology.common.recipe.LegacyIERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.TagOutput;
import com.google.gson.JsonObject;
import com.igteam.immersivegeology.common.block.multiblocks.recipe.CrystallizerRecipe;
import com.igteam.immersivegeology.common.block.multiblocks.recipe.GravitySeparatorRecipe;
import com.igteam.immersivegeology.core.lib.IGLib;
import com.igteam.immersivegeology.core.registration.IGMultiblockProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.conditions.ICondition.IContext;
import net.neoforged.neoforge.common.util.Lazy;
import org.jetbrains.annotations.Nullable;

public class GravitySeparatorRecipeSerializer extends LegacyIERecipeSerializer<GravitySeparatorRecipe>
{
	@Override
	public ItemStack getIcon()
	{
		return IGMultiblockProvider.GRAVITY_SEPARATOR.iconStack();
	}

	@Override
	public GravitySeparatorRecipe readFromJson(ResourceLocation resourceLocation, JsonObject json, IContext iContext)
	{
		TagOutput output = readOutput(json.get("result"));
		TagOutput byproduct = readOutput(json.get("byproduct"));
		float chance = GsonHelper.getAsFloat(json, "byproduct_chance");
		Ingredient input = Ingredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, GsonHelper.getAsJsonObject(json, "input")).getOrThrow();
		int time = GsonHelper.getAsInt(json, "time");
		int water = GsonHelper.getAsInt(json, "water");
		return new GravitySeparatorRecipe(resourceLocation, input, output, byproduct, chance, water, time);
	}

	@Override
	public @Nullable GravitySeparatorRecipe fromNetwork(ResourceLocation resourceLocation, FriendlyByteBuf buffer)
	{
		
		TagOutput output = readLazyStack(buffer);
		TagOutput byproduct = readLazyStack(buffer);
		float chance = buffer.readFloat();
		Ingredient input = Ingredient.CONTENTS_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buffer);
		int time = buffer.readInt();
		int water = buffer.readInt();
		return new GravitySeparatorRecipe(resourceLocation, input, output, byproduct, chance, water, time);
	}

	@Override
	public void toNetwork(FriendlyByteBuf buffer, GravitySeparatorRecipe recipe)
	{
		
		writeLazyStack(buffer, recipe.itemOutput);
		writeLazyStack(buffer, recipe.itemByproduct);
		buffer.writeFloat(recipe.getChance());
		Ingredient.CONTENTS_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buffer, recipe.itemIn);
		buffer.writeInt(recipe.getTotalProcessTime());
		buffer.writeInt(recipe.getTotalProcessWater());
	}
}
