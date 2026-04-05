package neon.nsp.data.campaign.customstart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.CharacterCreationData;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.impl.campaign.rulecmd.newgame.NGCAddStartingShipsByFleetType;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.util.MagicCampaign;
import org.magiclib.util.MagicVariables;
import exerelin.campaign.ExerelinSetupData;
import exerelin.campaign.PlayerFactionStore;
import exerelin.campaign.customstart.CustomStart;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ContactWithThreat extends CustomStart {

    protected List<String> ships = new ArrayList<>(
            Arrays.asList(
                    new String[]{
                            "wayfarer_Starting", "assault_unit_Type200", "skirmish_unit_Type101"}
            )
    );

    @Override
    public void execute(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        PlayerFactionStore.setPlayerFactionIdNGC(Factions.PLAYER);
        ExerelinSetupData.getInstance().freeStart = true;

        CharacterCreationData data = (CharacterCreationData) memoryMap.get(MemKeys.LOCAL).get("$characterData");

        NGCAddStartingShipsByFleetType.generateFleetFromVariantIds(dialog, data, null, ships);

        data.getStartingCargo().addCommodity("nsp_threat_processor", 2);


        MutableCharacterStatsAPI stats = data.getPerson().getStats();
        stats.setSkillLevel("nsp_threat_auto", 2f);

        // Optionally add a message to the dialog to confirm the skill was added
        dialog.getTextPanel().addParagraph("Gained skill: Abyss Rigging", Misc.getPositiveHighlightColor());

        data.addScriptBeforeTimePass(
                new Script() {
                    @Override
                    public void run() {

                        SectorEntityToken location = null;
                        for (Integer i = 0; i < 9; i++) {

                            List<String> themes = new ArrayList<>();
                            themes.add(Tags.THEME_INTERESTING);
                            themes.add(Tags.THEME_RUINS_MAIN);

                            List<String> notThemes = new ArrayList<>();
                            notThemes.add(MagicVariables.AVOID_COLONIZED_SYSTEM);
                            notThemes.add(MagicVariables.AVOID_BLACKHOLE_PULSAR);
                            notThemes.add(MagicVariables.AVOID_OCCUPIED_SYSTEM);
                            notThemes.add(Tags.THEME_SPECIAL);
                            notThemes.add("theme_hidden");

                            List<String> entities = new ArrayList<>();
                            entities.add(Tags.GAS_GIANT);

                            SectorEntityToken token = MagicCampaign.findSuitableTarget(
                                    null,
                                    null,
                                    "FAR",
                                    themes,
                                    notThemes,
                                    entities,
                                    false,
                                    true,
                                    false
                            );

                            if (token != null) {
                                location = token;
                                break;
                            }
                        }

                        Global.getSector().getMemoryWithoutUpdate().set("$nex_startLocation", location.getId());

                    }
                }
        );

        FireBest.fire(null, dialog, memoryMap, "ExerelinNGCStep4");
    }
}