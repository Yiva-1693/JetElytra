package com.yiva.jet_elytra.jet_elytra.network;

import java.util.Map;
import java.util.WeakHashMap;

/** Per-connection token bucket, shared by all client input packet types. */
final class ClientPacketRateLimiter {

    private static final double BURST = 80.0D;
    private static final double TOKENS_PER_NANOSECOND = 40.0D / 1_000_000_000.0D;

    // Values never retain their connection, so disconnected clients can be garbage collected.
    private final Map<Object, Budget> budgets = new WeakHashMap<>();

    synchronized boolean allow(final Object connection, final long now) {
        final Budget budget = this.budgets.computeIfAbsent(connection, ignored -> new Budget(now));
        if (budget.blocked) {
            return false;
        }
        final long elapsed = now - budget.updatedAt;
        budget.updatedAt = now;
        budget.tokens = Math.min(BURST, budget.tokens + Math.max(0L, elapsed) * TOKENS_PER_NANOSECOND);
        if (budget.tokens < 1.0D) {
            budget.blocked = true;
            return false;
        }
        budget.tokens -= 1.0D;
        return true;
    }

    private static final class Budget {
        private double tokens = BURST;
        private long updatedAt;
        private boolean blocked;

        private Budget(final long now) {
            this.updatedAt = now;
        }
    }
}
