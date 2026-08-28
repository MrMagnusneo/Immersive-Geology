import unittest
import subprocess
import sys
import tempfile
import zipfile
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
            "[ERROR] dependency failed to initialize",
            "[FATAL] unrecoverable runtime failure",
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


class GameplayJarGateTest(unittest.TestCase):
    entrypoint = "com/igteam/immersivegeology/ImmersiveGeology.class"
    metadata = '''modLoader="javafml"
loaderVersion="[1,)"
license="LGPL-3.0"

[[mods]]
modId="immersivegeology"
version="1.0.0"
displayName="Immersive Geology"
'''

    def write_source_roots(self, directory):
        main = directory / "main"
        generated = directory / "generated"
        files = {
            main / "pack.mcmeta": '{"pack":{"pack_format":15,"description":"test"}}',
            main / "META-INF/neoforge.mods.toml": self.metadata,
            main / "assets/immersivegeology/lang/en_us.json": '{"item.immersivegeology.test":"Test"}',
            generated / "data/immersivegeology/loot_tables/test.json": '{"type":"minecraft:empty"}',
            generated / ".cache/ignored.json": "not packaged",
        }
        for path, content in files.items():
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding="utf-8")
        return main, generated, files

    def write_jar(self, path, files):
        with zipfile.ZipFile(path, "w") as archive:
            for name, content in files.items():
                archive.writestr(name, content)

    def valid_jar_files(self):
        return {
            "pack.mcmeta": '{"pack":{"pack_format":15,"description":"test"}}',
            "META-INF/neoforge.mods.toml": self.metadata,
            "assets/immersivegeology/lang/en_us.json": '{"item.immersivegeology.test":"Test"}',
            "data/immersivegeology/loot_tables/test.json": '{"type":"minecraft:empty"}',
            self.entrypoint: b"\xca\xfe\xba\xbe\x00\x00\x00\x41",
        }

    def run_gate(self, jar, main, generated):
        return subprocess.run(
            [sys.executable, str(ROOT / "scripts/validate_gameplay_jar.py"), str(jar),
             "--source-root", str(main), "--source-root", str(generated)],
            capture_output=True, text=True, check=False,
        )

    def test_accepts_gameplay_jar_with_all_source_resources(self):
        with tempfile.TemporaryDirectory() as directory:
            directory = Path(directory)
            main, generated, _ = self.write_source_roots(directory)
            jar = directory / "ImmersiveGeology-1.0.0.jar"
            self.write_jar(jar, self.valid_jar_files())
            result = self.run_gate(jar, main, generated)
            self.assertEqual(0, result.returncode, result.stderr)

    def test_rejects_missing_generated_resource(self):
        with tempfile.TemporaryDirectory() as directory:
            directory = Path(directory)
            main, generated, _ = self.write_source_roots(directory)
            files = self.valid_jar_files()
            del files["data/immersivegeology/loot_tables/test.json"]
            jar = directory / "ImmersiveGeology-1.0.0.jar"
            self.write_jar(jar, files)
            result = self.run_gate(jar, main, generated)
            self.assertEqual(1, result.returncode)
            self.assertIn("data/immersivegeology/loot_tables/test.json", result.stderr)

    def test_rejects_invalid_packaged_json(self):
        with tempfile.TemporaryDirectory() as directory:
            directory = Path(directory)
            main, generated, _ = self.write_source_roots(directory)
            files = self.valid_jar_files()
            files["assets/immersivegeology/lang/en_us.json"] = "{invalid"
            jar = directory / "ImmersiveGeology-1.0.0.jar"
            self.write_jar(jar, files)
            result = self.run_gate(jar, main, generated)
            self.assertEqual(1, result.returncode)
            self.assertIn("invalid JSON", result.stderr)

    def test_rejects_wrong_or_unexpanded_neoforge_metadata(self):
        for metadata, expected in (
            (self.metadata.replace('modId="immersivegeology"', 'modId="wrong"'), "immersivegeology"),
            (self.metadata.replace('version="1.0.0"', 'version="${version}"'), "unexpanded"),
        ):
            with self.subTest(expected=expected):
                with tempfile.TemporaryDirectory() as directory:
                    directory = Path(directory)
                    main, generated, _ = self.write_source_roots(directory)
                    files = self.valid_jar_files()
                    files["META-INF/neoforge.mods.toml"] = metadata
                    jar = directory / "ImmersiveGeology-1.0.0.jar"
                    self.write_jar(jar, files)
                    result = self.run_gate(jar, main, generated)
                    self.assertEqual(1, result.returncode)
                    self.assertIn(expected, result.stderr)

    def test_rejects_invalid_or_non_java_21_entrypoint(self):
        for bytecode, expected in (
            (b"not a class", "invalid class header"),
            (b"\xca\xfe\xba\xbe\x00\x00\x00\x3d", "Java 21"),
        ):
            with self.subTest(expected=expected):
                with tempfile.TemporaryDirectory() as directory:
                    directory = Path(directory)
                    main, generated, _ = self.write_source_roots(directory)
                    files = self.valid_jar_files()
                    files[self.entrypoint] = bytecode
                    jar = directory / "ImmersiveGeology-1.0.0.jar"
                    self.write_jar(jar, files)
                    result = self.run_gate(jar, main, generated)
                    self.assertEqual(1, result.returncode)
                    self.assertIn(expected, result.stderr)

    def test_rejects_forge_metadata_and_non_gameplay_jar_names(self):
        for jar_name, extra_file, expected in (
            ("ImmersiveGeology-1.0.0.jar", "META-INF/mods.toml", "Forge mods.toml"),
            ("ImmersiveGeology-1.0.0-sources.jar", None, "sources"),
            ("ImmersiveGeology-1.0.0-datagen.jar", None, "datagen"),
        ):
            with self.subTest(jar_name=jar_name):
                with tempfile.TemporaryDirectory() as directory:
                    directory = Path(directory)
                    main, generated, _ = self.write_source_roots(directory)
                    files = self.valid_jar_files()
                    if extra_file:
                        files[extra_file] = "modLoader=\"javafml\""
                    jar = directory / jar_name
                    self.write_jar(jar, files)
                    result = self.run_gate(jar, main, generated)
                    self.assertEqual(1, result.returncode)
                    self.assertIn(expected, result.stderr)


if __name__ == "__main__":
    unittest.main()
