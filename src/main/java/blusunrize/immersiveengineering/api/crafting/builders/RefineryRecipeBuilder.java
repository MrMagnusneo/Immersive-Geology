package blusunrize.immersiveengineering.api.crafting.builders;

import blusunrize.immersiveengineering.api.crafting.FluidTagInput;
import blusunrize.immersiveengineering.api.crafting.RefineryRecipe;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/** IE 1.20 datagen API retained for Immersive Geology recipes. */
public class RefineryRecipeBuilder extends IEFinishedRecipe<RefineryRecipeBuilder>
{
    private RefineryRecipeBuilder()
    {
        super(RefineryRecipe.SERIALIZER.get());
        maxInputCount = 2;
    }

    public static RefineryRecipeBuilder builder(Fluid fluid, int amount)
    {
        return builder(new FluidStack(fluid, amount));
    }

    public static RefineryRecipeBuilder builder(FluidStack result)
    {
        return new RefineryRecipeBuilder().addFluid("result", result);
    }

    public RefineryRecipeBuilder addInput(FluidTagInput input)
    {
        return addFluidTag(generateSafeInputKey(), input);
    }

    public RefineryRecipeBuilder addInput(TagKey<Fluid> input, int amount)
    {
        return addFluidTag(generateSafeInputKey(), input, amount);
    }

    public RefineryRecipeBuilder addCatalyst(ItemLike catalyst)
    {
        return addIngredient("catalyst", catalyst);
    }

    public RefineryRecipeBuilder addCatalyst(TagKey<Item> catalyst)
    {
        return addIngredient("catalyst", catalyst);
    }

    public RefineryRecipeBuilder addCatalyst(Ingredient catalyst)
    {
        return addIngredient("catalyst", catalyst);
    }
}
