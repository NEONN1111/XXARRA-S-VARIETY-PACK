package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Voices;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.Faction;

import java.util.List;
import java.util.Map;

public class NSPExponentSetGiver extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {

        PersonAPI missionGiver = Global.getSector().getFaction(Factions.LUDDIC_CHURCH).createRandomPerson();
        missionGiver.setId("exponent_KnightContact");
        missionGiver.setFaction(Factions.LUDDIC_CHURCH);
        missionGiver.setPostId(Ranks.POST_GENERIC_MILITARY);
        missionGiver.setRankId(Ranks.KNIGHT_CAPTAIN);
        missionGiver.setImportance(PersonImportance.HIGH);
        missionGiver.setMarket(dialog.getInteractionTarget().getMarket());
//        missionGiver.setVoice(Voices.);
        Global.getSector().getMemoryWithoutUpdate().set("$exponent_KnightContact",missionGiver);

        return true;
    }
}
