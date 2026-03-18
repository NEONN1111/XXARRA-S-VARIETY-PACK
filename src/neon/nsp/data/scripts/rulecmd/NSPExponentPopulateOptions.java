package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
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

        return false;
    }
}
