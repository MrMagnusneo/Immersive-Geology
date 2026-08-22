import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


class RegistrationWorldFollowupTest(unittest.TestCase):
    def source(self, relative: str) -> str:
        return (ROOT / relative).read_text(encoding="utf-8")

    def test_deferred_holders_declare_their_registry_supertype(self):
        files = [
            "src/main/java/com/igteam/immersivegeology/core/registration/IGRegistrationHolder.java",
            "src/main/java/com/igteam/immersivegeology/core/registration/IGMenuTypes.java",
            "src/main/java/com/igteam/immersivegeology/core/registration/IGRecipeTypes.java",
            "src/main/java/com/igteam/immersivegeology/core/registration/IGRecipeSerializers.java",
            "src/main/java/com/igteam/immersivegeology/common/world/IGWorldGen.java",
            "src/main/java/com/igteam/immersivegeology/common/world/IGStructureTypes.java",
            "src/main/java/com/igteam/immersivegeology/common/particle/IGParticles.java",
        ]
        offenders = [path for path in files if "DeferredHolder<?," in self.source(path)]
        self.assertEqual([], offenders)

    def test_serialized_world_components_expose_map_codecs(self):
        files = [
            "src/main/java/com/igteam/immersivegeology/common/world/IGHeightProvider.java",
            "src/main/java/com/igteam/immersivegeology/common/world/IGDefaultPlacement.java",
            "src/main/java/com/igteam/immersivegeology/common/world/IGSparsePlacement.java",
            "src/main/java/com/igteam/immersivegeology/common/world/placements/IGCountPlacement.java",
            "src/main/java/com/igteam/immersivegeology/common/world/modifiers/IGOreRemovalModifier.java",
            "src/main/java/com/igteam/immersivegeology/common/loot/IGLootModifier.java",
        ]
        offenders = [path for path in files if re.search(r"public static final Codec<", self.source(path))]
        self.assertEqual([], offenders)

    def test_jigsaw_generation_supplies_1_21_policy_arguments(self):
        for name in (
            "RuinedMiningOutpost",
            "RuinedFactory",
            "LargeRuinedFactory",
            "RuinedElevator",
            "HydroVent",
        ):
            source = self.source(
                f"src/main/java/com/igteam/immersivegeology/common/world/structure/{name}.java"
            )
            self.assertIn("PoolAliasLookup.EMPTY", source, name)
            self.assertIn("DimensionPadding.ZERO", source, name)
            self.assertIn("LiquidSettings.APPLY_WATERLOGGING", source, name)


if __name__ == "__main__":
    unittest.main()
