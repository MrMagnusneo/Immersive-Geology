import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "src/datagen/java/com/igteam/immersivegeology/common/data/generators/IGWorldGenerationProvider.java"


class DatagenWorldFollowupTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = SOURCE.read_text(encoding="utf-8")

    def test_provider_uses_1_21_bootstrap_and_neoforge_registry_api(self):
        self.assertNotIn("BootstapContext", self.source)
        self.assertNotIn("common.world.ForgeBiomeModifiers", self.source)
        self.assertNotIn("registries.ForgeRegistries.Keys", self.source)
        self.assertGreaterEqual(self.source.count("BootstrapContext<"), 5)
        self.assertIn("NeoForgeBiomeModifiers.AddFeaturesBiomeModifier", self.source)
        self.assertIn("NeoForgeRegistries.Keys.BIOME_MODIFIERS", self.source)

    def test_ore_and_evaporite_generation_paths_remain_registered(self):
        self.assertEqual(2, len(re.findall(r"registerConfigured\(ctx, new ConfiguredFeature<>", self.source)))
        self.assertEqual(2, self.source.count("registerPlaced(ctx, placements)"))
        self.assertEqual(2, self.source.count("new AddFeaturesBiomeModifier("))
        self.assertIn("Decoration.UNDERGROUND_ORES", self.source)
        self.assertIn("Decoration.SURFACE_STRUCTURES", self.source)


if __name__ == "__main__":
    unittest.main()
