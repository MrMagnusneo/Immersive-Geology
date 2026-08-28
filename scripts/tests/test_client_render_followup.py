"""Compile-surface regression checks for the NeoForge 1.21.1 client port."""

from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]
CLIENT = ROOT / "src/main/java/com/igteam/immersivegeology/client"
INTEGRATION = ROOT / "src/main/java/com/igteam/immersivegeology/common/integration"


class ClientRenderFollowupTest(unittest.TestCase):
    def test_renderers_use_121_vertex_consumer_contract(self):
        sources = [
            CLIENT / "helper/IGFluidRenderHelper.java",
            CLIENT / "renderer/multiblocks/ChemicalReactorRenderer.java",
        ]
        combined = "\n".join(path.read_text(encoding="utf-8") for path in sources)
        self.assertIn("addVertex(", combined)
        self.assertIn("setColor(", combined)
        self.assertIn("setUv(", combined)
        self.assertNotIn(".vertex(", combined)
        self.assertNotIn(".endVertex()", combined)

    def test_fluid_screens_keep_region_based_overlays(self):
        helper = CLIENT / "menu/IGFluidInfoArea.java"
        self.assertTrue(helper.is_file())
        source = helper.read_text(encoding="utf-8")
        self.assertIn("overlayUMin", source)
        self.assertIn("overlayVMin", source)
        self.assertIn("GuiHelper.drawRepeatedFluidSpriteGui", source)

    def test_jei_uses_recipe_values_and_rich_fluid_tooltips(self):
        integration_source = "\n".join(
            path.read_text(encoding="utf-8") for path in INTEGRATION.glob("*.java")
        )
        self.assertIn(".map(RecipeHolder::value)", integration_source)
        self.assertNotIn("addTooltipCallback(JEIHelper.fluidTooltipCallback)", integration_source)
        self.assertIn("addRichTooltipCallback(JEIHelper.fluidTooltipCallback)", integration_source)

    def test_creative_menu_avoids_removed_or_private_apis(self):
        group = (CLIENT / "menu/IGItemGroup.java").read_text(encoding="utf-8")
        handler = (CLIENT / "menu/CreativeMenuHandler.java").read_text(encoding="utf-8")
        self.assertIn("createTypeAndComponentsSet", group)
        self.assertNotIn("createTypeAndTagSet", group)
        self.assertNotIn("CreativeModeInventoryScreen.selectedTab", handler)


if __name__ == "__main__":
    unittest.main()
