package neon.nsp.data.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class NSP_SetFighterColl extends BaseHullMod {

    // Apply effects after ship creation - this is where collision changes should happen
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship != null) {
            // Set collision class to FIGHTER
            // This is the actual API method you need
            ship.setCollisionClass(CollisionClass.FIGHTER);
        }
    }

}