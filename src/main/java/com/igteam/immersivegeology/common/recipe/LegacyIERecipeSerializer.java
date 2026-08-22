package com.igteam.immersivegeology.common.recipe;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import malte0811.dualcodecs.DualMapCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition.IContext;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Bridges IG's 1.20 JSON/network serializers to the codec-based 1.21 recipe API.
 * Recipe identity is owned by RecipeHolder in 1.21, so the legacy constructor ID
 * is a stable internal placeholder.
 */
public abstract class LegacyIERecipeSerializer<R extends Recipe<?>> extends IERecipeSerializer<R>
{
    private static final ResourceLocation LEGACY_ID = ResourceLocation.fromNamespaceAndPath("immersivegeology", "legacy_codec");

    public abstract R readFromJson(ResourceLocation id, JsonObject json, IContext context);
    public abstract @Nullable R fromNetwork(ResourceLocation id, FriendlyByteBuf buffer);
    public abstract void toNetwork(FriendlyByteBuf buffer, R recipe);

    @Override
    protected final DualMapCodec<RegistryFriendlyByteBuf, R> codecs()
    {
        MapCodec<R> jsonCodec = new MapCodec<>()
        {
            @Override
            public <T> DataResult<R> decode(DynamicOps<T> ops, MapLike<T> input)
            {
                T map = ops.createMap(input.entries());
                JsonElement json = ops.convertTo(JsonOps.INSTANCE, map);
                return DataResult.success(readFromJson(LEGACY_ID, json.getAsJsonObject(), IContext.EMPTY));
            }

            @Override
            public <T> RecordBuilder<T> encode(R input, DynamicOps<T> ops, RecordBuilder<T> prefix)
            {
                return prefix;
            }

            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops)
            {
                return Stream.empty();
            }
        };
        StreamCodec<RegistryFriendlyByteBuf, R> networkCodec = StreamCodec.ofMember(
                (recipe, buffer) -> toNetwork(buffer, recipe),
                buffer -> fromNetwork(LEGACY_ID, buffer)
        );
        return new DualMapCodec<>(jsonCodec, networkCodec);
    }
}
