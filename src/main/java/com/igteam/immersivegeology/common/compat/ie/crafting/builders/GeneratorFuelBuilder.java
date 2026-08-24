package com.igteam.immersivegeology.common.compat.ie.crafting.builders;

import blusunrize.immersiveengineering.api.energy.GeneratorFuel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public class GeneratorFuelBuilder extends IEFinishedRecipe<GeneratorFuelBuilder>
{
    private GeneratorFuelBuilder(TagKey<Fluid> fluid, int burnTime)
    {
        super(GeneratorFuel.SERIALIZER.get());
        addWriter(json -> json.addProperty("fluidTag", fluid.location().toString()));
        addWriter(json -> json.addProperty("burnTime", burnTime));
    }

    public static GeneratorFuelBuilder builder(TagKey<Fluid> fluid, int burnTime)
    {
        return new GeneratorFuelBuilder(fluid, burnTime);
    }
}
