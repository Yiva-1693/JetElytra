package com.yiva.jet_elytra.jet_elytra.gameplay;

import com.yiva.jet_elytra.jet_elytra.JetConfig;
import com.yiva.jet_elytra.jet_elytra.capability.PlayerFuel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;

/**
 * Finds and burns firework rockets for fuel. Shared by the automatic refill and by the right-click
 * interaction, so both paths agree on ordering and on how much a rocket is worth.
 */
public final class RefuelService {

    /** Fallback flight duration for a rocket whose NBT carries no usable {@code Flight} value. */
    private static final int DEFAULT_FLIGHT = 1;

    private RefuelService() {
    }

    /** Fuel a rocket is worth, based on its flight duration (1/2/3 -> 50/100/150 by default). */
    public static int fuelValue(final ItemStack stack) {
        return JetConfig.fuelForFlightDuration(flightDuration(stack));
    }

    public static int flightDuration(final ItemStack stack) {
        final CompoundTag fireworks = stack.getTagElement(FireworkRocketItem.TAG_FIREWORKS);
        if (fireworks == null || !fireworks.contains(FireworkRocketItem.TAG_FLIGHT, 99)) {
            return DEFAULT_FLIGHT;
        }
        final int flight = fireworks.getByte(FireworkRocketItem.TAG_FLIGHT);
        return flight < 1 ? DEFAULT_FLIGHT : Math.min(flight, 3);
    }

    public static boolean isFireworkRocket(final ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof FireworkRocketItem;
    }

    /**
     * Burns one rocket from the inventory and stores its fuel.
     *
     * <p>Search order is main hand, off hand, hotbar, then main inventory, exactly as specified. The
     * hotbar is the first nine slots of the inventory, so it is scanned once.</p>
     *
     * @return true when a rocket was consumed
     */
    public static boolean consumeOneAndRefuel(final Player player, final PlayerFuel fuel) {
        final Inventory inventory = player.getInventory();

        final ItemStack mainHand = player.getMainHandItem();
        if (isFireworkRocket(mainHand)) {
            return burn(player, fuel, mainHand);
        }

        final ItemStack offHand = player.getOffhandItem();
        if (isFireworkRocket(offHand)) {
            return burn(player, fuel, offHand);
        }

        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            final ItemStack stack = inventory.getItem(slot);
            if (isFireworkRocket(stack)) {
                return burn(player, fuel, stack);
            }
        }
        for (int slot = Inventory.getSelectionSize(); slot < inventory.getContainerSize(); slot++) {
            final ItemStack stack = inventory.getItem(slot);
            if (isFireworkRocket(stack)) {
                return burn(player, fuel, stack);
            }
        }
        return false;
    }

    /**
     * Adds one rocket's worth of fuel, then removes the rocket. Fuel is stored first so that
     * "rocket consumed but tank full" cannot happen.
     *
     * <p>The rocket is consumed even in creative mode: the design document explicitly asks for no
     * creative-mode special case, and fuel is burned in creative too.</p>
     *
     * @param stack a stack that is already known to hold at least one firework rocket
     */
    public static boolean burn(final Player player, final PlayerFuel fuel, final ItemStack stack) {
        fuel.refill(fuelValue(stack));
        stack.shrink(1);
        return true;
    }

    /** Automatic top-up: only runs while the tank is at or below the configured threshold. */
    public static boolean tryAutoRefuel(final Player player, final PlayerFuel fuel) {
        if (fuel.getFuel() > JetConfig.autoRefuelThreshold) {
            return false;
        }
        return consumeOneAndRefuel(player, fuel);
    }
}
