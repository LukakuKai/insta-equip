package dev.instantequip.mixin;

import dev.instantequip.config.InstantEquipConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Removes the first-person "raise" animation of the main hand when the held item changes.
 *
 * Three places are covered:
 *  1. After vanilla's per-tick update: if vanilla is still busy swapping the old item out, the
 *     swap is finished immediately and the item is held fully raised until the attack cooldown
 *     (which restarts on every switch and would otherwise pull the item down) has recovered.
 *  2. Right before every first-person render, so nothing that changes the values between tick
 *     and render (other mods, interaction code) can bring the animation back.
 *  3. resetEquipProgress, which vanilla/other code uses to restart the raise animation.
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

	@Unique
	private boolean instantequip$holdRaised = false;

	@Unique
	private int instantequip$logged = 0;

	@Inject(method = "updateHeldItems", at = @At("TAIL"))
	private void instantequip$afterTick(CallbackInfo ci) {
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

			if (this.instantequip$logged < 5) {
				this.instantequip$logged++;
				System.out.println("[instantequip] skipped raise animation for " + current.getItem());
			}
		}

		if (this.instantequip$holdRaised) {
			this.equipProgressMainHand = 1.0F;
			this.lastEquipProgressMainHand = 1.0F;

			if (player.getAttackCooldownProgress(1.0F) >= 1.0F) {
				this.instantequip$holdRaised = false;
			}
		}
	}

	// No descriptor on purpose: covers every renderItem overload, the handler only touches fields.
	@Inject(method = "renderItem", at = @At("HEAD"))
	private void instantequip$beforeRender(CallbackInfo ci) {
		if (!this.instantequip$holdRaised) {
			return;
		}

		ClientPlayerEntity player = this.client.player;
		if (player != null && InstantEquipConfig.isInstant(player.getMainHandStack())) {
			this.equipProgressMainHand = 1.0F;
			this.lastEquipProgressMainHand = 1.0F;
		}
	}

	@Inject(method = "resetEquipProgress", at = @At("HEAD"), cancellable = true)
	private void instantequip$noReset(Hand hand, CallbackInfo ci) {
		if (hand != Hand.MAIN_HAND) {
			return;
		}

		ClientPlayerEntity player = this.client.player;
		if (player != null && InstantEquipConfig.isInstant(player.getMainHandStack())) {
			ci.cancel();
		}
	}
}
