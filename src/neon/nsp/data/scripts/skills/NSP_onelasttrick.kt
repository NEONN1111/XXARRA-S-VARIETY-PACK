package neon.nsp.data.scripts.skills

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.combat.EmpArcEntityAPI.EmpArcParams
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.combat.CombatUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import kotlin.math.roundToInt

class NSP_onelasttrick: AfterShipCreationSkillEffect, BaseSkillEffectDescription() {

    // When the ship is destroyed, overloads all other ships within x units,
    // drains their ship system ammo and places it on cooldown
    // ???
    override fun apply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
    }

    override fun unapply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?) {
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        if (!ship!!.hasListenerOfClass(cs_spiteListener::class.java) && ship.stationSlot == null && !ship.isFighter){
            ship.addListener(cs_spiteListener(ship))
        }
    }

    override fun unapplyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
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
        val sys = (SYSTEM_MULT *100f).roundToInt()
        val minsys = (OVERLOAD_SECONDS * MIN_SYS_MULT).roundToInt()
        info!!.addPara("When destroyed, overloads all ships within ${RANGE.roundToInt()} units for ${OVERLOAD_SECONDS.roundToInt()} seconds", 0f).color = Misc.getHighlightColor()
        info.addPara("Will also drain victims' ship system ammo and place it on an extended cooldown equal to $sys% of base cooldown or $minsys seconds, whichever is greater", 0f).color = Misc.getHighlightColor()
    }
    class cs_spiteListener(var s: ShipAPI): HullDamageAboutToBeTakenListener {
        val params = EmpArcParams()
        init {
            params.segmentLengthMult = 8f
            params.zigZagReductionFactor = 0.15f
            params.fadeOutDist = 50f
            params.minFadeOutMult = 10f
            params.flickerRateMult = 0.3f
        }
        var did = false
        override fun notifyAboutToTakeHullDamage(
            param: Any?, ship: ShipAPI?, point: Vector2f?, damageAmount: Float
        ): Boolean {
            if (damageAmount >= ship!!.hitpoints && !did && !ship.isFighter && !ship.isStationModule){
                did = true // probably not needed but whatever
                val ships = CombatUtils.getShipsWithinRange(ship.location, RANGE)
                ships.forEach {
                    val arc = Global.getCombatEngine().spawnEmpArcVisual(ship.location, ship, it.location, it, 40f,  // thickness
                        Color.RED, Color(255, 255, 255, 255), params)
                    arc.coreWidthOverride = 20f
                    arc.setRenderGlowAtStart(false)
                    arc.setWarping(0.5f)
                    arc.setFadedOutAtStart(true)
                    arc.setSingleFlickerMode()
                    it.fluxTracker.beginOverloadWithTotalBaseDuration(OVERLOAD_SECONDS)
                    if (it.system != null){
                        if (it.system.ammoPerSecond > 0f){
                            it.system.ammo = 0
                        }
                        it.system.cooldownRemaining += (it.system.cooldown* SYSTEM_MULT).coerceAtLeast(OVERLOAD_SECONDS * MIN_SYS_MULT)
                    }
                    if (it.phaseCloak != null){
                        if (it.phaseCloak.ammoPerSecond > 0f){
                            it.phaseCloak.ammo = 0
                        }
                        it.phaseCloak.cooldownRemaining += (it.phaseCloak.cooldown* SYSTEM_MULT).coerceAtLeast(
                            OVERLOAD_SECONDS * MIN_SYS_MULT
                        )
                    }
                }
                ship.removeListener(this)
            }
            return false
        }
    }

    companion object{
        val SYSTEM_MULT = 2.50f
        val MIN_SYS_MULT = 1.50f
        val OVERLOAD_SECONDS = 5f
        val RANGE = 1200f
    }
}