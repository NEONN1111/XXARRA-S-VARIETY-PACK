package neon.nsp.data.scripts.skills

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect
import com.fs.starfarer.api.characters.LevelBasedEffect
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.AdvanceableListener
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import kotlin.math.roundToInt

class NSP_fullArsenal: AfterShipCreationSkillEffect, BaseSkillEffectDescription() {
    override fun getEffectDescription(level: Float): String {
        return ""
    }

    override fun getEffectPerLevelDescription(): String? {
        return null
    }

    override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
        return LevelBasedEffect.ScopeDescription.PILOTED_SHIP
    }

    override fun apply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
      //  stats!!.weaponDamageTakenMult.modifyMult(id, 0.50f)

    }

    override fun unapply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?) {

    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        if (!ship!!.hasListenerOfClass(NSP_v1arsenalListener::class.java)){
            ship.addListener(NSP_v1arsenalListener(ship))
        }
    }

    override fun unapplyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        ship!!.removeListenerOfClass(NSP_v1arsenalListener::class.java)
    }

    override fun hasCustomDescription(): Boolean {
        return true
    }

    override fun createCustomDescription(
        stats: MutableCharacterStatsAPI?,
        skill: SkillSpecAPI?,
        info: TooltipMakerAPI?,
        width: Float
    ) {
        info!!.addPara("When switching the selected weapon group, all weapons in the new group are instantly repaired and reload ammo equal to ${ammoSeconds.roundToInt()} seconds of passive ammo regeneration.", 0f).color = Misc.getHighlightColor()
        info.addPara("This effect has a cooldown of ${ammoCooldown.roundToInt()} seconds.", 0f).color = Misc.getHighlightColor()
        info.addPara("Recent hull damage taken can be restored by dealing hull damage. Only effective against crewed ships.", 0f).color = Misc.getHighlightColor()
    }
    class NSP_v1arsenalListener(var ship: ShipAPI): AdvanceableListener, DamageDealtModifier, HullDamageAboutToBeTakenListener{
        var pool = 0f
        val decay = 0.993f
        val decayTimer = IntervalUtil(0.05f,0.05f)
        var isReloadReady = true
        val cooldown = IntervalUtil(ammoCooldown, ammoCooldown)
        var prevGroup: WeaponGroupAPI? = null
        override fun advance(amount: Float) {
            if (Global.getCombatEngine().isPaused) return
            if (ship == Global.getCombatEngine().playerShip){
                Global.getCombatEngine().maintainStatusForPlayerShip(this.javaClass.name, null, "BLOOD", "${pool.roundToInt()} BLOOD", true)
            }
            decayTimer.advance(amount)
            if (decayTimer.intervalElapsed()){
                pool *= decay
            }
            if (prevGroup == null){
                prevGroup = ship.selectedGroupAPI
            }
            if (prevGroup == null) return
            val curr = ship.selectedGroupAPI
            if (!isReloadReady){
                cooldown.advance(amount)
                if (cooldown.intervalElapsed()){
                    isReloadReady = true
                }
            }
            if (curr != null && curr != prevGroup){
                prevGroup = curr
                if (!isReloadReady) return
                curr.weaponsCopy.forEach {
                    it.repair()
                    if (it.ammoTracker.usesAmmo() && it.ammoTracker.ammoPerSecond > 0f){
                        val reload = it.ammoTracker.ammoPerSecond* ammoSeconds
                        it.ammoTracker.ammo = (it.ammoTracker.ammo + reload.roundToInt()).coerceAtMost(it.ammoTracker.maxAmmo)
                    }
                }
                Global.getSoundPlayer().playSound("system_ammo_feeder", 1f, 1f, ship.location, ship.velocity)
                Global.getCombatEngine().addFloatingText(Vector2f(ship.location.x, ship.location.y + ship.collisionRadius), "Reloaded!", 32f, Color.ORANGE, ship, 0.5f, 1.5f)
                isReloadReady = false
            }
        }

        override fun notifyAboutToTakeHullDamage(
            param: Any?, ship: ShipAPI?, point: Vector2f?, damageAmount: Float
        ): Boolean {
            pool += damageAmount
            return false
        }

        override fun modifyDamageDealt(
            param: Any?, target: CombatEntityAPI?, damage: DamageAPI?, point: Vector2f?, shieldHit: Boolean
        ): String {
            if (target !is ShipAPI) return "oops"
            if (target.isAlive && target.owner != ship.owner && !target.isFighter && !target.isDrone && !target.hasListenerOfClass(
                    NSP_v1arsenalHealTracker::class.java)){
                target.addListener(NSP_v1arsenalHealTracker(ship))
            }
            return "ballin"
        }

    }
    class NSP_v1arsenalHealTracker(var dealer: ShipAPI): HullDamageAboutToBeTakenListener {
        override fun notifyAboutToTakeHullDamage(
            param: Any?, ship: ShipAPI?, point: Vector2f?, damageAmount: Float
        ): Boolean {
            if (param !is ShipAPI) return false
            if (param != dealer) return false // damage source is dealer
            if (ship!!.isHulk) return false
            if (Misc.isAutomated(ship)) return false
            if (damageAmount > 0f){
                val listener = dealer.listenerManager?.getListeners(NSP_v1arsenalListener::class.java)?.firstOrNull() ?: return false

                val pool = listener.pool
                val heal = Math.min(damageAmount/3f, pool)
            //    Global.getCombatEngine().combatUI.addMessage(0, "${heal.roundToInt().toString()}")
                listener.pool = (listener.pool - heal).coerceAtLeast(0f)
                dealer.hitpoints = (dealer.hitpoints + heal).coerceAtMost(dealer.maxHitpoints)
            }
            return false
        }
    }
    companion object{
        val ammoSeconds = 15f
        val ammoCooldown = 12f
        val firerateboost = 100f

    }
}