import gzip
import json
import re
import struct
import tomllib
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / ".github" / "workflows" / "neoforge-1.21.1-port.yml"
GENERATED = ROOT / "src" / "generated" / "resources"


class RuntimeValidationWorkflowTest(unittest.TestCase):
    def test_port_does_not_add_classes_to_dependency_module_packages(self):
        forbidden_roots = [
            ROOT / "src" / "main" / "java" / "net" / "minecraft",
            ROOT / "src" / "main" / "java" / "blusunrize",
        ]
        for split_package in forbidden_roots:
            with self.subTest(path=split_package):
                self.assertFalse(
                    split_package.exists(),
                    "compatibility types in dependency namespaces create Java module split-packages",
                )

    def test_fluid_tag_compatibility_type_is_imported_from_mod_namespace(self):
        source_roots = [ROOT / "src" / "main" / "java", ROOT / "src" / "datagen" / "java"]
        compatibility_type = (
            ROOT
            / "src/main/java/com/igteam/immersivegeology/common/compat/ie/crafting/FluidTagInput.java"
        )
        expected_import = (
            "import com.igteam.immersivegeology.common.compat.ie.crafting.FluidTagInput;"
        )
        offenders = []
        for source_root in source_roots:
            for path in source_root.rglob("*.java"):
                if path == compatibility_type:
                    continue
                source = path.read_text(encoding="utf-8")
                if "FluidTagInput" in source and expected_import not in source:
                    offenders.append(str(path.relative_to(ROOT)))
        self.assertFalse(offenders, "missing explicit compatibility import:\n" + "\n".join(offenders))

    def test_event_bus_subscribers_have_listener_methods(self):
        offenders = []
        source_root = ROOT / "src" / "main" / "java"
        for path in source_root.rglob("*.java"):
            source = path.read_text(encoding="utf-8")
            if "@EventBusSubscriber" in source and "@SubscribeEvent" not in source:
                offenders.append(str(path.relative_to(ROOT)))
        self.assertFalse(
            offenders,
            "NeoForge rejects automatic subscribers without @SubscribeEvent methods:\n"
            + "\n".join(offenders),
        )

    def test_paletted_atlas_outputs_are_tracked_for_model_validation(self):
        material = (
            ROOT
            / "src/main/java/com/igteam/immersivegeology/core/material/GeologyMaterial.java"
        ).read_text(encoding="utf-8")
        self.assertIn("PALETTED_TEXTURE", material)
        self.assertIn("trackGenerated(texture, PALETTED_TEXTURE)", material)
        block_models = (
            ROOT
            / "src/datagen/java/com/igteam/immersivegeology/common/data/generators/IGBlockStateProvider.java"
        ).read_text(encoding="utf-8")
        item_models = (
            ROOT
            / "src/datagen/java/com/igteam/immersivegeology/common/data/generators/IGItemModelProvider.java"
        ).read_text(encoding="utf-8")
        self.assertGreaterEqual(block_models.count("trackPalettedTexture("), 3)
        self.assertIn("trackPalettedTexture(", item_models)

        for compat_type in (
            "src/main/java/com/igteam/immersivegeology/core/material/data/stone/compat/tfc/MaterialTFCRawStone.java",
            "src/main/java/com/igteam/immersivegeology/core/material/data/stone/compat/adastra/MaterialAdAstraStone.java",
        ):
            source = (ROOT / compat_type).read_text(encoding="utf-8")
            with self.subTest(compat_type=compat_type):
                self.assertIn("trackOptionalTexture(", source)
                self.assertIn('withSuffix("_top")', source)
                self.assertIn('withSuffix("_side")', source)

    def test_declared_multiblock_sizes_match_structure_templates(self):
        declarations = {}
        declaration_pattern = re.compile(
            r'multiblocks/([^"\)]+)"\),\s*new BlockPos\([^)]*\),\s*'
            r'new BlockPos\([^)]*\),\s*new BlockPos\(([^)]*)\)',
            re.DOTALL,
        )
        multiblock_sources = (
            ROOT
            / "src/main/java/com/igteam/immersivegeology/common/block/multiblocks"
        )
        for path in multiblock_sources.glob("*.java"):
            match = declaration_pattern.search(path.read_text(encoding="utf-8"))
            if match:
                declarations[match.group(1)] = tuple(
                    int(value) for value in re.findall(r"-?\d+", match.group(2))
                )

        structures = (
            ROOT
            / "src/main/resources/data/immersivegeology/structure/multiblocks"
        )
        mismatches = []
        for path in structures.glob("*.nbt"):
            data = gzip.open(path, "rb").read()
            offset = 3  # root TAG_Compound and its empty name
            self.assertEqual(9, data[offset])  # first entry is the size TAG_List
            offset += 1
            name_length = struct.unpack(">H", data[offset : offset + 2])[0]
            offset += 2 + name_length
            self.assertEqual(3, data[offset])  # TAG_Int list entries
            offset += 1
            length = struct.unpack(">i", data[offset : offset + 4])[0]
            offset += 4
            template_size = struct.unpack(">" + "i" * length, data[offset : offset + 4 * length])
            declared_size = declarations.get(path.stem)
            if template_size != declared_size:
                mismatches.append(f"{path.stem}: template={template_size}, declared={declared_size}")
        self.assertFalse(mismatches, "\n".join(mismatches))

    def test_handcrafted_misc_item_models_are_not_replaced_by_generic_fallbacks(self):
        item_models = (
            ROOT
            / "src/datagen/java/com/igteam/immersivegeology/common/data/generators/IGItemModelProvider.java"
        ).read_text(encoding="utf-8")
        self.assertIn("i.getFlag()==ItemCategoryFlags.MISC", item_models)
        self.assertIn("continue;", item_models)

    def test_energy_pipe_uses_its_explicit_obj_item_model_only(self):
        item_models = (
            ROOT
            / "src/datagen/java/com/igteam/immersivegeology/common/data/generators/IGItemModelProvider.java"
        ).read_text(encoding="utf-8")
        self.assertIn("item.getFlag() == BlockCategoryFlags.ENERGY_PIPE", item_models)

    def test_gametest_template_uses_the_1_21_singular_resource_directory(self):
        template = (
            ROOT
            / "src/gametest/resources/data/immersivegeology/structure/test_area.nbt"
        )
        self.assertTrue(template.is_file())
        self.assertFalse(
            (ROOT / "src/gametest/resources/data/immersivegeology/structures").exists()
        )

    def test_spawn_map_diagnostic_writes_inside_the_build_directory(self):
        source = (
            ROOT
            / "src/gametest/java/com/igteam/immersivegeology/gametest/tests/SpawnChunkCapture.java"
        ).read_text(encoding="utf-8")
        self.assertIn('new File("build/gametest-results")', source)
        self.assertNotIn("server.getServerDirectory().toFile()", source)

    def test_legacy_recipe_codec_preserves_semantic_json_for_datagen(self):
        serializer = (
            ROOT
            / "src/main/java/com/igteam/immersivegeology/common/recipe/LegacyIERecipeSerializer.java"
        ).read_text(encoding="utf-8")
        self.assertIn("sourceJson.put(recipe, (JsonObject)json.deepCopy())", serializer)
        self.assertIn("JsonObject original = sourceJson.get(input)", serializer)

    def test_tfc_recipe_builder_uses_neoforge_load_conditions(self):
        builder = (
            ROOT
            / "src/datagen/java/com/igteam/immersivegeology/common/data/helper/TFCCollapseRecipeBuilder.java"
        ).read_text(encoding="utf-8")
        self.assertIn('"neoforge:conditions"', builder)
        self.assertIn('"neoforge:mod_loaded"', builder)
        self.assertNotIn('"forge:conditional"', builder)
        self.assertNotIn('"forge:mod_loaded"', builder)

    def test_generated_material_tags_use_the_neoforge_common_namespace(self):
        tags = (
            ROOT / "src/main/java/com/igteam/immersivegeology/common/tag/IGTags.java"
        ).read_text(encoding="utf-8")
        self.assertNotIn('fromNamespaceAndPath("forge"', tags)
        self.assertIn('fromNamespaceAndPath("c"', tags)

    def test_authored_resources_use_neoforge_condition_and_common_tag_namespaces(self):
        """Catch data files which NeoForge can no longer resolve at reload time."""
        stale_references = []
        data_root = ROOT / "src/main/resources/data"
        for path in data_root.rglob("*.json"):
            data = json.loads(path.read_text(encoding="utf-8"))
            for value in self._json_strings(data):
                if value.startswith("forge:") or value.startswith("#forge:"):
                    stale_references.append(f"{path.relative_to(ROOT)}: {value}")
        self.assertFalse(stale_references, "stale Forge data references:\n" + "\n".join(stale_references))

    def test_obj_models_and_access_transformer_do_not_target_removed_forge_types(self):
        stale_models = []
        for path in (ROOT / "src/main/resources/assets").rglob("*.json"):
            data = json.loads(path.read_text(encoding="utf-8"))
            if data.get("loader") == "forge:obj":
                stale_models.append(str(path.relative_to(ROOT)))
        self.assertFalse(stale_models, "obsolete Forge OBJ loaders:\n" + "\n".join(stale_models))

        access_transformer = (
            ROOT / "src/main/resources/META-INF/accesstransformer.cfg"
        ).read_text(encoding="utf-8")
        self.assertNotIn("net.minecraftforge.", access_transformer)

    def test_optional_fluid_outputs_use_the_codec_that_accepts_empty_stacks(self):
        """Empty secondary/chemical outputs are valid recipes, not invalid fluid stacks."""
        serializers = (
            "common/block/multiblocks/recipe/serializer/BasicChemicalRecipeSerializer.java",
            "common/block/multiblocks/recipe/serializer/ChemicalRecipeSerializer.java",
            "common/block/multiblocks/recipe/serializer/CentrifugeRecipeSerializer.java",
        )
        for relative_path in serializers:
            source = (ROOT / "src/main/java/com/igteam/immersivegeology" / relative_path).read_text(encoding="utf-8")
            with self.subTest(serializer=relative_path):
                self.assertIn("FluidStack.OPTIONAL_CODEC", source)

        recipe_builder = (
            ROOT / "src/main/java/com/igteam/immersivegeology/client/helper/IGRecipeBuilder.java"
        ).read_text(encoding="utf-8")
        self.assertIn("FluidStack.OPTIONAL_CODEC", recipe_builder)

    def test_skin_defaults_resolve_config_by_stable_multiblock_id(self):
        """Display names may differ from the registered skin IDs (for example, Crude Bloomery)."""
        config = (
            ROOT / "src/main/java/com/igteam/immersivegeology/common/config/IGServerConfig.java"
        ).read_text(encoding="utf-8")
        skin_block = (
            ROOT / "src/main/java/com/igteam/immersivegeology/common/block/multiblocks/part/SkinableMultiblockPart.java"
        ).read_text(encoding="utf-8")
        game_test = (
            ROOT / "src/gametest/java/com/igteam/immersivegeology/gametest/tests/CommonTests.java"
        ).read_text(encoding="utf-8")
        self.assertIn("getSkinConfig", config)
        self.assertIn("getSkinConfig", skin_block)
        self.assertIn("assertSkinConfiguration", game_test)

    def test_legacy_recipe_outputs_keep_tag_resolution_lazy(self):
        """Recipe reload must not turn populated tag outputs into ItemStack.EMPTY before tags resolve."""
        serializer = (
            ROOT / "src/main/java/com/igteam/immersivegeology/common/recipe/LegacyIERecipeSerializer.java"
        ).read_text(encoding="utf-8")
        self.assertIn("TagOutput output = TagOutput.CODECS.codec().parse", serializer)
        self.assertIn("return Lazy.of(output::get);", serializer)

    def test_mod_metadata_marks_jei_as_an_optional_dependency(self):
        metadata = tomllib.loads(
            (ROOT / "src/main/resources/META-INF/neoforge.mods.toml").read_text(encoding="utf-8")
        )
        dependencies = metadata["dependencies"]["immersivegeology"]
        self.assertFalse(
            any("mandatory" in dependency for dependency in dependencies),
            "NeoForge 1.21.1 dependency metadata uses type, not mandatory",
        )
        jei = next(dependency for dependency in dependencies if dependency["modId"] == "jei")
        self.assertEqual("optional", jei["type"])

    @staticmethod
    def _json_strings(value):
        if isinstance(value, str):
            yield value
        elif isinstance(value, list):
            for element in value:
                yield from RuntimeValidationWorkflowTest._json_strings(element)
        elif isinstance(value, dict):
            for element in value.values():
                yield from RuntimeValidationWorkflowTest._json_strings(element)

    def test_generated_resources_use_1_21_paths_and_semantic_recipes(self):
        self.assertFalse((GENERATED / "data/forge").exists())
        self.assertFalse((GENERATED / ".cache").exists())
        for namespace in (GENERATED / "data").iterdir():
            if not namespace.is_dir():
                continue
            for obsolete in ("recipes", "advancements", "loot_tables"):
                self.assertFalse((namespace / obsolete).exists(), f"obsolete pack path: {namespace / obsolete}")
            tags = namespace / "tags"
            for obsolete in ("items", "blocks", "fluids"):
                self.assertFalse((tags / obsolete).exists(), f"obsolete tag path: {tags / obsolete}")

        payload_files = []
        for path in (GENERATED / "data/immersivegeology/recipe").rglob("*.json"):
            if "immersivegeology:legacy_network" in path.read_text(encoding="utf-8"):
                payload_files.append(str(path.relative_to(ROOT)))
        self.assertFalse(payload_files, "opaque registry-ID recipe payloads:\n" + "\n".join(payload_files))

    def test_optional_tfc_generated_content_is_preserved_and_conditioned(self):
        collapse_dir = GENERATED / "data/immersivegeology/recipe/collapse"
        collapse = sorted(collapse_dir.glob("*.json"))
        self.assertEqual(1293, len(collapse))
        for path in collapse:
            data = json.loads(path.read_text(encoding="utf-8"))
            self.assertEqual("tfc:collapse", data.get("type"), path.name)
            self.assertEqual(
                [{"type": "neoforge:mod_loaded", "modid": "tfc"}],
                data.get("neoforge:conditions"),
                path.name,
            )

        manifest = ROOT / "scripts/optional_tfc_generated_resources.txt"
        expected = manifest.read_text(encoding="utf-8").splitlines()
        actual = sorted(
            str(path.relative_to(ROOT))
            for path in GENERATED.rglob("*.json")
            if path.is_relative_to(collapse_dir)
            or path.is_relative_to(GENERATED / "data/tfc")
            or path.name.endswith("_tfc.json")
        )
        self.assertEqual(expected, sorted(actual))

    def test_ci_runs_datagen_and_rejects_generated_resource_drift(self):
        workflow = WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("runData", workflow)
        self.assertIn("--diff-filter=ACMRTUXB", workflow)
        self.assertIn("optional_tfc_generated_resources.txt", workflow)
        self.assertIn("generated-resources-after-runData", workflow)

    def test_ci_starts_a_real_gametest_server(self):
        workflow = WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("runGameTestServer", workflow)


if __name__ == "__main__":
    unittest.main()
