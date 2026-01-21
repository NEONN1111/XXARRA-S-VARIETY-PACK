package neon.nsp.data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;

public class NSP_RotorSpinner implements EveryFrameWeaponEffectPlugin {


    private float angle = 0;
    private float turn_rate = 0;
    private boolean runOnce = true;
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) return;
        if (!weapon.getShip().isAlive()) return;

        if (runOnce) {
            turn_rate = weapon.getSpec().getTurnRate();
            runOnce = false;
        }

        float system = 1;
        if (weapon.getShip().getSystem().isActive()) system = 2;
        if (weapon.getShip().getSystem().isCoolingDown()) system = 0.5f;

        angle = MathUtils.clampAngle(angle + (turn_rate * system * amount));

        weapon.setCurrAngle(weapon.getShip().getFacing()+weapon.getSlot().getAngle() + angle);
    }
}
