package data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.world.nsp_dominatorMK1Intel;
import data.world.nsp_legionMK1Intel;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;



import java.util.List;
import java.util.Map;

import static java.awt.SystemColor.info;

public class nsp_addViableMK1Intel extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        SectorEntityToken dominator = Global.getSector().getEntityById("nsp_dominatorWreck"); // giga jank bypass because i forgot to tell the mod author to give the entities an ID
        SectorEntityToken legion = Global.getSector().getEntityById("nsp_legionWreck"); // it works, so who cares. not me!
        SectorEntityToken enforcer = Global.getSector().getEntityById("nsp_enforcerWreck");

        if (dominator != null){
            Global.getSector().getIntelManager().addIntel(new nsp_dominatorMK1Intel());
            Global.getSector().getIntelManager().addIntelToTextPanel(new nsp_dominatorMK1Intel(), dialog.getTextPanel());


        }
        if (legion != null){
            Global.getSector().getIntelManager().addIntel(new nsp_legionMK1Intel());
            Global.getSector().getIntelManager().addIntelToTextPanel(new nsp_legionMK1Intel(), dialog.getTextPanel());
        }
        if (enforcer != null){
            Global.getSector().getIntelManager().addIntel(new nsp_legionMK1Intel());
            Global.getSector().getIntelManager().addIntelToTextPanel(new nsp_legionMK1Intel(), dialog.getTextPanel());
        }



        return true;
    }
}
