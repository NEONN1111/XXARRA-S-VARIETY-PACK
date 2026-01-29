package neon.nsp.data.scripts.weapons;

import com.fs.starfarer.api.combat.*;

public class NSP_FluxSpinner implements EveryFrameWeaponEffectPlugin {

    private float angle = 0;
    private float baseTurnRate = 0;
    private boolean runOnce = true;
    private final float MIN_SPIN_MULTIPLIER = 1f;    // Minimum speed at 0% flux
    private final float MAX_SPIN_MULTIPLIER = 3f;    // Maximum speed at 100% flux

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) return;
        if (!weapon.getShip().isAlive()) return;

        if (runOnce) {
            baseTurnRate = weapon.getSpec().getTurnRate();
            runOnce = false;
        }

        // Get ship's current flux level (0.0 to 1.0)
        ShipAPI ship = weapon.getShip();
        float fluxRatio = ship.getFluxTracker().getFluxLevel();

        // Calculate spin multiplier based on flux level (instant response)
        float spinMultiplier = MIN_SPIN_MULTIPLIER +
                (MAX_SPIN_MULTIPLIER - MIN_SPIN_MULTIPLIER) * fluxRatio;

        // Update angle based on flux-enhanced turn rate
        float effectiveTurnRate = baseTurnRate * spinMultiplier;
        angle += effectiveTurnRate * amount;

        // Keep angle within 0-360 degrees
        if (angle > 360f) {
            angle -= 360f;
        } else if (angle < 0f) {
            angle += 360f;
        }

        // Apply rotation
        weapon.setCurrAngle(weapon.getShip().getFacing() + weapon.getSlot().getAngle() + angle);
    }
}