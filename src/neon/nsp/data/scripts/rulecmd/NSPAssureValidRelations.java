package neon.nsp.data.scripts.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class NSPAssureValidRelations extends BaseCommandPlugin {

    protected SectorEntityToken entity;

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {

        entity = dialog.getInteractionTarget();
        if (entity == null) return false;
        MarketAPI market = entity.getMarket();
        if (market == null) return false;

        if (Global.getSettings().isDevMode()) {
            dialog.getTextPanel().addPara("OLD DATA", Misc.getHighlightColor());
            dialog.getTextPanel().addPara("getRel to Player: " + market.getFaction().getRelToPlayer().getRel(), Color.RED);
            dialog.getTextPanel().addPara("getRepInt to Player: " + market.getFaction().getRelToPlayer().getRepInt(), Color.RED);
            dialog.getTextPanel().addPara("getLevel to Player: " + market.getFaction().getRelToPlayer().getLevel().getDisplayName(), Color.RED);
        }

        if (market.getFaction().getRelToPlayer().getRel() < 0.65f) {
            market.getFaction().getRelToPlayer().setRel(0.65f);
        }

        if (Global.getSettings().isDevMode()) {
            dialog.getTextPanel().addPara("NEW DATA", Misc.getHighlightColor());
            dialog.getTextPanel().addPara("getRel to Player: " + market.getFaction().getRelToPlayer().getRel(), Color.GREEN);
            dialog.getTextPanel().addPara("getRepInt to Player: " + market.getFaction().getRelToPlayer().getRepInt(), Color.GREEN);
            dialog.getTextPanel().addPara("getLevel to Player: " + market.getFaction().getRelToPlayer().getLevel().getDisplayName(), Color.GREEN);
        }

        return true;
    }
}
