package neon.nsp.data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.Global;

//jank solution to set ai controlled fabricators to have +100 ship system charges
//while player owned/controlled ones should only have the 4 in the ship_data.csv

public class ConstructionSwarmChargeSetter extends BaseHullMod {

    public static final int ADDED_CHARGES = 100;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        super.applyEffectsBeforeShipCreation(hullSize, stats, id);

    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        super.applyEffectsAfterShipCreation(ship, id);


        ship.getMutableStats().getSystemUsesBonus().unmodifyFlat(id);


        if (Global.getCombatEngine() != null) {

            if (ship.getOwner() != 0) {
                ship.getMutableStats().getSystemUsesBonus().modifyFlat(id, ADDED_CHARGES);
            }
        } else {

            if (!isInPlayerFleet(ship)) {
                ship.getMutableStats().getSystemUsesBonus().modifyFlat(id, ADDED_CHARGES);
            }
        }
    }
}