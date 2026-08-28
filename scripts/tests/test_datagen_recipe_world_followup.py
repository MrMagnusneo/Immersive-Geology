import pathlib
import unittest


ROOT = pathlib.Path(__file__).resolve().parents[2]
RECIPES = ROOT / "src/datagen/java/com/igteam/immersivegeology/common/data/generators/IGRecipes.java"


class DatagenRecipeFollowupTest(unittest.TestCase):
    def test_recipes_use_121_datagen_apis(self):
        source = RECIPES.read_text(encoding="utf-8")
        removed_api = (
            "blusunrize.immersiveengineering.api.crafting.builders.*",
            "blusunrize.immersiveengineering.data.Recipes.getTagCondition",
            "net.neoforged.neoforge.registries.ForgeRegistries",
            "ThermoelectricSourceBuilder",
            "Tags.Items.STRING)",
            "Tags.Items.COBBLESTONE)",
            ".addOre(",
            ".setFailchance(",
            ".setBackground(",
            "MineralMixBuilder.builder(overworld)",
            "MineralMixBuilder.builder(nether)",
            "MixerRecipeBuilder.builder(ChemicalEnum",
            "CrusherRecipeBuilder.builder(material",
            "builder.addInput(material.getItemTag(ore))",
            "BlastFurnaceFuelBuilder",
        )
        for symbol in removed_api:
            with self.subTest(symbol=symbol):
                self.assertNotIn(symbol, source)

        self.assertIn(
            "blusunrize.immersiveengineering.data.recipes.builder.*", source
        )
        self.assertIn("new ThermoelectricSource(", source)
        self.assertIn("new BlastFurnaceFuel(", source)


if __name__ == "__main__":
    unittest.main()
