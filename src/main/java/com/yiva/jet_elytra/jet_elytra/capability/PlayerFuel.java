package com.yiva.jet_elytra.jet_elytra.capability;

import com.yiva.jet_elytra.jet_elytra.JetConfig;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Per-player fuel tank and thrust state.
 *
 * <p>The tank itself is server-authoritative: all mutation happens server side, and the owning client
 * only ever sees the {@code S2C_FuelSync} value, so this interface deliberately has no data-watcher
 * plumbing. The same is true of {@code boostSpeed}, with one consequence worth remembering: the
 * capability is attached per <em>entity</em>, so the client-side player has an instance of its own
 * that nothing ever writes and that always reports a zero ramp. The client learns the ramp from
 * {@code S2C_ThrustSync} instead - see {@code JetThrustState}.</p>
 */
public interface PlayerFuel {

    Capability<PlayerFuel> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {
    });

    /** Read-only fallback so a missing capability can never crash the tick loop. */
    PlayerFuel EMPTY = new PlayerFuel() {
        private double fuel = JetConfig.fuelCapacity;
        private long refillStartTick = -1L;
        private boolean lifting;
        private double boostSpeed;
        private double lastSyncedFuel = Double.NaN;
        private double lastSyncedBoostSpeed = Double.NaN;
        private boolean initialized;

        @Override
        public double getFuel() {
            return this.fuel;
        }

        @Override
        public void setFuel(final double fuel) {
            this.fuel = fuel;
        }

        @Override
        public void consume(final double amount) {
            this.fuel = Math.max(0.0D, this.fuel - amount);
        }

        @Override
        public double refill(final double amount) {
            final double added = Math.min(amount, JetConfig.fuelCapacity - this.fuel);
            this.fuel += added;
            return added;
        }

        @Override
        public boolean isFuelFull() {
            return this.fuel >= JetConfig.fuelCapacity;
        }

        @Override
        public long getRefillStartTick() {
            return this.refillStartTick;
        }

        @Override
        public void beginRefillDelay(final long now) {
            if (this.refillStartTick < 0L) {
                this.refillStartTick = now + JetConfig.refillDelayTicks;
            }
        }

        @Override
        public void blockRefill(final long now) {
            this.refillStartTick = now + JetConfig.refillDelayTicks;
        }

        @Override
        public void completeRefill() {
            this.refillStartTick = -1L;
        }

        @Override
        public boolean isLifting() {
            return this.lifting;
        }

        @Override
        public void setLifting(final boolean lifting) {
            this.lifting = lifting;
        }

        @Override
        public double getBoostSpeed() {
            return this.boostSpeed;
        }

        @Override
        public void setBoostSpeed(final double speed) {
            this.boostSpeed = speed;
        }

        @Override
        public double getLastSyncedFuel() {
            return this.lastSyncedFuel;
        }

        @Override
        public void setLastSyncedFuel(final double fuel) {
            this.lastSyncedFuel = fuel;
        }

        @Override
        public double getLastSyncedBoostSpeed() {
            return this.lastSyncedBoostSpeed;
        }

        @Override
        public void setLastSyncedBoostSpeed(final double speed) {
            this.lastSyncedBoostSpeed = speed;
        }

        @Override
        public boolean isInitialized() {
            return this.initialized;
        }

        @Override
        public void setInitialized(final boolean initialized) {
            this.initialized = initialized;
        }
    };

    /** Convenience accessor; the capability is attached to every player, so this is always live. */
    static PlayerFuel of(final Player player) {
        return player.getCapability(CAPABILITY).orElse(EMPTY);
    }

    double getFuel();

    void setFuel(double fuel);

    /** Burns {@code amount}; never drops below zero. */
    void consume(double amount);

    /** Adds {@code amount}, clamped to the capacity. Returns how much was actually stored. */
    double refill(double amount);

    boolean isFuelFull();

    /** Level game time at which regeneration becomes allowed; negative means "not armed". */
    long getRefillStartTick();

    /**
     * Arms the regeneration delay if it is not already armed. Called on the "thrust stopped"
     * transition; keeping it idempotent means a player who holds W with an empty tank is not trapped
     * in a permanently delayed refill.
     */
    void beginRefillDelay(long now);

    /**
     * Pushes the regeneration window back to {@code now + delayTicks} and keeps it there. Called on
     * every tick that thrust is running, so regeneration can never fire while fuel is being burned.
     * When thrust stops, the already-armed window is exactly the one-second delay the design asks
     * for, which is why this uses the same window as {@link #beginRefillDelay(long)}.
     */
    void blockRefill(long now);

    /** Clears the armed delay, called when the tank reaches full. */
    void completeRefill();

    /** True while the player is holding the forward key with the jet thrusting. */
    boolean isLifting();

    void setLifting(boolean lifting);

    /**
     * Per-player thrust ramp in blocks per tick, written only by {@link
     * com.yiva.jet_elytra.jet_elytra.gameplay.JetThrust} on the server. Read back by the elytra
     * velocity override on the server, and mirrored to the client through {@code S2C_ThrustSync}
     * because the client's own copy of this value is never written.
     */
    double getBoostSpeed();

    void setBoostSpeed(double speed);

    /** Fingerprint of the last value pushed to the client, so only real changes are sent. */
    double getLastSyncedFuel();

    void setLastSyncedFuel(double fuel);

    /**
     * Fingerprint of the last thrust ramp pushed to the client. Transient by design: it only exists
     * to keep the sync cheap, so it is never persisted. {@code NaN} means "nothing sent yet", which
     * forces the first tick to sync even when the ramp is zero.
     */
    double getLastSyncedBoostSpeed();

    void setLastSyncedBoostSpeed(double speed);

    /** False until the tank has been filled once, so a fresh or respawned player starts at full. */
    boolean isInitialized();

    void setInitialized(boolean initialized);

    /** Mutable tank; one instance per player, persisted inside the player's NBT. */
    final class Impl implements PlayerFuel {

        private static final String TAG_FUEL = "JetFuel";
        private static final String TAG_REFILL_START = "JetRefillStart";
        private static final String TAG_LIFTING = "JetLifting";
        private static final String TAG_BOOST = "JetBoost";
        private static final String TAG_INITIALIZED = "JetInitialized";

        private double fuel = JetConfig.fuelCapacity;
        private long refillStartTick = -1L;
        private boolean lifting;
        private double boostSpeed;
        private double lastSyncedFuel = Double.NaN;
        private double lastSyncedBoostSpeed = Double.NaN;
        private boolean initialized;

        @Override
        public double getFuel() {
            return this.fuel;
        }

        @Override
        public void setFuel(final double fuel) {
            if (Double.isNaN(fuel)) {
                this.fuel = 0.0D;
            } else {
                this.fuel = Math.max(0.0D, Math.min(JetConfig.fuelCapacity, fuel));
            }
        }

        @Override
        public void consume(final double amount) {
            setFuel(this.fuel - amount);
        }

        @Override
        public double refill(final double amount) {
            final double before = this.fuel;
            setFuel(this.fuel + amount);
            return this.fuel - before;
        }

        @Override
        public boolean isFuelFull() {
            return this.fuel >= JetConfig.fuelCapacity;
        }

        @Override
        public long getRefillStartTick() {
            return this.refillStartTick;
        }

        @Override
        public void beginRefillDelay(final long now) {
            if (this.refillStartTick < 0L) {
                this.refillStartTick = now + JetConfig.refillDelayTicks;
            }
        }

        @Override
        public void blockRefill(final long now) {
            this.refillStartTick = now + JetConfig.refillDelayTicks;
        }

        @Override
        public void completeRefill() {
            this.refillStartTick = -1L;
        }

        @Override
        public boolean isLifting() {
            return this.lifting;
        }

        @Override
        public void setLifting(final boolean lifting) {
            this.lifting = lifting;
        }

        @Override
        public double getBoostSpeed() {
            return this.boostSpeed;
        }

        @Override
        public void setBoostSpeed(final double speed) {
            this.boostSpeed = speed;
        }

        @Override
        public double getLastSyncedFuel() {
            return this.lastSyncedFuel;
        }

        @Override
        public void setLastSyncedFuel(final double fuel) {
            this.lastSyncedFuel = fuel;
        }

        @Override
        public double getLastSyncedBoostSpeed() {
            return this.lastSyncedBoostSpeed;
        }

        @Override
        public void setLastSyncedBoostSpeed(final double speed) {
            this.lastSyncedBoostSpeed = speed;
        }

        @Override
        public boolean isInitialized() {
            return this.initialized;
        }

        @Override
        public void setInitialized(final boolean initialized) {
            this.initialized = initialized;
        }

        public void save(final CompoundTag tag) {
            tag.putDouble(TAG_FUEL, this.fuel);
            tag.putLong(TAG_REFILL_START, this.refillStartTick);
            tag.putBoolean(TAG_LIFTING, this.lifting);
            tag.putBoolean(TAG_INITIALIZED, this.initialized);
        }

        public void load(final CompoundTag tag) {
            if (tag.contains(TAG_FUEL)) {
                setFuel(tag.getDouble(TAG_FUEL));
            }
            this.refillStartTick = tag.contains(TAG_REFILL_START) ? tag.getLong(TAG_REFILL_START) : -1L;
            this.lifting = tag.getBoolean(TAG_LIFTING);
            // The thrust ramp is per-flight session state, never restored: a relog must not resume
            // the speed the player happened to log out at.
            this.boostSpeed = 0.0D;
            this.initialized = tag.getBoolean(TAG_INITIALIZED);
            this.lastSyncedFuel = Double.NaN;
            this.lastSyncedBoostSpeed = Double.NaN;
        }

        /** Wipes thrust state, used when a fresh or respawned player starts at full fuel. */
        public void resetToFull() {
            setFuel(JetConfig.fuelCapacity);
            this.refillStartTick = -1L;
            this.lifting = false;
            this.boostSpeed = 0.0D;
            this.lastSyncedFuel = Double.NaN;
            this.lastSyncedBoostSpeed = Double.NaN;
        }
    }

    /** Attaches the tank to a player entity. */
    final class Provider implements ICapabilitySerializable<CompoundTag> {

        private final Impl backend = new Impl();
        private final LazyOptional<PlayerFuel> optional = LazyOptional.of(() -> this.backend);

        public Impl backend() {
            return this.backend;
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(final @NotNull Capability<T> cap, final @Nullable Direction side) {
            return CAPABILITY.orEmpty(cap, this.optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            final CompoundTag tag = new CompoundTag();
            this.backend.save(tag);
            return tag;
        }

        @Override
        public void deserializeNBT(final CompoundTag nbt) {
            this.backend.load(nbt);
        }
    }
}
