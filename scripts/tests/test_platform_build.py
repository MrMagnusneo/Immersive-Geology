import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


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
