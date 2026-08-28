/*
 * Muddykat
 * Copyright (c) 2025
 *
 * This code is licensed under "GNU LESSER GENERAL PUBLIC LICENSE"
 * Details can be found in the license file in the root folder of this project
 */

package com.igteam.immersivegeology.gametest.tests;

import blusunrize.immersiveengineering.api.crafting.TagOutput;
import com.google.gson.JsonParser;
import com.igteam.immersivegeology.common.compat.ie.crafting.FluidTagInput;
import com.igteam.immersivegeology.common.block.multiblocks.recipe.CentrifugeRecipe;
import com.igteam.immersivegeology.core.lib.IGLib;
import com.mojang.serialization.JsonOps;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

public class ServerTests
{
	private static final String TEST_AREA = IGLib.MODID + ":test_area";
	public static List<TestFunction> all()
	{
		List<TestFunction> all = new ArrayList<>();
		all.addAll(basics());
		return all;
	}

	private static List<TestFunction> basics()
	{
		List<TestFunction> tests = new ArrayList<>();
		tests.add(new TestFunction(
				"server", "startup", TEST_AREA,
				200, 0, true, ServerTests::testServerStartup
		));

		tests.add(new TestFunction(
				"server", "connection", TEST_AREA,
				200, 0, true, ServerTests::testNetworkConnectivity
		));

		tests.add(new TestFunction(
				"server", "optional_fluid_recipe_outputs", TEST_AREA,
				200, 0, true, ServerTests::testOptionalFluidRecipeOutputs
		));
		tests.add(new TestFunction(
				"server", "tag_output_resolves_after_recipe_construction", TEST_AREA,
				200, 0, true, ServerTests::testTagOutputResolvesAfterRecipeConstruction
		));

		return tests;
	}

	private static void testServerStartup(GameTestHelper helper) {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		helper.assertTrue(server != null, "Server failed to initialize");
		helper.assertTrue(server.isRunning(), "Server is not running");
		helper.succeed();
	}

	private static void testNetworkConnectivity(GameTestHelper helper) {
		helper.runAfterDelay(40, () -> {
			try {
				MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
				helper.assertTrue(server.getPlayerList() != null,
						"Server player list not initialized");
				helper.succeed();
			} catch (Exception e) {
				helper.fail("Network test failed: " + e.getMessage());
			}
		});
	}

	private static void testOptionalFluidRecipeOutputs(GameTestHelper helper)
	{
		CentrifugeRecipe recipe = CentrifugeRecipe.RECIPES.getRecipes(helper.getLevel()).stream()
				.map(holder -> holder.value())
				.filter(value -> value.secondaryFluidOutput.get().isEmpty())
				.findFirst()
				.orElse(null);
		helper.assertTrue(recipe != null, "Missing centrifuge recipe with an empty optional fluid output");
		helper.assertTrue(!recipe.itemOutput.get().isEmpty(), "Centrifuge recipe resolved an item output as empty");
		helper.succeed();
	}

	private static void testTagOutputResolvesAfterRecipeConstruction(GameTestHelper helper)
	{
		TagOutput tagOutput = TagOutput.CODECS.codec().parse(
				JsonOps.INSTANCE, JsonParser.parseString("{\"tag\": \"c:ingots/iron\"}")
		).getOrThrow();
		CentrifugeRecipe recipe = new CentrifugeRecipe(
				ResourceLocation.fromNamespaceAndPath(IGLib.MODID, "tag_output_deferred_test"),
				new FluidTagInput(ResourceLocation.fromNamespaceAndPath("minecraft", "water"), 1),
				tagOutput,
				Lazy.of(() -> new FluidStack(Fluids.WATER, 1)),
				Lazy.of(() -> FluidStack.EMPTY),
				1, 1
		);
		helper.assertTrue(recipe.itemOutput.get().is(Items.IRON_INGOT),
				"Tag-backed output did not resolve after recipe construction");
		helper.succeed();
	}
}
