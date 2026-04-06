package neon.nsp.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;

public class ImprovisedAutomationPenalty extends BaseHullMod {

    private static final String MAIN_HULLMOD = "nsp_improvised_automation";
    private static final String AUTO_MODE = "nsp_improvised_auto";

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship == null) return;
        ShipVariantAPI variant = ship.getVariant();
        if (variant == null) return;

        // If the ship doesn't have the main hullmod anymore, remove this penalty
        if (!variant.hasHullMod(MAIN_HULLMOD)) {
            variant.getHullMods().remove(id);
            return;
        }

        // If the ship is in AUTO mode, remove this penalty (not needed for AI cores)
        if (variant.hasHullMod(AUTO_MODE)) {
            variant.getHullMods().remove(id);
            return;
        }
    }

    @Override
    public boolean canBeAddedOrRemovedNow(
            ShipAPI ship,
            MarketAPI marketOrNull,
            CampaignUIAPI.CoreUITradeMode mode
    ) {
        return false;
    }
}