/*
    By Tartiflette
 */
package neon.nsp.data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

import java.util.HashMap;
import java.util.Map;
//import java.awt.Color;
import org.lwjgl.util.vector.Vector2f;

public class NSP_MassFrameEffect implements EveryFrameWeaponEffectPlugin {

    private boolean runOnce = false, SHADER = false, sound = false;
    private ShipSystemAPI SYSTEM;
    private ShipAPI SHIP;
    private boolean active = false;
    private Vector2f source, target;
    private final float MAX_DISPLACEMENT = 1000;
    private final float MAX_REPULSION = 750;
    private final Map<HullSize, Float> MULT = new HashMap<>();

    {
        MULT.put(HullSize.DEFAULT, 1f);
        MULT.put(HullSize.FIGHTER, 0.75f);
        MULT.put(HullSize.FRIGATE, 0.5f);
        MULT.put(HullSize.DESTROYER, 0.3f);
        MULT.put(HullSize.CRUISER, 0.2f);
        MULT.put(HullSize.CAPITAL_SHIP, 0.1f);
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if (!runOnce) {
            runOnce = true;
            SHIP = weapon.getShip();
            SYSTEM = SHIP.getSystem();
            //distortion + light effects
            SHADER = Global.getSettings().getModManager().isModEnabled("shaderLib");
        }

        if (SYSTEM == null || engine.isPaused() || !SHIP.isAlive()) return;

        if (weapon.isFiring() && !sound) {
            sound = true;
            Global.getSoundPlayer().playSound("nsp_mass_annihilator_fire", 1, 1, weapon.getLocation(), weapon.getShip().getVelocity());
        } else if (!weapon.isFiring() && sound) {
            sound = false;
        }

    }
}