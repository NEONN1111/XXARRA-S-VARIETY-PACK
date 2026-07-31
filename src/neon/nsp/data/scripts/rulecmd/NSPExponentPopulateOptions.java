package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class NSPExponentPopulateOptions extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {

        String command = params.get(0).getString(memoryMap);
        if (command == null) return false;

        TextPanelAPI textPanel = dialog.getTextPanel();
        OptionPanelAPI optionPanel = dialog.getOptionPanel();
        VisualPanelAPI visualPanel = dialog.getVisualPanel();

        if (command.equals("acceptRefuse")) {
            optionPanel.clearOptions();
            optionPanel.addOption("\"I'll do it. Pray for my success.\"","EBEbarKnight_acceptFaithful");
            optionPanel.addOption("\"I'll find your fleet. And cast that Abomination back to Moloch.\"","EBEbarKnight_acceptPather");
            optionPanel.addOption("\"I'll see what I can do.\"","EBEbarKnight_acceptOther");
            optionPanel.addOption( "I'll have to decline.","EBEbarKnight_refuse");

            if (Misc.getCommissionFactionId() == null || (Misc.getCommissionFactionId() != null
                    && !Misc.getCommissionFactionId().equals(Factions.LUDDIC_PATH))) {
                optionPanel.setEnabled("EBEbarKnight_acceptPather",false);
                optionPanel.setTooltip("EBEbarKnight_acceptPather","Requires a Luddic Path commission");
                optionPanel.setTooltipHighlights("EBEbarKnight_acceptPather","Luddic Path");
                optionPanel.setTooltipHighlightColors("EBEbarKnight_acceptPather",
                        Global.getSector().getFaction(Factions.LUDDIC_PATH).getColor()
                );
            }
            return true;
        }
        if (command.equals("exponentCommLinkOne")) {
            optionPanel.clearOptions();
            optionPanel.addOption("\"I was sent to to hunt down an 'abomination'. Looks like I found it.\"","exponentCommLinkOneA");
            optionPanel.addOption("\"Not until I know who you are.\"","exponentCommLinkOneB");
            optionPanel.addOption("\"Maybe. After an explanation.\"","exponentCommLinkOneC");
            optionPanel.addOption( "\"You could die, demon.\"","valteilRefuse");

            if (!Global.getSector().getPlayerPerson().getMemoryWithoutUpdate().getBoolean("mostLuddicEthosPather")) {
                optionPanel.setEnabled("valteilRefuse",false);
                optionPanel.setTooltip("valteilRefuse","Requires you to be a follower of the Path");
                optionPanel.setTooltipHighlights("valteilRefuse","follower","Path");
                optionPanel.setTooltipHighlightColors("valteilRefuse",
                        Global.getSector().getFaction(Factions.LUDDIC_PATH).getColor()
                );
            }
            return true;
        }

        if (command.equals("exponentCommLinkOneAAA") || command.equals("exponentCommLinkOneAAB") || command.equals("exponentCommLinkOneABA")) {
            optionPanel.clearOptions();
            optionPanel.addOption("\"Got a sense you have a proposition.\"","exponentCommLinkOneC");
            optionPanel.addOption("\"I'm listening.\"","valteilContinueTwo");
            optionPanel.addOption("\"Sorry. Time's up.\"","valteilRefuse");

            if (memoryMap.get(MemKeys.GLOBAL).getBoolean("exponent_goAlone")) {
                optionPanel.setEnabled("valteilContinueTwo",false);
                optionPanel.setTooltip("valteilContinueTwo","No Church fleet present");
                optionPanel.setTooltipHighlights("valteilContinueTwo","Church");
                optionPanel.setTooltipHighlightColors("valteilContinueTwo",
                        Global.getSector().getFaction(Factions.LUDDIC_CHURCH).getColor()
                );
            }
            return true;
        }

        if (command.equals("exponentCommLinkOneB")) {
            optionPanel.clearOptions();
            optionPanel.addOption("\"Got a sense you have a proposition.\"","exponentCommLinkOneC");
            optionPanel.addOption("\"I'm listening.\"","valteilContinueTwo");
            optionPanel.addOption("\"I argue otherwise.\"","valteilRefuse");

            if (memoryMap.get(MemKeys.GLOBAL).getBoolean("exponent_goAlone")) {
                optionPanel.setEnabled("valteilContinueTwo",false);
                optionPanel.setTooltip("valteilContinueTwo","No Church fleet present");
                optionPanel.setTooltipHighlights("valteilContinueTwo","Church");
                optionPanel.setTooltipHighlightColors("valteilContinueTwo",
                        Global.getSector().getFaction(Factions.LUDDIC_CHURCH).getColor()
                );
            }
            return true;
        }

        return false;
    }
}
