package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Sounds;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.List;
import java.util.Map;

public class NSP_GetDerelictDefenders extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog,
                           List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        MemoryAPI memory = getEntityMemory(memoryMap);
        int type = params.get(0).getInt(memoryMap);
        Color color = new Color(100, 250, 210, 250);

        switch (type) {
            case 1:
                if (memory.getFleet("$defenderFleet") == null) {
                    return false;
                }
                if (memory.getFleet("$defenderFleet").getFaction().getId().equals(Factions.DERELICT)) {
                    return true;
                }
                break;

            case 2:
                dialog.getVisualPanel().showFleetInfo("Explorarium Automated Defenses",
                        memory.getFleet("$defenderFleet"), null, null);
                dialog.getOptionPanel().addOption("Transmit your Explorarium IFF code",
                        "nsp_yeetDerelicts", color, "");
                break;

            case 3:
                dialog.getVisualPanel().fadeVisualOut();
                memory.unset("$hasDefenders");
                memory.unset("$defenderFleet");
                memory.set("$defenderFleetDefeated", true);
                Global.getSoundPlayer().playUISound(Sounds.STORY_POINT_SPEND_TECHNOLOGY, 1f, 1f);
                return true;
        }

        return false;
    }
}