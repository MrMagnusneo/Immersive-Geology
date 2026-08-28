import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoForgePortTest
{
	@Test
	void neoForgeMetadataIsPackagedForTheJava21Runtime()
	{
		assertTrue(Runtime.version().feature() >= 21, "Minecraft 1.21.1 requires Java 21");
		assertNotNull(getClass().getClassLoader().getResource("META-INF/neoforge.mods.toml"));
	}
}
