// NOTICE: This file includes code adapted from Immersive Engineering.
// This code is used in accordance with the terms of the Blu's License of Common Sense,
// which requires disclosure of significant code usage.
// For more details, refer to the source at [https://github.com/BluSunrize/ImmersiveEngineering/tree/1.20.1].
//
// The original code has been modified to fit the requirements of this project.
// -\('-')/- ~Muddykat

package com.igteam.immersivegeology.common.data.generators.loot;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class IGLootProvider extends LootTableProvider
{
	public IGLootProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries)
	{
		super(output, Set.of(), List.of(), registries);
	}

	@Override
	public List<SubProviderEntry> getTables()
	{
		return ImmutableList.of(
				new SubProviderEntry(IGBlockLootProvider::new, LootContextParamSets.BLOCK),
				new SubProviderEntry(IGChestLootProvider::new, LootContextParamSets.CHEST)
		);
	}
}
