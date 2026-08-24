package com.igteam.immersivegeology.common.compat.ie.crafting.builders;

import blusunrize.immersiveengineering.api.crafting.MixerRecipe;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/** IE 1.20 datagen API retained for Immersive Geology recipes. */
public class MixerRecipeBuilder extends IEFinishedRecipe<MixerRecipeBuilder>
{
    private MixerRecipeBuilder()
    {
        super(MixerRecipe.SERIALIZER.get());
        setUseInputArray(6);
    }

    public static MixerRecipeBuilder builder(Fluid fluid, int amount)
    {
        return builder(new FluidStack(fluid, amount));
    }

    public static MixerRecipeBuilder builder(FluidStack result)
    {
        return new MixerRecipeBuilder().addFluid("result", result);
    }
}
