package com.yiva.jet_elytra.jet_elytra;

import com.mojang.logging.LogUtils;
import com.yiva.jet_elytra.jet_elytra.network.JetNetwork;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Jet Elytra - hold the vanilla forward key while gliding to burn fuel for thrust.
 *
 * <p>Everything that matters is server authoritative: the client only reports the forward key and
 * draws the fuel bar. See the design document for the full mechanic breakdown.</p>
 */
@Mod(Jet_Elytra.MODID)
public class Jet_Elytra {

    public static final String MODID = "jet_elytra";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Jet_Elytra() {
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        // World-scoped config: server authoritative, synced to clients by Forge.
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, JetConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(JetNetwork::register);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientSetup::init);
        LOGGER.info("Jet Elytra loaded: max speed {} blocks/s, {} fuel per second.",
                JetConfig.maxSpeedBlocksPerSecond, JetConfig.fuelConsumptionPerSecond);
    }

    @SubscribeEvent
    public void onServerStarting(final ServerStartingEvent event) {
        LOGGER.info("Jet Elytra ready on the server side.");
    }

    /** Keeps every client-only class reference out of the common class. */
    private static final class ClientSetup {

        private ClientSetup() {
        }

        static void init() {
            LOGGER.info("[JetElytra] client setup reached: HUD and key polling are live.");
        }
    }
}
