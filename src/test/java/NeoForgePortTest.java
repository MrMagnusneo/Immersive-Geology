import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

	@Test
	void bloomeryManualUsesTheMinecraft121ItemStackSchema() throws Exception
	{
		try(var stream = getClass().getClassLoader().getResourceAsStream(
				"assets/immersivegeology/manual/bloomery.json"
		))
		{
			assertNotNull(stream, "Bloomery manual entry must be packaged");
			JsonObject root = JsonParser.parseReader(
					new InputStreamReader(stream, StandardCharsets.UTF_8)
			).getAsJsonObject();
			JsonArray items = root.getAsJsonObject("bloomery_metals").getAsJsonArray("items");

			assertFalse(items.isEmpty(), "Bloomery metal display must contain items");
			for(var item : items)
			{
				JsonObject stack = item.getAsJsonObject();
				assertTrue(stack.has("id"), "Minecraft 1.21 ItemStack objects require an id");
				assertFalse(stack.has("item"), "The pre-1.21 item field is not a valid ItemStack id");
			}
		}
	}
}
