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
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc

class NSP_foulMachinery: AfterShipCreationSkillEffect, BaseSkillEffectDescription() {
    override fun apply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
    }

    override fun unapply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?) {
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        if (!ship!!.hasListenerOfClass(NSP_autoCheckerPlugin::class.java)){
            ship.addListener(NSP_autoCheckerPlugin(ship))
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
        info!!.addPara("-10% maximum combat readiness", 0f).color = Misc.getHighlightColor()
        info.addPara("Most AI cores have their automated ship multiplier increased by 50%", 0f).color = Misc.getHighlightColor()
        info.addPara("When this ship is deployed in combat, applies the following to all automated combatants :", 0f).color = Misc.getHighlightColor()
        info.addPara("  - +10% damage taken", 0f).color = Misc.getHighlightColor()
        info.addPara("  - +35% weapon and engine damage taken", 0f).color = Misc.getHighlightColor()
    }
    class NSP_autoCheckerPlugin(var ship: ShipAPI): AdvanceableListener {
        val checker = IntervalUtil(1f,1f)
        val mult = 1.10f
        val weaponmult = 1.35f
        val list = ArrayList<ShipAPI>()
        val id = Misc.genUID()
        override fun advance(amount: Float) {
            if (Global.getCombatEngine().isPaused || !ship.isAlive) return
            checker.advance(amount)
            if (checker.intervalElapsed()){
                if (!ship.isAlive || ship.isHulk || ship.isStationModule || ship.isFighter){
                    list.forEach {
                        it.mutableStats.apply {
                            hullDamageTakenMult.unmodify(id)
                            shieldDamageTakenMult.unmodify(id)
                            armorDamageTakenMult.unmodify(id)
                            weaponDamageTakenMult.unmodify(id)
                            engineDamageTakenMult.unmodify(id)
                        }
                        it.childModulesCopy.forEach { module ->
                            module.mutableStats.apply {
                                hullDamageTakenMult.unmodify(id)
                                shieldDamageTakenMult.unmodify(id)
                                armorDamageTakenMult.unmodify(id)
                                weaponDamageTakenMult.unmodify(id)
                                engineDamageTakenMult.unmodify(id)
                            }
                        }
                    }
                    ship.removeListener(this)
                    return
                }
                Global.getCombatEngine().ships.filter { !list.contains(it) && Misc.isAutomated(it) }.forEach {
                    list.add(it)
                    it.mutableStats.apply {
                        hullDamageTakenMult.modifyMult(id, mult)
                        shieldDamageTakenMult.modifyMult(id, mult)
                        armorDamageTakenMult.modifyMult(id, mult)
                        weaponDamageTakenMult.modifyMult(id, weaponmult)
                        engineDamageTakenMult.modifyMult(id, weaponmult)
                    }
                    it.childModulesCopy.forEach { module ->
                        module.mutableStats.apply {
                            hullDamageTakenMult.modifyMult(id, mult)
                            shieldDamageTakenMult.modifyMult(id, mult)
                            armorDamageTakenMult.modifyMult(id, mult)
                            weaponDamageTakenMult.modifyMult(id, weaponmult)
                            engineDamageTakenMult.modifyMult(id, weaponmult)
                        }
                    }
                }
            }
        }
    }
    companion object{
        val FOULMACHINERY_CRPENALTY = 0.10f
    }
}