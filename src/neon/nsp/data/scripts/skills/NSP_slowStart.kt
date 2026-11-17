package neon.nsp.data.scripts.skills

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.combat.CombatUtils
import org.lwjgl.util.vector.Vector2f

class NSP_slowStart: AfterShipCreationSkillEffect, BaseSkillEffectDescription() {

    // Manipulation of memories stored in regions C-8 and C-9 appear to have negative effects on spatial navigation.
    // ... Evidence? One of our senior technicians went to the wrong planet. Three times.
    // technically can be positive, i guess?
    override fun apply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
    }

    override fun unapply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?) {
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        if (!ship!!.customData.containsKey("\$cs_slowStart") && !ship.isStationModule) {
            ship.setCustomData("\$cs_slowStart", true)
            Global.getCombatEngine().addPlugin(cs_slowStartPlugin(ship))
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
        info!!.addPara("When deployed into combat, deployment is delayed by up to 30 seconds", 0f).color = Misc.getHighlightColor()
        info.addPara("Arrives from a random map border", 0f).color = Misc.getHighlightColor()
    }
    // listener won't run if ship isn't in play, so we need to use combat plugin
    class cs_slowStartPlugin(var ship: ShipAPI): BaseEveryFrameCombatPlugin(){
        val nearRange = 1500f
        val min = 15f
        val max = 30f
        val timer = IntervalUtil(min, max)
        val base = 2f // div by 2 to get map edges
        var w = Global.getCombatEngine().mapWidth/base
        var h = Global.getCombatEngine().mapHeight/base
        val dir = MathUtils.getRandomNumberInRange(1, 4) // top/right/bottom/left cw
        var p1 = Vector2f()
        var p2 = Vector2f()
        var facing = 90f // 90 is up... maybe
        init {
            when (dir){
                1 -> {
                    p1 = Vector2f(-w, h)
                    p2 = Vector2f(w, h)
                    facing = -90f

                }
                2 -> {
                    p1 = Vector2f(w, h)
                    p2 = Vector2f(w, -h)
                    facing = 180f


                }
                3 -> {
                    p1 = Vector2f(-w, -h)
                    p2 = Vector2f(w, -h)
                    facing = 90f

                }
                 else -> {
                     p1 = Vector2f(-w, -h)
                     p2 = Vector2f(-w, h)
                     facing = 0f

                 }
            }
        }
        var removed = false
        override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
            if (ship.parentStation != null || ship.isStationModule){ // don't do it to modules
                Global.getCombatEngine().removePlugin(this)
                return
            }
            if (Global.getCombatEngine().isEntityInPlay(ship) && !removed){
                removed = true
                Global.getCombatEngine().removeEntity(ship)
                ship.childModulesCopy.forEach { Global.getCombatEngine().removeEntity(it) }
            }
            if (Global.getCombatEngine().isPaused) return
            timer.advance(amount)
            if (timer.intervalElapsed()){
                var attempts = 0
                val loc = MathUtils.getRandomPointOnLine(p1, p2)
                while (attempts < 50 && CombatUtils.getShipsWithinRange(loc, nearRange)
                    // avoid hostile ships that aren't a hulk and bigger than a fighter
                    .any { !it.isHulk && it.hullSize > ShipAPI.HullSize.FIGHTER && it.owner != ship.owner }
                ){
                    attempts++
                    // attempt to avoid spawning near enemy ships
                    loc.set(MathUtils.getRandomPointOnLine(p1, p2))
                }
                ship.location.set(loc)
                ship.facing = facing
                Global.getCombatEngine().addEntity(ship)
                ship.childModulesCopy.forEach { Global.getCombatEngine().addEntity(it) }
                Global.getCombatEngine().removePlugin(this)
                return
            }
        }
    }
}