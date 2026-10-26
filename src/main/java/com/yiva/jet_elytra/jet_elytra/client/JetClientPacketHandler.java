package com.yiva.jet_elytra.jet_elytra.client;

import com.yiva.jet_elytra.jet_elytra.Jet_Elytra;
import com.yiva.jet_elytra.jet_elytra.gameplay.JetThrustState;
import com.yiva.jet_elytra.jet_elytra.network.JetNetwork;
import com.yiva.jet_elytra.jet_elytra.network.WStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Turns the vanilla forward key into {@code C2S_WState}.
 *
 * <p>Polled once per client tick, exactly as the design document specifies. Two things force a packet
 * even when the key itself did not change:</p>
 * <ul>
 *   <li>the glide state flipping (take-off or landing) - the server clears its stored key state when a
 *       player leaves glide, so entering glide has to re-assert the key immediately, otherwise
 *       "hold W, then take off" would need a key release and re-press;</li>
 *   <li>the world being joined.</li>
 * </ul>
 *
 * <p>Sneak is deliberately not polled here: it is a one-shot press, handled by
 * {@link JetFlightCancelHandler} from the key event.</p>
 */
@Mod.EventBusSubscriber(modid = Jet_Elytra.MODID, value = Dist.CLIENT)
public final class JetClientPacketHandler {

    /** Last state we told the server about. */
    private static boolean lastPressed = false;

    /** Last glide state we told the server about. */
    private static boolean lastGliding = false;

    /** Whether the previous tick had a world loaded, so leaving one can be detected. */
    private static boolean inWorld = false;

    private JetClientPacketHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(final TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            // Not in a world: forget the state so the first tick after joining re-syncs it, and drop
            // the thrust ramp so a stale value cannot move the player before the server re-states it.
            if (inWorld) {
                inWorld = false;
                JetThrustState.clear();
            }
            lastPressed = false;
            lastGliding = false;
            return;
        }

        inWorld = true;
        final boolean pressed = minecraft.options.keyUp.isDown();
        final boolean gliding = minecraft.player.isFallFlying();
        final boolean glideChanged = gliding != lastGliding;

        if (pressed != lastPressed || glideChanged) {
            lastPressed = pressed;
            lastGliding = gliding;
            JetNetwork.CHANNEL.sendToServer(new WStatePacket(pressed));
        }
    }

    /** Forgets the last state we sent; used when leaving a world so the next world starts clean. */
    public static void reset() {
        lastPressed = false;
        lastGliding = false;
        inWorld = false;
        JetThrustState.clear();
    }
}
