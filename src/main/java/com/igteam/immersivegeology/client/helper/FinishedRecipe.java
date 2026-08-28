package com.igteam.immersivegeology.client.helper;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.Nullable;

/**
 * Internal source-compatibility contract for the retained material recipe builders.
 *
 * <p>This deliberately lives in the Immersive Geology namespace. Defining it in
 * {@code net.minecraft.data.recipes} creates a split Java module package and prevents
 * NeoForge data generation from starting.</p>
 */
public interface FinishedRecipe
{
    void serializeRecipeData(JsonObject json);
    ResourceLocation getId();
    RecipeSerializer<?> getType();
    @Nullable JsonObject serializeAdvancement();
    @Nullable ResourceLocation getAdvancementId();
}
