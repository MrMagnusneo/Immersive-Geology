package blusunrize.immersiveengineering.api.crafting.builders;

import blusunrize.immersiveengineering.api.crafting.SqueezerRecipe;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/** IE 1.20 datagen API retained for Immersive Geology recipes. */
public class SqueezerRecipeBuilder extends IEFinishedRecipe<SqueezerRecipeBuilder>
{
    private SqueezerRecipeBuilder()
    {
        super(SqueezerRecipe.SERIALIZER.get());
    }

    public static SqueezerRecipeBuilder builder(Fluid fluid, int amount)
    {
        return new SqueezerRecipeBuilder().addFluid(fluid, amount);
    }

    public static SqueezerRecipeBuilder builder(FluidStack fluid)
    {
        return new SqueezerRecipeBuilder().addFluid(fluid);
    }

    public static SqueezerRecipeBuilder builder()
    {
        return new SqueezerRecipeBuilder();
    }
}
