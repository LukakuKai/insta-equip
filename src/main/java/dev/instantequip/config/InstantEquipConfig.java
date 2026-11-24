package dev.instantequip.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/** Stores which item categories get the instant (no raise animation) treatment. */
public final class InstantEquipConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<ItemCategory, Boolean> ENABLED = new EnumMap<>(ItemCategory.class);

	static {
		for (ItemCategory category : ItemCategory.values()) {
			ENABLED.put(category, true);
		}
	}

	/** Keep enabled items fully raised at all times (also removes the dip after attacking). */
	private static boolean keepRaised = true;

	private InstantEquipConfig() {
	}

	public static boolean isKeepRaised() {
		return keepRaised;
	}

	public static void toggleKeepRaised() {
		keepRaised = !keepRaised;
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("instantequip.json");
	}

	public static boolean isInstant(ItemStack stack) {
		return isEnabled(ItemCategory.of(stack));
	}

	public static boolean isEnabled(ItemCategory category) {
		return ENABLED.getOrDefault(category, true);
	}

	public static void set(ItemCategory category, boolean value) {
		ENABLED.put(category, value);
	}

	public static void toggle(ItemCategory category) {
		set(category, !isEnabled(category));
	}

	public static void setAll(boolean value) {
		for (ItemCategory category : ItemCategory.values()) {
			ENABLED.put(category, value);
		}
	}

	public static void load() {
		Path file = file();
		if (!Files.exists(file)) {
			save();
			return;
		}

		try {
			String text = Files.readString(file, StandardCharsets.UTF_8);
			JsonElement root = JsonParser.parseString(text);
			if (!root.isJsonObject()) {
				return;
			}
			JsonObject obj = root.getAsJsonObject();
			for (ItemCategory category : ItemCategory.values()) {
				if (obj.has(category.name())) {
					ENABLED.put(category, obj.get(category.name()).getAsBoolean());
				}
			}
			if (obj.has("KEEP_RAISED")) {
				keepRaised = obj.get("KEEP_RAISED").getAsBoolean();
			}
		} catch (Exception e) {
			System.err.println("[instantequip] Could not read config, using defaults: " + e);
		}
	}

	public static void save() {
		JsonObject obj = new JsonObject();
		for (ItemCategory category : ItemCategory.values()) {
			obj.addProperty(category.name(), isEnabled(category));
		}
		obj.addProperty("KEEP_RAISED", keepRaised);

		try {
			Path file = file();
			Files.createDirectories(file.getParent());
			Files.writeString(file, GSON.toJson(obj), StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.err.println("[instantequip] Could not save config: " + e);
		}
	}
}
