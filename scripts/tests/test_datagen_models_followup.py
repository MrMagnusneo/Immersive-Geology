from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]
GEN = ROOT / "src/datagen/java/com/igteam/immersivegeology/common/data/generators"
OWNED = (
    GEN / "IGBlockStateProvider.java",
    GEN / "IGComplexItemModelProvider.java",
    GEN / "IGItemModelProvider.java",
)


class DatagenModelsFollowupTest(unittest.TestCase):
    def test_models_use_public_texture_api(self):
        for path in OWNED:
            with self.subTest(path=path.name):
                self.assertNotIn(".textures.put(", path.read_text())

    def test_models_use_builtin_registries(self):
        sources = "\n".join(path.read_text() for path in OWNED)
        self.assertNotIn("ForgeRegistries", sources)
        self.assertIn("BuiltInRegistries.BLOCK.getKey(block)", sources)
        self.assertIn("BuiltInRegistries.ITEM.getKey(item.asItem())", sources)


if __name__ == "__main__":
    unittest.main()
