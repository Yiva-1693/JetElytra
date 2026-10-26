package com.yiva.jet_elytra.jet_elytra.network;

import com.yiva.jet_elytra.jet_elytra.capability.PlayerFuel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S_CancelFlight - "the sneak key was just pressed while I was gliding; get me out".
 *
 * <p>Sent from a key event, not from a tick, which is the whole point: the client only reports a
 * <em>fresh</em> press. Holding sneak before take-off therefore produces no packet at all, and the
 * glide starts normally - the press belonged to whatever the player was doing before the glide, not to
 * the glide. Only a press that happens while already flying cancels.</p>
 *
 * <p>The client stops its local glide immediately and reports the input. The server clears its own
 * glide and thrust state; the normal entity-data sync confirms the result to the client and other
 * players. Only the sending player's state is affected.</p>
 */
public record CancelFlightPacket() {

    public static void encode(final CancelFlightPacket packet, final FriendlyByteBuf buffer) {
        // No payload: the only information is the fact that the packet arrived.
    }

    public static CancelFlightPacket decode(final FriendlyByteBuf buffer) {
        return new CancelFlightPacket();
    }

    public static void handle(final CancelFlightPacket packet, final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        final ServerPlayer player = context.getSender();
        if (player != null) {
            player.stopFallFlying();
            final PlayerFuel fuel = PlayerFuel.of(player);
            fuel.setLifting(false);
            fuel.setBoostSpeed(0.0D);
            // Reassert zero on the next sync even if the previous thrust fingerprint was already zero.
            fuel.setLastSyncedBoostSpeed(Double.NaN);
        }
        context.setPacketHandled(true);
    }
}
