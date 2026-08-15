package neon.nsp.data.hullmods;

import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ImprovisedAutomationManual extends BaseHullMod {
    public static Map mag = new HashMap();

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

  //  @Override
  //  public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
 //
 //   }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addSectionHeading("Ship State", Alignment.MID, 5f);
        tooltip.addPara("The ship is currently %s.", 5f, Color.ORANGE, "crewed");
        //tooltip.addSectionHeading("Affected Deployment", Alignment.MID, 5f);
       // tooltip.addPara("As a result of the simplicity of crewed operation, the Deployment Point cost of this ship is reduced by %s/%s/%s/%s.", 5f, Color.ORANGE, "2","3","4","5");
    }
}