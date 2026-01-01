package neon.nsp.data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.CharacterCreationData;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.intel.FactionCommissionIntel;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.impl.campaign.rulecmd.NGCAddStandardStartingScript;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import exerelin.campaign.ExerelinSetupData;
import exerelin.campaign.PlayerFactionStore;
import exerelin.campaign.customstart.CustomStart;
import exerelin.utilities.StringHelper;
import second_in_command.SCUtils;
import second_in_command.specs.SCOfficer;

import java.util.*;

public class NSP_ToBeMined extends CustomStart {

    // need to do the following:
    // have cargo picker to buy cores (should be basically unlimited and uses remnant bucks) - can just copy my old code here
    // custom production menu to insta produce ships/hulls AND figure out shared market storage for non-markets (wtf??)
    // setup misc nexus interactions (tutorial text, etc etc bla bla)

    @Override
    public void execute(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        CharacterCreationData data = (CharacterCreationData) memoryMap.get(MemKeys.LOCAL).get("$characterData");

        dialog.getTextPanel().addPara("Note: Currently slightly experimental, may have some odd behavior.");
        dialog.getTextPanel().addPara("Start under the Explorarium faction, being able to access any Mothership for normal fleet operations.");
        dialog.getTextPanel().addPara("As you are starting with a Explorarium commission, you will likely be hostile to most factions.");

        //data.startingCargo.addCommodity(Commodities.GAMMA_CORE, 3f);
        //data.startingCargo.addCommodity(Commodities.BETA_CORE, 1f);
        int revenantCrew = Math.round(Global.getSettings().getHullSpec("revenant").getMaxCrew());
        int phantomCrew = Math.round(Global.getSettings().getHullSpec("phantom").getMaxCrew());
        data.getStartingCargo().addCrew(revenantCrew + phantomCrew);
        data.getStartingCargo().getCredits().add(100000f);

        //AddRemoveCommodity.addCommodityGainText(Commodities.GAMMA_CORE, 3, dialog.textPanel);
        //AddRemoveCommodity.addCommodityGainText(Commodities.BETA_CORE, 1, dialog.textPanel);
        data.addStartingFleetMember("rampart_Standard", FleetMemberType.SHIP);
        data.addStartingFleetMember("berserker_Assault", FleetMemberType.SHIP);
        data.addStartingFleetMember("defender_PD", FleetMemberType.SHIP);
        data.addStartingFleetMember("picket_Assault", FleetMemberType.SHIP);
        data.addStartingFleetMember("sentry_FS", FleetMemberType.SHIP);
        data.addStartingFleetMember("warden_Defense", FleetMemberType.SHIP);
        data.addStartingFleetMember("revenant_Elite", FleetMemberType.SHIP);
        data.addStartingFleetMember("phantom_Elite", FleetMemberType.SHIP);

        ExerelinSetupData.getInstance().freeStart = true;
        ExerelinSetupData.getInstance().randomStartLocation = false;
        PlayerFactionStore.setPlayerFactionIdNGC(Factions.DERELICT);

        CampaignFleetAPI tempFleet = FleetFactoryV3.createEmptyFleet(
                PlayerFactionStore.getPlayerFactionIdNGC(),
                FleetTypes.PATROL_SMALL,
                null
        );

        tempFleet.getFleetData().addFleetMember("rampart_Standard");
        tempFleet.getFleetData().addFleetMember("berserker_Assault");
        tempFleet.getFleetData().addFleetMember("defender_PD");
        tempFleet.getFleetData().addFleetMember("picket_Assault");
        tempFleet.getFleetData().addFleetMember("sentry_FS");
        tempFleet.getFleetData().addFleetMember("warden_Defense");
        tempFleet.getFleetData().addFleetMember("revenant_Elite");
        tempFleet.getFleetData().addFleetMember("phantom_Elite");

        data.addScript(new Script() {
            @Override
            public void run() {
                if (Global.getSettings().getModManager().isModEnabled("second_in_command")) {
                    SCOfficer officer = SCUtils.createRandomSCOfficer("sc_automated");
                    // Assuming SCUtils has increaseLevel method - adjust as needed
                    // officer.increaseLevel(1);

                    SCUtils.getPlayerData().addOfficerToFleet(officer);
                    SCUtils.getPlayerData().setOfficerInEmptySlotIfAvailable(officer);
                } else {
                    var player = Global.getSector().getPlayerPerson();
                    player.getStats().setSkillLevel(Skills.AUTOMATED_SHIPS, 2f);
                }

                CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
                Global.getSector().getFaction(Factions.DERELICT).getRelToPlayer().setRel(100f);
                Global.getSector().getMemoryWithoutUpdate().set("$nsp_MothershipStart", true);
                CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();

                data.getPerson().getStats().addPoints(1);
                Global.getSector().getPlayerPerson().getStats().setSkillLevel("nsp_paradeigma", 1f);
                NGCAddStandardStartingScript.adjustStartingHulls(fleet);

                fleet.getFleetData().ensureHasFlagship();
                for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                    float max = member.getRepairTracker().getMaxCR();
                    member.getRepairTracker().setCR(max);
                    cargo.addCommodity(Commodities.SUPPLIES, member.getCargoCapacity() * 0.7f);
                    cargo.addCommodity(Commodities.FUEL, member.getFuelCapacity() * 0.9f);
                    cargo.addCommodity(Commodities.HEAVY_MACHINERY, member.getCargoCapacity() * 0.1f);
                }
                fleet.getFleetData().setSyncNeeded();

                List<StarSystemAPI> systemsWithTag = new ArrayList<>();
                for (StarSystemAPI starSystem : Global.getSector().getStarSystems()) {
                    if (!starSystem.hasTag(Tags.THEME_DERELICT_MOTHERSHIP)) continue;
                    systemsWithTag.add(starSystem);
                }
                StarSystemAPI stationsystem = systemsWithTag.get(new Random().nextInt(systemsWithTag.size()));

//            if (Global.getSettings().modManager.isModEnabled("MODCalveraSystem")) {
//                stationsystem = Global.getSector().getStarSystem("calvera")
//            }

                SectorEntityToken station = stationsystem.addCustomEntity(
                        "nsp_MothershipStorage",
                        "Mothership Global Storage",
                        "station_side05",
                        Factions.NEUTRAL
                );
                Misc.setAbandonedStationMarket("nsp_MotherShipStorage", station);
                station.setSensorProfile(0f);
                station.setInteractionImage("icons", "derelictflag");
                station.getMarket().addIndustry(Industries.SPACEPORT);
                station.setCircularOrbitPointingDown(stationsystem.getCenter(), 0f, 10000f, 9999f);

                if (Global.getSettings().getModManager().isModEnabled("aotd_qol")) {
                    SectorEntityToken station1 = stationsystem.addCustomEntity(
                            "nsp_MotherShipStation",
                            "Derelict Station",
                            "station_side05",
                            Factions.DERELICT
                    );
                    MarketAPI market = Global.getFactory().createMarket("nsp_MotherShipMarket", "Derelict Market", 1);
                    market.setFactionId(Factions.DERELICT);
                    market.setPrimaryEntity(station1);
                    station1.setMarket(market);
                    market.addCondition(Conditions.POPULATION_1);
                    market.addSubmarket(Submarkets.SUBMARKET_OPEN);
                    market.addSubmarket(Submarkets.SUBMARKET_STORAGE);
                    market.addIndustry(Industries.SPACEPORT);
                    market.addIndustry(Industries.POPULATION);
                    market.addTag(Tags.STATION);
                    market.addTag(Tags.MARKET_NO_OFFICER_SPAWN);
                    market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);
                    Global.getSector().getEconomy().addMarket(market, false);

                    market.getCommodityData(Commodities.SUPPLIES).addTradeMod(Factions.DERELICT, 1500f, 30f);
                    market.getCommodityData(Commodities.FOOD).addTradeMod(Factions.DERELICT, 3000f, 30f);

                    station1.setSensorProfile(0f);
                    station1.setInteractionImage("icons", "derelictflag");
                    station1.setCircularOrbitPointingDown(stationsystem.getCenter(), 0f, 10000f, 9999f);
                }

                //station.market.addIndustry(Industries.SPACEPORT);
                //station.market.getSubmarket(Submarkets.SUBMARKET_STORAGE).cargo.addMothballedShip(FleetMemberType.SHIP, "fulgent_Assault", "Engiels");
                //station.market.getSubmarket(Submarkets.SUBMARKET_STORAGE).cargo.addMothballedShip(FleetMemberType.SHIP, "glimmer_Support", "Gomiel");
                //station.market.getSubmarket(Submarkets.SUBMARKET_STORAGE).cargo.addMothballedShip(FleetMemberType.SHIP, "glimmer_Support", "Halitosis");
            }
        });

        data.addScriptBeforeTimePass(new Script() {
            @Override
            public void run() {
                // Manual implementation of getCustomEntitiesWithType
                List<SectorEntityToken> nexii = new ArrayList<>();
                for (StarSystemAPI system : Global.getSector().getStarSystems()) {
                    for (SectorEntityToken entity : system.getCustomEntities()) {
                        if (Entities.DERELICT_MOTHERSHIP.equals(entity.getCustomEntityType())) {
                            nexii.add(entity);
                        }
                    }
                }

                // Filter out "derelict_mothership"
                List<SectorEntityToken> filteredNexii = new ArrayList<>();
                for (SectorEntityToken entity : nexii) {
                    if (!"derelict_mothership".equals(entity.getId())) {
                        filteredNexii.add(entity);
                    }
                }

                for (SectorEntityToken entity : filteredNexii) {
                    entity.getCargo().addAll(addNexusCargo(entity));
                }

                WeightedRandomPicker<SectorEntityToken> nexusstart = new WeightedRandomPicker<>();
                nexusstart.addAll(filteredNexii);
                SectorEntityToken startloc = nexusstart.pick();


                 Global.getSector().getIntelManager().addIntel(new NSP_MotherShipLocationIntel(), false);


                 Global.getSector().getListenerManager().addListener(new NSP_NexusRestocker());

                new FactionCommissionIntel(Global.getSector().getFaction(Factions.DERELICT)).missionAccepted();
                Global.getSector().getMemoryWithoutUpdate().set("$nex_startLocation", startloc.getId());
//            if (Global.getSettings().modManager.isModEnabled("MODCalveraSystem")) {
//                Global.getSector().memoryWithoutUpdate.set("\$nex_startLocation", "calvera_mothership")
//            }

                FactionAPI remmy = Global.getSector().getFaction(Factions.DERELICT);
                remmy.setShowInIntelTab(true);
            }
        });

        dialog.getVisualPanel().showFleetInfo(
                StringHelper.getString("exerelin_ngc", "playerFleet", true),
                tempFleet, null, null
        );

        dialog.getOptionPanel().addOption(StringHelper.getString("done", true), "nex_NGCDone");
        dialog.getOptionPanel().addOption(StringHelper.getString("back", true), "nex_NGCDoneBack");
        FireBest.fire(null, dialog, memoryMap, "ExerelinNGCStep4");
    }

    // add cargo to all nexus, undamaged ones get more stuff
    private CargoAPI addNexusCargo(SectorEntityToken nexus) {
        float mult = 1f;

        CargoAPI cargo = Global.getFactory().createCargo(false);
        cargo.addCommodity(Commodities.GAMMA_CORE,
                Math.round(8 + Math.random() * 7) * (int)mult);
        cargo.addCommodity(Commodities.BETA_CORE,
                Math.round(5 + Math.random() * 4) * (int)mult);
        cargo.addCommodity(Commodities.ALPHA_CORE,
                Math.round(4 + Math.random() * 3) * (int)mult);
        cargo.addCommodity(Commodities.FUEL,
                Math.round(9000 + Math.random() * 6000) * (int)mult);
        cargo.addCommodity(Commodities.SUPPLIES,
                Math.round(3000 + Math.random() * 2000) * (int)mult);

        Random random = new Random();

        if (random.nextFloat() > 0.97f) {
            cargo.addCommodity(Commodities.OMEGA_CORE, 1f);
        }

        return cargo;
    }
}