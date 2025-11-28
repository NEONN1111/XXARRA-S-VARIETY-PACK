package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class NSPExponentCheck extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {

        String command = params.get(0).getString(memoryMap);
        if (command == null) return false;

        if (command.equals("hasFactionCommission")) {
            if (Misc.getCommissionFactionId() == null) return false;
            else return Misc.getCommissionFactionId().equals(dialog.getInteractionTarget().getMarket().getFaction().getId());
        }
        else if (command.equals("isPather")) {
            if (Misc.getCommissionFaction() != null) return Misc.getCommissionFaction().getId().equals(Factions.LUDDIC_PATH);
            else return false;
        }
        else if (command.equals("canReportBackExp")) {
            boolean exponentNotCompleted = !Global.getSector().getMemoryWithoutUpdate().contains("$nsp_exponentCompleted");
            boolean beganExponent = Global.getSector().getMemoryWithoutUpdate().getBoolean("$nsp_beganExponent");
            boolean hasInvestigated = Global.getSector().getMemoryWithoutUpdate().getBoolean("$exponent_hasInvestigated");
            boolean hasReportedBack = !Global.getSector().getMemoryWithoutUpdate().getBoolean("$exponent_hasReported");
            MarketAPI originMarket = (MarketAPI) Global.getSector().getMemoryWithoutUpdate().get("$nsp_exponentMarket");
            boolean isOriginMarket;
            if (dialog.getInteractionTarget() != null && dialog.getInteractionTarget().getMarket() != null) {
                isOriginMarket = dialog.getInteractionTarget().getMarket() == originMarket;
            } else return false;

            return exponentNotCompleted && beganExponent && hasInvestigated && hasReportedBack && isOriginMarket;
        }
        else if (command.equals("welcomingOrHigher")) {
            RepLevel repLevel = dialog.getInteractionTarget().getMarket().getFaction().getRelationshipLevel(Factions.PLAYER);
            return (repLevel == RepLevel.WELCOMING || repLevel == RepLevel.FRIENDLY || repLevel == RepLevel.COOPERATIVE);
        }
        else if (command.equals("neutralOrFavorable")) {
            RepLevel repLevel = dialog.getInteractionTarget().getMarket().getFaction().getRelationshipLevel(Factions.PLAYER);
            return (repLevel == RepLevel.NEUTRAL || repLevel == RepLevel.FAVORABLE);
        }
        else if (command.equals("susOrLower")) {
            RepLevel repLevel = dialog.getInteractionTarget().getMarket().getFaction().getRelationshipLevel(Factions.PLAYER);
            return (repLevel == RepLevel.SUSPICIOUS || repLevel == RepLevel.VENGEFUL ||
                    repLevel == RepLevel.HOSTILE || repLevel == RepLevel.INHOSPITABLE );
        }
        return false;
    }
}
