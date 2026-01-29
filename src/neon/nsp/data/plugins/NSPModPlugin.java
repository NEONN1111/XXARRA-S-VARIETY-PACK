package neon.nsp.data.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.Misc;
import neon.nsp.data.listeners.DerelictBattleListener;
import neon.nsp.data.plugins.secgen.NSPInvictaSystemGen;
import neon.nsp.data.scripts.*;
import neon.nsp.data.world.nsp_dominatorGen;
import neon.nsp.data.world.testFilePlzIgnore;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;
import org.apache.log4j.Logger;
import neon.nsp.data.world.DomainShips;
import neon.nsp.data.world.nsp_legionGen;
import neon.nsp.data.world.nsp_onslaughtMK1Listener;

import java.util.ArrayList;


public class NSPModPlugin extends BaseModPlugin {
    public static boolean hasMagicLib = false;
    public static final String NSP_LUMINANCE_MISSILE_ID = "nsp_luminancemissile";
    public Logger log = Logger.getLogger(this.getClass());
    public testFilePlzIgnore thespawnerrr;
    public static boolean HAS_GRAPHICSLIB = false;

    @Override
    public void onGameLoad(boolean newGame) {
        // NSP initialization
        if (!Global.getSector().getGenericPlugins().hasPlugin(NSPSafeguard.class)) {
            Global.getSector().getGenericPlugins().addPlugin(new NSPSafeguard(), true);
        }
        if (!Global.getSector().getListenerManager().hasListenerOfClass(DerelictOddityTracker.class)) {
            Global.getSector().getListenerManager().addListener(new DerelictOddityTracker(), true);
        }
        if (!Global.getSector().getListenerManager().hasListenerOfClass(DerelictBattleListener.class)) {
            Global.getSector().getListenerManager().addListener(new DerelictBattleListener(), true);
        }
        try {
            SectorAPI sector = Global.getSector();
            InvictaCampaignPluginImpl plugin = new InvictaCampaignPluginImpl();
            sector.registerPlugin(plugin);
        } catch (Throwable t) {
            log.error("Failed to register InvictaCampaignPluginImpl", t);
        }
        try {
            SectorAPI sector = Global.getSector();
            ExponentCampaignPluginImpl plugin = new ExponentCampaignPluginImpl();
            sector.registerPlugin(plugin);
        } catch (Throwable t) {
            log.error("Failed to register ExponentCampaignPluginImpl", t);
        }
                     }




        // Derelict Start initialization
        //Global.getSector().registerPlugin((CampaignPlugin) new NSP_PlayerCore());

        //if (Global.getSector().getMemoryWithoutUpdate().getBoolean("$ds_nexusStart")) {
           // List<String> variants = new ArrayList<>(Global.getSettings().getAllVariantIds());

            //for (String shipId : Global.getSector().getFaction(Factions.DERELICT).getKnownShips()) {
               // if (!Global.getSettings().getHullSpec(shipId).getManufacturer().equals("Explorarium") &&
                      //  !shipId.equals("rat_genesis")) {
                    // Vague attempt to force remnants to re-learn hulls on save load if they don't have a default role
                   // String role = "combatSmall";
                    //com.fs.starfarer.api.combat.ShipAPI.HullSize hullSize = Global.getSettings().getHullSpec(shipId).getHullSize();
                    //switch (hullSize) {
                    //    case CAPITAL_SHIP:
                    //        role = "combatCapital";
                    //        break;
                    //    case CRUISER:
                    //        role = "combatLarge";
                    //        break;
                    //    case DESTROYER:
                    //        role = "combatMedium";
                    //        break;
                    //    default:
                    //        role = "combatSmall";
                    //}
                    //
                    //for (String variantId : variants) {
                    //    if (Global.getSettings().getVariant(variantId).getHullSpec().getHullId().equals(shipId)) {
                    //        Global.getSettings().addDefaultEntryForRole(role, variantId, 0f);
                    //        Global.getSettings().addEntryForRole(Factions.DERELICT, role, variantId, 0.5f);
                        //}
                   // }
               // }
            //}

