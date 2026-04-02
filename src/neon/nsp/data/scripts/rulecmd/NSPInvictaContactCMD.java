//package neon.nsp.data.scripts.rulecmd;
//
//import com.fs.starfarer.api.Global;
//import com.fs.starfarer.api.campaign.InteractionDialogAPI;
//import com.fs.starfarer.api.campaign.TextPanelAPI;
//import com.fs.starfarer.api.campaign.rules.MemoryAPI;
//import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
//import com.fs.starfarer.api.util.Misc;
//import neon.nsp.data.campaign.intel.misc.NSPInvictaConvIntel;
//
//import java.util.List;
//import java.util.Map;
//
//public class NSPInvictaContactCMD extends BaseCommandPlugin {
//
//    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
//        TextPanelAPI text = dialog.getTextPanel();
//
//        // Create the intel - this will also add Invicta as a contact
//        Global.getSector().getIntelManager().addIntel(new NSPInvictaConvIntel(), false);
//        // Progress the mission stage
//        return true;
//    }
//}