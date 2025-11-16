package data.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.Misc;
import data.scripts.*;
import data.world.nsp_dominatorGen;
import data.world.testFilePlzIgnore;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;
import org.apache.log4j.Logger;
import data.world.DomainShips;
import data.world.nsp_legionGen;
import data.world.nsp_onslaughtMK1Listener;

import java.util.ArrayList;


public class NSPModPlugin extends BaseModPlugin {

    public Logger log = Logger.getLogger(this.getClass());
    public testFilePlzIgnore thespawnerrr;
    public static boolean HAS_GRAPHICSLIB = false;

    public void onGameLoad(boolean newGame) {
        if (!Global.getSector().getGenericPlugins().hasPlugin(NSPSafeguard.class)) {
            Global.getSector().getGenericPlugins().addPlugin(new NSPSafeguard(), true);
        }
        if (!Global.getSector().getListenerManager().hasListenerOfClass(DerelictOddityTracker.class)) {
            Global.getSector().getListenerManager().addListener(new DerelictOddityTracker(), true);
        }
        try {
            SectorAPI sector = Global.getSector();
            InvictaCampaignPluginImpl plugin = new InvictaCampaignPluginImpl();
            sector.registerPlugin(plugin);
        } catch (Throwable t) {
        }
        try {
            SectorAPI sector = Global.getSector();
            ExponentCampaignPluginImpl plugin = new ExponentCampaignPluginImpl();
            sector.registerPlugin(plugin);
        } catch (Throwable t) {
        }
    }

    public void onNewGameAfterEconomyLoad() {
        CustomFleetsNSP.spawnFleetXIVictus();
        CustomFleetsNSP_Templar.spawnTemplar();
        CustomFleetsNSP_Gate.spawnNemetor();
        new nsp_people().nsp_createPeople();
        new tll_people().createTLLPeople();
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
       Global.getSector().getListenerManager().addListener(new nsp_onslaughtMK1Listener());
       new nsp_gen().generate(Global.getSector());
       new tll_gen().generate(Global.getSector());
       new tll_gen2().generate(Global.getSector());
}
    public static
    void Tll_Relations() {
        FactionAPI tll = Global.getSector().getFaction("tll");

        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            if (tll.equals(faction) || (faction.isPlayerFaction() && tll.getId().equals(Misc.getCommissionFactionId())))
                continue;
            tll.setRelationship(faction.getId(), -0.2f);
        }

        tll.setRelationship(Factions.LUDDIC_CHURCH, -0.2f);
        tll.setRelationship(Factions.LUDDIC_PATH, -0.9f);
        tll.setRelationship(Factions.TRITACHYON, -0.4f);
        tll.setRelationship(Factions.PERSEAN, -0.3f);
        tll.setRelationship(Factions.PIRATES, -0.6f);
        tll.setRelationship(Factions.INDEPENDENT, 0.7f);
        tll.setRelationship(Factions.DIKTAT, -0.5f);
        tll.setRelationship(Factions.LIONS_GUARD, -0.5f);
        tll.setRelationship(Factions.HEGEMONY, -0.6f);
        tll.setRelationship(Factions.REMNANTS, 0.4f);
        tll.setRelationship(Factions.PLAYER, -0.4f);

        if (Global.getSettings().getModManager().isModEnabled("blade_breakers")) {
            tll.setRelationship("blade_breakers", -0.7f);
        }
        if (Global.getSettings().getModManager().isModEnabled("exipirated")) {
            tll.setRelationship("exipirated", -0.6f);
        }
        if (Global.getSettings().getModManager().isModEnabled("gmda")) {
            tll.setRelationship("gmda", -0.6f);
        }
        if (Global.getSettings().getModManager().isModEnabled("gmda_patrol")) {
            tll.setRelationship("gmda_patrol", -0.6f);
        }
        if (Global.getSettings().getModManager().isModEnabled("draco")) {
            tll.setRelationship("draco", -0.6f);
        }
        if (Global.getSettings().getModManager().isModEnabled("fang")) {
            tll.setRelationship("fang", -0.6f);
        }
        if (Global.getSettings().getModManager().isModEnabled("HMI")) {
            tll.setRelationship("mess", -0.8f);
        }
        if (Global.getSettings().getModManager().isModEnabled("pigeonpun_projectsolace")) {
            tll.setRelationship("projectsolace", 0.5f);
        }
        if (Global.getSettings().getModManager().isModEnabled("tahlan")) {
            tll.setRelationship("tahlan_legioinfernalis", -0.8f);
        }
        if (Global.getSettings().getModManager().isModEnabled("diableavionics")) {
            tll.setRelationship("diableavionics", -0.7f);
        }
        if (Global.getSettings().getModManager().isModEnabled("scalartech")) {
            tll.setRelationship("scalartech", 0f);
        }
        if (Global.getSettings().getModManager().isModEnabled("HIVER")) {
            tll.setRelationship("HIVER", -0.7f);
        }
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
        log.info("Welcome to NSP! I'm in your hulls...");
    }
}
