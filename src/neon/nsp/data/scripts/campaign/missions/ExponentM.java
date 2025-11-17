package neon.nsp.data.scripts.campaign.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.academy.GADerelictArtifact;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch;
import com.fs.starfarer.api.impl.campaign.missions.hub.ReqMode;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import neon.nsp.data.scripts.NSPPeople;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class ExponentM extends HubMissionWithSearch {

    public static enum Stage {
        INVESTIGATE, //INVESTIGATE STRIKE FORCE
        REPORT_BACK,
        TAKE_THE_FIGHT,
        RETURN_POST_FIGHT,
        COMPLETED,
    }

    protected MarketAPI originMarket;
    // disabled invictus
    protected SectorEntityToken invictus;
    // disabled invictus system
    protected StarSystemAPI system1;
    // main system
    protected StarSystemAPI system2;

    @Override
    protected boolean create(MarketAPI createdAt, boolean barEvent) {
        originMarket = createdAt;
        Global.getSector().getMemoryWithoutUpdate().set("$nsp_exponentMarket",originMarket);

        setName("The Exponent");
        setStoryMission();
        setRepFactionChangesNone();
        setRepPersonChangesNone();
        setGiverFaction(Factions.LUDDIC_CHURCH);
        completedKey = "$nsp_exponentCompleted";

        // Disabled Invictus system
        requireSystemNotNebula();
        requireSystemInterestingAndNotUnsafeOrCore();
        requireSystemTags(ReqMode.ALL, Tags.THEME_RUINS);
        preferSystemUnexplored();

        system1 = pickSystem(true);

        if (system1 == null) {
            requireSystemTags(ReqMode.ALL, Tags.THEME_RUINS);
            system1 = pickSystem(true);
        }
        if (system1 == null) return false;

        // Exponent system
        requireSystemInterestingAndNotUnsafeOrCore();
        requireSystemTags(ReqMode.ALL, Tags.THEME_RUINS);
        preferSystemWithinRangeOf(system1.getLocation(), 5f, 20);
        preferSystemNotPulsar();
        preferSystemOnFringeOfSector();
        preferSystemUnexplored();
        requireSystemNot(system1);

        system2 = pickSystem(true);
        if (system2 == null) return false;

        if (!setGlobalReference("$exponent_ref")) return false;

        invictus = spawnEntity("nsp_exponent_invictus", new LocData(EntityLocationType.ORBITING_PLANET, null, system1));
        invictus.getMemoryWithoutUpdate().set("$exponent_invictus", true);
        makeImportant(invictus, "$exponent", Stage.INVESTIGATE);
        invictus.setFaction(Factions.LUDDIC_CHURCH);

        LocData invictus_flair_locdata = new LocData(invictus, false);
        spawnDebrisField(360f, 1.2f, invictus_flair_locdata);
        spawnShipGraveyard(Factions.LUDDIC_CHURCH, 6, 10, invictus_flair_locdata);

        beginStageTrigger(Stage.TAKE_THE_FIGHT);
        triggerCreateFleet(FleetSize.TINY, FleetQuality.VERY_HIGH, Factions.DERELICT, FleetTypes.MERC_SCOUT, system2);
        triggerFleetSetSingleShipOnly();
        triggerFleetSetFlagship(Global.getSettings().getVariant("nsp_exponent_enlightened"));
        triggerFleetSetName("The Exponent");
        triggerFleetSetNoFactionInName();
        triggerFleetNoAutoDespawn();
        triggerFleetNoJump();
        triggerSetFleetNotBusy();
        triggerMakeFleetIgnoredByOtherFleets();
        triggerMakeFleetIgnoreOtherFleetsExceptPlayer();
        triggerOrderFleetInterceptPlayer(true,false);
        triggerOrderFleetEBurn(1.0f);
        triggerFleetInterceptPlayerOnSight(false,Stage.TAKE_THE_FIGHT);
        triggerSetFleetFaction(Factions.DERELICT);
        triggerSetFleetCommander(NSPPeople.getPerson(NSPPeople.EXPONENT_CORE));
//        triggerPickLocationAroundEntity(originMarket.getPrimaryEntity(),0);
//        triggerPickLocationAtClosestToPlayerJumpPoint(system2);
        triggerPickLocationAroundEntity(getPlanetEntityFromSystem(system2),100);
        triggerSpawnFleetAtPickedLocation();
        triggerFleetMakeImportant("$nsp_exponent", Stage.TAKE_THE_FIGHT);
        endTrigger();

        // set our starting, success and failure stages
        setStartingStage(Stage.INVESTIGATE);
        setSuccessStage(Stage.COMPLETED);
//        setFailureStage(Stage.SOLD_SIERRA);
        setNoAbandon();

        // set stage transitions when certain global flags are set
        setStageOnGlobalFlag(Stage.REPORT_BACK, "$exponent_hasInvestigated");
        setStageOnGlobalFlag(Stage.TAKE_THE_FIGHT, "$exponent_hasReported");
        connectWithGlobalFlag(Stage.TAKE_THE_FIGHT, Stage.RETURN_POST_FIGHT, "$exponent_hasFought");
        setStageOnGlobalFlag(Stage.COMPLETED, "$exponent_completed");
//        setStageOnGlobalFlag(Stage.SOLD_SIERRA, "$apromise_soldsierra");
        return true;

    }

    private SectorEntityToken getPlanetEntityFromSystem(StarSystemAPI system2) {
        WeightedRandomPicker<PlanetAPI> picker = new WeightedRandomPicker<>();
        picker.addAll(system2.getPlanets());
        PlanetAPI pickerPlanet = picker.pick();
        Global.getSector().getMemoryWithoutUpdate().set("$system2Planet",pickerPlanet);
        return pickerPlanet;
    }


    // when Call-ing something that isn't a default option for a mission, it'll try and run this method
    // with "action" being the first parameter
    @Override
    protected boolean callAction(String action, String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        TextPanelAPI text = dialog.getTextPanel();

        // give Sierra core with special text
//        if (action.equals("giveCore")){
//            Global.getSector().getPlayerFleet().getCargo().addSpecial(new SpecialItemData(SotfIDs.SIERRA_CORE, null), 1);
//
//            text.setFontSmallInsignia();
//            text.addParagraph("Gained Sierra Core", Misc.getPositiveHighlightColor());
//            text.highlightInLastPara(SotfMisc.getSierraColor(), "Sierra Core");
//            text.setFontInsignia();
//            return true;
//        }
        if (action.equals("reportBack")) {
            makeImportant(originMarket, "$exponent", Stage.REPORT_BACK);
            Global.getSector().layInCourseFor(originMarket.getPrimaryEntity());
            return true;
        }

        return false;
    }

    protected void updateInteractionDataImpl() {
        set("$exponentSystemOne",system1);
        set("$exponentSystemTwo",system2);
        set("$exponent_system1SName", system1.getNameWithNoType());
        set("$exponent_system2SName", system2.getNameWithNoType());

    }

    // description when selected in intel screen
    @Override
    public void addDescriptionForNonEndStage(TooltipMakerAPI info, float width, float height) {
        float opad = 10f;
        Color h = Misc.getHighlightColor();

        if (currentStage == Stage.INVESTIGATE) {
            info.addPara("You've heard rumors from a patrol officer about a strike fleet being MIA after being " +
                    "sent out by the Knights of Ludd to eliminate the so called \"Exponent\".", opad);
            info.addPara(getGoToSystemTextShort(system1) + " and see if you can find out what happened to " +
                    "the strike fleet.", opad);
        } else if (currentStage == Stage.REPORT_BACK) {
            info.addPara("Arriving at the last known location of the strike fleet in " + system1.getNameWithLowercaseTypeShort() +
                    " you found a horrific sight.", opad);
            info.addPara("Report back at " + originMarket.getName() + " blah blah blah", opad);
        } else if (currentStage == Stage.TAKE_THE_FIGHT) {
            info.addPara("[Retrieved data from the Invictus, decrypted by contact, escort fleet for precaution and assurance of destruction]", opad);
            info.addPara("Defeat the Exponent and [something].", opad);
        } else if (currentStage == Stage.RETURN_POST_FIGHT) {
            info.addPara("You successfully defeated the Exponent in the " + system2.getNameWithLowercaseTypeShort() +
                    " and [something].", opad);
            info.addPara("Return to " + originMarket.getName() + " for debrief.", opad);
        }
        if (isDevMode()) {
            info.addPara("DEV: DREADNOUGHT LOCATION: " + system1.getNameWithLowercaseTypeShort(), opad);
            info.addPara("DEV: EXPONENT LOCATION: " + system2.getNameWithLowercaseTypeShort(), opad);
        }
    }

    // short description in popups and the intel entry
    @Override
    public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
        Color h = Misc.getHighlightColor();
        if (currentStage == Stage.INVESTIGATE) {
            info.addPara("Search the " +
                    system1.getNameWithLowercaseTypeShort(), tc, pad);
            return true;
        } else if (currentStage == Stage.REPORT_BACK) {
            info.addPara("Report back at " + originMarket.getName(), tc, pad);
            return true;
        } else if (currentStage == Stage.TAKE_THE_FIGHT) {
            info.addPara("Defeat the Exponent", tc, pad);
            return true;
        } else if (currentStage == Stage.RETURN_POST_FIGHT) {
            info.addPara("Return to " + originMarket.getName() + " for debrief.", tc, pad);
            return true;
        }

        return false;
    }
}
