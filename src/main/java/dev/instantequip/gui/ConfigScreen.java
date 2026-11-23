package dev.instantequip.gui;

import dev.instantequip.config.InstantEquipConfig;
import dev.instantequip.config.ItemCategory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/** Menu where you pick which item types are shown instantly without the raise animation. */
public class ConfigScreen extends Screen {

	private static final int BUTTON_W = 150;
	private static final int BUTTON_H = 20;
	private static final int GAP = 6;

	private final Screen parent;
	private final List<ButtonWidget> categoryButtons = new ArrayList<>();

	public ConfigScreen(Screen parent) {
		super(Text.literal("Instant Equip"));
		this.parent = parent;
	}

	private static Text label(ItemCategory category) {
		boolean on = InstantEquipConfig.isEnabled(category);
		return Text.literal(category.label + ": ")
			.append(Text.literal(on ? "ON" : "OFF").formatted(on ? Formatting.GREEN : Formatting.RED));
	}

	@Override
	protected void init() {
		this.categoryButtons.clear();

		ItemCategory[] categories = ItemCategory.values();
		int startX = this.width / 2 - BUTTON_W - GAP / 2;
		int startY = 40;

		for (int i = 0; i < categories.length; i++) {
			ItemCategory category = categories[i];
			int x = startX + (i % 2) * (BUTTON_W + GAP);
			int y = startY + (i / 2) * (BUTTON_H + GAP);

			ButtonWidget button = ButtonWidget.builder(label(category), b -> {
				InstantEquipConfig.toggle(category);
				b.setMessage(label(category));
			}).dimensions(x, y, BUTTON_W, BUTTON_H).build();

			this.categoryButtons.add(button);
			this.addDrawableChild(button);
		}

		int rows = (categories.length + 1) / 2;
		int allY = startY + rows * (BUTTON_H + GAP) + 6;

		this.addDrawableChild(ButtonWidget.builder(Text.literal("All ON"), b -> setAll(true))
			.dimensions(startX, allY, BUTTON_W, BUTTON_H).build());
		this.addDrawableChild(ButtonWidget.builder(Text.literal("All OFF"), b -> setAll(false))
			.dimensions(startX + BUTTON_W + GAP, allY, BUTTON_W, BUTTON_H).build());

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> this.close())
			.dimensions(this.width / 2 - 100, allY + BUTTON_H + GAP + 6, 200, BUTTON_H).build());
	}

	private void setAll(boolean value) {
		InstantEquipConfig.setAll(value);
		ItemCategory[] categories = ItemCategory.values();
		for (int i = 0; i < categories.length; i++) {
			this.categoryButtons.get(i).setMessage(label(categories[i]));
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFFFF);
		context.drawCenteredTextWithShadow(this.textRenderer,
			Text.literal("ON = item appears instantly (no raise animation)"), this.width / 2, 25, 0xFFAAAAAA);
	}

	@Override
	public void close() {
		InstantEquipConfig.save();
		this.client.setScreen(this.parent);
	}
}
