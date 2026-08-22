package net.minecraft.data.recipes;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.Nullable;

/** Source-compatibility contract for IG's retained 1.20 data builders. */
public interface FinishedRecipe
{
    void serializeRecipeData(JsonObject json);
    ResourceLocation getId();
    RecipeSerializer<?> getType();
    @Nullable JsonObject serializeAdvancement();
    @Nullable ResourceLocation getAdvancementId();
}
