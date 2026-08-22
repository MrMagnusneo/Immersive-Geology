/*
 * Muddykat
 * Copyright (c) 2024
 *
 * This code is licensed under "GNU LESSER GENERAL PUBLIC LICENSE"
 * Details can be found in the license file in the root folder of this project
 */

package com.igteam.immersivegeology.common.block.multiblocks.recipe.serializer;

import com.igteam.immersivegeology.common.recipe.LegacyIERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import com.google.gson.JsonObject;
import com.igteam.immersivegeology.common.block.multiblocks.recipe.PelletizerRecipe;
import com.igteam.immersivegeology.common.block.multiblocks.recipe.RotaryKilnRecipe;
import com.igteam.immersivegeology.core.registration.IGMultiblockProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.conditions.ICondition.IContext;
import net.neoforged.neoforge.common.util.Lazy;
import org.jetbrains.annotations.Nullable;

public class PelletizerRecipeSerializer extends LegacyIERecipeSerializer<PelletizerRecipe>
{
	@Override
	public ItemStack getIcon()
	{
		return IGMultiblockProvider.PELLETIZER.iconStack();
	}

	@Override
	public PelletizerRecipe readFromJson(ResourceLocation resourceLocation, JsonObject json, IContext iContext)
	{
		Lazy<ItemStack> output = readOutput(json.get("result"));
		IngredientWithSize input = IngredientWithSize.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json.get("input")).getOrThrow();
		int energy = GsonHelper.getAsInt(json, "energy");
		int time = GsonHelper.getAsInt(json, "time");
		return new PelletizerRecipe(resourceLocation, input, output, energy, time);
	}

	@Override
	public @Nullable PelletizerRecipe fromNetwork(ResourceLocation resourceLocation, FriendlyByteBuf buffer)
	{
		Lazy<ItemStack> output = readLazyStack(buffer);
		IngredientWithSize input = IngredientWithSize.STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf)buffer);
		int energy = buffer.readInt();
		int time = buffer.readInt();
		return new PelletizerRecipe(resourceLocation, input, output, energy, time);
	}

	@Override
	public void toNetwork(FriendlyByteBuf buffer, PelletizerRecipe recipe)
	{
		writeLazyStack(buffer, recipe.itemOutput);
		IngredientWithSize.STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf)buffer, recipe.itemIn);
		buffer.writeInt(recipe.getTotalProcessEnergy());
		buffer.writeInt(recipe.getTotalProcessTime());
	}
}
