package neon.nsp.data.scripts.campaign.missions.invicta;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.People;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.academy.GAIntro2;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.fleet.CampaignFleet;
import neon.nsp.data.campaign.intel.misc.NSPInvictaConvIntel;
import neon.nsp.data.scripts.campaign.missions.ExponentMission;
import neon.nsp.data.scripts.campaign.missions.ExponentMissionLCFleetEscort;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class FirstContactMission extends HubMissionWithSearch {

    public static enum Stage {
        GO_TO_VIGIL_SYSTEM,
        GO_TO_VIGIL_COMM_RELAY,
        COMPLETED,
    }

    protected PersonAPI invictonymous;

    @Override
    protected boolean create(MarketAPI createdAt, boolean barEvent) {

        // if already accepted by the player, abort
        if (!setGlobalReference("$invictaFirstContact_ref")) {
            //System.out.print("aborting because missions is already accepted");
            return false;
        }

        invictonymous = getImportantPerson("nsp_invicta_core");
        if (invictonymous == null) return false;

        setStartingStage(Stage.GO_TO_VIGIL_SYSTEM);
        addSuccessStages(Stage.COMPLETED);

        setStoryMission();

        StarSystemAPI vigil = Global.getSector().getStarSystem("Vigil");
        makeImportant(Global.getSector().getStarSystem("Vigil").getStar(), "$invictaFirstContact_toVigil", Stage.GO_TO_VIGIL_SYSTEM);
//        setStageOnGlobalFlag(Stage.GO_TO_VIGIL_SYSTEM, "$invictaFirstContact_started"); // so it isn't offered again
//        connectWithGlobalFlag(Stage.GO_TO_VIGIL_SYSTEM, Stage.GO_TO_VIGIL_COMM_RELAY, "$invictaFirstContact_vigilCommRelay");

        makeImportant(getVigilStarSystemCommRelay(), "$invictaFirstContact_vigilCommRelay", Stage.GO_TO_VIGIL_COMM_RELAY);
//        setStageOnGlobalFlag(Stage.GO_TO_VIGIL_COMM_RELAY, "$invictaFirstContact_commRelay");
        setStageOnEnteredLocation(Stage.GO_TO_VIGIL_COMM_RELAY, vigil);
        setStageOnGlobalFlag(Stage.COMPLETED, "$invictaFirstContact_completed");

        setRepFactionChangesNone();
        setRepPersonChangesNone();


        return true;
    }

    protected void updateInteractionDataImpl() {
        set("$invictaFirstContact_stage", getCurrentStage());
    }

    @Override
    public void addDescriptionForNonEndStage(TooltipMakerAPI info, float width, float height) {
        float opad = 10f;
        //Color h = Misc.getHighlightColor();
        if (currentStage == Stage.GO_TO_VIGIL_SYSTEM) {
            info.addPara("Go to the Vigil system.", opad);
        }
        if (currentStage == Stage.GO_TO_VIGIL_COMM_RELAY) {
            info.addPara("Go to Vigil's comm relay.", opad);
        }
    }

    @Override
    public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
        //Color h = Misc.getHighlightColor();
        if (currentStage == Stage.GO_TO_VIGIL_SYSTEM) {
            info.addPara("Go to the Vigil system", tc, pad);
            return true;
        }
        if (currentStage == Stage.GO_TO_VIGIL_COMM_RELAY) {
            info.addPara("Go to Vigil's comm relay", tc, pad);
            return true;
        }
        return false;
    }


    @Override
    public String getBaseName() {
        return "First Contact";
    }

    private SectorEntityToken getVigilStarSystemCommRelay() {
        StarSystemAPI vigil = Global.getSector().getStarSystem("Vigil");
        try {
            return vigil.getEntitiesWithTag(Tags.COMM_RELAY).get(0);
        } catch (Exception e) {
            throw new RuntimeException("Vigil star system is missing a comm relay.\n" + e);
        }
    }

}