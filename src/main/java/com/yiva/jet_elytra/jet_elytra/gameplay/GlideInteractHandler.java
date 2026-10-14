package com.yiva.jet_elytra.jet_elytra.gameplay;

import com.yiva.jet_elytra.jet_elytra.Jet_Elytra;
import com.yiva.jet_elytra.jet_elytra.capability.PlayerFuel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Intercepts the vanilla "boost while gliding" behaviour of firework rockets.
 *
 * <p>Only the gliding case is touched. Ground use, dispensers, crossbows and every other mod keep
 * their normal firework behaviour.</p>
 */
@Mod.EventBusSubscriber(modid = Jet_Elytra.MODID)
public final class GlideInteractHandler {

    private GlideInteractHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public static void onRightClickItem(final PlayerInteractEvent.RightClickItem event) {
        final Player player = event.getEntity();
        if (!player.isFallFlying()) {
            return;
        }

        final ItemStack stack = event.getItemStack();
        if (!RefuelService.isFireworkRocket(stack)) {
            return;
        }

        // Gliding + firework rocket: vanilla propulsion is always replaced by refuelling.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (event.getLevel().isClientSide()) {
            return;
        }

        final PlayerFuel fuel = PlayerFuel.of(player);
        if (fuel.isFuelFull()) {
            // Tank already full: the rocket is not consumed and nothing happens.
            return;
        }

        RefuelService.burn(player, fuel, stack);
        JetFlightHandler.syncNow(player);
    }
}
