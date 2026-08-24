/*
 * Muddykat
 * Copyright (c) 2024
 *
 * This code is licensed under "GNU LESSER GENERAL PUBLIC LICENSE"
 * Details can be found in the license file in the root folder of this project
 */

package com.igteam.immersivegeology.client.helper;

import blusunrize.immersiveengineering.api.crafting.FluidTagInput;
import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class IGRecipeBuilder<R extends IGRecipeBuilder<R>> implements FinishedRecipe {
	private final RecipeSerializer<?> serializer;
	private final List<Consumer<JsonObject>> writerFunctions;
	private ResourceLocation id;
	protected JsonArray inputArray = null;
	protected int inputCount = 0;
	protected int maxInputCount = 1;
	protected JsonArray resultArray = null;
	protected int resultCount = 0;
	protected int maxResultCount = 1;
	protected JsonArray conditions = null;

	protected IGRecipeBuilder(RecipeSerializer<?> serializer) {
		this.serializer = serializer;
		this.writerFunctions = new ArrayList<>();
	}

	protected boolean isComplete() {
		return true;
	}

	public void build(Consumer<FinishedRecipe> out, ResourceLocation id) {
		Preconditions.checkArgument(this.isComplete(), "This recipe is incomplete");
		this.id = id;
		out.accept(this);
	}

	public R addWriter(Consumer<JsonObject> writer) {
		Preconditions.checkArgument(this.id == null, "This recipe has already been finalized");
		this.writerFunctions.add(writer);
		return (R)this;
	}

	public R addCondition(ICondition condition) {
		if (this.conditions == null) {
			this.conditions = new JsonArray();
			this.addWriter((jsonObject) -> {
				jsonObject.add("neoforge:conditions", this.conditions);
			});
		}

		this.conditions.add(serialize(ICondition.CODEC, condition));
		return (R)this;
	}

	public R setTime(int time) {
		return this.addWriter((jsonObject) -> {
			jsonObject.addProperty("time", time);
		});
	}

	public R setEnergy(int energy) {
		return this.addWriter((jsonObject) -> {
			jsonObject.addProperty("energy", energy);
		});
	}

	public R setMultipleResults(int maxResultCount) {
		this.resultArray = new JsonArray();
		this.maxResultCount = maxResultCount;
		return this.addWriter((jsonObject) -> {
			jsonObject.add("results", this.resultArray);
		});
	}

	public R addMultiResult(JsonElement obj) {
		Preconditions.checkArgument(this.maxResultCount > 1, "This recipe does not support multiple results");
		Preconditions.checkArgument(this.resultCount < this.maxResultCount, "Recipe can only have " + this.maxResultCount + " results");
		this.resultArray.add(obj);
		++this.resultCount;
		return (R)this;
	}

	public R addResult(ItemLike itemProvider) {
		return this.addResult(new ItemStack(itemProvider));
	}

	public R addResult(ItemStack itemStack) {
		return this.resultArray != null ? this.addMultiResult(this.serializeItemStack(itemStack)) : this.addItem("result", itemStack);
	}

	public R addResult(Ingredient ingredient) {
		return this.resultArray != null ? this.addMultiResult(serialize(Ingredient.CODEC, ingredient)) : this.addWriter((jsonObject) -> {
			jsonObject.add("result", serialize(Ingredient.CODEC, ingredient));
		});
	}

	public R addResult(IngredientWithSize ingredientWithSize) {
		return this.resultArray != null ? this.addMultiResult(ingredientWithSize.serialize()) : this.addWriter((jsonObject) -> {
			jsonObject.add("result", ingredientWithSize.serialize());
		});
	}

	public R setUseInputArray(int maxInputCount, String key) {
		this.inputArray = new JsonArray();
		this.maxInputCount = maxInputCount;
		return this.addWriter((jsonObject) -> {
			jsonObject.add(key, this.inputArray);
		});
	}

	public R setUseInputArray(int maxInputCount) {
		return this.setUseInputArray(maxInputCount, "inputs");
	}

	public R addMultiInput(JsonElement obj) {
		Preconditions.checkArgument(this.maxInputCount > 1, "This recipe does not support multiple inputs");
		Preconditions.checkArgument(this.inputCount < this.maxInputCount, "Recipe can only have " + this.maxInputCount + " inputs");
		this.inputArray.add(obj);
		++this.inputCount;
		return (R)this;
	}

	public R addMultiInput(Ingredient ingredient) {
		return this.addMultiInput(serialize(Ingredient.CODEC, ingredient));
	}

	public R addMultiInput(IngredientWithSize ingredient) {
		return this.addMultiInput(ingredient.serialize());
	}

	protected String generateSafeInputKey() {
		Preconditions.checkArgument(this.inputCount < this.maxInputCount, "Recipe can only have " + this.maxInputCount + " inputs");
		String key = this.maxInputCount == 1 ? "input" : "input" + this.inputCount;
		++this.inputCount;
		return key;
	}

	public R addInput(ItemLike... itemProviders) {
		return this.inputArray != null ? this.addMultiInput(Ingredient.of(itemProviders)) : this.addIngredient(this.generateSafeInputKey(), itemProviders);
	}

	public R addInput(ItemStack... itemStacks) {
		return this.inputArray != null ? this.addMultiInput(Ingredient.of(itemStacks)) : this.addIngredient(this.generateSafeInputKey(), itemStacks);
	}

	public R addInput(TagKey<Item> tag) {
		return this.inputArray != null ? this.addMultiInput(Ingredient.of(tag)) : this.addIngredient(this.generateSafeInputKey(), tag);
	}

	public R addInput(Ingredient input) {
		return this.inputArray != null ? this.addMultiInput(input) : this.addIngredient(this.generateSafeInputKey(), input);
	}

	public R addInput(IngredientWithSize input) {
		return this.inputArray != null ? this.addMultiInput(input) : this.addIngredient(this.generateSafeInputKey(), input);
	}

	public JsonObject serializeItemStack(ItemStack stack) {
		return serialize(ItemStack.CODEC, stack).getAsJsonObject();
	}

	protected R addSimpleItem(String key, ItemLike item) {
		return this.addWriter((json) -> {
			json.addProperty(key, BuiltInRegistries.ITEM.getKey(item.asItem()).toString());
		});
	}

	public R addItem(String key, ItemLike item) {
		return this.addItem(key, new ItemStack(item));
	}

	public R addItem(String key, ItemStack stack) {
		Preconditions.checkArgument(!stack.isEmpty(), "May not add empty ItemStack to recipe");
		return this.addWriter((jsonObject) -> {
			jsonObject.add(key, this.serializeItemStack(stack));
		});
	}

	public R addIngredient(String key, ItemLike... itemProviders) {
		return this.addIngredient(key, Ingredient.of(itemProviders));
	}

	public R addIngredient(String key, ItemStack... itemStacks) {
		return this.addIngredient(key, Ingredient.of(itemStacks));
	}

	public R addIngredient(String key, TagKey<Item> tag) {
		return this.addIngredient(key, Ingredient.of(tag));
	}

	public R addIngredient(String key, Ingredient ingredient) {
		return this.addWriter((jsonObject) -> {
			jsonObject.add(key, serialize(Ingredient.CODEC, ingredient));
		});
	}

	public R addIngredient(String key, IngredientWithSize ingredient) {
		return this.addWriter((jsonObject) -> {
			jsonObject.add(key, ingredient.serialize());
		});
	}

	public R addFluid(String key, FluidStack fluidStack) {
		return this.addWriter((jsonObject) -> {
			jsonObject.add(key, serialize(FluidStack.CODEC, fluidStack));
		});
	}

	public R addFluid(FluidStack fluidStack) {
		return this.addFluid("fluid", fluidStack);
	}

	public R addFluid(Fluid fluid, int amount) {
		return this.addFluid("fluid", new FluidStack(fluid, amount));
	}

	public R addFluidTag(String key, FluidTagInput fluidTag) {
		return this.addWriter((jsonObject) -> {
			jsonObject.add(key, fluidTag.serialize());
		});
	}

	public R addFluidTag(String key, TagKey<Fluid> fluidTag, int amount) {
		return this.addFluidTag(key, new FluidTagInput(fluidTag, amount));
	}

	public R addFluidTag(TagKey<Fluid> fluidTag, int amount) {
		return this.addFluidTag("fluid", new FluidTagInput(fluidTag, amount));
	}

	public void serializeRecipeData(JsonObject jsonObject) {
		for(Consumer<JsonObject> writer : this.writerFunctions) {
			writer.accept(jsonObject);
		}
	}

	public ResourceLocation getId() {
		return this.id;
	}

	public RecipeSerializer<?> getType() {
		return this.serializer;
	}

	@Nullable
	public JsonObject serializeAdvancement() {
		return null;
	}

	@Nullable
	public ResourceLocation getAdvancementId() {
		return null;
	}

	protected static JsonObject serializeStackWithChance(IngredientWithSize ingredient, float chance, ICondition... conditions) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("chance", chance);
		jsonObject.add("output", ingredient.serialize());
		if (conditions.length > 0) {
			JsonArray conditionArray = new JsonArray();
			for(ICondition condition : conditions) {
				conditionArray.add(serialize(ICondition.CODEC, condition));
			}

			jsonObject.add("conditions", conditionArray);
		}

		return jsonObject;
	}

	private static <T> JsonElement serialize(Codec<T> codec, T value) {
		return codec.encodeStart(JsonOps.INSTANCE, value).getOrThrow();
	}
}
