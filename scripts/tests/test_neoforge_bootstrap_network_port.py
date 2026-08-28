"""Focused source-level contract checks for the NeoForge 1.21.1 port."""

from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/com/igteam/immersivegeology"


class NeoForgeBootstrapNetworkPortTest(unittest.TestCase):
    def read(self, relative: str) -> str:
        return (JAVA / relative).read_text()

    def test_bootstrap_uses_injected_loading_context(self):
        source = self.read("ImmersiveGeology.java")
        self.assertIn("ImmersiveGeology(ModContainer container, IEventBus modEventBus)", source)
        self.assertNotIn("FMLJavaModLoadingContext", source)
        self.assertIn("container.registerConfig", source)

    def test_deferred_registries_accept_the_mod_bus(self):
        for relative in ("common/world/IGWorldGen.java", "core/registration/IGRecipeTypes.java"):
            source = self.read(relative)
            self.assertIn("init(IEventBus", source)
            self.assertNotIn("FMLJavaModLoadingContext", source)

    def test_network_uses_payload_registration_and_distribution(self):
        handler = self.read("common/network/IGPacketHandler.java")
        message = self.read("common/network/msg/MessageSCRFail.java")
        self.assertIn("RegisterPayloadHandlersEvent", handler)
        self.assertIn("CustomPacketPayload", handler)
        self.assertIn("PacketDistributor.sendToServer", handler)
        self.assertNotIn("SimpleChannel", handler)
        self.assertIn("StreamCodec", message)
        self.assertIn("IPayloadContext", message)


if __name__ == "__main__":
    unittest.main()
