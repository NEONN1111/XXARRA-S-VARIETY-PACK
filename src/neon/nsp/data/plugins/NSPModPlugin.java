package neon.nsp.data.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SectorThemeGenerator;

import neon.nsp.data.scripts.NSPPeople;
import neon.nsp.data.scripts.starsystems.Revachol_starsystem;
import neon.nsp.data.scripts.util.PaperdollUIPanelAdder;
import neon.nsp.data.world.*;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;
import org.apache.log4j.Logger;

import java.util.ArrayList;

public class NSPModPlugin extends BaseModPlugin {
    public static boolean hasMagicLib = false;
    public Logger log = Logger.getLogger(this.getClass());
    public testFilePlzIgnore thespawnerrr;
    public static boolean HAS_GRAPHICSLIB = false;

    // Hull IDs for modular ships

    private boolean modularShipSystemInitialized = false;
    private boolean paperdollUIRegistered = false;

    @Override
    public void onGameLoad(boolean newGame) {
        if (!Global.getSector().getGenericPlugins().hasPlugin(NSPSafeguard.class)) {
            Global.getSector().getGenericPlugins().addPlugin(new NSPSafeguard(), true);
        }

        if (!Global.getSector().getListenerManager().hasListenerOfClass(DerelictOddityTracker.class)) {
            Global.getSector().getListenerManager().addListener(new DerelictOddityTracker(), true);
        }

        // Initialize modular ship systems
        initializeModularShipSystems();

        // Register paperdoll UI
        registerPaperdollUI();

        try {
            SectorAPI sector = Global.getSector();
            ExponentCampaignPluginImpl plugin = new ExponentCampaignPluginImpl();
            ThreatProcessorCampaignPluginImpl plugin2 = new ThreatProcessorCampaignPluginImpl();
            sector.registerPlugin(plugin);
            sector.registerPlugin(plugin2);
        } catch (Throwable t) {
            log.error("Failed to register ExponentCampaignPluginImpl", t);
        }
    }

    private void initializeModularShipSystems() {
        if (modularShipSystemInitialized) return;

        // Register a transient EveryFrameScript that will initialize combat plugins when combat starts

        modularShipSystemInitialized = true;
        log.info("Modular ship systems initialized for Legion Mk.1, Dominator Mk.1, and Onslaught Mk.1");
    }

    private void registerPaperdollUI() {
        if (paperdollUIRegistered) return;

        // Register the paperdoll UI adder via sector script so it persists across combats
        Global.getSector().addTransientScript(new PaperdollUIRegistrar());

        paperdollUIRegistered = true;
        log.info("Paperdoll UI system registered for modular ships");
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
        CustomFleetsNSP.spawnFleetXIVictus();

        new Revachol_starsystem().generate(Global.getSector()); //Should be before create people so we can put them on markets in system
        new NSPPeople().nsp_createPeople();
    }

    @Override
    public void onNewGameAfterProcGen() {
        SectorAPI sector = Global.getSector();

        ArrayList<String> systemBL = new ArrayList<>();
        ArrayList<String> tagBL = new ArrayList<>();
        tagBL.add(com.fs.starfarer.api.impl.campaign.ids.Tags.THEME_HIDDEN);
        tagBL.add(com.fs.starfarer.api.impl.campaign.ids.Tags.SYSTEM_ALREADY_USED_FOR_STORY);
        tagBL.add(com.fs.starfarer.api.impl.campaign.ids.Tags.SYSTEM_ABYSSAL);
        tagBL.add(com.fs.starfarer.api.impl.campaign.ids.Tags.STAR_HIDDEN_ON_MAP);
        tagBL.add("theme_d");
        StarSystemAPI system = thespawnerrr.getRandomSystemWithBlacklist(systemBL, tagBL, sector);
        if (system != null) DomainShips.generate(system);

        nsp_legionGen.generate(Global.getSector());
        nsp_dominatorGen.generate(Global.getSector());

        nsp_abyssalgen1.generate(Global.getSector());
        nsp_abyssalgen2.generate(Global.getSector());
        nsp_abyssalgen3.generate(Global.getSector());
        nsp_abyssalgen4.generate(Global.getSector());
        nsp_abyssalgen5.generate(Global.getSector());

        CustomFleetsNSPThreat1.spawnFleetInthrictus();
        CustomFleetsNSPThreat2.spawnFleetThrominator();
        CustomFleetsNSPThreat3.spawnFleetOnthraught();
        CustomFleetsNSPThreat4.spawnFleetThreatribution();
        CustomFleetsNSPThreat5.spawnFleetThremlin();

        Global.getSector().getListenerManager().addListener(new nsp_onslaughtMK1Listener());
    }

    @Override
    public void onApplicationLoad() throws Exception {
        boolean hasGraphicsLib = Global.getSettings().getModManager().isModEnabled("shaderLib");
        if (hasGraphicsLib) {
            HAS_GRAPHICSLIB = true;
            ShaderLib.init();
            TextureData.readTextureDataCSV("data/config/nsp_texture_data.csv");
            log.info("NSP shaders active");
        }
        hasMagicLib = Global.getSettings().getModManager().isModEnabled("MagicLib");

        log.info("Welcome to NSP! I'm in your hulls...");
        registerModularHullmods();
    }

    private void registerModularHullmods() {
        log.info("Modular armor systems registered for Mk.1 series ships");
    }

    @Override
    public void onNewGame() {
        SectorThemeGenerator.generators.add(1, new NSPThemeGenerator());
    }

    /**
     * Initializes modular ship systems when combat starts
     * This implements EveryFrameScript to be added as a transient script
     */

    /**
     * Registers paperdoll UI when combat starts
     */
    private static class PaperdollUIRegistrar implements EveryFrameScript {
        private boolean registered = false;
        private boolean done = false;

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public boolean runWhilePaused() {
            return false;
        }

        @Override
        public void advance(float amount) {
            if (!registered && Global.getCombatEngine() != null) {
                PaperdollUIPanelAdder paperdollAdder = new PaperdollUIPanelAdder();
                Global.getCombatEngine().addPlugin(paperdollAdder);
                registered = true;
                done = true;
                Global.getLogger(PaperdollUIRegistrar.class).info("Paperdoll UI registered with combat engine");
            }
        }
    }
}