            //if (!Global.getSector().getListenerManager().hasListenerOfClass(NSP_NexusRestocker.class)) {
                //Global.getSector().getListenerManager().addListener(new NSP_NexusRestocker());
            //}

            // (newGame) {
            //  FactionAPI player = Global.getSector().getFaction(Factions.PLAYER);
            //  FactionAPI remmy = Global.getSector().getFaction(Factions.DERELICT);
            //
            //  //boolean peacefulMode = LunaSettings.getBoolean("zz derelictstart", "peacefulMode");
            //
            //  for (FactionAPI faction : Global.getSector().getAllFactions()) {
            //      String factionId = faction.getId();
            //      if (factionId.equals(Factions.PLAYER)) continue;
            //      if (factionId.equals(Factions.DERELICT)) continue;
            //      if (factionId.equals("nex_derelict")) continue;
            //      if (factionId.equals(Factions.REMNANTS)) continue;
            //      if (factionId.equals(Factions.OMEGA)) continue;
            //      if (factionId.equals(Factions.HEGEMONY)) continue;
            //      if (factionId.equals(Factions.TRITACHYON)) continue;
            //      if (factionId.equals("sotf_dustkeepers")) continue;
            //      if (factionId.equals("sotf_dustkeepers_proxies")) continue;
            //      if (factionId.equals("sotf_sierra_faction")) continue;
            //      if (factionId.equals("sotf_dreaminggestalt")) continue;

                    //if (peacefulMode) {
                        //remmy.setRelationship(factionId, 0f);
                        //remmy.setRelationship(Factions.HEGEMONY, 0f);
                        //remmy.setRelationship(Factions.TRITACHYON, 0f);
                        //remmy.setRelationship(Factions.PIRATES, DiplomacyManager.STARTING_RELATIONSHIP_HOSTILE);
                        //player.setRelationship(Factions.PIRATES, DiplomacyManager.STARTING_RELATIONSHIP_HOSTILE);
                        //remmy.setRelationship(Factions.LUDDIC_PATH, DiplomacyManager.STARTING_RELATIONSHIP_HOSTILE);
                        //player.setRelationship(Factions.LUDDIC_PATH, DiplomacyManager.STARTING_RELATIONSHIP_HOSTILE);
                    //} else {
                       // player.setRelationship(factionId, DiplomacyManager.STARTING_RELATIONSHIP_HOSTILE);
                       // remmy.setRelationship(factionId, DiplomacyManager.STARTING_RELATIONSHIP_HOSTILE);
                   // }
               // }
               //
               // FactionAPI derelictFaction = Global.getSector().getFaction("derelict");
               // FactionAPI nexderelictFaction = Global.getSector().getFaction("nex_derelict");
               // if (derelictFaction != null && nexderelictFaction != null) {
               //     FactionAPI playerFaction = Global.getSector().getPlayerFaction();
               //     playerFaction.setRelationship(nexderelictFaction.getId(), 100f);
               //     nexderelictFaction.setRelationship(playerFaction.getId(), 100f);
               //     derelictFaction.setRelationship(nexderelictFaction.getId(), 100f);
               //     nexderelictFaction.setRelationship(derelictFaction.getId(), 100f);
               // }
               //
               // Global.getSector().getFaction(Factions.PLAYER).setRelationship(Factions.REMNANTS, 0f);
               // Global.getSector().getFaction(Factions.PLAYER).setRelationship(Factions.OMEGA, 0f);
               // Global.getSector().getFaction(Factions.DERELICT).setRelationship(Factions.REMNANTS, 0f);
               // Global.getSector().getFaction(Factions.DERELICT).setRelationship(Factions.OMEGA, 0f);
               //
               // if (Global.getSettings().getModManager().isModEnabled("secretsofthefrontier")) {
               //     Global.getSector().getFaction(Factions.PLAYER).setRelationship("sotf_dustkeepers", 0f);
               //     Global.getSector().getFaction(Factions.PLAYER).setRelationship("sotf_dustkeepers_proxies", 0f);
               //     Global.getSector().getFaction(Factions.PLAYER).setRelationship("sotf_sierra_faction", 0f);
               //     Global.getSector().getFaction(Factions.PLAYER).setRelationship("sotf_dreaminggestalt", 0f);
               //     Global.getSector().getFaction(Factions.DERELICT).setRelationship("sotf_dustkeepers", 0f);
               //     Global.getSector().getFaction(Factions.DERELICT).setRelationship("sotf_dustkeepers_proxies", 0f);
               //     Global.getSector().getFaction(Factions.DERELICT).setRelationship("sotf_sierra_faction", 0f);
               //     Global.getSector().getFaction(Factions.DERELICT).setRelationship("sotf_dreaminggestalt", 0f);
               // }
               //
               // for (com.fs.starfarer.api.fleet.FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
               //     member.getRepairTracker().setCR(1f);
               //     member.getStatus().setHullFraction(1f);
               // }
            //}

