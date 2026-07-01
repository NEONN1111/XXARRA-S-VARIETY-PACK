package neon.nsp.data.hullmods;

import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class ImprovisedAutomationManual extends BaseHullMod {

    @Override
    public int getDisplayCategoryIndex() {
        return 0;
    }

    @Override
    public int getDisplaySortOrder() {
        return 1;
    }

    @Override
    public boolean canBeAddedOrRemovedNow(
            ShipAPI ship,
            MarketAPI marketOrNull,
            CampaignUIAPI.CoreUITradeMode mode
    ) {

        return (ship != null && (ship.getCaptain() == null || ship.getCaptain().isDefault()));
    }

    @Override
    public String getCanNotBeInstalledNowReason(
            ShipAPI ship,
            MarketAPI marketOrNull,
            CampaignUIAPI.CoreUITradeMode mode
    ) {
        return "Must not have a captain assigned to remove.";
    }


    public void addPostDescriptionSection(
            TooltipMakerAPI tooltip,
            ShipAPI.HullSize hullSize,
            ShipAPI ship,
            float width,
            float opad,
            boolean isForModSpec
    ) {
        tooltip.addPara("This ship is currently in %s mode, and will require a standard crew compliment.", opad, Misc.getHighlightColor(), "manual");
    }
}