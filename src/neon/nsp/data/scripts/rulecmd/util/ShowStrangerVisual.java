package neon.nsp.data.scripts.rulecmd.util;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class ShowStrangerVisual extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {

        boolean minimal = params.get(0).getBoolean(memoryMap);
        dialog.getVisualPanel().showPersonInfo((PersonAPI) memoryMap.get(MemKeys.LOCAL).get("$dialogStranger"),minimal);

        return false;
    }
}
