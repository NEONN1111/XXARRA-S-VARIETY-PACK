package neon.nsp.data.scripts.rulecmd.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

/**
 * Dude it's totally not a copy of Iron Shell AddShip ruleCMD, which is
 * totally not a copy of Console Command's AddShip trust me!
 * nsp_AddOfficer <person_id>
 */
public class nsp_AddOfficer extends BaseCommandPlugin {
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        String variant1 = params.get(0).getString(memoryMap);
        PersonAPI person = Global.getSector().getImportantPeople().getPerson(variant1);

        Global.getSector().getPlayerFleet().getFleetData().addOfficer(person);
        AddRemoveCommodity.addOfficerGainText(person, dialog.getTextPanel());
        return true;

    }
}


