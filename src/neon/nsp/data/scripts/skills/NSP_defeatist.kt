package neon.nsp.data.scripts.skills

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.listeners.AdvanceableListener
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import kotlin.math.roundToInt

class NSP_defeatist: AfterShipCreationSkillEffect, BaseSkillEffectDescription() {
    override fun apply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
    }

    override fun unapply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?) {
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        if (!ship!!.hasListenerOfClass(NSP_defeatistListener::class.java)){
            ship.addListener(NSP_defeatistListener(ship))
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
        val DEALT_CENT = ((DEALT_MULT - 1f)*100f).roundToInt()
        val TAKEN_CENT = ((TAKEN_MULT - 1f)*100f).roundToInt()
        info!!.addPara("If the ship hasn't overloaded or fallen below 50% hitpoints this combat, increases all damage dealt by $DEALT_CENT%", 0f).color = Misc.getHighlightColor()
        info.addPara("Otherwise, increases all damage taken by $TAKEN_CENT%", 0f).color = Misc.getHighlightColor()
    }
    companion object{
        val DEALT_MULT = 1.20f
        val TAKEN_MULT = 1.20f
    }
    class NSP_defeatistListener(var ship: ShipAPI): AdvanceableListener {
        var defeated = false
        val ID = this.javaClass.name
        override fun advance(amount: Float) {
            if (Global.getCombatEngine().isPaused) return
            if (!defeated){
                ship.mutableStats.apply {
                    energyWeaponDamageMult.modifyMult(ID, DEALT_MULT)
                    ballisticWeaponDamageMult.modifyMult(ID, DEALT_MULT)
                    missileWeaponDamageMult.modifyMult(ID, DEALT_MULT)

                }
                ship.childModulesCopy.forEach {
                    it.mutableStats.apply {
                        energyWeaponDamageMult.modifyMult(ID, DEALT_MULT)
                        ballisticWeaponDamageMult.modifyMult(ID, DEALT_MULT)
                        missileWeaponDamageMult.modifyMult(ID, DEALT_MULT)
                    }
                }
                if (ship.fluxTracker.isOverloaded || ship.hitpoints < (ship.maxHitpoints/2f)){
                    defeated = true
                    ship.mutableStats.apply {
                        energyWeaponDamageMult.unmodify(ID)
                        ballisticWeaponDamageMult.unmodify(ID)
                        missileWeaponDamageMult.unmodify(ID)

                    }
                    ship.childModulesCopy.forEach {
                        it.mutableStats.apply {
                            energyWeaponDamageMult.unmodify(ID)
                            ballisticWeaponDamageMult.unmodify(ID)
                            missileWeaponDamageMult.unmodify(ID)
                        }
                    }
                }
            } else {
                ship.mutableStats.apply {
                    hullDamageTakenMult.modifyMult(ID, TAKEN_MULT)
                    shieldDamageTakenMult.modifyMult(ID, TAKEN_MULT)
                    armorDamageTakenMult.modifyMult(ID, TAKEN_MULT)

                }
                ship.childModulesCopy.forEach {
                    it.mutableStats.apply {
                        energyWeaponDamageMult.modifyMult(ID, TAKEN_MULT)
                        ballisticWeaponDamageMult.modifyMult(ID, TAKEN_MULT)
                        missileWeaponDamageMult.modifyMult(ID, TAKEN_MULT)
                    }
                }
            }
        }
    }
}