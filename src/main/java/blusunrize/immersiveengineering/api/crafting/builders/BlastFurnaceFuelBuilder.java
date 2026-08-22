package blusunrize.immersiveengineering.api.crafting.builders;

import blusunrize.immersiveengineering.api.crafting.BlastFurnaceFuel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/** IE 1.20 datagen API retained for Immersive Geology recipes. */
public class BlastFurnaceFuelBuilder extends IEFinishedRecipe<BlastFurnaceFuelBuilder>
{
    private BlastFurnaceFuelBuilder()
    {
        super(BlastFurnaceFuel.SERIALIZER.get());
        maxResultCount = 0;
    }

    public static BlastFurnaceFuelBuilder builder(ItemLike input)
    {
        return new BlastFurnaceFuelBuilder().addInput(input);
    }

    public static BlastFurnaceFuelBuilder builder(ItemStack input)
    {
        return new BlastFurnaceFuelBuilder().addInput(input);
    }

    public static BlastFurnaceFuelBuilder builder(TagKey<Item> input)
    {
        return new BlastFurnaceFuelBuilder().addInput(Ingredient.of(input));
    }
}
