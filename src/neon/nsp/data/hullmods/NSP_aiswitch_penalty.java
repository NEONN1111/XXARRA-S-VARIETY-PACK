package neon.nsp.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.HullModSpecAPI;

public class NSP_aiswitch_penalty extends BaseHullMod {

    // hidden op penalty hullmod, removed when the original goes
    
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship == null) return;
        ShipVariantAPI variant = ship.getVariant();
        if (variant == null) return;
        
        // can't use variant.spec because we change the spec itself to remove UNBOARDABLE
        if (!variant.hasHullMod("NSP_aiswitch")) {
          ShipHullSpecAPI spec = Global.getSettings().getHullSpec(variant.getHullSpec().getRestoredToHullId());
            variant.removeMod(this.getSpec().getId());
            
            if (spec != null && spec.isBuiltInMod(HullMods.AUTOMATED)) {
                variant.setHullSpecAPI(spec);
            }
        }
    }

    private HullModSpecAPI getSpec() {
        return null;
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