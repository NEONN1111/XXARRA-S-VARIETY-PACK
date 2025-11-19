package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class NSPCheckPostFight extends BaseCommandPlugin {
    protected MemoryAPI memory;
    protected InteractionDialogAPI dialog;
    protected Map<String, MemoryAPI> memoryMap;

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {


        boolean exponentNotCompleted = !Global.getSector().getMemoryWithoutUpdate().contains("$nsp_exponentCompleted");
        boolean beganExponent = Global.getSector().getMemoryWithoutUpdate().getBoolean("$nsp_beganExponent");
        boolean hasFought = Global.getSector().getMemoryWithoutUpdate().getBoolean("$exponent_hasFought");
        MarketAPI originMarket = (MarketAPI) Global.getSector().getMemoryWithoutUpdate().get("$nsp_exponentMarket");
        boolean isOriginMarket;
        if (dialog.getInteractionTarget() != null && dialog.getInteractionTarget().getMarket() != null) {
            isOriginMarket = dialog.getInteractionTarget().getMarket() == originMarket;
        } else return false;

        return exponentNotCompleted && beganExponent &&
                hasFought && isOriginMarket;

    }
}
