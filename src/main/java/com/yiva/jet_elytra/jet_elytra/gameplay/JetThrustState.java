package com.yiva.jet_elytra.jet_elytra.gameplay;

/**
 * The client's mirror of the server's thrust ramp.
 *
 * <p>Kept in a package-neutral class, not in {@code client}, because it is written by the network
 * handler on both logical sides' classpath: only a client ever receives {@code S2C_ThrustSync}, so
 * the value simply stays zero on a dedicated server.</p>
 *
 * <p><b>Why the mixin must not read the client's own {@code PlayerFuel} instead.</b> The tank is
 * attached per entity, so the client-side player has a capability of its own that nothing ever
 * updates; its {@code boostSpeed} would read zero forever. Worse, that capability is persisted with
 * the player's NBT, so a stale ramp could survive a relog. This holder is the single source of truth
 * for the client and is fed only by the server's packet.</p>
 */
public final class JetThrustState {

    /**
     * Blocks per tick the server wants the owning client's elytra to fly at; zero means "no jet".
     * Volatile because the network thread writes it while the client tick thread reads it.
     */
    private static volatile double serverSpeed;

    private JetThrustState() {
    }

    /**
     * Records the speed the server just pushed. Read-only input: the client never advances the ramp
     * itself, it only reproduces the value the server has already decided on.
     */
    public static void acceptServerSpeed(final double speed) {
        serverSpeed = Double.isFinite(speed) && speed > 0.0D ? speed : 0.0D;
    }

    /** The ramp the client should apply this tick, or zero when the jet is not running. */
    public static double serverSpeed() {
        return serverSpeed;
    }

    /** Used when leaving a world, so the next one cannot start with a stale ramp. */
    public static void clear() {
        serverSpeed = 0.0D;
    }
}
