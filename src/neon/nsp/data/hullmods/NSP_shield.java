package neon.nsp.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

public class NSP_shield extends BaseHullMod {

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getShield() != null) {
            ship.getShield().setRadius(ship.getShield().getRadius(), Global.getSettings().getSpriteName("SUPERDERELICT", "SPD_shield_inner"), Global.getSettings().getSpriteName("SUPERDERELICT", "SPD_shield_outer"));

        }
    }
}
