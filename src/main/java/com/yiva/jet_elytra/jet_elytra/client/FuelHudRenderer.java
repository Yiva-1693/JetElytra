package com.yiva.jet_elytra.jet_elytra.client;

import com.yiva.jet_elytra.jet_elytra.JetConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

/**
 * Draws the three-texture fuel bar in place of the vanilla experience widget.
 *
 * <p>Lifetime: the bar is on screen while the player is gliding, and stays on screen after the jet
 * is switched off so the refill can be watched. Once the tank is back at full it remains for
 * {@link #LINGER_TICKS} ticks (one second) and is then removed outright - no fade.</p>
 */
public final class FuelHudRenderer {

    /** Vanilla experience-bar geometry, which the bar deliberately matches. */
    public static final int BAR_WIDTH = 182;
    public static final int BAR_HEIGHT = 5;

    /** How long a full bar stays on screen before it is removed: one second. */
    private static final int LINGER_TICKS = 20;

    /** White final pass, a touch brighter than vanilla's green level number for a distinct fuel look. */
    private static final int TEXT_COLOR = 0xFFFFFF;

    private static boolean visible = false;

    /** Countdown started when the tank first came back to full; -1 while it is not full. */
    private static int lingerTicks = -1;

    private FuelHudRenderer() {
    }

    /**
     * Advances the visibility state machine once per client tick and reports whether the bar should
     * be drawn at all this tick.
     */
    public static boolean tickVisibility(final LocalPlayer player) {
        if (player == null) {
            reset();
            return false;
        }

        final boolean gliding = player.isFallFlying();
        final double fuel = JetConstants.getClientFuel();
        final double capacity = JetConstants.clientFuelCapacity();

        if (gliding || fuel < capacity - 1.0E-4D) {
            // Flying, or still topping the tank up: on screen, and no countdown running.
            visible = true;
            lingerTicks = -1;
            return true;
        }

        if (lingerTicks < 0) {
            // The tank has just reached full: start the one second countdown.
            lingerTicks = LINGER_TICKS;
        }

        if (lingerTicks > 0) {
            lingerTicks--;
            visible = true;
        } else {
            visible = false;
        }
        return visible;
    }

    /** Clears the visibility state, used when leaving a world. */
    public static void reset() {
        visible = false;
        lingerTicks = -1;
    }

    /** True while the bar should be on screen, including the one second linger after a refill. */
    public static boolean isVisible() {
        return visible;
    }

    /**
     * Draws the bar in the vanilla experience-bar slot.
     *
     * @param partialTick unused, accepted so both call sites can pass the frame's partial tick
     */
    public static void renderAtVanillaSlot(final Minecraft minecraft, final GuiGraphics graphics,
                                           final float partialTick) {
        render(graphics, barLeft(minecraft), barTop(minecraft));
        renderFuelText(graphics, minecraft.font,
                minecraft.getWindow().getGuiScaledWidth() / 2, textTop(minecraft));
    }

    /**
     * Draws the bar: dark background, then the blue progress fill, then the eight-segment grid on
     * top. Draw order matches the texture interfaces in {@link FuelHudTextures}.
     */
    public static void render(final GuiGraphics graphics, final int left, final int top) {
        final double fuel = JetConstants.getClientFuel();
        final double capacity = JetConstants.clientFuelCapacity();
        int filled = (int) (BAR_WIDTH * fuel / Math.max(1.0D, capacity));
        filled = Math.max(0, Math.min(BAR_WIDTH, filled));

        // Keep the correct blend state for textures that carry transparency, as vanilla does around
        // the hotbar. Also clear any tint left behind by another HUD element.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Layer 3: background.
        graphics.blit(FuelHudTextures.BAR_BACKGROUND, left, top, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);

        // Layer 2: fuel progress, drawn as the left `filled` pixels of the fill texture. With the
        // default 182 wide texture this is exactly 1:1; a texture with more columns is squeezed,
        // which is usually what you want for a smooth bar.
        if (filled > 0) {
            graphics.blit(FuelHudTextures.BAR_FILL, left, top, 0, 0, filled, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
        }

        // Layer 1: the eight-segment grid, on top of both of the others.
        graphics.blit(FuelHudTextures.BAR_GRID, left, top, 0, 0, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    /**
     * Draws the fuel amount centred on {@code centerX}, copying the vanilla level-number style: four
     * black offset passes for the outline, then the coloured pass on top.
     */
    public static void renderFuelText(final GuiGraphics graphics, final Font font, final int centerX,
                                      final int topY) {
        int fuel = (int) Math.round(JetConstants.getClientFuel());
        final int maxTextFuel = (int) Math.round(JetConstants.clientFuelCapacity());
        if (fuel < 0) {
            fuel = 0;
        } else if (fuel > maxTextFuel) {
            fuel = maxTextFuel;
        }
        final String text = Integer.toString(fuel);
        final int x = centerX - font.width(text) / 2;

        graphics.drawString(font, text, x + 1, topY, 0, false);
        graphics.drawString(font, text, x - 1, topY, 0, false);
        graphics.drawString(font, text, x, topY + 1, 0, false);
        graphics.drawString(font, text, x, topY - 1, 0, false);
        graphics.drawString(font, text, x, topY, TEXT_COLOR, false);
    }

    /** Where the bar sits: same anchor as the vanilla experience bar. */
    public static int barLeft(final Minecraft minecraft) {
        return minecraft.getWindow().getGuiScaledWidth() / 2 - BAR_WIDTH / 2;
    }

    /** Top edge of the bar, same anchor as the vanilla experience bar. */
    public static int barTop(final Minecraft minecraft) {
        return minecraft.getWindow().getGuiScaledHeight() - 32 + 3;
    }

    /** Top edge of the fuel number, same anchor as the vanilla experience level number. */
    public static int textTop(final Minecraft minecraft) {
        return minecraft.getWindow().getGuiScaledHeight() - 31 - 4;
    }
}
