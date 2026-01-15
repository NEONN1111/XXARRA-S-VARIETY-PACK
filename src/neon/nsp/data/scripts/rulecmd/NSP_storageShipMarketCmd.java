package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.input.Keyboard;
import java.util.List;
import java.util.Map;

public class NSP_storageShipMarketCmd extends BaseCommandPlugin {

    @Override
    public boolean execute(
            String ruleId,
            InteractionDialogAPI dialog,
            List<Misc.Token> params,
            Map<String, MemoryAPI> memoryMap
    ) {
        dialog.getOptionPanel().clearOptions();
        dialog.getTextPanel().clear();

        String shipName = Global.getSector().getMemoryWithoutUpdate().getString("$nsp_storageShipName");
        dialog.getTextPanel().addPara("The " + shipName + " maneuvers to make a stop, preparing its services for your use.");

        //  dialog.getOptionPanel().addOption("Manufactory", "sb_undineProductionManagement");
        dialog.getOptionPanel().addOption("Armament Storage", "marketOpenCargo");
        dialog.getOptionPanel().setShortcut("marketOpenCargo", Keyboard.KEY_I, false, false, false, false);

        dialog.getOptionPanel().addOption("Frigate Hangar", "marketOpenFleet");
        dialog.getOptionPanel().setShortcut("marketOpenFleet", Keyboard.KEY_F, false, false, false, false);

        dialog.getOptionPanel().addOption("Refit your ships", "marketOpenRefit");
        dialog.getOptionPanel().setShortcut("marketOpenRefit", Keyboard.KEY_R, false, false, false, false);

        dialog.getOptionPanel().addOption("Leave", "defaultLeave");
        dialog.getOptionPanel().setShortcut("defaultLeave", Keyboard.KEY_ESCAPE, false, false, false, true);

        dialog.makeOptionOpenCore("marketOpenRefit", CoreUITabId.REFIT, CampaignUIAPI.CoreUITradeMode.OPEN);

        return true;
    }
}