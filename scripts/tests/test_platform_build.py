import unittest
import subprocess
import sys
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


class RuntimeLogGateTest(unittest.TestCase):
    def run_gate(self, log, kind="gametest"):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "runtime.log"
            path.write_text(log, encoding="utf-8")
            return subprocess.run(
                [sys.executable, str(ROOT / "scripts/validate_runtime_log.py"),
                 "--kind", kind, str(path)],
                capture_output=True, text=True, check=False,
            )

    def test_accepts_completed_server_tests_and_datagen(self):
        for kind, log in (
            ("gametest", "Started game test server\nAll 18 required tests passed :)\nBUILD SUCCESSFUL\n"),
            ("datagen", "[main/WARN] TFC is NOT loaded\nBUILD SUCCESSFUL\n"),
        ):
            with self.subTest(kind=kind):
                result = self.run_gate(log, kind)
                self.assertEqual(0, result.returncode, result.stderr)

    def test_rejects_errors_even_when_gradle_and_gametests_succeed(self):
        success = "Started game test server\nAll 18 required tests passed :)\nBUILD SUCCESSFUL\n"
        for failure in (
            "[main/ERROR] [minecraft/RecipeManager]: Parsing error loading recipe test:ore",
            '[Server thread/ERROR] [IG]: Cannot read field "default_skin_ordinal" because "config" is null',
            "[main/WARN] Not all defined tags for registry biome are present in data pack: forge:is_swamp",
            "[main/WARN] Failed to build Recipe Method [immersivegeology:centrifuge]",
            "[main/INFO] Failed Recipe for cloudy_brine: Fluid must not be minecraft:empty",
        ):
            with self.subTest(failure=failure):
                result = self.run_gate(failure + "\n" + success)
                self.assertEqual(1, result.returncode, result.stderr)
                self.assertIn(failure, result.stderr)

    def test_rejects_empty_truncated_or_zero_test_logs(self):
        for log in ("", "BUILD SUCCESSFUL\n", "Started game test server\nAll 0 required tests passed :)\nBUILD SUCCESSFUL\n",
                    "Started game test server\nAll 18 required tests passed :)\n"):
            with self.subTest(log=log):
                result = self.run_gate(log)
                self.assertEqual(1, result.returncode, result.stderr)


class PlatformBuildContractTest(unittest.TestCase):
    def test_uses_moddevgradle_and_java_21(self):
        build = (ROOT / "build.gradle").read_text(encoding="utf-8")
        self.assertIn("net.neoforged.moddev", build)
        self.assertIn("neoForge {", build)
        self.assertIn("JavaLanguageVersion.of(21)", build)
        self.assertNotIn("net.minecraftforge.gradle", build)

    def test_targets_exact_1_21_1_platform(self):
        properties = (ROOT / "version.properties").read_text(encoding="utf-8")
        self.assertIn("version_mc=1.21.1", properties)
        self.assertIn("version_neoforge=21.1.234", properties)
        self.assertIn("version_ie=12.4.2-194", properties)

    def test_uses_neoforge_metadata_only(self):
        metadata = ROOT / "src/main/resources/META-INF/neoforge.mods.toml"
        self.assertTrue(metadata.is_file())
        self.assertFalse((ROOT / "src/main/resources/META-INF/mods.toml").exists())
        text = metadata.read_text(encoding="utf-8")
        self.assertIn('modId="neoforge"', text)
        self.assertIn('versionRange="[${version_neoforge_min},)"', text)

    def test_configuration_does_not_mutate_version_properties(self):
        settings = (ROOT / "settings.gradle").read_text(encoding="utf-8")
        build = (ROOT / "build.gradle").read_text(encoding="utf-8")
        self.assertNotIn("FileOutputStream", settings)
        self.assertNotIn("taskGraph.whenReady", build)
        self.assertIn("tasks.register('bumpVersion')", build)

    def test_wrapper_and_metadata_match_java_21_toolchain(self):
        wrapper = (ROOT / "gradle/wrapper/gradle-wrapper.properties").read_text(encoding="utf-8")
        self.assertIn("gradle-9.3.0-bin.zip", wrapper)


if __name__ == "__main__":
    unittest.main()
