package neon.nsp.data.scripts.campaign.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch;
import com.fs.starfarer.api.impl.campaign.missions.hub.ReqMode;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.fleet.CampaignFleet;
import neon.nsp.data.scripts.NSPPeople;

import java.awt.Color;
import java.util.List;
import java.util.Map;

import static neon.nsp.data.scripts.util.NSPRanks.POST_DETECTIVE;

public class DiscoElysiumMission extends HubMissionWithSearch {

    public static enum Stage {
        PROLOGUE,
        ACT_1,
        ACT_2,
        ACT_3,
        ACT_4,
        EPILOGUE,
        COMPLETED,
    }

    protected MarketAPI originMarket = Global.getSector().getEconomy().getMarket("nsp_revanhol_market");
    protected MarketAPI TriTachMarket = Global.getSector().getEconomy().getMarket("nsp_deora_market");
    protected MarketAPI stationMarket = Global.getSector().getEconomy().getMarket("nsp_revanchol_miningstation_market");
    protected PersonAPI missionGiver = Global.getSector().getImportantPeople().getPerson(NSPPeople.HARRYDISCODUBOIS);


    protected static final float DELAY_PROLOGUE_TO_ACT1 = 3f;
    protected static final float DELAY_ACT1_TO_ACT2 = 2f;
    protected static final float DELAY_ACT2_TO_ACT3 = 2f;
    protected static final float DELAY_ACT3_TO_ACT4 = 2f;
    protected static final float DELAY_ACT4_TO_EPILOGUE = 2f;

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

        makeImportant(originMarket, "$disco_mission", Stage.PROLOGUE);

        setStageOnGlobalFlag(Stage.ACT_1, "$disco_mission_prologue_complete");
        setStageOnGlobalFlag(Stage.ACT_2, "$disco_mission_act1_complete");
        setStageOnGlobalFlag(Stage.ACT_3, "$disco_mission_act2_complete");
        setStageOnGlobalFlag(Stage.ACT_4, "$disco_mission_act3_complete");
        setStageOnGlobalFlag(Stage.EPILOGUE, "$disco_mission_act4_complete");
        setStageOnGlobalFlag(Stage.COMPLETED, "$disco_mission_epilogue_complete");

        beginStageTrigger(Stage.ACT_4);
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
        triggerPickLocationAroundPlayer(1000f);
        triggerSpawnFleetAtPickedLocation();
        triggerFleetMakeImportant("$disco_mission_hostile_fleet", Stage.ACT_4);
        triggerSetFleetMemoryValue(MemFlags.MEMORY_KEY_MAKE_PREVENT_DISENGAGE, true);
        triggerSaveFleetRef(Global.getSector().getMemoryWithoutUpdate(), "$disco_mission_mercenary_fleet");
        triggerFleetAddDefeatTrigger("$disco_mission_fleet_defeated");
        triggerSetFleetAlwaysPursue();
        endTrigger();


        setStartingStage(Stage.PROLOGUE);
        setSuccessStage(Stage.COMPLETED);
        setNoAbandon();

