package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import java.util.List;
import java.util.Map;

public class NSPShowInvictusPersonVisual extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {


        String command = "";
        if (!params.isEmpty()) {
            command = params.get(0).getString(memoryMap);
        }

        VisualPanelAPI visualPanel = dialog.getVisualPanel();
        FactionAPI luddicChurch = Global.getSector().getFaction(Factions.KOL);

        // Luddic Knight-Captain portrait pickers (to ensure a proper captain looking portrait)
        WeightedRandomPicker<String> knightPortraitsMale = new WeightedRandomPicker<>();
        WeightedRandomPicker<String> knightPortraitsFemale = new WeightedRandomPicker<>();
        knightPortraitsMale.add("graphics/portraits/portrait_luddic05.png");
        knightPortraitsMale.add("graphics/portraits/portrait_luddic06.png");
        knightPortraitsFemale.add("graphics/portraits/portrait_luddic07.png");
        knightPortraitsMale.add("graphics/portraits/portrait_luddic08.png"); // Helmeted knight
        knightPortraitsFemale.add("graphics/portraits/portrait_luddic08.png"); // Helmeted knight
        knightPortraitsMale.add("graphics/portraits/portrait_luddic09.png");
        knightPortraitsFemale.add("graphics/portraits/portrait_luddic10.png");
        knightPortraitsFemale.add("graphics/portraits/portrait_luddic11.png");

        PersonAPI invictusKnightCaptain;
        if (!memoryMap.get(MemKeys.LOCAL).contains("$knightCaptain")) {
            invictusKnightCaptain = luddicChurch.createRandomPerson();

            // Set to proper knight-captain portrait sprite
            if (invictusKnightCaptain.isFemale()) {
                String pickedPortrait = knightPortraitsFemale.pick();
                knightPortraitsFemale.remove(pickedPortrait);
                invictusKnightCaptain.setPortraitSprite(pickedPortrait);
            }
            else {
                String pickedPortrait = knightPortraitsMale.pick();
                knightPortraitsFemale.remove(pickedPortrait);
                invictusKnightCaptain.setPortraitSprite(pickedPortrait);
            }

            invictusKnightCaptain.setFaction(Factions.KOL);
            invictusKnightCaptain.setId("nspInvictusKnightCaptain");
            invictusKnightCaptain.setRankId(Ranks.KNIGHT_CAPTAIN);
            invictusKnightCaptain.setPostId("luddicKnight");

            memoryMap.get(MemKeys.LOCAL).set("$knightCaptain",invictusKnightCaptain);
        }
        else {
            invictusKnightCaptain = (PersonAPI) memoryMap.get(MemKeys.LOCAL).get("$knightCaptain");
            knightPortraitsFemale.remove(invictusKnightCaptain.getPortraitSprite());
        }

        visualPanel.showPersonInfo(invictusKnightCaptain,false,true);

        if (command.equals("showSecond")) {
            PersonAPI invictusSecondKnight = luddicChurch.createRandomPerson();
            if (invictusSecondKnight.isFemale()) {
                String pickedPortrait = knightPortraitsFemale.pick();
                knightPortraitsFemale.remove(pickedPortrait);
                invictusSecondKnight.setPortraitSprite(pickedPortrait);
            }
            else {
                String pickedPortrait = knightPortraitsMale.pick();
                knightPortraitsFemale.remove(pickedPortrait);
                invictusSecondKnight.setPortraitSprite(pickedPortrait);
            }
            invictusSecondKnight.setFaction(Factions.KOL);
            invictusSecondKnight.setId("nspInvictusKnightCaptain");
            invictusSecondKnight.setRankId("luddicKnight");
            invictusSecondKnight.setPostId("luddicKnight");

            memoryMap.get(MemKeys.LOCAL).set("$secondKnight",invictusSecondKnight);

            visualPanel.showSecondPerson(invictusSecondKnight);

        }


        return false;
    }
}
