import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/com/igteam/immersivegeology"


class NeoForgeApiMigrationTest(unittest.TestCase):
    def read(self, relative):
        return (JAVA / relative).read_text(encoding="utf-8")

    def test_geothermal_biomes_use_dynamic_world_registry(self):
        recipe = self.read("common/block/multiblocks/recipe/GeothermalBiomeRecipe.java")
        serializer = self.read(
            "common/block/multiblocks/recipe/serializer/GeothermalBiomeRecipeSerializer.java"
        )
        self.assertIn("registryOrThrow(Registries.BIOME)", recipe)
        self.assertIn("holder.tags()", recipe)
        self.assertIn("buffer.writeResourceLocation(recipe.biomes.leftNonnull())", serializer)
        self.assertNotIn("ForgeRegistries", recipe + serializer)

    def test_crates_and_drill_heads_preserve_data_components(self):
        for relative in (
            "common/block/entity/crate/IGCrateEntity.java",
            "common/block/IGBlockContainerItem.java",
            "common/item/IGGenericDrillHead.java",
            "common/item/IGMBFormationItem.java",
        ):
            source = self.read(relative)
            self.assertIn("DataComponents.CUSTOM_DATA", source, relative)
            self.assertNotIn("getOrCreateTag()", source, relative)
            self.assertNotIn(".hasTag()", source, relative)

    def test_ingredient_item_and_fluid_serializers_use_stream_codecs(self):
        serializers = (
            "GravitySeparatorRecipeSerializer.java",
            "ChemicalRepairSerializer.java",
            "IndustrialSluiceRecipeSerializer.java",
            "BloomeryFuelSerializer.java",
            "ChemicalRecipeSerializer.java",
            "BasicChemicalRecipeSerializer.java",
        )
        for name in serializers:
            source = self.read("common/block/multiblocks/recipe/serializer/" + name)
            self.assertIn("STREAM_CODEC", source, name)
            for obsolete in (
                "Ingredient.fromNetwork(",
                "Ingredient.fromJson(",
                ".readItem()",
                "FluidStack.readFromPacket(",
            ):
                self.assertNotIn(obsolete, source, name)

    def test_multiblock_removal_preserves_reactor_failure_hook(self):
        part = self.read("common/block/multiblocks/part/SkinableMultiblockPart.java")
        self.assertIn("IRemovalAwareMultiblockState", part)
        self.assertIn("removalAware.onMultiblockPartRemoved(helper.getContext())", part)
        self.assertIn("level.invalidateCapabilities(pos)", part)
        self.assertLess(
            part.index("removalAware.onMultiblockPartRemoved(helper.getContext())"),
            part.index("level.invalidateCapabilities(pos)"),
        )

    def test_vanilla_recipe_contract_uses_crafting_input(self):
        repair = self.read("common/recipe/IGRepairItemRecipe.java")
        self.assertIn("CraftingInput inv", repair)
        self.assertIn("HolderLookup.Provider access", repair)
        self.assertNotIn("CraftingContainer", repair)


if __name__ == "__main__":
    unittest.main()