            //if (Global.getSettings().getModManager().isModEnabled("IndEvo")) {
               // Global.getSector().getFaction(Factions.PLAYER).setRelationship("IndEvo_derelict", 0f);
              //  Global.getSector().getFaction(Factions.DERELICT).setRelationship("IndEvo_derelict", 0f);
            //}

           // if (Global.getSettings().getModManager().isModEnabled("aotd_qol")) {
           //     com.fs.starfarer.api.campaign.econ.MarketAPI market = Global.getSector().getEconomy().getMarket("ds_nexusMarket");
           //     if (market != null) {
           //         if (market.hasCondition(Conditions.DECIVILIZED_SUBPOP)) {
           //             market.removeCondition(Conditions.DECIVILIZED_SUBPOP);
           //         }
           //         if (market.hasCondition(Conditions.DECIVILIZED)) {
           //             market.removeCondition(Conditions.DECIVILIZED);
           //         }
           //         if (market.hasCondition(Conditions.RECENT_UNREST)) {
           //             market.removeCondition(Conditions.RECENT_UNREST);
           //         }
           //         for (int i = 2; i <= 10; i++) {
           //             String condId = "population_" + i;
           //             if (market.hasCondition(condId)) {
           //                 market.removeCondition(condId);
           //                 market.addCondition(Conditions.POPULATION_1);
           //             }
           //         }
           //         if (market.getSize() != 1) {
           //             market.setSize(1);
           //         }
           //         if (market.getCommodityData(Commodities.SUPPLIES).getAvailable() < 1500) {
           //             market.getCommodityData(Commodities.SUPPLIES).addTradeMod(Factions.DERELICT, 1500f, 30f);
           //         }
           //         if (market.getCommodityData(Commodities.FOOD).getAvailable() < 3000) {
           //             market.getCommodityData(Commodities.FOOD).addTradeMod(Factions.DERELICT, 3000f, 30f);
           //         }
           //     }
           // }

           // FactionAPI remmy = Global.getSector().getFaction(Factions.DERELICT);
           // remmy.setShowInIntelTab(true);

    @Override
    public void onNewGameAfterEconomyLoad() {
        // NSP content
        CustomFleetsNSP.spawnFleetXIVictus();
        CustomFleetsNSP_Templar.spawnTemplar();
        CustomFleetsNSP_Gate.spawnNemetor();
        new NSPPeople().nsp_createPeople();
        new tll_people().createTLLPeople();
    }


    @Override
    public void onNewGameAfterProcGen() {

        // NSP content
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
        new NSPInvictaSystemGen().generate(Global.getSector());
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

    }
    @Override
    public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip)    {
        switch (missile.getProjectileSpecId()) {
            case NSP_LUMINANCE_MISSILE_ID:
                return new PluginPick<MissileAIPlugin>(new nsp_DroneDeployerAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        return null;
    }


    // TLL Relations method from original NSP mod plugin
    public static void Tll_Relations() {
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
}