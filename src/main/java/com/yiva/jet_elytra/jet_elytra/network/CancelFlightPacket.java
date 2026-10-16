package com.yiva.jet_elytra.jet_elytra.network;

import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * C2S_CancelFlight - "the sneak key was just pressed while I was gliding; get me out".
 *
 * <p>Sent from a key event, not from a tick, which is the whole point: the client only reports a
 * <em>fresh</em> press. Holding sneak before take-off therefore produces no packet at all, and the
 * glide starts normally - the press belonged to whatever the player was doing before the glide, not to
 * the glide. Only a press that happens while already flying cancels.</p>
 *
 * <p>Empty on purpose. The client reports the input, the server decides, and the server checks
 * {@code isFallFlying()} itself rather than trusting a flag: a stale or replayed packet then cannot
 * end a glide that started after it was sent. That check is also what keeps this from being an
 * authority hole - the client cannot ask to stop a glide it is not in.</p>
 */
public record CancelFlightPacket() {

    private static final Logger LOGGER = LogUtils.getLogger();

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
            if (player.isFallFlying()) {
                player.stopFallFlying();
                LOGGER.info("[JetElytra] server: {} cancelled the glide (sneak pressed in flight)",
                        player.getGameProfile().getName());
            } else {
                LOGGER.info("[JetElytra] server: {} asked to cancel but is not gliding; ignored",
                        player.getGameProfile().getName());
            }
        }
        context.setPacketHandled(true);
    }
}
