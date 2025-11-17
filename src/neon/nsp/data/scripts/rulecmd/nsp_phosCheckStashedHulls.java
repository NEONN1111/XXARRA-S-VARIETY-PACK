package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

import static neon.nsp.data.scripts.rulecmd.nsp_displayAutomatedPicker.*;
import static neon.nsp.data.scripts.rulecmd.nsp_displaySafeguardPicker.*;

public class nsp_phosCheckStashedHulls extends BaseCommandPlugin {
    // if the player currently has a hull with phos but not ready, and we open comm link, display the hull name and how long it's going to be
    static String thing = "\"If you're wondering about the ship in our care...\"";
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (Global.getSector().getMemoryWithoutUpdate().get(safeguardpickedKey) != null){
            dialog.getTextPanel().addPara(thing);
            FleetMemberAPI member = (FleetMemberAPI) Global.getSector().getMemoryWithoutUpdate().get(safeguardpickedKey);
            float days =  Global.getSector().getMemoryWithoutUpdate().getExpire(safeguarddaysKey);
            String endpara = String.valueOf((int) days) + " days.\"";
            if (days < 1f) endpara = "under a day.\"";
            String name = member.getShipName();
            String spritename = member.getHullSpec().getSpriteName();
            TooltipMakerAPI tip = dialog.getTextPanel().beginTooltip();
            TooltipMakerAPI image =  tip.beginImageWithText(spritename, 64f);
            image.addTitle(safeguardTitle, Misc.getHighlightColor());
            image.addPara("\"The " + name + " should be prepared for transfer in roughly " + endpara, 5f).setHighlight(name,String.valueOf((int) days) );
            tip.addImageWithText(10f);
            dialog.getTextPanel().addTooltip();

            return true;
        }
        if (Global.getSector().getMemoryWithoutUpdate().get(automatedpickedKey) != null){
            dialog.getTextPanel().addPara(thing);
            FleetMemberAPI member = (FleetMemberAPI) Global.getSector().getMemoryWithoutUpdate().get(automatedpickedKey);
            float days =  Global.getSector().getMemoryWithoutUpdate().getExpire(automateddaysKey);
            String endpara = String.valueOf((int) days) + " days.\"";
            if (days < 1f) endpara = "under a day.\"";
            String name = member.getShipName();
            String spritename = member.getHullSpec().getSpriteName();
            TooltipMakerAPI tip = dialog.getTextPanel().beginTooltip();
            TooltipMakerAPI image =  tip.beginImageWithText(spritename, 64f);
            image.addTitle(autoSystemsTitle, Misc.getHighlightColor());
            image.addPara("\"The " + name + " should be prepared for transfer in roughly " + endpara, 5f).setHighlight(name,String.valueOf((int) days) );
            tip.addImageWithText(10f);
            dialog.getTextPanel().addTooltip();
            return true;
        }
        return false;
    }
}
