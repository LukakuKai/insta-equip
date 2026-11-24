package dev.instantequip.gui;

import dev.instantequip.config.InstantEquipConfig;
import dev.instantequip.config.ItemCategory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Lists the items of a group (other items / blocks / all) with their icon, so each one can be
 * switched on/off explicitly. An explicit choice wins over the category setting of the main menu.
 */
public class ItemListScreen extends Screen {

	private static final int ITEM_W = 140;
	private static final int BTN_H = 20;
	private static final int GAP = 6;

	private enum Filter {
		OTHER("Other items"),
		BLOCKS("Blocks"),
		ALL("All items");

		final String label;

		Filter(String label) {
			this.label = label;
		}

		Filter next() {
			return values()[(this.ordinal() + 1) % values().length];
		}
	}

	private record Entry(Item item, ItemStack stack, String name, String idPath, ItemCategory category) {
	}

	private static List<Entry> allEntries;

	private static List<Entry> allEntries() {
		if (allEntries == null) {
			List<Entry> list = new ArrayList<>();
			for (Item item : Registries.ITEM) {
				if (item == Items.AIR) {
					continue;
				}
				ItemStack stack = new ItemStack(item);
				list.add(new Entry(item, stack, stack.getName().getString(),
					Registries.ITEM.getId(item).getPath(), ItemCategory.of(item)));
			}
			list.sort(Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER));
			allEntries = list;
		}
		return allEntries;
	}

	private final Screen parent;
	private Filter filter = Filter.OTHER;
	private String search = "";
	private int page = 0;
	private int pageCount = 1;

	private int cols;
	private int rows;
	private int gridX;
	private int gridTop;

	private List<Entry> filtered = new ArrayList<>();
	private final List<Entry> shown = new ArrayList<>();
	private final List<ButtonWidget> itemButtons = new ArrayList<>();

	private ButtonWidget prevButton;
	private ButtonWidget nextButton;

	public ItemListScreen(Screen parent) {
		super(Text.literal("Instant Equip - Items"));
		this.parent = parent;
	}

	private Text filterLabel() {
		return Text.literal("Show: " + this.filter.label);
	}

	private static Text itemLabel(Entry entry) {
		boolean on = InstantEquipConfig.isInstantItem(entry.item());
		String name = entry.name();
		if (name.length() > 11) {
			name = name.substring(0, 10) + "..";
		}

		MutableText text = Text.literal(name + ": ");
		if (InstantEquipConfig.hasOverride(entry.item())) {
			text.formatted(Formatting.YELLOW);
		}
		return text.append(Text.literal(on ? "ON" : "OFF").formatted(on ? Formatting.GREEN : Formatting.RED));
	}

	@Override
	protected void init() {
		this.itemButtons.clear();
		this.shown.clear();

		this.cols = Math.max(1, Math.min(3, (this.width - 20) / (ITEM_W + GAP)));
		this.gridTop = 52;
		this.rows = Math.max(1, (this.height - this.gridTop - 56) / (BTN_H + GAP));

		int gridW = this.cols * ITEM_W + (this.cols - 1) * GAP;
		this.gridX = (this.width - gridW) / 2;

		int filterW = 120;
		int searchW = Math.max(60, gridW - filterW - GAP);

		TextFieldWidget searchField = new TextFieldWidget(this.textRenderer, this.gridX, 26, searchW, 18,
			Text.literal("Search"));
		searchField.setPlaceholder(Text.literal("Search items..."));
		searchField.setMaxLength(50);
		searchField.setText(this.search);
		searchField.setChangedListener(text -> {
			this.search = text;
			this.page = 0;
			this.refresh();
		});
		this.addDrawableChild(searchField);

		this.addDrawableChild(ButtonWidget.builder(this.filterLabel(), b -> {
			this.filter = this.filter.next();
			this.page = 0;
			b.setMessage(this.filterLabel());
			this.refresh();
		}).dimensions(this.gridX + searchW + GAP, 25, filterW, 20).build());

		int navY = this.height - 50;
		this.prevButton = ButtonWidget.builder(Text.literal("<"), b -> {
			this.page--;
			this.refresh();
		}).dimensions(this.gridX, navY, 40, 20).build();
		this.nextButton = ButtonWidget.builder(Text.literal(">"), b -> {
			this.page++;
			this.refresh();
		}).dimensions(this.gridX + gridW - 40, navY, 40, 20).build();
		this.addDrawableChild(this.prevButton);
		this.addDrawableChild(this.nextButton);

		int bottomY = this.height - 26;
		int halfW = (gridW - GAP) / 2;
		this.addDrawableChild(ButtonWidget.builder(Text.literal("Reset item choices"), b -> {
			InstantEquipConfig.clearOverrides();
			this.refresh();
		}).dimensions(this.gridX, bottomY, halfW, BTN_H).build());
		this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> this.close())
			.dimensions(this.gridX + halfW + GAP, bottomY, halfW, BTN_H).build());

		this.refresh();
	}

	private void refresh() {
		String query = this.search.trim().toLowerCase(Locale.ROOT);

		List<Entry> result = new ArrayList<>();
		for (Entry entry : allEntries()) {
			boolean inFilter = switch (this.filter) {
				case OTHER -> entry.category() == ItemCategory.OTHER;
				case BLOCKS -> entry.category() == ItemCategory.BLOCK;
				case ALL -> true;
			};
			if (!inFilter) {
				continue;
			}
			if (!query.isEmpty()
				&& !entry.name().toLowerCase(Locale.ROOT).contains(query)
				&& !entry.idPath().contains(query)) {
				continue;
			}
			result.add(entry);
		}
		this.filtered = result;

		int perPage = this.cols * this.rows;
		this.pageCount = Math.max(1, (this.filtered.size() + perPage - 1) / perPage);
		this.page = Math.max(0, Math.min(this.page, this.pageCount - 1));

		for (ButtonWidget button : this.itemButtons) {
			this.remove(button);
		}
		this.itemButtons.clear();
		this.shown.clear();

		int start = this.page * perPage;
		int end = Math.min(this.filtered.size(), start + perPage);
		for (int i = start; i < end; i++) {
			int index = i - start;
			int x = this.gridX + (index % this.cols) * (ITEM_W + GAP);
			int y = this.gridTop + (index / this.cols) * (BTN_H + GAP);
			Entry entry = this.filtered.get(i);

			ButtonWidget button = ButtonWidget.builder(itemLabel(entry), btn -> {
				InstantEquipConfig.toggleItem(entry.item());
				btn.setMessage(itemLabel(entry));
			}).dimensions(x, y, ITEM_W, BTN_H).build();

			this.addDrawableChild(button);
			this.itemButtons.add(button);
			this.shown.add(entry);
		}

		this.prevButton.active = this.page > 0;
		this.nextButton.active = this.page < this.pageCount - 1;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFFFF);

		for (int i = 0; i < this.shown.size(); i++) {
			ButtonWidget button = this.itemButtons.get(i);
			context.drawItem(this.shown.get(i).stack(), button.getX() + 2, button.getY() + 2);
		}

		String info = "Page " + (this.page + 1) + "/" + this.pageCount
			+ "  -  " + this.filtered.size() + " items"
			+ "  -  " + InstantEquipConfig.overrideCount() + " set explicitly (yellow)";
		context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(info), this.width / 2,
			this.height - 44, 0xFFAAAAAA);
	}

	@Override
	public void close() {
		InstantEquipConfig.save();
		this.client.setScreen(this.parent);
	}
}
