package neon.nsp.data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.Global;

public class ConstructionSwarmChargeSetter extends BaseHullMod {

    public static final String BONUS_ID = "construction_swarm_charge_bonus";
    public static final int ADDED_CHARGES = 100;

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        super.applyEffectsBeforeShipCreation(hullSize, stats, id);
        stats.getSystemUsesBonus().modifyFlat(BONUS_ID, ADDED_CHARGES);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        super.applyEffectsAfterShipCreation(ship, id);

        if (ship == null) return;

        boolean isPlayerShip = false;

        if (Global.getCombatEngine() != null) {
            isPlayerShip = (ship.getOwner() == 0);
        } else {
            isPlayerShip = isInPlayerFleet(ship);
        }

        if (isPlayerShip) {
            ship.getMutableStats().getSystemUsesBonus().unmodifyFlat(BONUS_ID);
        }

        if (Global.getCombatEngine() != null) {
            float maxUses = ship.getSystem() != null ? ship.getSystem().getMaxAmmo() : -1;
            System.out.println("[" + ship.getName() + "] Owner: " + ship.getOwner() +
                    " | Max System Uses: " + maxUses +
                    " | IsPlayer: " + isPlayerShip);
        }
    }
}