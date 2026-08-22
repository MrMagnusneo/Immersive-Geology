/*
 * Muddykat
 * Copyright (c) 2024
 *
 * This code is licensed under "GNU LESSER GENERAL PUBLIC LICENSE"
 * Details can be found in the license file in the root folder of this project
 */

package com.igteam.immersivegeology.common.block.multiblocks.logic;

import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IMultiblockComponent;

import blusunrize.immersiveengineering.api.energy.AveragingEnergyStorage;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IClientTickableComponent;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IServerTickableComponent;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.RedstoneControl;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IInitialMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.*;
import blusunrize.immersiveengineering.client.utils.TextUtils;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.interfaces.MBOverlayText;
import blusunrize.immersiveengineering.common.blocks.multiblocks.process.MultiblockProcessInMachine;
import blusunrize.immersiveengineering.common.blocks.multiblocks.process.MultiblockProcessor;
import blusunrize.immersiveengineering.common.blocks.multiblocks.process.ProcessContext.ProcessContextInMachine;
import blusunrize.immersiveengineering.common.fluids.ArrayFluidHandler;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.inventory.SlotwiseItemHandler;
import blusunrize.immersiveengineering.common.util.inventory.SlotwiseItemHandler.IOConstraint;
import blusunrize.immersiveengineering.common.util.inventory.SlotwiseItemHandler.IOConstraintGroup;
import blusunrize.immersiveengineering.common.util.inventory.WrappingItemHandler;
import blusunrize.immersiveengineering.common.util.inventory.WrappingItemHandler.IntRange;
import com.igteam.immersivegeology.common.block.multiblocks.logic.CrystallizerLogic.State;
import com.igteam.immersivegeology.common.block.multiblocks.recipe.CrystallizerRecipe;
import com.igteam.immersivegeology.common.block.multiblocks.recipe.FoundryRecipe;
import com.igteam.immersivegeology.common.block.multiblocks.shapes.FoundryShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;
import java.util.function.Consumer;
import java.util.function.Function;

public class FoundryLogic implements IMultiblockLogic<FoundryLogic.State>, IServerTickableComponent<FoundryLogic.State>, IClientTickableComponent<FoundryLogic.State>, MBOverlayText<FoundryLogic.State>
{
    public static final BlockPos REDSTONE_IN = new BlockPos(0,1,1);

    private static final int ENERGY_CAPACITY = 48000;
    private static final CapabilityPosition ENERGY_INPUT = new CapabilityPosition(0,1,0, RelativeBlockFace.LEFT);
    private static final CapabilityPosition FLUID_INPUT_CAP = new CapabilityPosition(1,2,0, RelativeBlockFace.UP);
    private static final MultiblockFace OUTPUT_POS = new MultiblockFace(3,1,1, RelativeBlockFace.RIGHT);
    private static final CapabilityPosition ITEM_OUTPUT_CAP = CapabilityPosition.opposing(OUTPUT_POS);
    public static final int TANK_VOLUME = FluidType.BUCKET_VOLUME;

    @Override
    public void tickClient(IMultiblockContext<State> context) {

    }

    @Override
    public void tickServer(IMultiblockContext<State> context) {
        final State state = context.getState();
        final int tank_amount = state.tank.getFluidAmount();
        state.processor.tickServer(state, context.getLevel(), state.rsState.isEnabled(context));
        tryRunRecipe(state, context.getLevel().getRawLevel());
        if(tank_amount != state.tank.getFluidAmount()) context.requestMasterBESync();
    }

    private void tryRunRecipe(State state, Level level)
    {
        if(state.energy.getEnergyStored() <= 0 || state.processor.getQueueSize() >= state.processor.getMaxQueueSize()) return;

        final FluidStack input = state.tank.getFluid();
        if(input.isEmpty()) return;
        net.minecraft.world.item.crafting.RecipeHolder<FoundryRecipe> recipe = FoundryRecipe.findRecipe(level, input);
        if(recipe == null) return;
        MultiblockProcessInMachine<FoundryRecipe> process = new MultiblockProcessInMachine<>(recipe);
        if(input.isEmpty()) process.setInputTanks(1);
        int drainSimulation = state.tank.drain(recipe.value().fluidIn.getAmount(), FluidAction.SIMULATE).getAmount();
        int drainAmount = recipe.value().fluidIn.getAmount();
        if(state.processor.addProcessToQueue(process, level, true) && drainSimulation == drainAmount)
        {
            state.processor.addProcessToQueue(process, level, false);
            state.tank.drain(recipe.value().fluidIn.getAmount(), FluidAction.EXECUTE).getAmount();
        }
    }

    @Override
    public State createInitialState(IInitialMultiblockContext<State> capability) {
        return new FoundryLogic.State(capability);
    }

