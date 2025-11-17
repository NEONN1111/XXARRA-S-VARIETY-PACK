package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import neon.nsp.data.scripts.NSPPeople;

import java.util.List;
import java.util.Map;

public class nsp_addPersonToComms extends BaseCommandPlugin {
    // adds person to market comm directory of player fleet interaction target
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        String personID = params.get(0).getString(memoryMap);
        PersonAPI person = NSPPeople.getPerson(personID);
        if (person == null) return false;
        dialog.getInteractionTarget().getMarket().getCommDirectory().addPerson(person);
        return true;
    }
}
