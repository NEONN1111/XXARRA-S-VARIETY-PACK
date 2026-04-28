package neon.nsp.data.scripts.campaign.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionFleetAutoDespawn;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import neon.nsp.data.scripts.NSPPeople;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DiscoElysiumMission extends HubMissionWithSearch {

    public static enum Stage {
        INVESTIGATION_1, //on Revanchol
        INVESTIGATION_2, //Go to station
        INVESTIGATION_3, //Go to Deora
        GO_BACK_TO_REVANCHOL, //short stage to "hide" Tribunal
        TRIBUNAL, //Fight the Mercs
        ARREST, //Go nack to Revanchol again
        COMPLETED, //Good job, you are good girl/boy
        FAILED //If refused to help or refused to fight Tribunal
    }

    protected MarketAPI originMarket = Global.getSector().getEconomy().getMarket("nsp_revanhol_market");
    protected MarketAPI TriTachMarket = Global.getSector().getEconomy().getMarket("nsp_deora_market");
    protected MarketAPI stationMarket = Global.getSector().getEconomy().getMarket("nsp_revanchol_miningstation_market");
    protected PersonAPI missionGiver = Global.getSector().getImportantPeople().getPerson(NSPPeople.HARRYDISCODUBOIS);

    protected static final float DELAY_TO_INVESTIGATION_2 = 3f;
    protected static final float DELAY_TO_INVESTIGATION_3 = 2f;
    protected static final float DELAY_TO_TRIBUNAL = 2f;
    protected static final float DELAY_TO_ARREST = 10f;

    @Override
    protected boolean create(MarketAPI createdAt, boolean barEvent) {
        /*
        if (!createdAt.getId().equals("nsp_revachol")) {
            return false;
        }
        */

        //I don't think it actually suppose to be bar event even if it starts in bar
        //Moved character to NSPPeople as he likely to be referenced a lot
        setPersonOverride(missionGiver);

        PersonAPI person = getPerson();
        if (person == null) return false;

        MarketAPI market = person.getMarket();
        if (market == null) return false;

        if (!setPersonMissionRef(person, "$disco_mission_ref")) {
            return false;
        }

        if (barEvent) {
            setGiverIsPotentialContactOnSuccess();
        }

        originMarket = createdAt;
        Global.getSector().getMemoryWithoutUpdate().set("$disco_mission_market", originMarket);

        setName("Disco Elysium");
        setStoryMission();

        setCreditReward(50000);
        setXPReward(75000);
        setRepRewardFaction(0.3f);
        setRepRewardPerson(0.2f);

        setGiverFaction(Factions.INDEPENDENT);
        completedKey = "$disco_mission_completed";

        if (!setGlobalReference("$disco_mission_ref")) return false;

        setStartingStage(Stage.INVESTIGATION_1);
        setStageOnGlobalFlag(Stage.INVESTIGATION_2, "$disco_mission_inv1_complete");
        setStageOnGlobalFlag(Stage.INVESTIGATION_3, "$disco_mission_inv2_complete");
        setStageOnGlobalFlag(Stage.TRIBUNAL, "$disco_mission_inv3_complete");
        setStageOnGlobalFlag(Stage.ARREST, "$disco_mission_act3_complete"); //should use $disco_mission_fleet_defeated but for now we're not testing that
        setStageOnGlobalFlag(Stage.COMPLETED, "$disco_mission_arrest_complete");
        setSuccessStage(Stage.COMPLETED);
        setFailureStage(Stage.FAILED);
        setStageOnGlobalFlag(Stage.FAILED, "$disco_mission_failed");
        setNoAbandon();

        beginStageTrigger(Stage.TRIBUNAL);
        triggerCreateFleet(FleetSize.LARGE, FleetQuality.SMOD_3, Factions.TRITACHYON, FleetTypes.PATROL_LARGE, originMarket.getPrimaryEntity());
        triggerFleetSetFlagship(Global.getSettings().getVariant("paragon_Elite"));
        triggerFleetSetName("Mercenary Tribunal");
        triggerFleetNoAutoDespawn();
        triggerFleetAllowJump();
        triggerMakeFleetIgnoredByOtherFleets();
        triggerMakeFleetIgnoreOtherFleetsExceptPlayer();
        triggerSetFleetMemoryValue(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER,true);
        triggerSetFleetMemoryValue(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE,false);
        triggerSetFleetMemoryValue(MemFlags.MEMORY_KEY_MAKE_PREVENT_DISENGAGE,true);
        triggerSetFleetMemoryValue(MemFlags.MEMORY_KEY_PURSUE_PLAYER,true);
        triggerSetFleetMemoryValue(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT,true);
        triggerSetFleetFaction(Factions.MERCENARY);
        triggerPickLocationAroundPlayer(200f);
        triggerSpawnFleetAtPickedLocation();
        triggerFleetMakeImportant("$disco_mission_hostile_fleet", Stage.TRIBUNAL);
        triggerSetFleetMemoryValue(MemFlags.MEMORY_KEY_MAKE_PREVENT_DISENGAGE, true);
        triggerSaveFleetRef(Global.getSector().getMemoryWithoutUpdate(), "$disco_mission_mercenary_fleet");
        triggerOrderFleetInterceptPlayer();
        triggerFleetAddDefeatTrigger("$disco_mission_fleet_defeated");
        triggerSetFleetAlwaysPursue();
        endTrigger();

        return true;
    }

    //Spawns and orders Harry fleet to move to point - After Tribunal orders need to be handled separately
    //isTargetTheMarket - makes fleet despawn at target location if true
    private void createHarryAndKimFleet(MarketAPI fleetOrigin, SectorEntityToken fleetTarget, boolean isTargetTheMarket){
        FleetCreatorMission m = new FleetCreatorMission(new Random());

        m.beginFleet();
        m.triggerCreateFleet(FleetSize.TINY, FleetQuality.DEFAULT, Factions.INDEPENDENT, FleetTypes.PATROL_MEDIUM, fleetOrigin.getPrimaryEntity());
        m.triggerFleetSetFlagship(Global.getSettings().getVariant("apogee_Balanced"));
        m.triggerAddShips("buffalo_Standard", "dram_Light", "gemini_Standard", "sunder_Support");
        m.triggerSetFleetOfficers(OfficerNum.ALL_SHIPS, OfficerQuality.LOWER);
        m.triggerSetFleetDoctrineComp(5, 0, 0);
        m.triggerSetFleetCommander(missionGiver);

        m.triggerFleetMakeImportant("$disco_mission_harry_fleet");
        m.triggerSetFleetMemoryValue(MemFlags.MEMORY_KEY_SOURCE_MARKET, fleetOrigin);
        m.triggerSetFleetFaction(Factions.INDEPENDENT);
        m.triggerSetFleetMemoryValue(MemFlags.MEMORY_KEY_MISSION_IMPORTANT,true);
        m.triggerFleetSetNoFactionInName();
        m.triggerFleetNoAutoDespawn();
        m.triggerFleetSetName("Harry's Fleet");
        m.triggerPatrolAllowTransponderOff();
        m.triggerOrderFleetInterceptPlayer();
        m.triggerMakeFleetIgnoredByOtherFleets();
        m.triggerMakeFleetIgnoreOtherFleetsExceptPlayer();

        CampaignFleetAPI fleet = m.createFleet();
        fleet.setId("nsp_disco_mission_HarryFleet");
        fleet.removeScriptsOfClass(MissionFleetAutoDespawn.class);
        fleetOrigin.getContainingLocation().addEntity(fleet);
        fleet.setLocation(fleetOrigin.getPrimaryEntity().getLocation().x, fleetOrigin.getPrimaryEntity().getLocation().y);
        fleet.setFacing(new Random().nextFloat() * 360f);

        if(isTargetTheMarket) {
            fleet.getAI().clearAssignments();
            fleet.getAI().addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
                    fleetTarget, 50,"Flying to " + fleetTarget.getName(), null);
        }
        else {
            fleet.getAI().clearAssignments();
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, fleetTarget, 50, "", null);
        }
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        /*
        checkAndAdvanceStage(Stage.INVESTIGATION_1, Stage.INVESTIGATION_2,
                "$disco_mission_prologue_complete", DELAY_TO_INVESTIGATION_2);
        checkAndAdvanceStage(Stage.INVESTIGATION_2, Stage.INVESTIGATION_3,
                "$disco_mission_act1_complete", DELAY_TO_INVESTIGATION_3);
        checkAndAdvanceStage(Stage.INVESTIGATION_3, Stage.TRIBUNAL,
                "$disco_mission_act2_complete", DELAY_TO_TRIBUNAL);
        checkAndAdvanceStage(Stage.TRIBUNAL, Stage.ARREST,
                "$disco_mission_act3_complete", DELAY_TO_ARREST);
         */
        checkIsMissionFailed();
    }

    //I assume it doesn't do anything yet
    protected void checkAndAdvanceStage(Stage current, Stage next, String flag, float delayDays) {
        if (getCurrentStage() == current && Global.getSector().getMemoryWithoutUpdate().getBoolean(flag)) {
            float elapsed = getElapsedInCurrentStage();
            float currentTime = Global.getSector().getClock().getTimestamp();

            if (elapsed >= delayDays) {
                setCurrentStage(next);
            }
        }
    }

    float failuredelay = 10;
    //Method to auto fail Tribunal if you don't complete it soon
    private void checkIsMissionFailed(){
        if(getCurrentStage() != Stage.TRIBUNAL){
            return;
        }

        float elapsed = getElapsedInCurrentStage();
        if(elapsed > failuredelay){
            Global.getSector().getMemoryWithoutUpdate().set("$disco_mission_failed", true);
            originMarket.setFactionId(Factions.TRITACHYON);
            stationMarket.setFactionId(Factions.PIRATES);
        }
    }

    private void setCurrentStage(Stage next) {
        if(next == Stage.INVESTIGATION_2){

        }
        else if(next == Stage.INVESTIGATION_3){

        }
        else if(next == Stage.TRIBUNAL){

        }
        else if(next == Stage.ARREST){

        }
    }

    @Override
    protected boolean callAction(String action, String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        TextPanelAPI text = dialog.getTextPanel();

        //Maybe change to switch? Functionally will be the same
        switch (action) {
            case "DiscoStartMission" -> {
                makeImportant(originMarket, "$disco_mission_inv1", Stage.INVESTIGATION_1);
                return true;
            }
            case "DiscoCompleteInvestigation1" -> {
                Global.getSector().getMemoryWithoutUpdate().set("$disco_prologue_complete", true);
                makeImportant(stationMarket, "$disco_mission_inv2", Stage.INVESTIGATION_2);
                createHarryAndKimFleet(originMarket, stationMarket.getPrimaryEntity(), true);
                return true;
            }
            case "DiscoCompleteInvestigation2" -> {
                Global.getSector().getMemoryWithoutUpdate().set("$disco_mission_act1_complete", true);
                makeImportant(TriTachMarket, "$disco_mission_inv3", Stage.INVESTIGATION_3);
                createHarryAndKimFleet(stationMarket, TriTachMarket.getPrimaryEntity(), true);
                return true;
            }
            case "DiscoCompleteInvestigation3" -> {
                Global.getSector().getMemoryWithoutUpdate().set("$disco_mission_act2_complete", true);
                makeImportant(originMarket, "$disco_mission_act3", Stage.TRIBUNAL);
                createHarryAndKimFleet(TriTachMarket, originMarket.getPrimaryEntity(), true);
                return true;
            }
            case "DiscoCompleteTribunal" -> {
                Global.getSector().getMemoryWithoutUpdate().set("$disco_mission_act3_complete", true);
                //Fight fleet to progress
                //makeImportant(originMarket, "$disco_mission_act4", Stage.ARREST);
                return true;
                //Fight fleet to progress
                //makeImportant(originMarket, "$disco_mission_act4", Stage.ARREST);
            }
            case "DiscoCompleteArrest" -> {
                Global.getSector().getMemoryWithoutUpdate().set("$disco_mission_act4_complete", true);
                makeImportant(originMarket, "$disco_mission_act_epilogue", Stage.ARREST);
                return true;
            }
            case "DiscoFailedMission" -> {
                Global.getSector().getMemoryWithoutUpdate().set("$disco_mission_failed", true);
                //setCurrentStage(Stage.FAILED);
                originMarket.setFactionId(Factions.TRITACHYON);
                stationMarket.setFactionId(Factions.PIRATES);
                return true;
            }
        }

        beginStageTrigger(Stage.INVESTIGATION_2);
        makeImportant(stationMarket, "$disco_mission_inv2_complete", Stage.INVESTIGATION_2);
        createHarryAndKimFleet(originMarket, stationMarket.getPrimaryEntity(), true);
        endTrigger();

        beginStageTrigger(Stage.INVESTIGATION_3);
        makeImportant(TriTachMarket, "$disco_mission_inv3_complete", Stage.INVESTIGATION_2);
        createHarryAndKimFleet(stationMarket, TriTachMarket.getPrimaryEntity(), true);
        endTrigger();


        return true;
    }

    protected void updateInteractionDataImpl() {
        set("$disco_mission_current_stage", getCurrentStage());
        set("$disco_mission_market", originMarket);
        set("$disco_mission_market_name", originMarket.getName());
    }

    @Override
    public void addDescriptionForNonEndStage(TooltipMakerAPI info, float width, float height) {
        float opad = 10f;
        Color h = Misc.getHighlightColor();

        if (currentStage == Stage.INVESTIGATION_1) {
            info.addPara("After talking with Harry and Kim you been directed to join them in investigating crime scene on %s.",
                    opad, Misc.getHighlightColor(), originMarket.getName());

            float remainingDelay = getRemainingDelay(DELAY_TO_INVESTIGATION_2);
            if (remainingDelay > 0) {
                info.addPara("Next stage is available in: %s days.", opad, Misc.getHighlightColor(), "" + String.format("%.1f", remainingDelay));
            }
            else {
                info.addPara("Next stage is now available.", opad, Misc.getHighlightColor(), "now");
            }
        } else if (currentStage == Stage.INVESTIGATION_2) {
            info.addPara("You seemed to pick up trail from few witnesses you found an the " + originMarket.getName() + "."
                    + "They lead you after SomeName, which rumored to run away to %s.",
                    opad, Misc.getHighlightColor(), stationMarket.getName());

            float remainingDelay = getRemainingDelay(DELAY_TO_INVESTIGATION_3);
            if (remainingDelay > 0) {
                info.addPara("Next stage is available in: %s days.", opad, Misc.getHighlightColor(), "" + String.format("%.1f", remainingDelay));
            }
            else {
                info.addPara("Next stage is now available.", opad, Misc.getHighlightColor(), "now");
            }
        } else if (currentStage == Stage.INVESTIGATION_3) {
            info.addPara("Act 2: placeholder", opad);
            float remainingDelay = getRemainingDelay(DELAY_TO_TRIBUNAL);
            if (remainingDelay > 0) {
                info.addPara("Next stage is available in: %s days.", opad, Misc.getHighlightColor(), "" + String.format("%.1f", remainingDelay));
            }
            else {
                info.addPara("Next stage is now available.", opad, Misc.getHighlightColor(), "now");
            }
        } else if (currentStage == Stage.TRIBUNAL) {
            info.addPara("Act 3: placeholder", opad);
            float remainingDelay = getRemainingDelay(DELAY_TO_ARREST);
            if (remainingDelay > 0) {
                info.addPara("Next stage is available in: %s days.", opad, Misc.getHighlightColor(), "" + String.format("%.1f", remainingDelay));
            }
            else {
                info.addPara("Next stage is now available.", opad, Misc.getHighlightColor(), "now");
            }
        } else if (currentStage == Stage.ARREST) {
            info.addPara("Act 4: placeholder", opad);
        }
    }

    /*
    protected float getRemainingDelay(String timeFlag, float delayDays) {
        if (!Global.getSector().getMemoryWithoutUpdate().contains(timeFlag)) {
            return delayDays;
        }
        float flagSetTime = Global.getSector().getMemoryWithoutUpdate().getFloat(timeFlag);
        float currentTime = Global.getSector().getClock().getTimestamp();
        float elapsed = getElapsedInCurrentStage(); // i think having this means you don't need memkeys for date stage started
        //float elapsed = currentTime - flagSetTime;
        return Math.max(0, delayDays - elapsed);
    }
    */

    //Same method as above but without memkey, if Xxarra will not need/want it, safe to delete
    protected float getRemainingDelay(float delayDays) {
        float elapsed = getElapsedInCurrentStage();
        return Math.max(0, delayDays - elapsed);
    }

    @Override
    public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
        if (currentStage == Stage.INVESTIGATION_1) {
            float remaining = getRemainingDelay(DELAY_TO_INVESTIGATION_2);
            info.addPara("INVESTIGATION_1 placeholder (" + String.format("%.1f", remaining) + " days remaining)\n"
                    + "Go to %s", pad, tc, originMarket.getFaction().getBaseUIColor(), "" + originMarket.getName());
            return true;
        } else if (currentStage == Stage.INVESTIGATION_2) {
            float remaining = getRemainingDelay(DELAY_TO_INVESTIGATION_3);
            info.addPara("INVESTIGATION_2 placeholder (" + String.format("%.1f", remaining) + " days remaining)\n"
                    + "Go to %s", pad, tc, stationMarket.getFaction().getBaseUIColor(), "" + stationMarket.getName());
            return true;
        } else if (currentStage == Stage.INVESTIGATION_3) {
            float remaining = getRemainingDelay(DELAY_TO_TRIBUNAL);
            info.addPara("INVESTIGATION_3 placeholder (" + String.format("%.1f", remaining) + " days remaining)\n"
                    + "Go to %s", pad, tc, TriTachMarket.getFaction().getBaseUIColor(), "" + TriTachMarket.getName());
            return true;
        } else if (currentStage == Stage.TRIBUNAL) {
            float remaining = getRemainingDelay(DELAY_TO_ARREST);
            info.addPara("TRIBUNAL placeholder\n"
                    + "Intervene against mercenaries (%s)", pad, tc, Misc.getNegativeHighlightColor(),  String.format("%.1f", remaining) + "days remaining until the mission is failed");
            return true;
        } else if (currentStage == Stage.ARREST) {
            float remaining = getRemainingDelay(DELAY_TO_ARREST);
            info.addPara("ARREST placeholder (" + String.format("%.1f", remaining) + " days remaining)\n"
                    + "Go to %s", pad, tc, TriTachMarket.getFaction().getBaseUIColor(), "" + TriTachMarket.getName());
            return true;
        }

        return false;
    }
}
