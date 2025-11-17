package neon.nsp.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;

public class NSP_MessengerAmbient extends BaseHullMod {

    private Object loop1;
    private Object loop2;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if ((ship == null) || !ship.isAlive()) {
            return;
        }

        Global.getSoundPlayer().playLoop("nsp_messenger_ambience", loop1, 1f, 0.3f, ship.getLocation(), Misc.ZERO);

        float vol;
        if (ship.getSystem().isActive()) {
            vol = Math.max(0.01f, ship.getSystem().getEffectLevel());
        } else {
            vol = 0.01f;
        }
        Global.getSoundPlayer().playLoop("nsp_messenger_ambience", loop2, 1f, 0.3f, ship.getLocation(), Misc.ZERO);
    }
}
