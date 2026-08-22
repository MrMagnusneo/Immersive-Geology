from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]
DATAGEN = ROOT / "src/datagen/java"


class DatagenGametestFollowupTest(unittest.TestCase):
    def test_datagen_uses_121_deferred_holders_and_nbt_limits(self):
        sources = "\n".join(path.read_text() for path in DATAGEN.rglob("*.java"))
        self.assertNotIn("RegistryObject", sources)
        self.assertNotIn("NbtIo.readCompressed(input);", sources)
        self.assertIn("NbtAccounter.unlimitedHeap()", sources)

    def test_recipe_and_loot_providers_receive_registry_lookup(self):
        data_provider = (DATAGEN / "com/igteam/immersivegeology/common/data/IGDataProvider.java").read_text()
        recipes = (DATAGEN / "com/igteam/immersivegeology/common/data/generators/IGRecipes.java").read_text()
        loot = (DATAGEN / "com/igteam/immersivegeology/common/data/generators/loot/IGLootProvider.java").read_text()
        self.assertIn("new IGRecipes(out, lookup)", data_provider)
        self.assertIn("new IGLootProvider(out, lookup)", data_provider)
        self.assertIn("buildRecipes(@NotNull RecipeOutput", recipes)
        self.assertIn("super(output, registries)", recipes)
        self.assertIn("super(output, Set.of(), List.of(), registries)", loot)

    def test_loot_tables_use_resource_keys_and_registry_enchantments(self):
        loot_dir = DATAGEN / "com/igteam/immersivegeology/common/data/generators/loot"
        sources = "\n".join(path.read_text() for path in loot_dir.rglob("*.java"))
        self.assertNotIn("BiConsumer<ResourceLocation,", sources)
        self.assertIn("BiConsumer<ResourceKey<LootTable>,", sources)
        self.assertNotIn("new EnchantmentPredicate(Enchantments.", sources)
        self.assertNotIn("ApplyBonusCount.addOreBonusCount(Enchantments.", sources)


if __name__ == "__main__":
    unittest.main()
