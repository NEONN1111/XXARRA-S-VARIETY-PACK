package neon.nsp.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImprovisedAutomationCleanup extends BaseHullMod {

    private static final String MAIN_HULLMOD = "nsp_improvised_automation";
    private static final Set<String> CLEANED_SHIPS = new HashSet<>();

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship == null || ship.getVariant() == null) return;

        String shipId = ship.getId();

        if (CLEANED_SHIPS.contains(shipId)) return;

        ShipVariantAPI variant = ship.getVariant();


        if (variant.hasHullMod(MAIN_HULLMOD)) {
            boolean removed = false;

            List<String> hullmodsCopy = new ArrayList<>(variant.getHullMods());
            for (String mod : hullmodsCopy) {
                if ("automated".equals(mod)) {
                    variant.getHullMods().remove(mod);
                    removed = true;
                }
            }

            if (variant.getPermaMods() != null) {
                Set<String> permaCopy = new HashSet<>(variant.getPermaMods());
                for (String mod : permaCopy) {
                    if ("automated".equals(mod)) {
                        variant.getPermaMods().remove(mod);
                        removed = true;
                    }
                }
            }


            if (variant.getHullSpec() != null && variant.getHullSpec().getBuiltInMods() != null) {
                if (variant.getHullSpec().getBuiltInMods().contains("automated")) {

                    Global.getLogger(this.getClass()).warn("Ship " + ship.getName() + " has built-in automated hullmod in hull spec");
                }
            }

            if (removed) {
                CLEANED_SHIPS.add(shipId);
                Global.getLogger(this.getClass()).info("Cleanup hullmod removed vanilla Automated from: " + ship.getName());
            }
        } else {

            variant.getHullMods().remove(id);
        }
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false; // Hidden hullmod - cannot be manually added
    }
}