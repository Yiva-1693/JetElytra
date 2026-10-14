package com.yiva.jet_elytra.jet_elytra.client;

import com.yiva.jet_elytra.jet_elytra.Jet_Elytra;
import net.minecraft.resources.ResourceLocation;

/**
 * The three texture interfaces of the fuel bar, in the z-order the bar is drawn with.
 *
 * <p>Drop the three PNG files into
 * {@code src/main/resources/assets/jet_elytra/textures/gui/} and the bar picks them up; no code
 * change is needed. A plain 182x5 PNG is the default size (the vanilla experience bar's size, which
 * is also the size the bar is drawn at).</p>
 *
 * <p>Draw order, lowest priority first - later layers are painted on top:</p>
 * <ol>
 *   <li>{@link #BAR_BACKGROUND} - layer 3, dark blue background of the whole bar.</li>
 *   <li>{@link #BAR_FILL} - layer 2, bright blue fuel progress. Only the left
 *       {@code fuel / capacity} fraction of its width is drawn.</li>
 *   <li>{@link #BAR_GRID} - layer 1, the eight-segment grid overlay, drawn on top of everything
 *       else.</li>
 * </ol>
 */
public final class FuelHudTextures {

    /** Directory the three textures live in. */
    public static final String DIRECTORY = "textures/gui/";

    /** Layer 3 (bottom): the dark blue bar background. */
    public static final ResourceLocation BAR_BACKGROUND = texture("fuel_bar_background");

    /** Layer 2 (middle): the bright blue fuel progress bar. */
    public static final ResourceLocation BAR_FILL = texture("fuel_bar_fill");

    /** Layer 1 (top): the grid that splits the bar into eight segments. */
    public static final ResourceLocation BAR_GRID = texture("fuel_bar_grid");

    private FuelHudTextures() {
    }

    private static ResourceLocation texture(final String name) {
        return new ResourceLocation(Jet_Elytra.MODID, DIRECTORY + name + ".png");
    }
}
