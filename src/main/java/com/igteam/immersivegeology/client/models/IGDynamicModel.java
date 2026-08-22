/*
 * Muddykat
 * Copyright (c) 2024
 *
 * This code is licensed under "GNU LESSER GENERAL PUBLIC LICENSE"
 * Details can be found in the license file in the root folder of this project
 */

package com.igteam.immersivegeology.client.models;

import blusunrize.immersiveengineering.api.ApiUtils;
import com.igteam.immersivegeology.core.lib.IGLib;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = IGLib.MODID, bus = Bus.MOD, value = Dist.CLIENT)
public class IGDynamicModel
{
	private static final List<ModelResourceLocation> MODELS = new ArrayList<>();
	private final String raw_name;
	@SubscribeEvent
	public static void registerModels(ModelEvent.RegisterAdditional ev)
	{
		for(ModelResourceLocation model : MODELS)
			ev.register(model);
	}

	private final ModelResourceLocation name;

	public IGDynamicModel(String desc)
	{
		// References a generated json file
		raw_name = desc;
		this.name = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(IGLib.MODID, "dynamic/"+desc));
		MODELS.add(this.name);
	}

	public BakedModel get()
	{
		final BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
		return blockRenderer.getBlockModelShaper().getModelManager().getModel(name);
	}

	public List<BakedQuad> getNullQuads()
	{
		return getNullQuads(ModelData.EMPTY);
	}

	public List<BakedQuad> getNullQuads(ModelData data)
	{
		return get().getQuads(null, null, ApiUtils.RANDOM_SOURCE, data, null);
	}

	public ResourceLocation getName()
	{
		return name;
	}

	public String getSerializedName()
	{
		return raw_name;
	}
}
