package neon.nsp.data.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SectorThemeGenerator;

import neon.nsp.data.scripts.*;
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

    @Override
    public void onGameLoad(boolean newGame) {

        if (!Global.getSector().getGenericPlugins().hasPlugin(NSPSafeguard.class)) {
            Global.getSector().getGenericPlugins().addPlugin(new NSPSafeguard(), true);
        }

        if (!Global.getSector().getListenerManager().hasListenerOfClass(DerelictOddityTracker.class)) {
            Global.getSector().getListenerManager().addListener(new DerelictOddityTracker(), true);
        }
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

    @Override
    public void onNewGameAfterEconomyLoad() {

        CustomFleetsNSP.spawnFleetXIVictus();
        new NSPPeople().nsp_createPeople();
    }


    @Override
    public void onNewGameAfterProcGen() {

        SectorAPI sector = Global.getSector();

        ArrayList<String> systemBL = new ArrayList<>();
        ArrayList<String> tagBL = new ArrayList<>();
        tagBL.add(Tags.THEME_HIDDEN);
        tagBL.add(Tags.SYSTEM_ALREADY_USED_FOR_STORY);
        tagBL.add(Tags.SYSTEM_ABYSSAL);
        tagBL.add(Tags.STAR_HIDDEN_ON_MAP);
        tagBL.add("theme_d"); // if people are still running DME, i guess?
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

        // new NSPInvictaSystemGen().generate(Global.getSector());
        //  new nsp_dump_system().generate(Global.getSector());
    }

    @Override
    public void onApplicationLoad() throws Exception {
        boolean hasGraphicsLib = Global.getSettings().getModManager().isModEnabled("shaderLib");
        if (hasGraphicsLib) {
            HAS_GRAPHICSLIB = true;
            ShaderLib.init();
            // LightData.readLightDataCSV((String) "data/config/example_lights_data.csv");
            TextureData.readTextureDataCSV((String) "data/config/nsp_texture_data.csv");
            log.info("NSP shaders active");
        }
        hasMagicLib = Global.getSettings().getModManager().isModEnabled("MagicLib");

        log.info("Welcome to NSP! I'm in your hulls...");
    }

    @Override
    public void onNewGame() {
        SectorThemeGenerator.generators.add(1, new NSPThemeGenerator());
    }
}