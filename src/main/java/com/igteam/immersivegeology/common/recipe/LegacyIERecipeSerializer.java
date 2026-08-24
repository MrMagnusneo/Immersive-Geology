package com.igteam.immersivegeology.common.recipe;

import blusunrize.immersiveengineering.api.crafting.IERecipeSerializer;
import blusunrize.immersiveengineering.api.crafting.StackWithChance;
import blusunrize.immersiveengineering.api.crafting.TagOutput;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import io.netty.buffer.Unpooled;
import malte0811.dualcodecs.DualMapCodec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.conditions.ICondition.IContext;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.jetbrains.annotations.Nullable;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.stream.Stream;

/**
 * Bridges IG's 1.20 JSON/network serializers to the codec-based 1.21 recipe API.
 * Recipe identity is owned by RecipeHolder in 1.21, so the legacy constructor ID
 * is a stable internal placeholder.
 */
public abstract class LegacyIERecipeSerializer<R extends Recipe<?>> extends IERecipeSerializer<R>
{
    private static final ResourceLocation LEGACY_ID = ResourceLocation.fromNamespaceAndPath("immersivegeology", "legacy_codec");
    private static final String NETWORK_PAYLOAD_KEY = "immersivegeology:legacy_network";
    private final Map<R, JsonObject> sourceJson = Collections.synchronizedMap(new WeakHashMap<>());

    public abstract R readFromJson(ResourceLocation id, JsonObject json, IContext context);
    public abstract @Nullable R fromNetwork(ResourceLocation id, FriendlyByteBuf buffer);
    public abstract void toNetwork(FriendlyByteBuf buffer, R recipe);

    protected static Lazy<ItemStack> readOutput(JsonElement json)
    {
        ItemStack stack = TagOutput.CODECS.codec().parse(JsonOps.INSTANCE, json).getOrThrow().get();
        return Lazy.of(() -> stack);
    }

    protected static Lazy<ItemStack> readLazyStack(FriendlyByteBuf buffer)
    {
        ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf)buffer);
        return Lazy.of(() -> stack);
    }

    protected static void writeLazyStack(FriendlyByteBuf buffer, Lazy<ItemStack> stack)
    {
        ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf)buffer, stack.get());
    }

    protected static @Nullable StackWithChance readConditionalStackWithChance(JsonObject json, IContext context)
    {
        StackWithChance stack = StackWithChance.OPTIONAL_BASIC_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        for(var condition : stack.conditions())
            if(!condition.test(context))
                return null;
        return stack;
    }

    @Override
    protected final DualMapCodec<RegistryFriendlyByteBuf, R> codecs()
    {
        MapCodec<R> jsonCodec = new MapCodec<>()
        {
            @Override
            public <T> DataResult<R> decode(DynamicOps<T> ops, MapLike<T> input)
            {
                T encodedPayload = input.get(NETWORK_PAYLOAD_KEY);
                if(encodedPayload!=null)
                    return ops.getStringValue(encodedPayload).flatMap(LegacyIERecipeSerializer.this::decodePayload);

                T map = ops.createMap(input.entries());
                JsonElement json = ops.convertTo(JsonOps.INSTANCE, map);
                try
                {
                    R recipe = Objects.requireNonNull(
                            readFromJson(LEGACY_ID, json.getAsJsonObject(), IContext.EMPTY),
                            "Legacy recipe JSON decoder returned null"
                    );
                    sourceJson.put(recipe, json.deepCopy());
                    return DataResult.success(recipe);
                }
                catch(RuntimeException ex)
                {
                    return DataResult.error(() -> "Failed to decode legacy IG recipe JSON: "+ex.getMessage());
                }
            }

            @Override
            public <T> RecordBuilder<T> encode(R input, DynamicOps<T> ops, RecordBuilder<T> prefix)
            {
                JsonObject original = sourceJson.get(input);
                if(original!=null)
                {
                    for(var entry : original.entrySet())
                        prefix.add(ops.createString(entry.getKey()), JsonOps.INSTANCE.convertTo(ops, entry.getValue()));
                    return prefix;
                }
                DataResult<T> payload = encodePayload(input).map(ops::createString);
                return prefix.add(NETWORK_PAYLOAD_KEY, payload);
            }

            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops)
            {
                return Stream.of(ops.createString(NETWORK_PAYLOAD_KEY));
            }
        };
        StreamCodec<RegistryFriendlyByteBuf, R> networkCodec = StreamCodec.ofMember(
                (recipe, buffer) -> toNetwork(buffer, recipe),
                buffer -> Objects.requireNonNull(
                        fromNetwork(LEGACY_ID, buffer), "Legacy recipe network decoder returned null"
                )
        );
        return new DualMapCodec<>(jsonCodec, networkCodec);
    }

    private DataResult<String> encodePayload(R recipe)
    {
        RegistryFriendlyByteBuf buffer = newRegistryBuffer(Unpooled.buffer());
        try
        {
            toNetwork(buffer, recipe);
            byte[] encoded = new byte[buffer.readableBytes()];
            buffer.getBytes(buffer.readerIndex(), encoded);
            return DataResult.success(Base64.getEncoder().encodeToString(encoded));
        }
        catch(RuntimeException ex)
        {
            return DataResult.error(() -> "Failed to encode legacy IG recipe: "+ex.getMessage());
        }
        finally
        {
            buffer.release();
        }
    }

    private DataResult<R> decodePayload(String encoded)
    {
        final byte[] bytes;
        try
        {
            bytes = Base64.getDecoder().decode(encoded);
        }
        catch(IllegalArgumentException ex)
        {
            return DataResult.error(() -> "Invalid legacy IG recipe network payload: "+ex.getMessage());
        }

        RegistryFriendlyByteBuf buffer = newRegistryBuffer(Unpooled.wrappedBuffer(bytes));
        try
        {
            R recipe = fromNetwork(LEGACY_ID, buffer);
            return recipe!=null
                    ?DataResult.success(recipe)
                    :DataResult.error(() -> "Legacy recipe network decoder returned null");
        }
        catch(RuntimeException ex)
        {
            return DataResult.error(() -> "Failed to decode legacy IG recipe network payload: "+ex.getMessage());
        }
        finally
        {
            buffer.release();
        }
    }

    private static RegistryFriendlyByteBuf newRegistryBuffer(io.netty.buffer.ByteBuf buffer)
    {
        RegistryAccess registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        return new RegistryFriendlyByteBuf(buffer, registryAccess, ConnectionType.NEOFORGE);
    }
}
