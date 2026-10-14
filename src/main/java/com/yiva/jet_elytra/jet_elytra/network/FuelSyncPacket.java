package com.yiva.jet_elytra.jet_elytra.network;

import com.yiva.jet_elytra.jet_elytra.JetConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** S2C_FuelSync - authoritative fuel value for the receiving player's own HUD. */
public record FuelSyncPacket(double fuel) {

    public static void encode(final FuelSyncPacket packet, final FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.fuel());
    }

    public static FuelSyncPacket decode(final FriendlyByteBuf buffer) {
        return new FuelSyncPacket(buffer.readDouble());
    }

    public static void handle(final FuelSyncPacket packet, final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        JetConstants.setClientFuel(packet.fuel());
        context.setPacketHandled(true);
    }
}
