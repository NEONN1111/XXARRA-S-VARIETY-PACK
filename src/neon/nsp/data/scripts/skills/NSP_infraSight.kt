package neon.nsp.data.scripts.skills

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect
import com.fs.starfarer.api.characters.LevelBasedEffect
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.AdvanceableListener
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.combat.AIUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.util.MagicRender
import java.awt.Color

class NSP_infraSight: AfterShipCreationSkillEffect, BaseSkillEffectDescription() {


    override fun hasCustomDescription(): Boolean {
        return true
    }

    override fun createCustomDescription(
        stats: MutableCharacterStatsAPI?,
        skill: SkillSpecAPI?,
        info: TooltipMakerAPI?,
        width: Float
    ) {
      //  info!!.addPara("Identify weak points on the hull of nearby enemy ships that the ship and its allies can exploit", 0f).color = Misc.getHighlightColor()
        info!!.addPara("Nearby enemies have a weakness revealed at a random point on their hull, marked by a target reticule", 0f).color = Misc.getHighlightColor()
        info.addPara("Damage dealt to this point is increased by 40% and deals additional damage to weapons and engines", 0f).color = Misc.getHighlightColor()
        info.addPara("The reticule moves to a random position after sustaining enough damage", 0f).color = Misc.getHighlightColor()
    }
    override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
        return LevelBasedEffect.ScopeDescription.PILOTED_SHIP
    }

    override fun apply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
    }

    override fun unapply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?) {
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        if (!ship!!.hasListenerOfClass(NSP_weaknessExploitApplyer::class.java)){
            ship.addListener(NSP_weaknessExploitApplyer(ship))
        }
    }

    override fun unapplyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        ship!!.removeListenerOfClass(NSP_weaknessExploitApplyer::class.java)
    }
    class NSP_weaknessExploitApplyer(var ship: ShipAPI): AdvanceableListener {
        val range = 1400f
        val id = ship.id
        override fun advance(amount: Float) {
            if (!ship.isAlive){
                return
            }
            AIUtils.getNearbyEnemies(ship, range).filter { !it.isFighter && it.hullSize > ShipAPI.HullSize.FIGHTER
                    && it.isAlive
                    && it.exactBounds != null
                    && !it.customData.containsKey("\$$id" + "_exploit") }.forEach {
                it.addListener(nsp_weaknessExploitDamageMod(ship, it))
                it.setCustomData("\$$id" + "_exploit", true)
            }
        }
    }

    class nsp_weaknessExploitDamageMod(var source: ShipAPI, var target: ShipAPI): AdvanceableListener, DamageTakenModifier {
        val ID = "nsp_weaknessExploit"
        val sound_ID = "nsp_hitmarker"
        val mult = 1.40f
        val baserad = 32f
        var targetLocation = target.exactBounds?.segments?.randomOrNull()?.p1
        val sprite = Global.getSettings().getSprite("fx", "nsp_brightreticule") // was REALLY hard to see holo target
        val radius = (target.collisionRadius/4f).coerceIn(baserad, baserad*4f) // flat radius is bad, scale on target width so it's not stupid vs frigs
        var boosteddmg = 0f
        val threshold = target.maxHitpoints / 1.5f
        var alpha = 0f
        var angle = 0f
        val spinner = IntervalUtil(0.05f,0.05f)
        var countdown = 15f
        val basecd = 15f
        val spin = 3f
        val color = Color.WHITE
        override fun advance(amount: Float) {
            if (Global.getCombatEngine().isPaused) return
            if (targetLocation == null || !target.isAlive){ // avoid oopsie if somehow tagging something w/ no bounds
                target.removeListener(this)
                return
            }
            countdown -= amount // to avoid reticule being stuck in position impossible to reach, i guess?
            // mainly happens on ships w/ weird bounds (paragon) or very large trenches (falcon)
            if (countdown <= 0f){
                resetReticule()
            }
            if (source != Global.getCombatEngine().playerShip) return
            alpha = (alpha + amount).coerceAtMost(1f)
            sprite.alphaMult = alpha
            if (target.isPhased) alpha = target.extraAlphaMult
            spinner.advance(amount)
            if (spinner.intervalElapsed()) angle += spin
            target.exactBounds.update(target.location, target.facing)
            MagicRender.singleframe(sprite, targetLocation, Vector2f(radius,radius), angle, color, false, CombatEngineLayers.ABOVE_PARTICLES)

        }

        override fun modifyDamageTaken(
            param: Any?, target: CombatEntityAPI?, damage: DamageAPI?, point: Vector2f?, shieldHit: Boolean
        ): String {
            var dmgsource: ShipAPI? = null
            if (param is DamagingProjectileAPI){
                dmgsource = param.source
            }
            else if (param is BeamAPI){
                dmgsource = param.source
            }
            if (dmgsource == null || dmgsource != source) return "oops"
            if (shieldHit || damage == null) return "oops"
            if (!MathUtils.isPointWithinCircle(point, targetLocation, radius/1.5f)) return "oops" // slightly generous
            damage.modifier.modifyMult(ID, mult)
            countdown = basecd
            Global.getSoundPlayer().playSound(sound_ID, MathUtils.getRandomNumberInRange(0.96f, 1.04f),1f, point, Vector2f())
            val base: Float = damage.computeDamageDealt(Global.getCombatEngine().elapsedInLastFrame)
            val emp = base / 2f
            damage.fluxComponent += emp // boost dmg dealt to weapons/engines at impact point by giving it emp dmg
            boosteddmg += base
            if (boosteddmg > threshold){
               resetReticule()
            }
            return this.javaClass.name
        }
        private fun resetReticule(){
            countdown = basecd
            boosteddmg = 0f
            alpha = 0f
            targetLocation = this.target.exactBounds.segments.randomOrNull()?.p1
        }
    }
}