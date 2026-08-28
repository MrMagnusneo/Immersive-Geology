/*
 * Muddykat
 * Copyright (c) 2024
 *
 * This code is licensed under "GNU LESSER GENERAL PUBLIC LICENSE"
 * Details can be found in the license file in the root folder of this project
 */

package com.igteam.immersivegeology.common.block.multiblocks.recipe.serializer;

import com.igteam.immersivegeology.common.recipe.LegacyIERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.StackWithChance;
import blusunrize.immersiveengineering.api.crafting.TagOutput;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.igteam.immersivegeology.common.block.multiblocks.recipe.IndustrialSluiceRecipe;
import com.igteam.immersivegeology.core.registration.IGMultiblockProvider;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.conditions.ICondition.IContext;
import net.neoforged.neoforge.common.util.Lazy;
import org.jetbrains.annotations.Nullable;

public class IndustrialSluiceRecipeSerializer extends LegacyIERecipeSerializer<IndustrialSluiceRecipe>
{
	@Override
	public ItemStack getIcon()
	{
		return new ItemStack(Items.APPLE);
	}

	@Override
	public IndustrialSluiceRecipe readFromJson(ResourceLocation resourceLocation, JsonObject json, IContext iContext)
	{
		Ingredient input = Ingredient.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, GsonHelper.getAsJsonObject(json, "input")).getOrThrow();

		TagOutput primary = readOutput(json.get("result"));

		NonNullList<StackWithChance> byproducts = readByproductsFromJson(json);

		int time = GsonHelper.getAsInt(json, "time");
		int water = GsonHelper.getAsInt(json, "water");
		int energy = GsonHelper.getAsInt(json, "energy");
		return new IndustrialSluiceRecipe(resourceLocation, input, primary, byproducts, water, time, energy);
	}

	private NonNullList<StackWithChance> readByproductsFromJson(JsonObject obj) {
		// Probably not the most effective way to store the information, but if it works -\('-')/- ~Muddykat
		int amount_of_byproducts = GsonHelper.getAsInt(obj, "amount_of_byproducts");
		NonNullList<StackWithChance> list = NonNullList.createWithCapacity(amount_of_byproducts);

		JsonArray jsonArray = GsonHelper.getAsJsonArray(obj, "byproducts");

		for (int index = 0; index < amount_of_byproducts; index++) {
			JsonObject byproductObject = jsonArray.get(index).getAsJsonObject();
			StackWithChance byproduct = readConditionalStackWithChance(byproductObject, IContext.EMPTY);
			if(byproduct != null)
				list.add(byproduct);
		}

		return list;
	}

	@Override
	public @Nullable IndustrialSluiceRecipe fromNetwork(ResourceLocation resourceLocation, FriendlyByteBuf buffer)
	{
		TagOutput primary = readLazyStack(buffer);

		NonNullList<StackWithChance> byproducts = readByproducts(buffer);

		Ingredient input = Ingredient.CONTENTS_STREAM_CODEC.decode((net.minecraft.network.RegistryFriendlyByteBuf) buffer);
		int time = buffer.readInt();
		int water = buffer.readInt();
		int energy = buffer.readInt();
		return new IndustrialSluiceRecipe(resourceLocation, input, primary, byproducts, water, time, energy);
	}

	private NonNullList<StackWithChance> readByproducts(FriendlyByteBuf buffer)
	{
		int size = buffer.readInt();
		NonNullList<StackWithChance> item_list = NonNullList.createWithCapacity(size);
		for(int index = 0; index < size; index++)
		{
			item_list.add(StackWithChance.CODECS.streamCodec().decode((net.minecraft.network.RegistryFriendlyByteBuf)buffer));
		}

		return item_list;
	}

	private void writeByproducts(FriendlyByteBuf buffer, IndustrialSluiceRecipe recipe)
	{
		NonNullList<StackWithChance> byproducts = recipe.getByproducts();

		int size = byproducts.size();

		buffer.writeInt(size);
		for(int index = 0; index < size; index++)
		{
			StackWithChance.CODECS.streamCodec().encode((net.minecraft.network.RegistryFriendlyByteBuf)buffer, byproducts.get(index));
		}
	}

	@Override
	public void toNetwork(FriendlyByteBuf buffer, IndustrialSluiceRecipe recipe)
	{
		writeLazyStack(buffer, recipe.itemOutput);
		writeByproducts(buffer, recipe);
		Ingredient.CONTENTS_STREAM_CODEC.encode((net.minecraft.network.RegistryFriendlyByteBuf) buffer, recipe.itemIn);
		buffer.writeInt(recipe.getTotalProcessTime());
		buffer.writeInt(recipe.getTotalProcessWater());
		buffer.writeInt(recipe.getTotalProcessEnergy());
	}
}
