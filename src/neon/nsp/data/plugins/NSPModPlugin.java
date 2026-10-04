package neon.nsp.data.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SectorThemeGenerator;

import lunalib.lunaSettings.LunaSettings;
import neon.nsp.data.ids.NSP_People;
import neon.nsp.data.ids.NSP_lunasettings;
import neon.nsp.data.plugins.AICoreOfficerPlugins.NSP_ExponentCore_CampaignPluginImpl;
import neon.nsp.data.plugins.AICoreOfficerPlugins.NSP_ThreatProcessor_CampaignPluginImpl;
import neon.nsp.data.scripts.starsystems.Revachol_starsystem;
import neon.nsp.data.scripts.util.PaperdollUIPanelAdder;
import neon.nsp.data.world.*;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;
import org.apache.log4j.Logger;

import java.util.ArrayList;

public class NSPModPlugin extends BaseModPlugin {
    public Logger log = Logger.getLogger(this.getClass());

    //Mod interaction checks
    public static boolean hasMagicLib = false;
    public static boolean hasGraphicsLib = false;
    private boolean use_lunasettings = false;

    // Hull IDs for modular ships
    private boolean modularShipSystemInitialized = false;
    private boolean paperdollUIRegistered = false;

    @Override
    public void onGameLoad(boolean newGame) {
        initializeModularShipSystems(); // Functionally does nothing
        registerPaperdollUI(); //Transient, so need to be re-added on each reload

        //If no lunalib: if - true, if yes lunalib: checks settings
        if (!use_lunasettings || Boolean.TRUE.equals(LunaSettings.getBoolean("NSP", NSP_lunasettings.UNIQUE_SENTINELS))) {
            //As it just overrides vanilla settings should work by just preventing override
            if (!Global.getSector().getGenericPlugins().hasPlugin(NSPSafeguard.class)) {
                Global.getSector().getGenericPlugins().addPlugin(new NSPSafeguard(), true);
            }
        }

        if (!Global.getSector().getListenerManager().hasListenerOfClass(DerelictOddityTracker.class)) {
            Global.getSector().getListenerManager().addListener(new DerelictOddityTracker(), true);
        }

        try {
            //AI cores campaign plugins
            Global.getSector().registerPlugin(new NSP_ExponentCore_CampaignPluginImpl());
            Global.getSector().registerPlugin(new NSP_ThreatProcessor_CampaignPluginImpl());
        } catch (Throwable t) {
            log.error("Failed to register NSP_ExponentCore_CampaignPluginImpl", t);
        }
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
        NSP_CustomFleets_XIVictus.spawnFleetXIVictus();

        new Revachol_starsystem().generate(Global.getSector()); //Should be before create people so we can put them on markets in system
        new NSP_People().nsp_createPeople();
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
        StarSystemAPI system = getRandomStarSystemsWithBlacklist.getRandomSystemWithBlacklist(systemBL, tagBL, sector);
        if (system != null) DomainShips.generate(system);

        //V: probably can be moved to separate gen class to not clutter plugin? Or at least be more clearly named and organised
        nsp_legionGen.generate(Global.getSector());
        nsp_dominatorGen.generate(Global.getSector());

        nsp_abyssalgen1.generate(Global.getSector());
        nsp_abyssalgen2.generate(Global.getSector());
        nsp_abyssalgen3.generate(Global.getSector());
        nsp_abyssalgen4.generate(Global.getSector());
        nsp_abyssalgen5.generate(Global.getSector());

        NSP_CustomFleets_Threat1_Inthrictus.spawnFleetInthrictus();
        NSP_CustomFleets_Threat2_Throminator.spawnFleetThrominator();
        NSP_CustomFleets_Threat3_Onthraught.spawnFleetOnthraught();
        NSP_CustomFleets_Threat4_Threatribution.spawnFleetThreatribution();
        NSP_CustomFleets_Threat5_Thremlin.spawnFleetThremlin();

        Global.getSector().getListenerManager().addListener(new nsp_onslaughtMK1Listener());
    }

    @Override
    public void onApplicationLoad() throws Exception {
        hasGraphicsLib = Global.getSettings().getModManager().isModEnabled("shaderLib");
        hasMagicLib = Global.getSettings().getModManager().isModEnabled("MagicLib");
        use_lunasettings = Global.getSettings().getModManager().isModEnabled("lunalib");

        if (hasGraphicsLib) {
            ShaderLib.init();
            TextureData.readTextureDataCSV("data/config/nsp_texture_data.csv");
            log.info("NSP shaders active");
        }

        log.info("Welcome to XVP! I'm in your hulls...");
        registerModularHullmods();
    }


    @Override
    public void onNewGame() {
        SectorThemeGenerator.generators.add(1, new NSPThemeGenerator());
    }

    //V: Moved modular stuff after plugin specific functions
    //Outdated or unimplemented? Currently, plugin initialised in config
    private void initializeModularShipSystems() {
        if (modularShipSystemInitialized) return;

        // Register a transient EveryFrameScript that will initialize combat plugins when combat starts
        modularShipSystemInitialized = true;
        log.info("Modular ship systems initialized for Legion Mk.1, Dominator Mk.1, and Onslaught Mk.1");
    }

    private void registerModularHullmods() {
        log.info("Modular armor systems registered for Mk.1 series ships");
    }

    private void registerPaperdollUI() {
        if (paperdollUIRegistered) return;

        // Register the paperdoll UI adder via sector script so it persists across combats
        Global.getSector().addTransientScript(new PaperdollUIRegistrar());

        paperdollUIRegistered = true;
        log.info("Paperdoll UI system registered for modular ships");
    }

    /* Registers paperdoll UI when combat starts */
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

