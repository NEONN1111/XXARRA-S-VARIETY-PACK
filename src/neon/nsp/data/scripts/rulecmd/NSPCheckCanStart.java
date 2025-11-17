package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class NSPCheckCanStart extends BaseCommandPlugin {
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

        return Global.getSector().getPlayerStats().getLevel() >= 5 &&
                !Global.getSector().getMemoryWithoutUpdate().contains("$nsp_exponentCompleted") &&
                !Global.getSector().getMemoryWithoutUpdate().getBoolean("$nsp_beganExponent");
    }
}
