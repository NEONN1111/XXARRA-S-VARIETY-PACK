package neon.nsp.data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import neon.nsp.data.scripts.util.CollisionUtils;
import neon.nsp.data.scripts.util.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;

public class nsp_luminancebay implements MissileAIPlugin, GuidedMissileAI {

    private CombatEngineAPI engine;
    private final MissileAPI missile;
    private CombatEntityAPI target;
    private ShipAPI launchingShip;
    private boolean hasSpawned = false; // ADD THIS FLAG

    public nsp_luminancebay(MissileAPI missile, ShipAPI launchingShip) {
        if (engine != Global.getCombatEngine()) {
            this.engine = Global.getCombatEngine();
        }
        this.missile = missile;
        this.launchingShip = launchingShip;
        missile.setArmingTime(missile.getArmingTime()-(float)(Math.random()/4));
    }

    @Override
    public void advance(float amount) {
        //skip the AI if the game is paused, the missile is engineless or fading
        if (engine.isPaused() || hasSpawned) {return;} // ADD hasSpawned CHECK

        if(!CollisionUtils.isPointWithinCollisionCircle(missile.getLocation(), launchingShip)) {
            // Check if ANY ship is in range, not loop through all of them
            boolean shipsInRange = !CombatUtils.getShipsWithinRange(missile.getLocation(), 500f).isEmpty();

            if(shipsInRange && MathUtils.getDistance(missile,launchingShip) > 80f &&
                    !CollisionUtils.isPointWithinBounds(missile.getLocation(),launchingShip)) {

                missile.setArmingTime(0f);
                CombatFleetManagerAPI cfm = engine.getFleetManager(1);
                cfm.setSuppressDeploymentMessages(true);
                ShipAPI pod = cfm.spawnShipOrWing("nsp_parasite_standard",missile.getLocation(),0f);
                pod.setOwner(missile.getSource().getOriginalOwner());
                pod.setFacing(missile.getFacing());
                pod.getVelocity().set(missile.getVelocity());
                pod.getMutableStats().getFighterRefitTimeMult().modifyPercent(pod.getId(),9999f);

                cfm.setSuppressDeploymentMessages(false);
                hasSpawned = true; // SET FLAG TO PREVENT FURTHER SPAWNING
            }
        }

        if(missile.isArmed()) {
            engine.removeEntity(missile);
        }
    }

    @Override
    public CombatEntityAPI getTarget() {
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
        this.target = target;
    }
}