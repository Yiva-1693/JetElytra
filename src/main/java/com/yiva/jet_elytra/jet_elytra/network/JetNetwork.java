package com.yiva.jet_elytra.jet_elytra.network;

import com.yiva.jet_elytra.jet_elytra.Jet_Elytra;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Channel definition plus the three packets the design document asks for. */
public final class JetNetwork {

    private static final String PROTOCOL_VERSION = "1";

    /** Packet ids must stay stable across versions. */
    private static final int ID_W_STATE = 0;
    private static final int ID_FUEL_SYNC = 1;
    private static final int ID_THRUST_SYNC = 2;
    private static final int ID_CANCEL_FLIGHT = 3;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Jet_Elytra.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private JetNetwork() {
    }

    /**
     * Registers every packet. Each message has exactly one handler; the W-state and cancel-flight
     * packets are handled server side (they are only ever sent client to server), and the two sync
     * packets are handled on whichever side receives them, which is always the owning client.
     */
    public static void register() {
        CHANNEL.messageBuilder(WStatePacket.class, ID_W_STATE)
                .encoder(WStatePacket::encode)
                .decoder(WStatePacket::decode)
                .consumerMainThread(WStatePacket::handle)
                .add();

        CHANNEL.messageBuilder(CancelFlightPacket.class, ID_CANCEL_FLIGHT)
                .encoder(CancelFlightPacket::encode)
                .decoder(CancelFlightPacket::decode)
                .consumerMainThread(CancelFlightPacket::handle)
                .add();

        CHANNEL.messageBuilder(FuelSyncPacket.class, ID_FUEL_SYNC)
                .encoder(FuelSyncPacket::encode)
                .decoder(FuelSyncPacket::decode)
                .consumerMainThread(FuelSyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(ThrustSyncPacket.class, ID_THRUST_SYNC)
                .encoder(ThrustSyncPacket::encode)
                .decoder(ThrustSyncPacket::decode)
                .consumerMainThread(ThrustSyncPacket::handle)
                .add();
    }
}
