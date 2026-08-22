package blusunrize.immersiveengineering.api.crafting.builders;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import com.igteam.immersivegeology.client.helper.IGRecipeBuilder;

/** Compatibility base retained for IG's data builders on the 1.21 toolchain. */
public class IEFinishedRecipe<R extends IEFinishedRecipe<R>> extends IGRecipeBuilder<R>
{
    protected IEFinishedRecipe(IERecipeSerializer<?> serializer)
    {
        super(serializer);
    }
}