    @Override
    public void dropExtraItems(State state, Consumer<ItemStack> drop)
    {
        MBInventoryUtils.dropItems(state.getInventory(), drop);
    }

    @Override
    public void registerCapabilities(IMultiblockComponent.CapabilityRegistrar<State> register)
    {
        register.register(Capabilities.EnergyStorage.BLOCK, (state, position) ->
                position.side()==null || ENERGY_INPUT.equals(position) ? state.energyCap : null);
        register.register(Capabilities.FluidHandler.BLOCK, (state, position) ->
                FLUID_INPUT_CAP.equals(position) ? state.fInputCap : null);
        register.register(Capabilities.ItemHandler.BLOCK, (state, position) ->
                ITEM_OUTPUT_CAP.equals(position) ? state.itemOutputCap : null);
    }

    @Nullable
    @Override
    public List<Component> getOverlayText(FoundryLogic.State state, BlockPos pos, BlockHitResult hit, Player player, boolean b)
    {
        if(Utils.isFluidRelatedItemStack(player.getItemInHand(InteractionHand.MAIN_HAND)))
            return List.of(TextUtils.formatFluidStack(state.tank.getFluid()));
        return null;
    }

    @Override
    public Function<BlockPos, VoxelShape> shapeGetter(ShapeType shapeType) {
        return FoundryShape.GETTER;
    }

    public static class State implements IMultiblockState, ProcessContextInMachine<FoundryRecipe>
    {
        public final RedstoneControl.RSState rsState = RedstoneControl.RSState.enabledByDefault();
        public final AveragingEnergyStorage energy = new AveragingEnergyStorage(ENERGY_CAPACITY);
        private final MultiblockProcessor<FoundryRecipe, ProcessContextInMachine<FoundryRecipe>> processor;
        public final SlotwiseItemHandler inventory;
        private final IFluidHandler fInputCap;
        private final IItemHandler itemOutputCap;
        private final IEnergyStorage energyCap;
        private final Supplier<IItemHandler> output;
        public final FluidTank tank = new FluidTank(TANK_VOLUME);

        public State(IInitialMultiblockContext<State> ctx){
            this.energyCap = this.energy;
            this.output = ctx.getCapabilityAt(Capabilities.ItemHandler.BLOCK, OUTPUT_POS);
            this.processor = new MultiblockProcessor<>(
                    1, 0, 1, ctx.getMarkDirtyRunnable(), FoundryRecipe.RECIPES::getById
            );
            this.inventory = SlotwiseItemHandler.makeWithGroups(
                    List.of(new IOConstraintGroup(IOConstraint.NO_CONSTRAINT, 1)), ctx.getMarkDirtyRunnable()
            );
            Runnable changedAndSync = () -> {
                ctx.getSyncRunnable().run();
                ctx.getMarkDirtyRunnable().run();
            };
            this.itemOutputCap = new WrappingItemHandler(
                    inventory, false, true, new IntRange(0, 1)
            );
            this.fInputCap = new ArrayFluidHandler(tank, true, true, changedAndSync);
        }

        @Override
        public void writeSaveNBT(CompoundTag nbt, HolderLookup.Provider provider){
            nbt.put("energy", energy.serializeNBT(provider));
            nbt.put("processor", processor.toNBT(provider));
            nbt.put("tank", tank.writeToNBT(provider, new CompoundTag()));
            nbt.put("inventory", inventory.serializeNBT(provider));
        }

        @Override
        public void readSaveNBT(CompoundTag nbt, HolderLookup.Provider provider){
            energy.deserializeNBT(provider, nbt.getCompound("energy"));
            tank.readFromNBT(provider, nbt.getCompound("tank"));
            inventory.deserializeNBT(provider, nbt.getCompound("inventory"));
            processor.fromNBT(nbt.getList("processor", Tag.TAG_COMPOUND), (getter, data, registries) -> new MultiblockProcessInMachine<>(getter, data), provider);
        }

        @Override
        public void writeSyncNBT(CompoundTag nbt, HolderLookup.Provider provider)
        {
            writeSaveNBT(nbt, provider);
        }

        @Override
        public void readSyncNBT(CompoundTag nbt, HolderLookup.Provider provider)
        {
            readSaveNBT(nbt, provider);
        }

        @Override
        public int[] getOutputSlots()
        {
            return new int[]{0};
        }

        @Override
        public IFluidTank[] getInternalTanks()
        {
            return new FluidTank[]{tank};
        }

        @Override
        public IItemHandlerModifiable getInventory()
        {
            return inventory.getRawHandler();
        }

        @Override
        public AveragingEnergyStorage getEnergy()
        {
            return energy;
        }
    }

}
