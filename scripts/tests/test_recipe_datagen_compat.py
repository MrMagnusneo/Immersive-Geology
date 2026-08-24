"""Focused contracts for the retained IE 1.20 recipe/datagen compatibility API."""

from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]
BUILDERS = ROOT / "src/main/java/com/igteam/immersivegeology/common/compat/ie/crafting/builders"
IG_BUILDER = ROOT / "src/main/java/com/igteam/immersivegeology/client/helper/IGRecipeBuilder.java"
LEGACY_SERIALIZER = ROOT / "src/main/java/com/igteam/immersivegeology/common/recipe/LegacyIERecipeSerializer.java"


class RecipeDatagenCompatibilityTest(unittest.TestCase):
    def test_all_legacy_ie_builders_are_present(self):
        expected = {
            "ArcFurnaceRecipeBuilder.java",
            "BlastFurnaceFuelBuilder.java",
            "BlastFurnaceRecipeBuilder.java",
            "CrusherRecipeBuilder.java",
            "MixerRecipeBuilder.java",
            "RefineryRecipeBuilder.java",
            "SqueezerRecipeBuilder.java",
        }
        present = {path.name for path in BUILDERS.glob("*RecipeBuilder.java")}
        present.add("BlastFurnaceFuelBuilder.java") if (BUILDERS / "BlastFurnaceFuelBuilder.java").is_file() else None
        self.assertTrue(expected <= present, expected - present)

    def test_builder_schema_keeps_ie_recipe_fields(self):
        contracts = {
            "ArcFurnaceRecipeBuilder.java": ('"additives"', '"secondaries"', '"slag"', "addResult"),
            "BlastFurnaceRecipeBuilder.java": ('"slag"', "addResult", "addSlag"),
            "BlastFurnaceFuelBuilder.java": ("addInput", "maxResultCount = 0"),
            "CrusherRecipeBuilder.java": ('"secondaries"', "addResult", "addSecondary"),
            "MixerRecipeBuilder.java": ('"result"', "setUseInputArray(6)"),
            "RefineryRecipeBuilder.java": ('"result"', '"catalyst"', "maxInputCount = 2"),
            "SqueezerRecipeBuilder.java": ("addFluid", "builder()"),
        }
        for filename, fields in contracts.items():
            source = (BUILDERS / filename).read_text(encoding="utf-8")
            with self.subTest(filename=filename):
                for contract in fields:
                    self.assertIn(contract, source)

    def test_shared_builder_uses_121_codecs_not_removed_json_apis(self):
        source = IG_BUILDER.read_text(encoding="utf-8")
        for required in ("Ingredient.CODEC", "ItemStack.CODEC", "FluidStack.CODEC", "ICondition.CODEC"):
            self.assertIn(required, source)
        for removed in (
            "CraftingHelper.serialize",
            "ingredient.toJson()",
            "stack.hasTag()",
            "stack.getTag()",
            "ApiUtils.jsonSerializeFluidStack",
        ):
            self.assertNotIn(removed, source)

    def test_legacy_recipe_codec_encodes_and_decodes_json_and_network(self):
        source = LEGACY_SERIALIZER.read_text(encoding="utf-8")
        self.assertIn("toNetwork(buffer, recipe)", source)
        self.assertIn("fromNetwork(LEGACY_ID, buffer)", source)
        self.assertIn("prefix.add", source)
        self.assertIn("JsonOps.INSTANCE.convertTo(ops, entry.getValue())", source)
        self.assertIn("DataResult.error", source)


if __name__ == "__main__":
    unittest.main()
