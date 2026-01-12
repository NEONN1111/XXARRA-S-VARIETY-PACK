package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class NSPExponentLCEnvoyPersonVisual extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {

//        boolean minimal = false;
//        if (!params.isEmpty()) {
//            minimal = params.get(0).getBoolean(memoryMap);
//        }

        PersonAPI envoy = Global.getSector().getFaction(Factions.LUDDIC_CHURCH).createRandomPerson(FullName.Gender.ANY);
        envoy.setId("NSPExponentEnvoy");
        envoy.setPortraitSprite("graphics/portraits/portrait_luddic14.png");
        envoy.setPostId("luddicEnvoy");
        envoy.setRankId("luddicEnvoy");

        dialog.getVisualPanel().showPersonInfo(envoy, false);

        return true;
    }
}
