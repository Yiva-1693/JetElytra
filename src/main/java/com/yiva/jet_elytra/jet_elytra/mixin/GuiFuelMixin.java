package com.yiva.jet_elytra.jet_elytra.mixin;

import com.yiva.jet_elytra.jet_elytra.client.FuelHudRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla experience bar with the jet fuel bar while the player is glide-flying.
 *
 * <p>In 1.20.1 {@code renderExperienceBar} (SRG {@code m_280276_}) draws <b>both</b> the bar and the
 * experience <b>level number</b>: the number is rendered further down the same method, after the
 * {@code "expLevel"} profiler push. A single HEAD injection therefore covers the whole widget -
 * there is no separate {@code renderExperienceLevel} method to patch.</p>
 *
 * <p>This only takes the slot while gliding, so that on landing vanilla gets its own bar back and can
 * play its usual animations. The fuel bar keeps being drawn by {@code JetClientHudHandler} while the
 * tank regenerates, and is removed the moment it is full again.</p>
 */
@Mixin(Gui.class)
public abstract class GuiFuelMixin {

    /**
     * The official (dev) name is used here on purpose. Verified with the Mixin 0.8.5 annotation
     * processor: writing the SRG name {@code m_280276_} makes the AP fail to resolve the target
     * ("Unable to determine descriptor for @Inject target method") and it then emits an EMPTY
     * refmap, so the injection could not be applied in the dev environment. With the official
     * name the AP resolves the target and writes
     * {@code renderExperienceBar -> Lnet/minecraft/client/gui/Gui;m_280276_(Lnet/minecraft/client/gui/GuiGraphics;I)V}
     * into the {@code searge} section of jet_elytra.refmap.json, which is what production runs on.
     */
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void jetelytra$renderFuelBar(final GuiGraphics graphics, final int x, final CallbackInfo ci) {
        final Minecraft minecraft = Minecraft.getInstance();
        // The visibility state machine (advanced once per client tick) owns the lifetime: while it
        // says the bar is on screen, the fuel widget takes the slot in every game mode.
        if (!FuelHudRenderer.isVisible()) {
            return;
        }

        FuelHudRenderer.renderAtVanillaSlot(minecraft, graphics, 0.0F);
        ci.cancel();
    }
}
