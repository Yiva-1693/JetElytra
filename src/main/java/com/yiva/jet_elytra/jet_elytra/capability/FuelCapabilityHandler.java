package com.yiva.jet_elytra.jet_elytra.capability;

import com.yiva.jet_elytra.jet_elytra.JetConfig;
import com.yiva.jet_elytra.jet_elytra.Jet_Elytra;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Wires the fuel tank onto every player, carries it through death, and fills it up the first time a
 * player is ever seen.
 */
public final class FuelCapabilityHandler {

    private static final ResourceLocation ID = new ResourceLocation(Jet_Elytra.MODID, "fuel");

    private FuelCapabilityHandler() {
    }

    /** Mod bus: exposes the capability type to Forge so it can be resolved. */
    @Mod.EventBusSubscriber(modid = Jet_Elytra.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {

        private ModBus() {
        }

        @SubscribeEvent
        public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
            event.register(PlayerFuel.class);
        }
    }

    /** Game bus: attach / clone / initial fill. */
    @Mod.EventBusSubscriber(modid = Jet_Elytra.MODID)
    public static final class GameBus {

        private GameBus() {
        }

        @SubscribeEvent
        public static void onAttachCapabilities(final AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player) {
                event.addCapability(ID, new PlayerFuel.Provider());
            }
        }

        /** Death / dimension change: the design says fuel always comes back full. */
        @SubscribeEvent
        public static void onPlayerClone(final PlayerEvent.Clone event) {
            final Player replacement = event.getEntity();
            event.getOriginal().reviveCaps();
            final PlayerFuel.Impl fresh = replacement.getCapability(PlayerFuel.CAPABILITY)
                    .filter(PlayerFuel.Impl.class::isInstance)
                    .map(PlayerFuel.Impl.class::cast)
                    .orElse(null);
            if (fresh != null) {
                // Full tank, no leftover thrust state. The capability is brand new on the clone, so
                // this is the only place the reset has to happen. The key flag is cleared alongside the
                // ramp even though resetToFull already does it: the client only re-sends that flag when
                // the key actually changes, so a stale true would keep thrust switched on until the
                // player released W.
                fresh.resetToFull();
                fresh.setLifting(false);
                fresh.setInitialized(true);
            }
            event.getOriginal().invalidateCaps();
        }

        /** First join of a brand new player, and the plug for legacy worlds without stored fuel. */
        @SubscribeEvent
        public static void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
            final Player player = event.getEntity();
            player.getCapability(PlayerFuel.CAPABILITY).ifPresent(fuel -> {
                if (!fuel.isInitialized()) {
                    fuel.setFuel(JetConfig.fuelCapacity);
                    fuel.setInitialized(true);
                }
            });
        }
    }
}
