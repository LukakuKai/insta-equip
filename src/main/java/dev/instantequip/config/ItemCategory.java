package dev.instantequip.config;

import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

/**
 * Item groups that can be switched on/off in the Instant Equip menu.
 * Detection is done via the item id so it also works for new tools/weapons
 * (copper, netherite, spears, ...) without depending on specific item tags.
 */
public enum ItemCategory {
	MACE("Mace"),
	SWORD("Swords"),
	AXE("Axes"),
	PICKAXE("Pickaxes"),
	SHOVEL("Shovels"),
	HOE("Hoes"),
	SPEAR("Spears"),
	TRIDENT("Trident"),
	BOW("Bow"),
	CROSSBOW("Crossbow"),
	SHIELD("Shield"),
	BLOCK("Blocks"),
	EMPTY("Empty hand"),
	OTHER("Other items");

	public final String label;

	ItemCategory(String label) {
		this.label = label;
	}

	public static ItemCategory of(ItemStack stack) {
		if (stack.isEmpty()) {
			return EMPTY;
		}

		String path = Registries.ITEM.getId(stack.getItem()).getPath();

		if (path.equals("mace")) return MACE;
		if (path.equals("sword") || path.endsWith("_sword")) return SWORD;
		if (path.equals("axe") || path.endsWith("_axe")) return AXE;
		if (path.endsWith("pickaxe")) return PICKAXE;
		if (path.endsWith("shovel")) return SHOVEL;
		if (path.endsWith("_hoe")) return HOE;
		if (path.endsWith("_spear") || path.equals("spear")) return SPEAR;
		if (path.equals("trident")) return TRIDENT;
		if (path.equals("bow")) return BOW;
		if (path.equals("crossbow")) return CROSSBOW;
		if (path.equals("shield")) return SHIELD;
		if (stack.getItem() instanceof BlockItem) return BLOCK;

		return OTHER;
	}
}
