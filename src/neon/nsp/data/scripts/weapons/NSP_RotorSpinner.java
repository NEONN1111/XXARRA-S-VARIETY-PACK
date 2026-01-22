package neon.nsp.data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
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

        float systemMult = 1;
        //System dependent rotation for Xxarra
        ShipSystemAPI system = weapon.getShip().getSystem();

        if (system.isActive()) systemMult = 20;
        if (system.isCoolingDown()) systemMult = 0.7f + 0.7f * (system.getCooldown() - system.getCooldownRemaining()) / system.getCooldown();
        angle = MathUtils.clampAngle(angle + (turn_rate * systemMult * amount));

        weapon.setCurrAngle(weapon.getShip().getFacing()+weapon.getSlot().getAngle() + angle);
    }
}
