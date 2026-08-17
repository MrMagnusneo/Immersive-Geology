package com.igteam.immersivegeology.common.block.helper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/** A reload-safe neighbor lookup using NeoForge's level capability API. */
public final class MultiblockCapabilityReference<T> implements Supplier<T>
{
    private final BlockEntity local;
    private final BlockCapability<T, Direction> capability;
    private final Supplier<BlockPos> target;
    private final Direction side;

    private MultiblockCapabilityReference(
            BlockEntity local, BlockCapability<T, Direction> capability,
            Supplier<BlockPos> target, Direction side
    )
    {
        this.local = local;
        this.capability = capability;
        this.target = target;
        this.side = side;
    }

    public static <T> Map<Direction, MultiblockCapabilityReference<T>> forAllNeighbors(
            BlockEntity local, BlockCapability<T, Direction> capability
    )
    {
        Map<Direction, MultiblockCapabilityReference<T>> neighbors = new EnumMap<>(Direction.class);
        for(Direction direction : Direction.values())
            neighbors.put(direction, forNeighbor(local, capability, direction));
        return neighbors;
    }

    public static <T> MultiblockCapabilityReference<T> forNeighbor(
            BlockEntity local, BlockCapability<T, Direction> capability, Direction side
    )
    {
        return forRelative(local, capability, side.getNormal(), side.getOpposite());
    }

    public static <T> MultiblockCapabilityReference<T> forRelative(
            BlockEntity local, BlockCapability<T, Direction> capability, BlockPos offset, Direction side
    )
    {
        return new MultiblockCapabilityReference<>(local, capability,
                () -> local.getBlockPos().offset(offset), side);
    }

    @Override
    public @Nullable T get()
    {
        return getNullable();
    }

    public @Nullable T getNullable()
    {
        if(local.isRemoved()||local.getLevel()==null) return null;
        return local.getLevel().getCapability(capability, target.get(), side);
    }
}
