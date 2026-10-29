package neon.nsp.data.scripts.util

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.listeners.AdvanceableListener
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.combat.listeners.DamageListener
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener
import com.fs.starfarer.api.input.InputEventAPI
import org.lwjgl.util.vector.Vector2f

const val MODULE_DEAD = "module_dead"
const val MODULE_HULKED = "module_hulked"
const val MODULE_LISTENERS_ADDED = "module_listeners_added"

class ArmorParent: BaseHullMod() {
    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String?) {
        if (!ship.hasListenerOfClass(ExplosionOcclusionRaycast::class.java)) ship.addListener(ExplosionOcclusionRaycast())
    }

    override fun advanceInCombat(ship: ShipAPI, amount: Float) {
        if(ship.childModulesCopy.isEmpty() || ship.hasTag(MODULE_LISTENERS_ADDED)) return
        ship.addTag(MODULE_LISTENERS_ADDED)
        ship.childModulesCopy.forEach { module ->
            if (!module.hasListenerOfClass(ArmorModuleChild::class.java)) module.addListener(ArmorModuleChild(module))
            if (!module.hasListenerOfClass(ExplosionOcclusionRaycast::class.java)) module.addListener(
                ExplosionOcclusionRaycast()
            )
        }
    }

    class ArmorModuleChild(val module: ShipAPI): DamageListener, HullDamageAboutToBeTakenListener, AdvanceableListener {
        override fun advance(amount: Float) {
            val engine = Global.getCombatEngine()

            if (Global.getCurrentState() != GameState.COMBAT || engine == null || !Global.getCombatEngine().isEntityInPlay(module) ||
                module.parentStation?.isAlive != true || module.hitpoints <= 0 || module.hasTag(MODULE_DEAD)) return

            val pad = 50f
            val moduleInMap = (module.location.x in (pad - engine.mapWidth / 2)..(engine.mapWidth / 2 - pad)) &&
                    (module.location.y in (pad - engine.mapHeight / 2)..(engine.mapHeight / 2 - pad))

            if (!module.hasTag(MODULE_HULKED) && moduleInMap) {
                // only teleport to inside the map border
                val borderEdgeX = if (module.location.getX() > 0) engine.mapWidth / 2 else -engine.mapWidth / 2
                val borderEdgeY = if (module.location.getY() > 0) engine.mapHeight / 2 else -engine.mapHeight / 2

                module.location.set(borderEdgeX, borderEdgeY)

                module.isDrone = true
                module.addTag(MODULE_HULKED)

                module.captain = module.parentStation.captain
            }


            if (module.hasTag(MODULE_HULKED) && !module.isDrone && !module.hasTag(MODULE_DEAD)) {
                module.isDrone = true
            }

            // sync hardflux level with parent hull for polarized armor purposes
            val moduleFlux = module.parentStation.fluxLevel * module.maxFlux
            module.fluxTracker.currFlux = moduleFlux
            module.fluxTracker.hardFlux = moduleFlux
        }

        // Temporarily unset drone so damage effects display properly, then reapply
        override fun reportDamageApplied(source: Any, target: CombatEntityAPI, result: ApplyDamageResultAPI) {
            val module = target as ShipAPI
            if (module.hasTag(MODULE_HULKED) && module.isDrone && module.hitpoints > 0 && !module.hasTag(MODULE_DEAD)) {
                module.isDrone = false
                // Schedule reapplication of drone state for the next frame
                // This allows damage effects to play properly
                Global.getCombatEngine().addPlugin(object : com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin() {
                    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
                        if (module.isDrone == false && module.hitpoints > 0 && !module.hasTag(MODULE_DEAD)) {
                            module.isDrone = true
                        }
                        Global.getCombatEngine().removePlugin(this)
                    }
                })
            }
        }

        override fun notifyAboutToTakeHullDamage(param: Any, module: ShipAPI, point: Vector2f, damageAmount: Float): Boolean {
            if (module.hitpoints <= damageAmount && !module.hasTag(MODULE_DEAD)) {
                module.isDrone = false
                module.addTag(MODULE_DEAD)
            }
            return false
        }
    }
}

