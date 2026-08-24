import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / ".github" / "workflows" / "neoforge-1.21.1-port.yml"


class RuntimeValidationWorkflowTest(unittest.TestCase):
    def test_port_does_not_add_classes_to_dependency_module_packages(self):
        forbidden_roots = [
            ROOT / "src" / "main" / "java" / "net" / "minecraft",
            ROOT / "src" / "main" / "java" / "blusunrize",
        ]
        for split_package in forbidden_roots:
            with self.subTest(path=split_package):
                self.assertFalse(
                    split_package.exists(),
                    "compatibility types in dependency namespaces create Java module split-packages",
                )

    def test_fluid_tag_compatibility_type_is_imported_from_mod_namespace(self):
        source_roots = [ROOT / "src" / "main" / "java", ROOT / "src" / "datagen" / "java"]
        compatibility_type = (
            ROOT
            / "src/main/java/com/igteam/immersivegeology/common/compat/ie/crafting/FluidTagInput.java"
        )
        expected_import = (
            "import com.igteam.immersivegeology.common.compat.ie.crafting.FluidTagInput;"
        )
        offenders = []
        for source_root in source_roots:
            for path in source_root.rglob("*.java"):
                if path == compatibility_type:
                    continue
                source = path.read_text(encoding="utf-8")
                if "FluidTagInput" in source and expected_import not in source:
                    offenders.append(str(path.relative_to(ROOT)))
        self.assertFalse(offenders, "missing explicit compatibility import:\n" + "\n".join(offenders))

    def test_event_bus_subscribers_have_listener_methods(self):
        offenders = []
        source_root = ROOT / "src" / "main" / "java"
        for path in source_root.rglob("*.java"):
            source = path.read_text(encoding="utf-8")
            if "@EventBusSubscriber" in source and "@SubscribeEvent" not in source:
                offenders.append(str(path.relative_to(ROOT)))
        self.assertFalse(
            offenders,
            "NeoForge rejects automatic subscribers without @SubscribeEvent methods:\n"
            + "\n".join(offenders),
        )

    def test_paletted_atlas_outputs_are_tracked_for_model_validation(self):
        material = (
            ROOT
            / "src/main/java/com/igteam/immersivegeology/core/material/GeologyMaterial.java"
        ).read_text(encoding="utf-8")
        self.assertIn("PALETTED_TEXTURE", material)
        self.assertIn("trackGenerated(selectedTexture, PALETTED_TEXTURE)", material)

    def test_ci_runs_datagen_and_rejects_generated_resource_drift(self):
        workflow = WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("runData", workflow)
        self.assertIn("git diff --exit-code -- src/generated/resources", workflow)

    def test_ci_starts_a_real_gametest_server(self):
        workflow = WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("runGameTestServer", workflow)


if __name__ == "__main__":
    unittest.main()
