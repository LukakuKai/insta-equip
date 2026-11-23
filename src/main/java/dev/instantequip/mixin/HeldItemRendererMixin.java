package dev.instantequip.mixin;

import dev.instantequip.config.InstantEquipConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Removes the first-person "raise" animation of the main hand when the held item changes.
 *
 * Vanilla lowers the old item, swaps it and then raises the new one. The raise speed is also
 * tied to the attack cooldown, which restarts on every switch. After vanilla has run its
 * update, we check whether it is still busy swapping (its stored stack differs from what the
 * player really holds). If so, and the new item is enabled in the menu, we finish the swap
 * immediately and keep the item fully raised until the cooldown has recovered.
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
	@Final
	private MinecraftClient client;

	@org.spongepowered.asm.mixin.Unique
	private boolean instantequip$holdRaised = false;

	@Inject(method = "updateHeldItems", at = @At("TAIL"))
	private void instantequip$skipRaise(CallbackInfo ci) {
		ClientPlayerEntity player = this.client.player;
		if (player == null) {
			this.instantequip$holdRaised = false;
			return;
		}

		ItemStack current = player.getMainHandStack();
		if (!InstantEquipConfig.isInstant(current)) {
			this.instantequip$holdRaised = false;
			return;
		}

		// Vanilla still thinks the old item is in hand -> it is lowering it. Skip that.
		if (this.mainHand != current) {
			this.mainHand = current;
			this.instantequip$holdRaised = true;
		}

		if (this.instantequip$holdRaised) {
			this.equipProgressMainHand = 1.0F;
			this.lastEquipProgressMainHand = 1.0F;

			if (player.getAttackCooldownProgress(1.0F) >= 1.0F) {
				this.instantequip$holdRaised = false;
			}
		}
	}
}
