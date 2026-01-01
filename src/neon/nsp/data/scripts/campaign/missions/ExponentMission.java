package neon.nsp.data.scripts.campaign.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
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
import neon.nsp.data.scripts.plugins.ExponentFIDConfig;
import neon.nsp.data.scripts.plugins.ExponentLCFleetFidConfig;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExponentMission extends HubMissionWithSearch {

    public static enum Stage {
        FIRST_TALK,
        INVESTIGATE, // Investigate strike force disappearance
        REPORT_BACK, // Report back your findings
        TAKE_THE_FIGHT_LCF, // Hunt down The Exponent with LC fleet support
        // In case of joining The Exponent
        TAKE_THE_FIGHT_EXP, // Join The Exponent against The Church
        CONTACT_EXPONENT, // Contact the Exponent post fight
        // ///////////////////////////////
        TAKE_THE_FIGHT_ALONE, // Hunt down The Exponent alone, but with a knight on-board
        RETURN_POST_FIGHT,
        RETURN_NO_FIGHT,

        // Failure Stages
        EXPONENT_JOINED,
        RELEASED_EXPONENT,
        RECOVERED_EXPONENT,

        COMPLETED,
    }

    protected MarketAPI originMarket;
    // disabled invictus
    protected SectorEntityToken invictus;
    // disabled invictus system
    protected StarSystemAPI system1;
    // main system
    protected StarSystemAPI system2;
    protected PlanetAPI exponentSpawnPlanet;

    @Override
    protected boolean create(MarketAPI createdAt, boolean barEvent) {

//        if (barEvent) {
            setGiverRank(Ranks.KNIGHT_CAPTAIN);
            setGiverPost("luddicKnight");
            giverGender = FullName.Gender.ANY;
            setGiverPortrait(Global.getSector().getFaction(Factions.LUDDIC_CHURCH).getPortraits(giverGender).pick());
            setGiverImportance(pickHighImportance());
            findOrCreateGiver(createdAt, false, false);
//        }

        PersonAPI person = getPerson();
        if (person == null) return false;
        MarketAPI market = person.getMarket();
        if (market == null) return false;

        if (!setPersonMissionRef(person, "$exponent_ref")) {
            return false;
        }
//
//        if (barEvent) {
            setGiverIsPotentialContactOnSuccess();
//        }

        person.setId("exponent_KnightContact");
        Global.getSector().getImportantPeople().addPerson(person);
//        Global.getSector().getMemoryWithoutUpdate().set("$nsp_exponentContact",person);

        originMarket = createdAt;
        Global.getSector().getMemoryWithoutUpdate().set("$nsp_exponentMarket",originMarket);

        setName("The Exponent");
        setStoryMission();

//        setRepFactionChangesNone();
//        setRepPersonChangesNone();
        setCreditReward(100000); // Credits Reward
        setXPReward(50000); // XP Reward
        setRepRewardFaction(0.2f); // Rep Reward (Faction)
        setRepRewardPerson(0.2f); // Rep Reward (Quest Giver, if Important person)

        setGiverFaction(Factions.LUDDIC_CHURCH);
        completedKey = "$nsp_exponentCompleted";

        // Disabled Invictus system
        requireSystemNotNebula();
        requireSystemNotHasPulsar();
        requireSystemNotBlackHole();
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
        requireSystemNotBlackHole();
        requireSystemHasNumPlanets(2);
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

        Global.getSector().getFaction("nsp_exponent").setRelationship(Factions.PLAYER,RepLevel.HOSTILE);


        makeImportant(originMarket, "$exponent", Stage.FIRST_TALK);

        /// ///////////////////////////////// EXPONENT SPAWN CODE /////////////////////////////////
        beginStageTrigger(Stage.TAKE_THE_FIGHT_LCF,Stage.TAKE_THE_FIGHT_ALONE);
        triggerCreateFleet(FleetSize.SMALL, FleetQuality.VERY_HIGH, "nsp_exponent", FleetTypes.PATROL_SMALL, system2);
        triggerFleetSetSingleShipOnly();
        triggerFleetSetFlagship(Global.getSettings().getVariant("nsp_exponent_ascendant"));
        triggerFleetSetName("The Exponent");
        triggerFleetSetNoFactionInName();
        triggerFleetNoAutoDespawn();
        triggerFleetNoJump();
        triggerSetFleetNotBusy();
        triggerMakeFleetIgnoredByOtherFleets();
        triggerMakeFleetIgnoreOtherFleetsExceptPlayer();
        triggerOrderFleetInterceptPlayer(false,false);
        triggerOrderFleetEBurn(1.0f);
        triggerFleetInterceptPlayerOnSight(false,Stage.TAKE_THE_FIGHT_LCF,Stage.TAKE_THE_FIGHT_ALONE);
        triggerSetFleetFaction("nsp_exponent");
        triggerSetFleetCommander(NSPPeople.getPerson(NSPPeople.EXPONENT_CORE));
//        triggerPickLocationAroundEntity(originMarket.getPrimaryEntity(),0);
//        triggerPickLocationAtClosestToPlayerJumpPoint(system2);
        triggerPickLocationAroundEntity(getPlanetEntityFromSystem(system2),100);
        triggerSpawnFleetAtPickedLocation();
        triggerFleetMakeImportant("$nsp_exponent", Stage.TAKE_THE_FIGHT_LCF,Stage.TAKE_THE_FIGHT_EXP,Stage.TAKE_THE_FIGHT_ALONE);
        triggerFleetAddDefeatTrigger("nspExponentPostFight");
        triggerSetFleetMemoryValue(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY,true);
        triggerSetFleetMemoryValue("$hailing", true);
        triggerSetFleetMemoryValue(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN,new ExponentFIDConfig.ExpFIDConfig());
        triggerSetFleetMemoryValue(MemFlags.MEMORY_KEY_MAKE_PREVENT_DISENGAGE,true);
        triggerSetFleetMemoryValue("$nsp_isExponentFleet",true);
        triggerSaveFleetRef(Global.getSector().getMemoryWithoutUpdate(),"$nsp_exponentFleet");
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
        triggerFleetMakeImportant("$nsp_exponent", Stage.TAKE_THE_FIGHT_LCF,Stage.TAKE_THE_FIGHT_EXP,Stage.TAKE_THE_FIGHT_ALONE);
        triggerSetFleetMemoryValue(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN,new ExponentLCFleetFidConfig.LuddicEscortFIDConfig());
        triggerSetFleetMemoryValue(MemFlags.MEMORY_KEY_MAKE_PREVENT_DISENGAGE,true);
        triggerSetFleetMemoryValue("$nsp_isExponentLCEscort",true);
        triggerSaveFleetRef(Global.getSector().getMemoryWithoutUpdate(),"$nsp_exponentLuddicFleet");
//        triggerFleetAddDefeatTrigger("nspFoughtLCF");
        endTrigger();
        /// ///////////////////////////////////////////////////////////////////////////////////////






        // set our starting, success and failure stages
//        setStartingStage(Stage.INVESTIGATE);
        setStartingStage(Stage.FIRST_TALK);
        setSuccessStage(Stage.COMPLETED);
//        setFailureStage(Stage.SOLD_SIERRA);
        addFailureStages(Stage.EXPONENT_JOINED,Stage.RELEASED_EXPONENT,Stage.RECOVERED_EXPONENT);
        setNoAbandon();

        // set stage transitions when certain global flags are set
//        setStageOnGlobalFlag(Stage.REPORT_BACK, "$exponent_hasInvestigated");
//        setStageOnGlobalFlag(Stage.TAKE_THE_FIGHT_LCF, "$exponent_goWithFleet");
//        setStageOnGlobalFlag(Stage.TAKE_THE_FIGHT_ALONE, "$exponent_goAlone");
//        connectWithGlobalFlag(Stage.TAKE_THE_FIGHT_LCF, Stage.RETURN_POST_FIGHT, "$exponent_hasFought");
//        connectWithGlobalFlag(Stage.TAKE_THE_FIGHT_ALONE, Stage.RETURN_POST_FIGHT, "$exponent_hasFought");

        connectWithGlobalFlag(Stage.FIRST_TALK, Stage.INVESTIGATE, "$exponent_hadFirstTalk");
        connectWithGlobalFlag(Stage.INVESTIGATE, Stage.REPORT_BACK, "$exponent_hasInvestigated");
        connectWithGlobalFlag(Stage.REPORT_BACK, Stage.TAKE_THE_FIGHT_LCF, "$exponent_goWithFleet");
        // In case of joining The Exponent in the fight
        connectWithGlobalFlag(Stage.TAKE_THE_FIGHT_LCF,Stage.TAKE_THE_FIGHT_EXP, "$exponent_joinExponent");
        connectWithGlobalFlag(Stage.TAKE_THE_FIGHT_EXP,Stage.CONTACT_EXPONENT, "$exponent_contactExponent");

        // ///////////////////////////////

        connectWithGlobalFlag(Stage.TAKE_THE_FIGHT_LCF, Stage.RETURN_POST_FIGHT, "$exponent_hasFought");
        connectWithGlobalFlag(Stage.REPORT_BACK, Stage.TAKE_THE_FIGHT_ALONE, "$exponent_goAlone");
        connectWithGlobalFlag(Stage.TAKE_THE_FIGHT_ALONE, Stage.RETURN_POST_FIGHT, "$exponent_hasFought");
        connectWithGlobalFlag(Stage.RETURN_POST_FIGHT, Stage.COMPLETED, "$exponent_completed");

//        // Releasing the Exponent instead of letting it join you
//        connectWithGlobalFlag(Stage.CONTACT_EXPONENT,Stage.RETURN_NO_FIGHT, "$exponent_releasedExponent");
//        connectWithGlobalFlag(Stage.TAKE_THE_FIGHT_ALONE,Stage.RETURN_NO_FIGHT, "$exponent_releasedExponent");
//        // Have the Exponent join you instead of releasing it
//        connectWithGlobalFlag(Stage.CONTACT_EXPONENT,Stage.RETURN_NO_FIGHT, "$exponent_exponentJoined");


        setStageOnGlobalFlag(Stage.COMPLETED, "$exponent_completed");
        setStageOnGlobalFlag(Stage.EXPONENT_JOINED, "$exponent_exponentJoined");
        setStageOnGlobalFlag(Stage.RELEASED_EXPONENT, "$exponent_releasedExponent");
        setStageOnGlobalFlag(Stage.RECOVERED_EXPONENT, "$exponent_recoveredExponent");
//        setStageOnGlobalFlag(Stage.SOLD_SIERRA, "$apromise_soldsierra");

        setRepRewardFaction(0.2f);
        setCreditReward(300000);

        return true;

    }

    private SectorEntityToken getPlanetEntityFromSystem(StarSystemAPI system2) {
        WeightedRandomPicker<PlanetAPI> picker = new WeightedRandomPicker<>();
        List<PlanetAPI> possiblePlanets = new ArrayList<>();
        for (PlanetAPI planet : system2.getPlanets()) {
            if (planet == system2.getStar()) continue;
            possiblePlanets.add(planet);
        }
        picker.addAll(possiblePlanets);
        PlanetAPI pickerPlanet = picker.pick();
        for (PlanetAPI planet : system2.getPlanets()) {
            if (planet != pickerPlanet) continue;
            Global.getSector().getMemoryWithoutUpdate().set("$system2Planet",planet);
            exponentSpawnPlanet = planet;
            return planet;
        }
        throw new RuntimeException("Failed to spawn the Exponent");
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
            CampaignFleet luddicEscort = (CampaignFleet) Global.getSector().getMemoryWithoutUpdate().get("$nsp_exponentLuddicFleet");
            if (luddicEscort != null) {
//                luddicFleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE,Global.getSector().getPlayerFleet(),9999f,"Escort  ing your fleet");
                luddicEscort.addAbility(Abilities.TRANSVERSE_JUMP);
                luddicEscort.addScript(new ExponentMissionLCFleetEscort(luddicEscort));
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
            makeImportant(originMarket, "$exponent", Stage.RETURN_POST_FIGHT,Stage.RETURN_NO_FIGHT);

            CampaignFleet luddicEscort = (CampaignFleet) Global.getSector().getMemoryWithoutUpdate().get("$nsp_exponentLuddicFleet");
            luddicEscort.removeScriptsOfClass(ExponentMissionLCFleetEscort.class);
            luddicEscort.clearAssignments();
            luddicEscort.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,originMarket.getPlanetEntity(),9999f);
            return true;
        }

        if (action.equals("transferExponent")) {
            Global.getSector().getFaction("nsp_exponent").setRelationship(Factions.PLAYER,RepLevel.COOPERATIVE);
            CampaignFleet exponentFleet = (CampaignFleet) dialog.getInteractionTarget();
            FleetMemberAPI newMember = Global.getFactory().createFleetMember(FleetMemberType.SHIP,"nsp_exponent_enlightened");
//            newMember.setCaptain(exponentFleet.getCommander());
            Global.getSector().getPlayerFleet().getFleetData().addFleetMember(newMember);
            exponentFleet.despawn();
            return true;
        }
        if (action.equals("releaseExponent")) {
//            Global.getSector().getFaction("nsp_exponent").setRelationship(Factions.PLAYER,RepLevel.COOPERATIVE);
            CampaignFleet exponentFleet = (CampaignFleet) dialog.getInteractionTarget();
            exponentFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PURSUE_PLAYER,false);
            exponentFleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_DO_NOT_IGNORE_PLAYER,false);
            exponentFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_IGNORE_PLAYER_COMMS,true);
            exponentFleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS,true);
            exponentFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE,false);
            exponentFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED,true);
            exponentFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_PREVENT_DISENGAGE,false);
            exponentFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE,true);
            exponentFleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,getPlanetEntityFromSystem(system2),9999f);
            return true;
        }

        if (action.equals("recoverExponent")) {
//            setCurrentStage(Stage.RECOVERED_EXPONENT,dialog,memoryMap);
            Global.getSector().getFaction(Factions.LUDDIC_CHURCH).setRelationship(Factions.PLAYER,-0.49f);
            dialog.getTextPanel().setFontSmallInsignia();
            dialog.getTextPanel().addPara("Relationship with the Luddic Church %s, currently at %s",Misc.getGrayColor(),Misc.getNegativeHighlightColor(),"reduced to inhospitable","50/100 (inhospitable)");
            dialog.getTextPanel().setFontInsignia();
            return true;
        }

        if (action.equals("dialogTest")) {

            Global.getSector().getMemoryWithoutUpdate().set("$nsp_defExponentShip", true);


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

        return true;
    }

    protected void updateInteractionDataImpl() {
        set("$exponentSystemOne",system1);
        set("$exponentSystemTwo",system2);
        set("$exponent_system1SName", system1.getNameWithNoType());
        set("$exponent_system2SName", system2.getNameWithNoType());
        set("$exponentCurrentStage", getCurrentStage());
//        set("$exponent_Contact", getPerson());
//        set("$exponent_manOrWoman", getPerson().getManOrWoman());
//        set("$exponent_hisOrHer", getPerson().getHisOrHer());

    }

    // description when selected in intel screen
    @Override
    public void addDescriptionForNonEndStage(TooltipMakerAPI info, float width, float height) {
        float opad = 10f;
        Color h = Misc.getHighlightColor();

        if (currentStage == Stage.FIRST_TALK) {
            info.addPara("You chose to follow some Luddic Knight's attendant to meet them.", opad);
            info.addPara("See why you have been flagged down specifically", opad);
        } else if (currentStage == Stage.INVESTIGATE) {
            info.addPara("You've heard rumors from a senior Luddic Knight about a strike fleet being MIA after being " +
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
        } else if (currentStage == Stage.TAKE_THE_FIGHT_EXP) {
            info.addPara("[Chose to join The Exponent's side in the fight to repel the Luddic Church escort fleet]", opad);
            info.addPara("Defeat the Luddic Church fleet and assure The Exponent's survival.", opad);
        } else if (currentStage == Stage.CONTACT_EXPONENT) {
            info.addPara("[Repelled the Luddic Church escort fleet, answer the Exponent's hail]", opad);
            info.addPara("Answer the Exponent's Hail.", opad);
        } else if (currentStage == Stage.RETURN_POST_FIGHT) {
            info.addPara("You successfully defeated the Exponent in the " + system2.getNameWithLowercaseTypeShort() +
                    " and [something].", opad);
            info.addPara("Return to " + originMarket.getName() + " for debrief.", opad);
        } else if (currentStage == Stage.RETURN_NO_FIGHT) {
            info.addPara("You chose not to fight the Exponent in the " + system2.getNameWithLowercaseTypeShort() + ".", opad);
            info.addPara("Return to " + originMarket.getName() + " and report back.", opad);
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
        if (currentStage == Stage.FIRST_TALK) {
            info.addPara("Talk to the senior Luddic Knight.", tc, pad);
            return true;
        } else if (currentStage == Stage.INVESTIGATE) {
            info.addPara("Search the " + system1.getNameWithLowercaseTypeShort() + ".", tc, pad);
            return true;
        } else if (currentStage == Stage.REPORT_BACK) {
            info.addPara("Report back at " + originMarket.getName() + ".", tc, pad);
            return true;
        } else if (currentStage == Stage.TAKE_THE_FIGHT_ALONE) {
            info.addPara("Defeat the Exponent.", tc, pad);
            return true;
        } else if (currentStage == Stage.TAKE_THE_FIGHT_LCF) {
            info.addPara("Defeat the Exponent.", tc, pad);
            return true;
        } else if (currentStage == Stage.TAKE_THE_FIGHT_EXP) {
            info.addPara("Defeat the Luddic Church escort fleet and assure The Exponent's survival.", tc, pad);
            return true;
        } else if (currentStage == Stage.CONTACT_EXPONENT) {
            info.addPara("Answer the Exponent's Hail.", tc, pad);
            return true;
        } else if (currentStage == Stage.RETURN_POST_FIGHT) {
            info.addPara("Return to " + originMarket.getName() + " for debrief.", tc, pad);
            return true;
        } else if (currentStage == Stage.RETURN_NO_FIGHT) {
            info.addPara("Return to " + originMarket.getName() + " and report back.", tc, pad);
            return true;
        }

        return false;
    }
}
