package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class NSPExponentSwitchSides extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {


        if (!(dialog.getPlugin() instanceof FleetInteractionDialogPluginImpl))
            throw new RuntimeException("Dialog is not an instance of FleetInteractionDialogPluginImpl");

//        FleetInteractionDialogPluginImpl fid = (FleetInteractionDialogPluginImpl) Global.getSector().getCampaignUI().getCurrentInteractionDialog().getPlugin();
        FleetInteractionDialogPluginImpl fid = (FleetInteractionDialogPluginImpl) dialog.getPlugin();
        FleetEncounterContext context = (FleetEncounterContext) fid.getContext();
        // Get Luddic Fleet from context
//        CampaignFleetAPI luddicFleet = null;
//        for (CampaignFleetAPI playerSideFleet : context.getBattle().getPlayerSide()) {
//            if (!playerSideFleet.getName().contains("Placeholder")) continue;
//            luddicFleet = playerSideFleet;
//            break;
//        }
//        if (luddicFleet == null) throw new NullPointerException("NSPExponentSwitchSides > Unable to identify Luddic Church fleet");
//        Console.showMessage(luddicFleet.getName());

        CampaignFleetAPI exponentFleet = (CampaignFleetAPI) Global.getSector().getMemoryWithoutUpdate().get("$nsp_exponentFleet");
        CampaignFleetAPI luddicFleet = (CampaignFleetAPI) Global.getSector().getMemoryWithoutUpdate().get("$nsp_exponentLuddicFleet");
        FleetDataAPI exponentFleetData = exponentFleet.getFleetData();
        FleetDataAPI luddicFleetFleetData = luddicFleet.getFleetData();

        BattleAPI battleAPI = context.getBattle();
        battleAPI.leave(exponentFleet,false);
        battleAPI.leave(luddicFleet,false);
        battleAPI.join(exponentFleet, BattleAPI.BattleSide.ONE);
        battleAPI.join(luddicFleet, BattleAPI.BattleSide.TWO);

        CampaignFleetAPI attacker = luddicFleet;
        CampaignFleetAPI defender = exponentFleet;
        attacker.setFaction(Factions.LUDDIC_CHURCH);

        // Switch Luddic fleets to be the attacker fleet, and have the Exponent join the player as ally.
        for (FleetMemberAPI luddicMember : luddicFleetFleetData.getMembersListCopy()) {
            context.getBattle().getCombinedFor(attacker).getFleetData().addFleetMember(luddicMember);
        }
        context.getBattle().getCombinedFor(attacker).setFaction(Factions.LUDDIC_CHURCH);
        for (FleetMemberAPI exponentMember : exponentFleetData.getMembersListCopy()) {
            context.getBattle().getCombinedFor(defender).getFleetData().addFleetMember(exponentMember);
        }

        defender.getFaction().setRelationship(Factions.PLAYER,0.5f);

        dialog.setInteractionTarget(attacker);
//        exponentFleet = (CampaignFleetAPI) Global.getSector().getMemoryWithoutUpdate().get("$nsp_exponentFleet");
//        exponentFleet.getFaction().setRelationship(Factions.PLAYER,0.5f);

        return true;
    }
}
