/*
 * Compatibility bridge for Immersive Engineering's pre-1.21 FluidTagInput.
 *
 * NeoForge 1.21 moved sized fluid ingredients into its own crafting API.  IG's
 * recipe surface is intentionally kept source-compatible while serializers are
 * migrated, and this class delegates matching semantics to the 1.21 registry.
 */
package blusunrize.immersiveengineering.api.crafting;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class FluidTagInput implements Predicate<FluidStack>
{
    private final Either<TagKey<Fluid>, List<ResourceLocation>> fluids;
    private final int amount;
    private final @Nullable CompoundTag legacyNbt;

    public FluidTagInput(Either<TagKey<Fluid>, List<ResourceLocation>> fluids, int amount, @Nullable CompoundTag legacyNbt)
    {
        this.fluids = fluids;
        this.amount = amount;
        this.legacyNbt = legacyNbt;
    }

    public FluidTagInput(TagKey<Fluid> tag, int amount, @Nullable CompoundTag legacyNbt)
    {
        this(Either.left(tag), amount, legacyNbt);
    }

    public FluidTagInput(TagKey<Fluid> tag, int amount)
    {
        this(tag, amount, null);
    }

    public FluidTagInput(ResourceLocation tag, int amount, @Nullable CompoundTag legacyNbt)
    {
        this(TagKey.create(Registries.FLUID, tag), amount, legacyNbt);
    }

    public FluidTagInput(ResourceLocation tag, int amount)
    {
        this(tag, amount, null);
    }

    public static FluidTagInput deserialize(JsonElement input)
    {
        Preconditions.checkArgument(input instanceof JsonObject, "FluidTagInput must be a JSON object");
        JsonObject json = input.getAsJsonObject();
        return new FluidTagInput(ResourceLocation.parse(GsonHelper.getAsString(json, "tag")), GsonHelper.getAsInt(json, "amount"));
    }

    public FluidTagInput withAmount(int newAmount)
    {
        return new FluidTagInput(fluids, newAmount, legacyNbt);
    }

    @Override
    public boolean test(@Nullable FluidStack stack)
    {
        return stack!=null&&stack.getAmount() >= amount&&testIgnoringAmount(stack);
    }

    public boolean testIgnoringAmount(@Nullable FluidStack stack)
    {
        if(stack==null||stack.isEmpty()) return false;
        return fluids.map(stack.getFluid()::is, ids -> ids.contains(BuiltInRegistries.FLUID.getKey(stack.getFluid())));
    }

    public List<FluidStack> getMatchingFluidStacks()
    {
        return fluids.map(
                tag -> BuiltInRegistries.FLUID.getTag(tag)
                        .stream().flatMap(holders -> holders.stream()).map(Holder::value)
                        .map(fluid -> new FluidStack(fluid, amount)).toList(),
                ids -> ids.stream().map(BuiltInRegistries.FLUID::get).filter(fluid -> fluid!=null)
                        .map(fluid -> new FluidStack(fluid, amount)).toList()
        );
    }

    public JsonElement serialize()
    {
        JsonObject json = new JsonObject();
        fluids.ifLeft(tag -> json.addProperty("tag", tag.location().toString()));
        json.addProperty("amount", amount);
        return json;
    }

    public int getAmount()
    {
        return amount;
    }

    /** Exposes the equivalent native 1.21 ingredient used by IE multiblock processing. */
    public SizedFluidIngredient asSizedIngredient()
    {
        return fluids.map(
                tag -> SizedFluidIngredient.of(tag, amount),
                ids -> new SizedFluidIngredient(
                        FluidIngredient.of(ids.stream().map(BuiltInRegistries.FLUID::get)
                                .filter(java.util.Objects::nonNull).toArray(Fluid[]::new)),
                        amount
                )
        );
    }

    public FluidStack getRandomizedExampleStack(int random)
    {
        List<FluidStack> matching = getMatchingFluidStacks();
        return matching.get((random/20)%matching.size());
    }

    public static FluidTagInput read(FriendlyByteBuf input)
    {
        int count = input.readVarInt();
        List<ResourceLocation> matching = new ArrayList<>(count);
        for(int i = 0; i < count; ++i) matching.add(input.readResourceLocation());
        int amount = input.readInt();
        CompoundTag nbt = input.readBoolean()?input.readNbt(): null;
        return new FluidTagInput(Either.right(matching), amount, nbt);
    }

    public void write(FriendlyByteBuf output)
    {
        List<ResourceLocation> matching = getMatchingFluidStacks().stream()
                .map(FluidStack::getFluid).map(BuiltInRegistries.FLUID::getKey).toList();
        output.writeVarInt(matching.size());
        matching.forEach(output::writeResourceLocation);
        output.writeInt(amount);
        output.writeBoolean(legacyNbt!=null);
        if(legacyNbt!=null) output.writeNbt(legacyNbt);
    }

    public boolean extractFrom(IFluidHandler handler, FluidAction action)
    {
        for(int tank = 0; tank < handler.getTanks(); ++tank)
        {
            FluidStack inTank = handler.getFluidInTank(tank);
            if(testIgnoringAmount(inTank))
            {
                FluidStack requested = inTank.copyWithAmount(amount);
                FluidStack simulated = handler.drain(requested, FluidAction.SIMULATE);
                if(simulated.getAmount() >= amount)
                {
                    if(action!=FluidAction.SIMULATE) handler.drain(requested, action);
                    return true;
                }
            }
        }
        return false;
    }
}
