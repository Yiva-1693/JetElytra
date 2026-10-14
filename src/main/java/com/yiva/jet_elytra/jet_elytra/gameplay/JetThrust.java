package com.yiva.jet_elytra.jet_elytra.gameplay;

import com.yiva.jet_elytra.jet_elytra.JetConfig;
import com.yiva.jet_elytra.jet_elytra.capability.PlayerFuel;

/**
 * Pure per-tick fuel and thrust decision model.
 *
 * <p>Deliberately free of Minecraft types: it takes the current input state plus the gas tank and
 * says what this tick should look like. {@link JetFlightHandler} applies it to players, and the
 * smoke test drives it directly, so the two can never drift apart.</p>
 */
public final class JetThrust {

    /** What one tick of the model decided. */
    public record Race(boolean thrusting, double rampSpeed) {
    }

    private JetThrust() {
    }

    /**
     * Runs one server tick of the thrust state machine.
     *
     * <p>The thrust decision is taken from this tick's arguments only, never from state left behind
     * by an earlier tick. Holding the forward key before, during or after take-off therefore behaves
     * identically: the moment the player is gliding, the key is down and there is enough fuel,
     * thrust starts on that very tick.</p>
     *
     * @param fuel         the player's tank, mutated in place
     * @param gliding      whether the player is fall-flying
     * @param keyHeld      whether the forward key is held, as reported for this tick
     * @param currentSpeed the player's current velocity magnitude in blocks per tick
     * @param gameTime     level game time, used for the regeneration window
     * @return the tick outcome, including the ramp speed the travel mixin should use
     */
    public static Race tick(final PlayerFuel fuel, final boolean gliding, final boolean keyHeld,
                            final double currentSpeed, final long gameTime) {
        // This tick's input is authoritative, so the stored flag always reflects the current key.
        fuel.setLifting(gliding && keyHeld);

        if (!gliding) {
            // Landed: the thrust ramp is gone for good. This is the only place it drops to zero, so
            // it is the take-off moment that decides whether thrust starts from rest.
            fuel.setBoostSpeed(0.0D);
            return new Race(false, 0.0D);
        }

        if (!keyHeld) {
            // Still gliding with the key released: thrust stops, but the ramp is only bled off at the
            // same rate it was built up. Releasing and re-pressing W mid-flight therefore keeps the
            // speed you had instead of dropping you back to a single increment.
            final double decaying = fuel.getBoostSpeed() - JetConfig.accelerationPerTick();
            fuel.setBoostSpeed(Math.max(0.0D, decaying));
            return new Race(false, 0.0D);
        }

        if (fuel.getFuel() <= 0.0D) {
            // Tank dry: never thrust, no matter what the key says.
            fuel.setBoostSpeed(0.0D);
            return new Race(false, 0.0D);
        }

        // `alreadyThrusting` is decided solely by the ramp this method itself left behind on the
        // previous tick. Deriving it from anything else would let another writer bypass the
        // low-fuel entry guard below.
        final boolean alreadyThrusting = fuel.getBoostSpeed() > 0.0D;

        if (!alreadyThrusting && fuel.getFuel() < JetConfig.minFuelToStart) {
            // Low-fuel guard: thrust cannot be *entered* below the threshold. Thrust that is
            // already running keeps burning all the way down to zero, which is why this check is
            // gated on `alreadyThrusting`.
            return new Race(false, 0.0D);
        }

        // Entering thrust seeds the ramp with the speed the player already has, so re-engaging the
        // jet mid-flight picks up from the current velocity instead of dropping back to one
        // increment. Thrust is never a brake. While thrusting the ramp just grows by one increment.
        double ramp = alreadyThrusting
                ? fuel.getBoostSpeed()
                : Math.max(0.0D, Math.min(JetConfig.maxSpeedPerTick(), currentSpeed));

        fuel.consume(JetConfig.consumptionPerTick());
        // Keep the regeneration window pushed back for as long as fuel is being burned. The +1
        // makes the window start *after* this tick: the last thrusting tick T therefore permits
        // regeneration from T+1+delay, which is exactly "wait one full second after thrust ended".
        fuel.blockRefill(gameTime + 1L);

        if (fuel.getFuel() <= 0.0D) {
            // Ran dry this tick: thrust stops immediately.
            fuel.setFuel(0.0D);
            fuel.setBoostSpeed(0.0D);
            return new Race(false, 0.0D);
        }

        // Ramp for one tick. The mixin reads this value on the *next* tick, which is exactly the
        // progression the design asks for: entering thrust reports one increment above the speed
        // that was carried in, and the terminal speed is reached after `accelerationTicks` ticks.
        final double speed = Math.min(JetConfig.maxSpeedPerTick(), ramp + JetConfig.accelerationPerTick());
        fuel.setBoostSpeed(speed);
        return new Race(true, speed);
    }

    /**
     * Regeneration: fuel comes back at a rate chosen by whether the player is still fall-flying.
     *
     * <p>The two rates are deliberately different. A glide trickles fuel back at
     * {@code fuelRefillInFlightPerSecond}, which is below the burn rate, so sustained thrust always
     * costs the player something; a player who has landed refills at the much faster
     * {@code fuelRefillOutOfFlightPerSecond}, so a normal hop back onto the elytra starts from a
     * useful tank. Both are world config values.</p>
     *
     * @param fuel     the player's tank, mutated in place
     * @param gliding  whether the player is fall-flying this tick
     * @param gameTime level game time, used for the regeneration window
     */
    public static void regenerate(final PlayerFuel fuel, final boolean gliding, final long gameTime) {
        if (fuel.isFuelFull()) {
            fuel.completeRefill();
            return;
        }
        if (fuel.getRefillStartTick() < 0L || gameTime < fuel.getRefillStartTick()) {
            return;
        }
        fuel.refill(gliding ? JetConfig.refillPerTickInFlight() : JetConfig.refillPerTickOutOfFlight());
        if (fuel.isFuelFull()) {
            fuel.completeRefill();
        }
    }
}
