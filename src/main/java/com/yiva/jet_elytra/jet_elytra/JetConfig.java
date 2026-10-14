package com.yiva.jet_elytra.jet_elytra;

import com.yiva.jet_elytra.jet_elytra.gameplay.JetDiagnostics;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Every tunable from the design document, exposed as a server-side (world) config so the values can
 * be dialled in during play-testing.
 *
 * <p>The static fields below hold the live values and default to the calibrated design numbers, so
 * they are always usable even when no config file has been loaded yet (for example on a client that
 * is still on the main menu).</p>
 */
@Mod.EventBusSubscriber(modid = Jet_Elytra.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class JetConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.DoubleValue FUEL_CAPACITY = BUILDER
            .comment("Maximum fuel a player can hold.")
            .defineInRange("fuelCapacity", 200.0D, 1.0D, 100000.0D);

    public static final ForgeConfigSpec.DoubleValue FUEL_CONSUMPTION_PER_SECOND = BUILDER
            .comment("Fuel burned per second while the jet is thrusting (25/s = 1.25 per tick).")
            .defineInRange("fuelConsumptionPerSecond", 25.0D, 0.0D, 10000.0D);

    public static final ForgeConfigSpec.DoubleValue FUEL_REFILL_IN_FLIGHT_PER_SECOND = BUILDER
            .comment("Fuel regenerated per second while the player is fall-flying (gliding).")
            .defineInRange("fuelRefillInFlightPerSecond", 12.0D, 0.0D, 10000.0D);

    public static final ForgeConfigSpec.DoubleValue FUEL_REFILL_OUT_OF_FLIGHT_PER_SECOND = BUILDER
            .comment("Fuel regenerated per second while the player is NOT fall-flying (on the ground,",
                    "walking, riding). This is the faster of the two so a landed player can take off again.")
            .defineInRange("fuelRefillOutOfFlightPerSecond", 30.0D, 0.0D, 10000.0D);

    public static final ForgeConfigSpec.IntValue REFILL_DELAY_TICKS = BUILDER
            .comment("Ticks to wait after thrust ends before regeneration begins.")
            .defineInRange("refillDelayTicks", 20, 0, 72000);

    public static final ForgeConfigSpec.DoubleValue MIN_FUEL_TO_START = BUILDER
            .comment("Fuel needed to *enter* thrust mode. Thrust already running keeps burning down to 0.")
            .defineInRange("minFuelToStart", 20.0D, 0.0D, 100000.0D);

    public static final ForgeConfigSpec.DoubleValue AUTO_REFUEL_THRESHOLD = BUILDER
            .comment("Fuel at or below which a firework rocket is consumed from the inventory automatically.")
            .defineInRange("autoRefuelThreshold", 50.0D, 0.0D, 100000.0D);

    public static final ForgeConfigSpec.DoubleValue MAX_SPEED_BLOCKS_PER_SECOND = BUILDER
            .comment("Terminal thrust speed in blocks per second (33.5 b/s = 1.675 blocks per tick).")
            .defineInRange("maxSpeedBlocksPerSecond", 33.5D, 0.0D, 1000.0D);

    public static final ForgeConfigSpec.IntValue ACCELERATION_TICKS = BUILDER
            .comment("Ticks spent ramping from 0 to the terminal speed.")
            .defineInRange("accelerationTicks", 30, 1, 72000);

    public static final ForgeConfigSpec.IntValue PARTICLE_COUNT = BUILDER
            .comment("Firework trail particles spawned behind a thrusting player each tick.")
            .defineInRange("particleCount", 2, 0, 64);

    public static final ForgeConfigSpec.DoubleValue PARTICLE_SPREAD = BUILDER
            .comment("Random offset applied to each trail particle, in blocks.")
            .defineInRange("particleSpread", 0.3D, 0.0D, 8.0D);

    public static final ForgeConfigSpec.IntValue SYNC_INTERVAL_TICKS = BUILDER
            .comment("How often the server pushes the fuel value to the owning client.")
            .defineInRange("syncIntervalTicks", 5, 1, 200);

    public static final ForgeConfigSpec.IntValue FIREWORK_FUEL_FLIGHT_1 = BUILDER
            .comment("Fuel granted by a flight-duration 1 firework rocket.")
            .defineInRange("fireworkFuelFlight1", 50, 0, 100000);

    public static final ForgeConfigSpec.IntValue FIREWORK_FUEL_FLIGHT_2 = BUILDER
            .comment("Fuel granted by a flight-duration 2 firework rocket.")
            .defineInRange("fireworkFuelFlight2", 100, 0, 100000);

    public static final ForgeConfigSpec.IntValue FIREWORK_FUEL_FLIGHT_3 = BUILDER
            .comment("Fuel granted by a flight-duration 3 firework rocket.")
            .defineInRange("fireworkFuelFlight3", 150, 0, 100000);

    public static final ForgeConfigSpec.BooleanValue DEBUG_LOG = BUILDER
            .comment("Write logs/jet_elytra-diagnostic.log: one line per tick covering the whole",
                    "input -> packet -> thrust -> velocity chain. Turn off once the behaviour is right.")
            .define("debugLog", false);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // ---- live values, used by the gameplay code ----

    public static double fuelCapacity = 200.0D;
    public static double fuelConsumptionPerSecond = 25.0D;
    public static double fuelRefillInFlightPerSecond = 12.0D;
    public static double fuelRefillOutOfFlightPerSecond = 30.0D;
    public static int refillDelayTicks = 20;
    public static double minFuelToStart = 20.0D;
    public static double autoRefuelThreshold = 50.0D;
    public static double maxSpeedBlocksPerSecond = 33.5D;
    public static int accelerationTicks = 30;
    public static int particleCount = 2;
    public static double particleSpread = 0.3D;
    public static int syncIntervalTicks = 5;
    public static int fireworkFuelFlight1 = 50;
    public static int fireworkFuelFlight2 = 100;
    public static int fireworkFuelFlight3 = 150;
    public static boolean debugLog = false;

    private JetConfig() {
    }

    /** Fuel burned per tick while thrusting. */
    public static double consumptionPerTick() {
        return fuelConsumptionPerSecond / 20.0D;
    }

    /**
     * Fuel regenerated per tick while fall-flying. Slower than the grounded rate on purpose: a glide
     * should not be able to sustain the jet indefinitely without the player spending rockets.
     */
    public static double refillPerTickInFlight() {
        return fuelRefillInFlightPerSecond / 20.0D;
    }

    /** Fuel regenerated per tick while not fall-flying; the faster of the two rates. */
    public static double refillPerTickOutOfFlight() {
        return fuelRefillOutOfFlightPerSecond / 20.0D;
    }

    /** Terminal speed in blocks per tick. */
    public static double maxSpeedPerTick() {
        return maxSpeedBlocksPerSecond / 20.0D;
    }

    /** Per-tick speed increment applied while ramping up. */
    public static double accelerationPerTick() {
        return maxSpeedPerTick() / Math.max(1, accelerationTicks);
    }

    /** Fuel granted by a rocket of the given flight duration; anything unexpected counts as level 1. */
    public static int fuelForFlightDuration(int flight) {
        return switch (flight) {
            case 2 -> fireworkFuelFlight2;
            case 3 -> fireworkFuelFlight3;
            default -> fireworkFuelFlight1;
        };
    }

    @SubscribeEvent
    static void onConfigLoad(final ModConfigEvent event) {
        load();
    }

    @SubscribeEvent
    static void onConfigReload(final ModConfigEvent.Reloading event) {
        load();
    }

    /** Pulls the live values out of the spec, falling back to the design numbers when unavailable. */
    public static void load() {
        try {
            fuelCapacity = FUEL_CAPACITY.get();
            fuelConsumptionPerSecond = FUEL_CONSUMPTION_PER_SECOND.get();
            fuelRefillInFlightPerSecond = FUEL_REFILL_IN_FLIGHT_PER_SECOND.get();
            fuelRefillOutOfFlightPerSecond = FUEL_REFILL_OUT_OF_FLIGHT_PER_SECOND.get();
            refillDelayTicks = REFILL_DELAY_TICKS.get();
            minFuelToStart = MIN_FUEL_TO_START.get();
            autoRefuelThreshold = AUTO_REFUEL_THRESHOLD.get();
            maxSpeedBlocksPerSecond = MAX_SPEED_BLOCKS_PER_SECOND.get();
            accelerationTicks = ACCELERATION_TICKS.get();
            particleCount = PARTICLE_COUNT.get();
            particleSpread = PARTICLE_SPREAD.get();
            syncIntervalTicks = SYNC_INTERVAL_TICKS.get();
            fireworkFuelFlight1 = FIREWORK_FUEL_FLIGHT_1.get();
            fireworkFuelFlight2 = FIREWORK_FUEL_FLIGHT_2.get();
            fireworkFuelFlight3 = FIREWORK_FUEL_FLIGHT_3.get();
            debugLog = DEBUG_LOG.get();
            JetDiagnostics.enabled = debugLog;
        } catch (final IllegalStateException notLoadedYet) {
            // Config not available on this side yet - keep the defaults.
        }
    }
}
