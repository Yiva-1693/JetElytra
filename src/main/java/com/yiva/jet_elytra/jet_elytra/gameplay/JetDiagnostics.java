package com.yiva.jet_elytra.jet_elytra.gameplay;

import com.mojang.logging.LogUtils;
import com.yiva.jet_elytra.jet_elytra.capability.PlayerFuel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Writes a plain text trace of the thrust pipeline to {@code logs/jet_elytra-diagnostic.log}.
 *
 * <p>Only active when {@code debugLog} is enabled in the mod config. Every line answers one question
 * in the input -> packet -> decision -> velocity chain, so a single flight tells us exactly which
 * link is broken.</p>
 */
public final class JetDiagnostics {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String FILE_NAME = "jet_elytra-diagnostic.log";
    private static final long FLUSH_INTERVAL_TICKS = 20L;

    /** Toggled from the config; when false every call here returns immediately. */
    public static volatile boolean enabled = false;

    private static final StringBuilder BUFFER = new StringBuilder();
    private static long lastFlush;
    private static Path file;

    private JetDiagnostics() {
    }

    /**
     * Records one server tick of the thrust pipeline. Called every tick by the flight handler, so the
     * trace shows the whole picture rather than only the interesting moments.
     */
    public static void tick(final MinecraftServer server, final ServerPlayer player, final PlayerFuel fuel,
                            final boolean gliding, final boolean keyHeld, final boolean thrusting,
                            final double travelledBlocks) {
        if (!enabled) {
            return;
        }

        final long time = server.overworld().getGameTime();
        BUFFER.append("t=").append(time)
                .append(" player=").append(player.getGameProfile().getName())
                .append(" gliding=").append(gliding)
                .append(" keyPacket=").append(keyHeld)
                .append(" fuel=").append(String.format("%.3f", fuel.getFuel()))
                .append(" ramp=").append(String.format("%.4f", fuel.getBoostSpeed()))
                .append(" thrusting=").append(thrusting)
                .append(" velocity=").append(String.format("%.4f", player.getDeltaMovement().length()))
                .append(" moved=").append(String.format("%.4f", travelledBlocks))
                .append('\n');

        if (time - lastFlush >= FLUSH_INTERVAL_TICKS) {
            lastFlush = time;
            flush(server);
        }
    }

    /** Records a one-off event (login, logout, right-click refuel, config load...). */
    public static void event(final String message) {
        if (!enabled) {
            return;
        }
        BUFFER.append("EVENT ").append(message).append('\n');
        flush(null);
    }

    private static synchronized void flush(final MinecraftServer server) {
        if (BUFFER.length() == 0) {
            return;
        }
        try {
            if (file == null) {
                final Path logDir = FMLPaths.GAMEDIR.get().resolve("logs");
                Files.createDirectories(logDir);
                file = logDir.resolve(FILE_NAME);
                Files.writeString(file, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
            }
            Files.writeString(file, BUFFER.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            BUFFER.setLength(0);
        } catch (final IOException e) {
            LOGGER.warn("Jet Elytra: could not write the diagnostic log", e);
            enabled = false;
        }
    }
}
