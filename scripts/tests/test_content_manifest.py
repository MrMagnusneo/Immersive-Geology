import tempfile
import unittest
from pathlib import Path

from scripts.content_manifest import ManifestError, collect_manifest, compare_manifests


class ContentManifestTest(unittest.TestCase):
    def test_collects_geology_registration_surfaces(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "src/main/java/com/example/Registration.java"
            source.parent.mkdir(parents=True)
            source.write_text(
                '''
                class Registration {
                    void init() {
                        registerItem("prospector_kit", Item::new);
                        registerBlock("ore_block", Block::new);
                        registerFluid("acid", Fluid::new);
                        registerFluidType("acid_type", Type::new);
                        REGISTER.register("serializer", Serializer::new);
                        registerMultiblock("foundry", logic, structure);
                    }
                }
                ''',
                encoding="utf-8",
            )
            enum_source = root / "src/main/java/com/example/MetalEnum.java"
            enum_source.write_text(
                '''public enum MetalEnum implements MaterialInterface<MaterialMetal> {
                    Iron(new MaterialIron()),
                    StainlessSteel(new MaterialStainlessSteel());
                }''',
                encoding="utf-8",
            )
            resource = root / "src/generated/resources/data/immersivegeology/recipes/test.json"
            resource.parent.mkdir(parents=True)
            resource.write_text('{"type":"immersivegeology:test"}\n', encoding="utf-8")

            manifest = collect_manifest(root)

            self.assertEqual(["ore_block"], manifest["blocks"])
            self.assertEqual(["prospector_kit"], manifest["items"])
            self.assertEqual(["acid"], manifest["fluids"])
            self.assertEqual(["acid_type"], manifest["fluid_types"])
            self.assertEqual(["foundry"], manifest["multiblocks"])
            self.assertEqual(["MetalEnum.Iron", "MetalEnum.StainlessSteel"], manifest["materials"])
            self.assertIn("data/immersivegeology/recipes/test.json", manifest["resource_paths"])

    def test_port_comparison_allows_edits_but_rejects_removed_files_and_ids(self):
        baseline = {
            "materials": ["MetalEnum.Iron", "MetalEnum.Copper"],
            "java_sources": {"src/main/java/A.java": "old", "src/main/java/B.java": "old"},
            "resource_hashes": {"assets/a.json": "old"},
        }
        current = {
            "materials": ["MetalEnum.Iron"],
            "java_sources": {"src/main/java/A.java": "new"},
            "resource_hashes": {"assets/a.json": "new"},
        }

        differences = compare_manifests(baseline, current, allow_content_changes=True)

        self.assertEqual(["MetalEnum.Copper"], differences["materials"]["removed"])
        self.assertEqual(["src/main/java/B.java"], differences["java_sources"]["removed"])
        self.assertNotIn("changed", differences["java_sources"])
        self.assertNotIn("resource_hashes", differences)

    def test_strict_comparison_reports_changed_resources(self):
        differences = compare_manifests(
            {"resource_hashes": {"assets/a.json": "old"}},
            {"resource_hashes": {"assets/a.json": "new"}},
        )
        self.assertEqual(["assets/a.json"], differences["resource_hashes"]["changed"])

    def test_accepts_documented_one_to_one_replacement(self):
        differences = compare_manifests(
            {"resource_paths": ["META-INF/mods.toml"]},
            {"resource_paths": ["META-INF/neoforge.mods.toml"]},
            allow_content_changes=True,
            replacements={"resource_paths": {"META-INF/mods.toml": "META-INF/neoforge.mods.toml"}},
        )
        self.assertEqual({}, differences)

    def test_rejects_duplicate_resources_across_source_roots(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for source_root in ("src/main/resources", "src/generated/resources"):
                resource = root / source_root / "assets/immersivegeology/test.json"
                resource.parent.mkdir(parents=True)
                resource.write_text("{}", encoding="utf-8")

            with self.assertRaisesRegex(ManifestError, "Duplicate resource paths"):
                collect_manifest(root)

    def test_real_manifest_covers_all_primary_surfaces(self):
        root = Path(__file__).resolve().parents[2]
        manifest = collect_manifest(root)

        for category in (
            "materials",
            "recipe_types",
            "recipe_serializers",
            "multiblocks",
            "menus",
            "config_keys",
            "integrations",
            "java_sources",
            "resource_hashes",
        ):
            self.assertTrue(manifest[category], category)


if __name__ == "__main__":
    unittest.main()
