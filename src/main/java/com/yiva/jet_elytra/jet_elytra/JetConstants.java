package com.yiva.jet_elytra.jet_elytra;

/**
 * Contract constants shared by the client and the server.
 *
 * <p>Anything which must be identical on both sides lives here as a compile-time constant.
 * Everything that only the server needs is tunable at runtime through {@link JetConfig}; the client
 * mirrors the capacity out of the synced server config.</p>
 */
public final class JetConstants {

    private JetConstants() {
    }

    /** Design capacity, used as the default before any config has been read. */
    public static final double MAX_FUEL = 200.0D;

    /**
     * Client-side view of the player's fuel, kept up to date by {@code S2C_FuelSync}.
     * Always within {@code [0, clientFuelCapacity()]}.
     */
    private static volatile double clientFuel = MAX_FUEL;

    public static double getClientFuel() {
        return clientFuel;
    }

    /**
     * Capacity as the client knows it: the synced server config when available, the design default
     * otherwise. Always at least 1 so the HUD maths can never divide by zero.
     */
    public static double clientFuelCapacity() {
        final double configured = JetConfig.fuelCapacity;
        return configured >= 1.0D ? configured : 1.0D;
    }

    public static void setClientFuel(double fuel) {
        if (Double.isNaN(fuel) || fuel < 0.0D) {
            fuel = 0.0D;
        } else {
            final double capacity = clientFuelCapacity();
            if (fuel > capacity) {
                fuel = capacity;
            }
        }
        clientFuel = fuel;
    }
}
