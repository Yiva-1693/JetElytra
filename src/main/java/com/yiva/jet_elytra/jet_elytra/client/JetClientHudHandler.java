package com.yiva.jet_elytra.jet_elytra.client;

import com.yiva.jet_elytra.jet_elytra.JetConstants;
import com.yiva.jet_elytra.jet_elytra.Jet_Elytra;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Drives the visibility state machine of the fuel bar, once per client tick.
 *
 * <p>The drawing itself is done by {@code GuiFuelMixin}, which replaces the vanilla experience bar.
 * Vanilla calls that method every frame in every game mode, so there is no mode the bar can miss.
 * This class only decides whether the bar should be on screen at all - the mixin asks it and either
 * takes the widget over or leaves vanilla alone.</p>
 */
@Mod.EventBusSubscriber(modid = Jet_Elytra.MODID, value = Dist.CLIENT)
public final class JetClientHudHandler {

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private static int ticks;

    private JetClientHudHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(final TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        FuelHudRenderer.tickVisibility(minecraft.player);

        // Throttled sanity trace: the client's own position, so the server's thrust can be compared
        // against what the player actually experiences.
        if (minecraft.player != null && ++ticks % 20 == 0) {
            LOGGER.info("[JetElytra] client: gliding={} fuel(bar)={} pos={} velocity={}",
                    minecraft.player.isFallFlying(),
                    String.format("%.1f", JetConstants.getClientFuel()),
                    String.format("%.2f/%.2f/%.2f", minecraft.player.getX(), minecraft.player.getY(),
                            minecraft.player.getZ()),
                    String.format("%.3f", minecraft.player.getDeltaMovement().length()));
        }
    }
}
