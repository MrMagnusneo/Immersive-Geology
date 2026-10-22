/*
 * Muddykat
 * Copyright (c) 2025
 *
 * This code is licensed under "GNU LESSER GENERAL PUBLIC LICENSE"
 * Details can be found in the license file in the root folder of this project
 */

package com.igteam.immersivegeology.gametest.tests;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.igteam.immersivegeology.core.lib.IGLib;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.locating.IModFile;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Walks every registered Immersive Geology block and item and checks that the client resources it needs are packaged:
 * a blockstate, an item model, every parent model and OBJ file, and every texture (directly or through the paletted
 * atlas permutations). A missing entry renders as the purple/black "missing texture" in game.
 */
public class AssetTests
{
	private static final String TEST_AREA = IGLib.MODID+":test_area";
	// Namespaces whose assets are not available on a dedicated server or belong to optional mods
	private static final Set<String> UNCHECKED_NAMESPACES = Set.of("minecraft", "neoforge", "tfc", "ad_astra");

	public static List<TestFunction> all()
	{
		return List.of(new TestFunction(
				"assets", "registered_content_has_models_and_textures", TEST_AREA,
				200, 0, true, AssetTests::testRegisteredContentHasAssets
		));
	}

	private static void testRegisteredContentHasAssets(GameTestHelper helper)
	{
		AssetIndex index = new AssetIndex();
		List<String> problems = new ArrayList<>();
		for(Block block : BuiltInRegistries.BLOCK)
		{
			ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
			if(!IGLib.MODID.equals(id.getNamespace()))
				continue;
			ResourceLocation blockstate = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "blockstates/"+id.getPath()+".json");
			Optional<JsonElement> json = index.readJson(blockstate);
			if(json.isEmpty())
				problems.add("block "+id+": missing blockstate");
			else
				index.collectBlockstateModels(json.get(), "block "+id, problems);
		}
		for(Item item : BuiltInRegistries.ITEM)
		{
			ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
			if(!IGLib.MODID.equals(id.getNamespace()))
				continue;
			// Fluid blocks have no item form that is ever rendered
			if(item instanceof BlockItem blockItem&&blockItem.getBlock() instanceof LiquidBlock)
				continue;
			index.checkModel(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "item/"+id.getPath()), "item "+id, problems);
		}
		if(!problems.isEmpty())
		{
			List<String> shown = problems.stream().distinct().limit(40).toList();
			IGLib.IG_LOGGER.error("Missing client assets ({}):\n{}", problems.size(), String.join("\n", problems.stream().distinct().toList()));
			helper.fail(problems.size()+" missing client assets, first: "+String.join("; ", shown));
			return;
		}
		helper.succeed();
	}

	private static final class AssetIndex
	{
		private final Map<String, Optional<IModFile>> modFiles = new HashMap<>();
		private final Set<ResourceLocation> checkedModels = new HashSet<>();
		private final Set<ResourceLocation> palettedTextures = new HashSet<>();

		AssetIndex()
		{
			readJson(ResourceLocation.withDefaultNamespace("atlases/blocks.json"), IGLib.MODID).ifPresent(atlas -> {
				for(JsonElement source : atlas.getAsJsonObject().getAsJsonArray("sources"))
				{
					JsonObject obj = source.getAsJsonObject();
					if(!obj.has("type")||!obj.get("type").getAsString().endsWith("paletted_permutations"))
						continue;
					String separator = obj.has("separator")?obj.get("separator").getAsString(): "_";
					Set<String> keys = obj.getAsJsonObject("permutations").keySet();
					for(JsonElement texture : obj.getAsJsonArray("textures"))
						for(String key : keys)
							palettedTextures.add(ResourceLocation.parse(texture.getAsString()+separator+key));
				}
			});
		}

		void collectBlockstateModels(JsonElement element, String owner, List<String> problems)
		{
			if(element.isJsonObject())
			{
				for(Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet())
				{
					if(entry.getKey().equals("model")&&entry.getValue().isJsonPrimitive())
						checkModel(ResourceLocation.parse(entry.getValue().getAsString()), owner, problems);
					else
						collectBlockstateModels(entry.getValue(), owner, problems);
				}
			}
			else if(element.isJsonArray())
				for(JsonElement child : element.getAsJsonArray())
					collectBlockstateModels(child, owner, problems);
		}

		void checkModel(ResourceLocation model, String owner, List<String> problems)
		{
			if(isUnchecked(model)||!checkedModels.add(model))
				return;
			Optional<JsonElement> json = readJson(ResourceLocation.fromNamespaceAndPath(model.getNamespace(), "models/"+model.getPath()+".json"));
			if(json.isEmpty())
			{
				problems.add(owner+": missing model "+model);
				return;
			}
			checkModelContents(json.get(), owner+" -> "+model, problems);
		}

		private void checkModelContents(JsonElement element, String owner, List<String> problems)
		{
			if(element.isJsonArray())
			{
				for(JsonElement child : element.getAsJsonArray())
					checkModelContents(child, owner, problems);
				return;
			}
			if(!element.isJsonObject())
				return;
			for(Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet())
			{
				JsonElement value = entry.getValue();
				switch(entry.getKey())
				{
					case "parent" -> {
						if(value.isJsonPrimitive())
							checkModel(ResourceLocation.parse(value.getAsString()), owner, problems);
					}
					case "textures" -> {
						if(value.isJsonObject())
							for(JsonElement texture : value.getAsJsonObject().asMap().values())
								if(texture.isJsonPrimitive()&&!texture.getAsString().startsWith("#"))
									checkTexture(ResourceLocation.parse(texture.getAsString()), owner, problems);
					}
					case "model" -> {
						// OBJ loader models point directly at the .obj file
						if(value.isJsonPrimitive()&&value.getAsString().endsWith(".obj"))
						{
							ResourceLocation obj = ResourceLocation.parse(value.getAsString());
							if(!isUnchecked(obj)&&readFile(obj).isEmpty())
								problems.add(owner+": missing OBJ "+obj);
						}
						else
							checkModelContents(value, owner, problems);
					}
					default -> checkModelContents(value, owner, problems);
				}
			}
		}

		private void checkTexture(ResourceLocation texture, String owner, List<String> problems)
		{
			if(isUnchecked(texture)||palettedTextures.contains(texture))
				return;
			if(readFile(ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), "textures/"+texture.getPath()+".png")).isEmpty())
				problems.add(owner+": missing texture "+texture);
		}

		private boolean isUnchecked(ResourceLocation location)
		{
			return UNCHECKED_NAMESPACES.contains(location.getNamespace())||!ModList.get().isLoaded(location.getNamespace());
		}

		Optional<JsonElement> readJson(ResourceLocation location)
		{
			return readJson(location, location.getNamespace());
		}

		Optional<JsonElement> readJson(ResourceLocation location, String modId)
		{
			return readFile(location, modId).flatMap(path -> {
				try(Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
				{
					return Optional.of(JsonParser.parseReader(reader));
				} catch(IOException|RuntimeException e)
				{
					return Optional.empty();
				}
			});
		}

		private Optional<Path> readFile(ResourceLocation location)
		{
			return readFile(location, location.getNamespace());
		}

		private Optional<Path> readFile(ResourceLocation location, String modId)
		{
			Optional<IModFile> modFile = modFiles.computeIfAbsent(modId, id -> Optional.ofNullable(ModList.get().getModFileById(id)).map(info -> info.getFile()));
			return modFile.map(file -> file.findResource("assets", location.getNamespace(), location.getPath()))
					.filter(Files::exists);
		}
	}
}
