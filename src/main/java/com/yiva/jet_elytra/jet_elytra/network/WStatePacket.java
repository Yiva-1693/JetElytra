package com.yiva.jet_elytra.jet_elytra.network;

import com.yiva.jet_elytra.jet_elytra.capability.PlayerFuel;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * C2S_WState - "the forward key went down / came up".
 *
 * <p>The client only sends this when the state actually flips, so the packet rate is two per
 * press instead of twenty per second.</p>
 */
public record WStatePacket(boolean pressed) {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void encode(final WStatePacket packet, final FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.pressed());
    }

    public static WStatePacket decode(final FriendlyByteBuf buffer) {
        return new WStatePacket(buffer.readBoolean());
    }

    /** Server half: records the key state on the sending player's fuel tank. */
    public static void handle(final WStatePacket packet, final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        final ServerPlayer player = context.getSender();
        if (player != null) {
            PlayerFuel.of(player).setLifting(packet.pressed());
            LOGGER.info("[JetElytra] server: received C2S_WState pressed={} from {} (gliding={})",
                    packet.pressed(), player.getGameProfile().getName(), player.isFallFlying());
        }
        context.setPacketHandled(true);
    }
}
