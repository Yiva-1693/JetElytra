package com.yiva.jet_elytra.jet_elytra.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.yiva.jet_elytra.jet_elytra.Jet_Elytra;
import com.yiva.jet_elytra.jet_elytra.network.CancelFlightPacket;
import com.yiva.jet_elytra.jet_elytra.network.JetNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

/**
 * Turns a fresh sneak press into {@code C2S_CancelFlight}: pressing sneak while gliding gets the player
 * out of the glide.
 *
 * <p><b>Why an event and not a per-tick poll.</b> The behaviour asked for is "a press made during the
 * glide cancels it", and holding sneak before take-off has to be ignored, because that press was made
 * before the glide existed. A key event describes exactly that: it fires once, at the moment of the
 * press. Polling {@code keyShift.isDown()} every tick cannot express it - it only sees that the key is
 * held, and cannot tell a new press from one that predates take-off. Trying to repair that with
 * per-tick bookkeeping is what made the earlier version cancellable by a key the player was already
 * holding down.</p>
 *
 * <p>Two guards keep this to the intended case:</p>
 * <ul>
 *   <li>the press is ignored unless the local player is gliding <em>at that instant</em>. This is the
 *       guard that makes "hold sneak, then take off" a no-op: when sneak went down there was no glide,
 *       and by the time the glide starts no new press arrives.</li>
 *   <li>only the sneak binding counts, compared as its key code rather than hard-coded, so a remapped
 *       sneak key still works.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = Jet_Elytra.MODID, value = Dist.CLIENT)
public final class JetFlightCancelHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private JetFlightCancelHandler() {
    }

    @SubscribeEvent
    public static void onKey(final InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        final InputConstants.Key sneak = minecraft.options.keyShift.getKey();
        if (event.getKey() != sneak.getValue()) {
            return;
        }

        // The guard that matters: no glide, no cancellation. A press made before take-off lands here and
        // does nothing, which is exactly the intended behaviour.
        if (!minecraft.player.isFallFlying()) {
            return;
        }

        JetNetwork.CHANNEL.sendToServer(new CancelFlightPacket());
        LOGGER.info("[JetElytra] client: sent C2S_CancelFlight (sneak pressed while gliding)");
    }
}
