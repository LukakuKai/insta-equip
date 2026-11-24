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
 * Covered places:
 *  1. After vanilla's per-tick update: if vanilla is still swapping the old item out, the swap is
 *     finished immediately.
 *  2. Right before every first-person render: the swap is also finished here, so the new item
 *     shows up on the very next frame instead of after the next tick, and nothing that touches
 *     the values between tick and render can bring the animation back.
 *  3. resetEquipProgress, which restarts the raise animation after some interactions.
 *
 * Vanilla also lowers the item after every attack, because the raise height follows the attack
 * cooldown. While "keep raised" is on (default) enabled items are held fully raised all the time;
 * otherwise only right after a switch until the cooldown has recovered.
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

	@Unique
	private void instantequip$apply(ClientPlayerEntity player) {
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

		if (this.instantequip$holdRaised || InstantEquipConfig.isKeepRaised()) {
			this.equipProgressMainHand = 1.0F;
			this.lastEquipProgressMainHand = 1.0F;
		}
	}

	@Inject(method = "updateHeldItems", at = @At("TAIL"))
	private void instantequip$afterTick(CallbackInfo ci) {
		ClientPlayerEntity player = this.client.player;
		if (player == null) {
			this.instantequip$holdRaised = false;
			return;
		}

		this.instantequip$apply(player);

		if (this.instantequip$holdRaised && player.getAttackCooldownProgress(1.0F) >= 1.0F) {
			this.instantequip$holdRaised = false;
		}
	}

	// No descriptor on purpose: covers every renderItem overload, the handler only touches fields.
	@Inject(method = "renderItem", at = @At("HEAD"))
	private void instantequip$beforeRender(CallbackInfo ci) {
		ClientPlayerEntity player = this.client.player;
		if (player != null) {
			this.instantequip$apply(player);
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
