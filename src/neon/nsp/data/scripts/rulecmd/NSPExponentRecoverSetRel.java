package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class NSPExponentRecoverSetRel extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {

        dialog.getTextPanel().setFontSmallInsignia();
        dialog.getTextPanel().addPara("Relationship with the Luddic Church %s, currently at %s",Misc.getGrayColor(),Misc.getNegativeHighlightColor(),"reduced to inhospitable","50/100 (inhospitable)");
        dialog.getTextPanel().setFontInsignia();

        return true;
    }
}
