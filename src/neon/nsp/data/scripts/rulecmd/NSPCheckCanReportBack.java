package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class NSPCheckCanReportBack extends BaseCommandPlugin {
    protected MemoryAPI memory;
    protected InteractionDialogAPI dialog;
    protected Map<String, MemoryAPI> memoryMap;

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
//        this.dialog = dialog;
//        this.memoryMap = memoryMap;
//
//        String cmd = null;
//        cmd = params.get(0).getString(memoryMap);
//        String param = null;
//        if (params.size() > 1) {
//            param = params.get(1).getString(memoryMap);
//        }
//
//        TextPanelAPI text = dialog.getTextPanel();
        boolean minLevelFive = Global.getSector().getPlayerStats().getLevel() >= 5;
        boolean exponentNotCompleted = !Global.getSector().getMemoryWithoutUpdate().contains("$nsp_exponentCompleted");
        boolean beganExponent = Global.getSector().getMemoryWithoutUpdate().getBoolean("$nsp_beganExponent");
        boolean hasInvestigated = Global.getSector().getMemoryWithoutUpdate().getBoolean("$exponent_hasInvestigated");
        boolean hasReportedBack = !Global.getSector().getMemoryWithoutUpdate().getBoolean("$exponent_hasReported");
        MarketAPI originMarket = (MarketAPI) Global.getSector().getMemoryWithoutUpdate().get("$nsp_exponentMarket");
        boolean isOriginMarket;
        if (dialog.getInteractionTarget() != null && dialog.getInteractionTarget().getMarket() != null) {
            isOriginMarket = dialog.getInteractionTarget().getMarket() == originMarket;
        } else return false;

        return minLevelFive && exponentNotCompleted && beganExponent &&
                hasInvestigated && hasReportedBack && isOriginMarket;
    }
}
