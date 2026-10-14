package com.yiva.jet_elytra.jet_elytra.gameplay;

import com.yiva.jet_elytra.jet_elytra.JetConfig;
import com.yiva.jet_elytra.jet_elytra.Jet_Elytra;
import com.yiva.jet_elytra.jet_elytra.capability.PlayerFuel;
import com.yiva.jet_elytra.jet_elytra.network.FuelSyncPacket;
import com.yiva.jet_elytra.jet_elytra.network.JetNetwork;
import com.yiva.jet_elytra.jet_elytra.network.ThrustSyncPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * The authoritative per-tick simulation: thrust fuel burn, regeneration, automatic rocket top-up and
 * the trail particles. The decisions themselves live in {@link JetThrust}; this class only supplies
 * the world, the player and the network.
 *
 * <p>Flow per player, per tick:</p>
 * <ol>
 *   <li>{@link JetThrust#tick} burns fuel and reports whether the jet is thrusting.</li>
 *   <li>When it is not thrusting, the regeneration delay is armed.</li>
 *   <li>Regeneration runs once its delay has elapsed, until the tank is full or thrust restarts. It
 *       uses the in-flight rate while the player is still fall-flying and the faster out-of-flight
 *       rate once they have landed, so leaving the glide is what makes topping up quick.</li>
 *   <li>Automatic rocket consumption is checked last, so it can even fire while thrusting.</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = Jet_Elytra.MODID)
public final class JetFlightHandler {

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private JetFlightHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(final TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        final ServerLevel level = player.serverLevel();
        final long gameTime = level.getGameTime();
        final PlayerFuel fuel = PlayerFuel.of(player);
        final boolean gliding = player.isFallFlying();
        // Read the key state directly instead of trusting a flag written on an earlier tick: this is
        // what makes "hold W, then start gliding" enter thrust immediately.
        final boolean keyHeld = fuel.isLifting();
        final double beforeX = player.getX();
        final double beforeY = player.getY();
        final double beforeZ = player.getZ();

        final JetThrust.Race race = JetThrust.tick(fuel, gliding, keyHeld,
                player.getDeltaMovement().length(), gameTime);
        if (race.thrusting()) {
            spawnTrail(player);
            // Throttled server-side trace: the thrust decision plus the position it produced.
            if (gameTime % 20L == 0L) {
                LOGGER.info("[JetElytra] server: thrusting gliding={} key={} fuel={} ramp={} pos={}",
                        gliding, keyHeld, String.format("%.2f", fuel.getFuel()),
                        String.format("%.3f", race.rampSpeed()),
                        String.format("%.2f/%.2f/%.2f", player.getX(), player.getY(), player.getZ()));
            }
        } else {
            // Thrust is not running. The model has already armed the regeneration window one second
            // ahead on the last thrusting tick, so this call only matters for a player who was never
            // thrusting at all (for example holding W with an empty tank, or leaving glide).
            fuel.beginRefillDelay(gameTime);
        }

        if (gliding) {
            // Automatic top-up can fire mid-thrust; the sudden fuel jump is expected behaviour.
            RefuelService.tryAutoRefuel(player, fuel);
        }

        JetThrust.regenerate(fuel, gliding, gameTime);
        syncFuel(player, fuel, gameTime);
        // Without this the client never learns the ramp, and its own elytra maths - which is the one
        // that actually decides where the player goes - keeps overwriting the jet with glide speed.
        syncThrust(player, fuel);

        // Diagnostic trace: one line per tick covering the whole input -> thrust -> velocity chain.
        final double moved = Math.sqrt(Math.pow(player.getX() - beforeX, 2)
                + Math.pow(player.getY() - beforeY, 2)
                + Math.pow(player.getZ() - beforeZ, 2));
        JetDiagnostics.tick(level.getServer(), player, fuel, gliding, keyHeld, race.thrusting(), moved);
    }

    /** Vanilla firework trail particles, server side, so every nearby player sees them. */
    private static void spawnTrail(final ServerPlayer player) {
        final int count = JetConfig.particleCount;
        if (count <= 0) {
            return;
        }
        final double spread = JetConfig.particleSpread;
        player.serverLevel().sendParticles(ParticleTypes.FIREWORK,
                player.getX(), player.getY(), player.getZ(),
                count, spread, spread, spread, 0.0D);
    }

    /**
     * Pushes the fuel value to the owning client every {@code syncIntervalTicks} ticks, plus
     * immediately whenever the value moves enough to change the displayed integer.
     */
    private static void syncFuel(final ServerPlayer player, final PlayerFuel fuel, final long gameTime) {
        final double current = fuel.getFuel();
        final double last = fuel.getLastSyncedFuel();
        final boolean changed = Double.isNaN(last) || Math.abs(current - last) >= 0.5D;
        final boolean due = gameTime % Math.max(1, JetConfig.syncIntervalTicks) == 0L;
        if (!changed && !due) {
            return;
        }
        fuel.setLastSyncedFuel(current);
        JetNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new FuelSyncPacket(current));
    }

    /** Forces a sync, used by the right-click path so the HUD reacts instantly. */
    public static void syncNow(final Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            final PlayerFuel fuel = PlayerFuel.of(serverPlayer);
            final double current = fuel.getFuel();
            fuel.setLastSyncedFuel(current);
            JetNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new FuelSyncPacket(current));
        }
    }

    /**
     * Pushes the thrust ramp to the owning client, but only when the value the client is holding has
     * gone stale.
     *
     * <p>This is what makes the jet actually move the player. The client recomputes its elytra
     * velocity every tick from the server's copy of the ramp, and the client - not the server - is
     * authoritative over its own position, so an override applied on the server alone is thrown away
     * by the next position packet. Change detection keeps the cost at one small packet per tick
     * during the ramp and nothing at all once the terminal speed is held.</p>
     */
    private static void syncThrust(final ServerPlayer player, final PlayerFuel fuel) {
        final double current = fuel.getBoostSpeed();
        final double last = fuel.getLastSyncedBoostSpeed();
        if (!Double.isNaN(last) && current == last) {
            return;
        }
        fuel.setLastSyncedBoostSpeed(current);
        JetNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ThrustSyncPacket(current));
    }

    /** A player who disconnects must not leave stale thrust state behind for the next session. */
    @SubscribeEvent
    public static void onPlayerLoggedOut(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            final PlayerFuel fuel = PlayerFuel.of(player);
            fuel.setLifting(false);
            fuel.setBoostSpeed(0.0D);
            // Forget what the client was last told as well. Leaving the fingerprint at the ramp that
            // was in force when the player quit would make the next session's first sync look
            // unchanged, so the zero ramp would never be sent and the client would keep flying at the
            // old speed with no fuel burning.
            fuel.setLastSyncedBoostSpeed(Double.NaN);
        }
    }
}
