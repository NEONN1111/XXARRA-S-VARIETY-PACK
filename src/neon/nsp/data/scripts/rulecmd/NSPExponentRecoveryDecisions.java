package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class NSPExponentRecoveryDecisions extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {

        OptionPanelAPI optionPanel = dialog.getOptionPanel();
        optionPanel.clearOptions();
        optionPanel.addOption("Recover the Exponent","exponentDestroyedPreRecover");
        optionPanel.addOption("Permanently destroy the Exponent","exponentDestroyedPerma");
        optionPanel.addOptionConfirmation("exponentDestroyedPreRecover","Recovering the Exponent will " +
                "heavily sour relations with the Luddic Church. Are you certain of your choice?","Yes","No");
        optionPanel.addOptionConfirmation("exponentDestroyedPerma","Permanently destroying the Exponent " +
                "will increase your standing with the Luddic Church. Are you certain of your choice?","Yes","No");

        return true;
    }
}
