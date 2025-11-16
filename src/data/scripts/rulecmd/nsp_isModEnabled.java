package data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class nsp_isModEnabled extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        String modID = params.get(0).getString(memoryMap);
        if (Global.getSettings().getModManager().isModEnabled(modID)) return true;
        return false; // AdjustRepActivePerson COOPERATIVE 15 $player.scholarshipThemeUseAI
    }
}
