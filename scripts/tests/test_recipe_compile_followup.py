"""Offline source contracts for the NeoForge 1.21 recipe compile migration."""

from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]
RECIPE_ROOT = ROOT / "src/main/java/com/igteam/immersivegeology/common/block/multiblocks/recipe"
COMPAT_ROOT = ROOT / "src/main/java/com/igteam/immersivegeology/common/compat/ie/crafting"


class RecipeCompileFollowupTest(unittest.TestCase):
    def recipe_sources(self):
        yield from RECIPE_ROOT.rglob("*.java")
        yield from COMPAT_ROOT.rglob("*.java")
        yield ROOT / "src/main/java/com/igteam/immersivegeology/common/recipe/IGGeoRecipe.java"

    def test_recipe_constructors_and_caches_use_121_api(self):
        offenders = []
        for path in self.recipe_sources():
            source = path.read_text(encoding="utf-8")
            for removed in (
                "super(LAZY_EMPTY",
                "for(BallmillRecipe recipe : RECIPES.getRecipes",
                "for(CentrifugeRecipe recipe : RECIPES.getRecipes",
                "for(CrystallizerRecipe recipe : RECIPES.getRecipes",
                "for(FoundryRecipe recipe : RECIPES.getRecipes",
                "for(PelletizerRecipe recipe : RECIPES.getRecipes",
            ):
                if removed in source:
                    offenders.append(f"{path.relative_to(ROOT)}: {removed}")
        self.assertFalse(offenders, "\n".join(offenders))

    def test_serializers_do_not_call_removed_120_helpers(self):
        offenders = []
        for path in (RECIPE_ROOT / "serializer").glob("*.java"):
            source = path.read_text(encoding="utf-8")
            for removed in (
                "IngredientWithSize.deserialize(",
                "IngredientWithSize.read(",
                ".itemIn.write(buffer)",
                ".input.write(buffer)",
                "ApiUtils.jsonDeserializeFluidStack(",
                "buffer.readFluidStack()",
                "buffer.writeFluidStack(",
                "StackWithChance.read(",
                "StackWithChance.write(",
            ):
                if removed in source:
                    offenders.append(f"{path.relative_to(ROOT)}: {removed}")
        self.assertFalse(offenders, "\n".join(offenders))


if __name__ == "__main__":
    unittest.main()
