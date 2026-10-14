package com.yiva.jet_elytra.jet_elytra.network;

import com.yiva.jet_elytra.jet_elytra.gameplay.JetThrustState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C_ThrustSync - the authoritative thrust ramp speed for the receiving player's own elytra.
 *
 * <p>The server owns the thrust model, so the client has no way of working the ramp out for itself.
 * While only the server knew the ramp, the velocity override in {@code ElytraThrustMixin} ran on the
 * server alone: the client kept recomputing vanilla glide velocity in its own {@code travel}, and
 * because the client is authoritative over its own position the player flew at plain glide speed no
 * matter how much fuel was burning.</p>
 *
 * <p>The packet therefore carries the one number the client needs to reproduce the same motion, and
 * is only sent while that number actually changes. It is deliberately not part of
 * {@link FuelSyncPacket}: fuel needs a fastest-change guarantee, the ramp needs change detection.</p>
 */
public record ThrustSyncPacket(double speed) {

    public static void encode(final ThrustSyncPacket packet, final FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.speed());
    }

    public static ThrustSyncPacket decode(final FriendlyByteBuf buffer) {
        return new ThrustSyncPacket(buffer.readDouble());
    }

    /** Client half: mirrors the value into the state read by the elytra mixin. */
    public static void handle(final ThrustSyncPacket packet, final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        JetThrustState.acceptServerSpeed(packet.speed());
        context.setPacketHandled(true);
    }
}
