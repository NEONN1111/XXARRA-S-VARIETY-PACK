package neon.nsp.data.scripts.campaign.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch;
import com.fs.starfarer.api.impl.campaign.missions.hub.ReqMode;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import com.fs.starfarer.campaign.fleet.CampaignFleet;
import neon.nsp.data.scripts.NSPPeople;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class ExponentM extends HubMissionWithSearch {

    public static enum Stage {
        INVESTIGATE, //INVESTIGATE STRIKE FORCE
        REPORT_BACK,
        TAKE_THE_FIGHT_LCF,
        TAKE_THE_FIGHT_ALONE,
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
        requireSystemNotHasPulsar();
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
        requireSystemNotNebula();
        requireSystemNotHasPulsar();
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

        /// ///////////////////////////////// EXPONENT SPAWN CODE /////////////////////////////////
        beginStageTrigger(Stage.TAKE_THE_FIGHT_LCF);
        triggerCreateFleet(FleetSize.SMALL, FleetQuality.VERY_HIGH, "nsp_exponent", FleetTypes.PATROL_SMALL, system2);
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
        triggerFleetInterceptPlayerOnSight(false,Stage.TAKE_THE_FIGHT_LCF,Stage.TAKE_THE_FIGHT_ALONE);
        triggerSetFleetFaction("nsp_exponent");
        triggerSetFleetCommander(NSPPeople.getPerson(NSPPeople.EXPONENT_CORE));
//        triggerPickLocationAroundEntity(originMarket.getPrimaryEntity(),0);
//        triggerPickLocationAtClosestToPlayerJumpPoint(system2);
        triggerPickLocationAroundEntity(getPlanetEntityFromSystem(system2),100);
        triggerSpawnFleetAtPickedLocation();
        triggerFleetMakeImportant("$nsp_exponent", Stage.TAKE_THE_FIGHT_LCF,Stage.TAKE_THE_FIGHT_ALONE);
        triggerSaveFleetRef(Global.getSector().getMemoryWithoutUpdate(),"$nsp_exponentFleet");
        triggerFleetAddDefeatTrigger("nspExponentPostFight");
        endTrigger();
        /// ///////////////////////////////////////////////////////////////////////////////////////

        /// ///////////////////////////////// LC ESCORT SPAWN CODE ////////////////////////////////
        beginStageTrigger(Stage.TAKE_THE_FIGHT_LCF);
        triggerCreateFleet(FleetSize.LARGE,FleetQuality.SMOD_1,Factions.LUDDIC_CHURCH,FleetTypes.TASK_FORCE,originMarket.getPrimaryEntity());
        triggerFleetSetFlagship(Global.getSettings().getVariant("retribution_Standard"));
        triggerFleetSetName("Placeholder");
        triggerFleetNoAutoDespawn();
        triggerFleetAllowJump();
        triggerMakeFleetIgnoredByOtherFleets();
        triggerMakeFleetIgnoreOtherFleetsExceptPlayer();
        triggerSetFleetFaction(Factions.LUDDIC_CHURCH);
        triggerSetFleetDoctrineQuality(4,4,15);
        triggerPickLocationAroundPlayer(0f);
        triggerSpawnFleetAtPickedLocation();
        triggerFleetMakeImportant("$nsp_exponent", Stage.TAKE_THE_FIGHT_LCF);
        triggerSaveFleetRef(Global.getSector().getMemoryWithoutUpdate(),"$nsp_exponentLuddicFleet");
        endTrigger();
        /// ///////////////////////////////////////////////////////////////////////////////////////






        // set our starting, success and failure stages
        setStartingStage(Stage.INVESTIGATE);
        setSuccessStage(Stage.COMPLETED);
//        setFailureStage(Stage.SOLD_SIERRA);
        setNoAbandon();

        // set stage transitions when certain global flags are set
        setStageOnGlobalFlag(Stage.REPORT_BACK, "$exponent_hasInvestigated");
        setStageOnGlobalFlag(Stage.TAKE_THE_FIGHT_LCF, "$exponent_goWithFleet");
        setStageOnGlobalFlag(Stage.TAKE_THE_FIGHT_ALONE, "$exponent_goAlone");
        connectWithGlobalFlag(Stage.TAKE_THE_FIGHT_LCF, Stage.RETURN_POST_FIGHT, "$exponent_hasFought");
        connectWithGlobalFlag(Stage.TAKE_THE_FIGHT_ALONE, Stage.RETURN_POST_FIGHT, "$exponent_hasFought");
        setStageOnGlobalFlag(Stage.COMPLETED, "$exponent_completed");
//        setStageOnGlobalFlag(Stage.SOLD_SIERRA, "$apromise_soldsierra");

        setRepRewardFaction(0.2f);
        setCreditReward(300000);

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
//            Global.getSector().layInCourseFor(originMarket.getPrimaryEntity());
            return true;
        }

        if (action.equals("fleetFollowPlayer")) {
            /// ///////////////////////////////// LC FLEET UPDATE CODE ////////////////////////////////
            CampaignFleet luddicFleet = (CampaignFleet) Global.getSector().getMemoryWithoutUpdate().get("$nsp_exponentLuddicFleet");
            if (luddicFleet != null) {
//                luddicFleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE,Global.getSector().getPlayerFleet(),9999f,"Escorting your fleet");
                luddicFleet.addAbility(Abilities.TRANSVERSE_JUMP);
                luddicFleet.addScript(new ExponenMissionLCFleetEscort(luddicFleet));
            } else throw new RuntimeException("[NSP] Exponent mission escort fleet does not exist (somehow)");

            /// ///////////////////////////////// EXPONENT UPDATE CODE ////////////////////////////////
            CampaignFleet exponentFleet = (CampaignFleet) Global.getSector().getMemoryWithoutUpdate().get("$nsp_exponentFleet");
            if (exponentFleet != null) {
//import com.fs.starfarer.campaign.fleet.CampaignFleet;
//                CampaignFleet exponentFleet = (CampaignFleet) Global.getSector().getMemoryWithoutUpdate().get("$nsp_exponentFleet");
                exponentFleet.getFlagship().setShipName("Unknown");
                exponentFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER,true);
                exponentFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE,false);
//                exponentFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE,false);
                exponentFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE,true);
            }
            return true;
        }

        if (action.equals("reportDebrief")) {
            makeImportant(originMarket, "$exponent", Stage.RETURN_POST_FIGHT);
            return true;
        }

        if (action.equals("dialogTest")) {

            Global.getSector().getMemoryWithoutUpdate().set("$defeatedExponent", true);


            ShipRecoverySpecial.PerShipData ship = new ShipRecoverySpecial.PerShipData("nsp_exponent_Hull", ShipRecoverySpecial.ShipCondition.WRECKED, 0f);
            ship.shipName = "Unknown";
            DerelictShipEntityPlugin.DerelictShipData params1 = new DerelictShipEntityPlugin.DerelictShipData(ship, false);
            CustomCampaignEntityAPI entity = (CustomCampaignEntityAPI) BaseThemeGenerator.addSalvageEntity(
                    Global.getSector().getPlayerFleet().getContainingLocation(),
                    Entities.WRECK, Factions.NEUTRAL, params1);
            Misc.makeImportant(entity, "exponent");
            entity.getMemoryWithoutUpdate().set("$exponent", true);
            entity.getLocation().x = Global.getSector().getPlayerFleet().getLocation().x + (50f - (float) Math.random() * 100f);
            entity.getLocation().y = Global.getSector().getPlayerFleet().getLocation().y + (50f - (float) Math.random() * 100f);

            ShipRecoverySpecial.ShipRecoverySpecialData data = new ShipRecoverySpecial.ShipRecoverySpecialData(null);
            data.notNowOptionExits = true;
            data.noDescriptionText = true;
            DerelictShipEntityPlugin dsep = (DerelictShipEntityPlugin) entity.getCustomPlugin();
            ShipRecoverySpecial.PerShipData copy = (ShipRecoverySpecial.PerShipData) dsep.getData().ship.clone();
            copy.variant = Global.getSettings().getVariant(copy.variantId).clone();
            copy.variantId = null;
            copy.variant.addTag(Tags.SHIP_CAN_NOT_SCUTTLE);
            copy.variant.addTag(Tags.SHIP_UNIQUE_SIGNATURE);
            data.addShip(copy);
            Misc.setSalvageSpecial(entity, data);

            dialog.setInteractionTarget(entity);
            RuleBasedInteractionDialogPluginImpl plugin = new RuleBasedInteractionDialogPluginImpl("ExponentPostFightTwo");
            dialog.setPlugin(plugin);
            plugin.init(dialog);

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
        } else if (currentStage == Stage.TAKE_THE_FIGHT_LCF) {
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
        } else if (currentStage == Stage.TAKE_THE_FIGHT_LCF) {
            info.addPara("Defeat the Exponent", tc, pad);
            return true;
        } else if (currentStage == Stage.RETURN_POST_FIGHT) {
            info.addPara("Return to " + originMarket.getName() + " for debrief.", tc, pad);
            return true;
        }

        return false;
    }
}
