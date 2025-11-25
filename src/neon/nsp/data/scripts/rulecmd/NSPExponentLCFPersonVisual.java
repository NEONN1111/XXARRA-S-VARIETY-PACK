package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class NSPExponentLCFPersonVisual extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {

        boolean minimal = false;
        if (!params.isEmpty()) {
            minimal = params.get(0).getBoolean(memoryMap);
        }
        CampaignFleetAPI luddicFleet = (CampaignFleetAPI) Global.getSector().getMemoryWithoutUpdate().get("$nsp_exponentLuddicFleet");
        dialog.getVisualPanel().showPersonInfo(luddicFleet.getCommander(), minimal);

        return true;
    }
}
