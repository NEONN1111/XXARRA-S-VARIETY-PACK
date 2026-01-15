package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import java.util.List;
import java.util.Map;

public class NSP_getShowPlayership extends BaseCommandPlugin {

    @Override
    public boolean execute(
            String ruleId,
            InteractionDialogAPI dialog,
            List<Misc.Token> params,
            Map<String, MemoryAPI> memoryMap
    ) {
        // Get the ship hull ID from the first parameter
        String ship = params.get(0).getString(memoryMap);

        // Iterate through the player's fleet members
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            if (member.getHullSpec().getBaseHullId().equals(ship)) {
                // Show the fleet member info in the visual panel
                dialog.getVisualPanel().showFleetMemberInfo(member);

                // Store the ship name in memory
                Global.getSector().getMemoryWithoutUpdate().set("$NSP_shownMemberName", member.getShipName());

                return true;
            }
        }

        return false;
    }
}