        return true;
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);


        checkAndAdvanceStage(Stage.PROLOGUE, Stage.ACT_1, "$disco_mission_prologue_complete", "$disco_mission_prologue_complete_time", DELAY_PROLOGUE_TO_ACT1);
        checkAndAdvanceStage(Stage.ACT_1, Stage.ACT_2, "$disco_mission_act1_complete", "$disco_mission_act1_complete_time", DELAY_ACT1_TO_ACT2);
        checkAndAdvanceStage(Stage.ACT_2, Stage.ACT_3, "$disco_mission_act2_complete", "$disco_mission_act2_complete_time", DELAY_ACT2_TO_ACT3);
        checkAndAdvanceStage(Stage.ACT_3, Stage.ACT_4, "$disco_mission_act3_complete", "$disco_mission_act3_complete_time", DELAY_ACT3_TO_ACT4);
        checkAndAdvanceStage(Stage.ACT_4, Stage.EPILOGUE, "$disco_mission_act4_complete", "$disco_mission_act4_complete_time", DELAY_ACT4_TO_EPILOGUE);
    }

    protected void checkAndAdvanceStage(Stage current, Stage next, String flag, String timeFlag, float delayDays) {
        if (getCurrentStage() == current && Global.getSector().getMemoryWithoutUpdate().getBoolean(flag)) {
            float flagSetTime = Global.getSector().getMemoryWithoutUpdate().getFloat(timeFlag);
            float currentTime = Global.getSector().getClock().getTimestamp();

            if (currentTime >= flagSetTime + delayDays) {
                setCurrentStage(next);
            }
        }
    }

    private void setCurrentStage(Stage next) {
    }

    @Override
    protected boolean callAction(String action, String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        TextPanelAPI text = dialog.getTextPanel();

        if (action.equals("DiscoCompletePrologue")) {
            Global.getSector().getMemoryWithoutUpdate().set("$disco_prologue_complete", true);
            Global.getSector().getMemoryWithoutUpdate().set("$disco_prologue_complete_time", Global.getSector().getClock().getTimestamp());
            makeImportant(originMarket, "$disco_mission_act1", Stage.ACT_1);
            return true;
        }

        if (action.equals("DiscoCompleteAct1")) {
            Global.getSector().getMemoryWithoutUpdate().set("$disco_mission_act1_complete", true);
            Global.getSector().getMemoryWithoutUpdate().set("$disco_mission_act1_complete_time", Global.getSector().getClock().getTimestamp());
            makeImportant(stationMarket, "$disco_mission_act2", Stage.ACT_2);
            return true;
        }

        if (action.equals("DiscoCompleteAct2")) {
            Global.getSector().getMemoryWithoutUpdate().set("$disco_mission_act2_complete", true);
            Global.getSector().getMemoryWithoutUpdate().set("$disco_mission_act2_complete_time", Global.getSector().getClock().getTimestamp());
            makeImportant(TriTachMarket, "$disco_mission_act3", Stage.ACT_3);
            return true;
        }

        if (action.equals("DiscoCompleteAct3")) {
            Global.getSector().getMemoryWithoutUpdate().set("$disco_mission_act3_complete", true);
            Global.getSector().getMemoryWithoutUpdate().set("$disco_mission_act3_complete_time", Global.getSector().getClock().getTimestamp());
            makeImportant(originMarket, "$disco_mission_act4", Stage.ACT_4);
            return true;
        }

        if (action.equals("DiscoCompleteAct4")) {
            Global.getSector().getMemoryWithoutUpdate().set("$disco_mission_act4_complete", true);
            Global.getSector().getMemoryWithoutUpdate().set("$disco_mission_act4_complete_time", Global.getSector().getClock().getTimestamp());
            makeImportant(originMarket, "$disco_mission_act_epilogue", Stage.EPILOGUE);
            return true;
        }

        if (action.equals("DiscoCompleteEpilogue")) {
            Global.getSector().getMemoryWithoutUpdate().set("$disco_mission_epilogue_complete", true);
            setCurrentStage(Stage.COMPLETED);
            return true;
        }

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

        if (currentStage == Stage.PROLOGUE) {
            info.addPara("PROLOGUE placeholder " + originMarket.getName() + ".", opad);
            info.addPara("PROLOGUE placeholder.", opad);
        } else if (currentStage == Stage.ACT_1) {
            info.addPara("Act 1:placeholder", opad);

            float remainingDelay = getRemainingDelay("$disco_mission_prologue_complete_time", DELAY_PROLOGUE_TO_ACT1);
            if (remainingDelay > 0) {
               // info.addPara("Next stage available in: " + String.format("%.1f", remainingDelay) + " days.", Misc.getHighlightColor(), opad);
            }
        } else if (currentStage == Stage.ACT_2) {
            info.addPara("Act 2: placeholder", opad);
            float remainingDelay = getRemainingDelay("$disco_mission_act1_complete_time", DELAY_ACT1_TO_ACT2);
            if (remainingDelay > 0) {
               // info.addPara("Next stage available in: " + String.format("%.1f", remainingDelay) + " days.", Misc.getHighlightColor(), opad);
            }
        } else if (currentStage == Stage.ACT_3) {
            info.addPara("Act 3: placeholder", opad);
            float remainingDelay = getRemainingDelay("$disco_mission_act2_complete_time", DELAY_ACT2_TO_ACT3);
            if (remainingDelay > 0) {
               // info.addPara("Next stage available in: " + String.format("%.1f", remainingDelay) + " days.", Misc.getHighlightColor(), opad);
            }
        } else if (currentStage == Stage.ACT_4) {
            info.addPara("Act 4: placeholder", opad);
            float remainingDelay = getRemainingDelay("$disco_mission_act3_complete_time", DELAY_ACT3_TO_ACT4);
            if (remainingDelay > 0) {
               // info.addPara("Next stage available in: " + String.format("%.1f", remainingDelay) + " days.", Misc.getHighlightColor(), opad);
            }
        } else if (currentStage == Stage.EPILOGUE) {
            info.addPara("EPILOGUE placeholder" + originMarket.getName() + " placeholder", opad);
            float remainingDelay = getRemainingDelay("$disco_mission_act4_complete_time", DELAY_ACT4_TO_EPILOGUE);
            if (remainingDelay > 0) {
               // info.addPara("Next stage available in: " + String.format("%.1f", remainingDelay) + " days.", Misc.getHighlightColor(), opad);
            }
        }
    }

    protected float getRemainingDelay(String timeFlag, float delayDays) {
        if (!Global.getSector().getMemoryWithoutUpdate().contains(timeFlag)) {
            return delayDays;
        }
        float flagSetTime = Global.getSector().getMemoryWithoutUpdate().getFloat(timeFlag);
        float currentTime = Global.getSector().getClock().getTimestamp();
        float elapsed = getElapsedInCurrentStage();
        //float elapsed = currentTime - flagSetTime;
        return Math.max(0, delayDays - elapsed);
    }

    @Override
    public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
        if (currentStage == Stage.PROLOGUE) {
            info.addPara("PROLOGUE placeholder" + originMarket.getName() + ".", tc, pad);
            return true;
        } else if (currentStage == Stage.ACT_1) {
            float remaining = getRemainingDelay("$disco_mission_prologue_complete_time", DELAY_PROLOGUE_TO_ACT1);
            info.addPara("ACT_1 placeholder (" + String.format("%.1f", remaining) + " days remaining)", tc, pad);
            return true;
        } else if (currentStage == Stage.ACT_2) {
            float remaining = getRemainingDelay("$disco_mission_act1_complete_time", DELAY_ACT1_TO_ACT2);
            info.addPara("ACT_2 placeholder (" + String.format("%.1f", remaining) + " days remaining)", tc, pad);
            return true;
        } else if (currentStage == Stage.ACT_3) {
            float remaining = getRemainingDelay("$disco_mission_act2_complete_time", DELAY_ACT2_TO_ACT3);
            info.addPara("ACT_3 placeholder (" + String.format("%.1f", remaining) + " days remaining)", tc, pad);
            return true;
        } else if (currentStage == Stage.ACT_4) {
            info.addPara("ACT_4 placeholder", tc, pad);
            return true;
        } else if (currentStage == Stage.EPILOGUE) {
            info.addPara("EPILOGUE placeholder " + originMarket.getName() + " to complete the mission.", tc, pad);
            return true;
        }

        return false;
    }
}
