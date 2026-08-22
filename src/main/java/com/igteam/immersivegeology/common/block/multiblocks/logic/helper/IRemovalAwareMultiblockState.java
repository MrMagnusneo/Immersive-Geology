/*
 * Muddykat
 * Copyright (c) 2025
 *
 * This code is licensed under "GNU LESSER GENERAL PUBLIC LICENSE"
 * Details can be found in the license file in the root folder of this project
 */

package com.igteam.immersivegeology.common.block.multiblocks.logic.helper;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;

/**
 * State lifecycle callback for machines with gameplay behavior on removal.
 * Capability invalidation is handled separately by the level capability API.
 */
public interface IRemovalAwareMultiblockState
{
	void onMultiblockPartRemoved(IMultiblockContext<?> ctx);
}
