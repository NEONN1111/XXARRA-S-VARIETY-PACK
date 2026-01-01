package neon.nsp.data.scripts.rulecmd.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class StrangerGenerator extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {

        FactionAPI faction = Global.getSector().getFaction(params.get(0).getString(memoryMap));
        if (memoryMap.get(MemKeys.LOCAL).get("$dialogStranger") == null) {
            PersonAPI stranger = faction.createRandomPerson();
            stranger.setId("dialogStranger");
            stranger.setPostId("nsp_stranger");
            stranger.setFaction(faction.getId());
            memoryMap.get(MemKeys.LOCAL).set("$dialogStranger",stranger);
        }

        return false;
    }
}
