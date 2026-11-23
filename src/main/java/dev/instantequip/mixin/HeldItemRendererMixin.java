package dev.instantequip.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Removes the first-person "raise" animation of the main hand when the held item changes.
 *
 * Vanilla lowers the old item, swaps it, then slowly raises the new one (the raise speed is
 * additionally tied to the attack cooldown, which is reset on every item switch). We skip all
 * of that: the new item is shown fully raised from the first frame.
 */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

	@Shadow
	private float equipProgressMainHand;

	@Shadow
	private float lastEquipProgressMainHand;

	@Shadow
	private ItemStack mainHand;

	@Shadow
	@org.spongepowered.asm.mixin.Final
	private MinecraftClient client;

	/** True from the moment of a switch until the attack cooldown is full again. */
	private boolean instantequip$skipRaise = false;

	@Inject(method = "updateHeldItems", at = @At("HEAD"))
	private void instantequip$onUpdateStart(CallbackInfo ci) {
		ClientPlayerEntity player = this.client.player;
		if (player == null) {
			return;
		}

		ItemStack current = player.getMainHandStack();
		if (!ItemStack.areItemsEqual(this.mainHand, current)) {
			// Make vanilla believe the right item was already equipped -> no lowering phase.
			this.mainHand = current;
			this.instantequip$skipRaise = true;
			this.equipProgressMainHand = 1.0F;
			this.lastEquipProgressMainHand = 1.0F;
		}
	}

	@Inject(method = "updateHeldItems", at = @At("TAIL"))
	private void instantequip$onUpdateEnd(CallbackInfo ci) {
		if (!this.instantequip$skipRaise) {
			return;
		}

		ClientPlayerEntity player = this.client.player;
		if (player == null) {
			this.instantequip$skipRaise = false;
			return;
		}

		// Keep the item fully raised while the (reset) attack cooldown recovers,
		// otherwise vanilla would drag it down again to match the cooldown.
		this.equipProgressMainHand = 1.0F;
		this.lastEquipProgressMainHand = 1.0F;

		if (player.getAttackCooldownProgress(0.0F) >= 1.0F) {
			this.instantequip$skipRaise = false;
		}
	}
}
