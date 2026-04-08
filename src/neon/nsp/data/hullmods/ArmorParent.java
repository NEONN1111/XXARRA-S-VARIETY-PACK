package neon.nsp.data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import neon.nsp.data.plugins.ExplosionOcclusionRaycast;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Point;

public class ArmorParent extends BaseHullMod {
    public static final String MODULE_DEAD = "module_dead";
    public static final String MODULE_HULKED = "module_hulked";
    public static final String MODULE_LISTENERS_ADDED = "module_listeners_added";

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (!ship.hasListenerOfClass(ExplosionOcclusionRaycast.class)) {
            ship.addListener(new ExplosionOcclusionRaycast());
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship.getChildModulesCopy().isEmpty() || ship.hasTag(MODULE_LISTENERS_ADDED)) return;
        ship.addTag(MODULE_LISTENERS_ADDED);
        for (ShipAPI module : ship.getChildModulesCopy()) {
            if (!module.hasListenerOfClass(ArmorModuleChild.class)) {
                module.addListener(new ArmorModuleChild(module));
            }
            if (!module.hasListenerOfClass(ExplosionOcclusionRaycast.class)) {
                module.addListener(new ExplosionOcclusionRaycast());
            }
        }
    }

    public static class ArmorModuleChild implements DamageListener, HullDamageAboutToBeTakenListener, AdvanceableListener {
        private final ShipAPI module;

        public ArmorModuleChild(ShipAPI module) {
            this.module = module;
        }

        @Override
        public void advance(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (Global.getCurrentState() != GameState.COMBAT || engine == null ||
                    !engine.isEntityInPlay(module) || module.getParentStation() == null ||
                    !module.getParentStation().isAlive() || module.getHitpoints() <= 0 ||
                    module.hasTag(MODULE_DEAD)) return;

            float pad = 50f;
            boolean moduleInMap = (module.getLocation().x >= (pad - engine.getMapWidth() / 2) &&
                    module.getLocation().x <= (engine.getMapWidth() / 2 - pad)) &&
                    (module.getLocation().y >= (pad - engine.getMapHeight() / 2) &&
                            module.getLocation().y <= (engine.getMapHeight() / 2 - pad));

            if (!module.hasTag(MODULE_HULKED) && moduleInMap) {
                float borderEdgeX = module.getLocation().getX() > 0 ? engine.getMapWidth() / 2 : -engine.getMapWidth() / 2;
                float borderEdgeY = module.getLocation().getY() > 0 ? engine.getMapHeight() / 2 : -engine.getMapHeight() / 2;

                module.getLocation().set(borderEdgeX, borderEdgeY);
                module.setHulk(true);
                module.setDrone(true);
                module.addTag(MODULE_HULKED);
                module.setCaptain(module.getParentStation().getCaptain());
            }

            if (!module.isHulk() && module.hasTag(MODULE_HULKED)) {
                module.setHulk(true);
            }

            float moduleFlux = module.getParentStation().getFluxLevel() * module.getMaxFlux();
            module.getFluxTracker().setCurrFlux(moduleFlux);
            module.getFluxTracker().setHardFlux(moduleFlux);
        }

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            ShipAPI shipTarget = (ShipAPI) target;
            if (shipTarget.isHulk() && shipTarget.getHitpoints() > 0 && !shipTarget.hasTag(MODULE_DEAD)) {
                shipTarget.setHulk(false);
            }
        }

        @Override
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI target, Vector2f point, float damageAmount) {
            if (target.getHitpoints() <= damageAmount && !target.hasTag(MODULE_DEAD)) {
                target.setHulk(false);
                target.setDrone(false);
                target.addTag(MODULE_DEAD);
            }
            return false;
        }
    }
}