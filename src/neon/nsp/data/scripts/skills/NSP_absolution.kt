package neon.nsp.data.scripts.skills

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.AdvanceableListener
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription
import com.fs.starfarer.api.loading.ProjectileSpawnType
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import kotlin.math.roundToInt

class NSP_absolution: BaseSkillEffectDescription(),AfterShipCreationSkillEffect {

    //

    override fun apply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
        val v = stats?.variant ?: return
        if (v.hasHullMod(HullMods.ILL_ADVISED)) {
            stats.apply {
                weaponMalfunctionChance.unmodify("ill_advised")
                criticalMalfunctionChance.unmodify("ill_advised")
                energyRoFMult.modifyMult(id, ROF_MULT)
                energyAmmoRegenMult.modifyMult(id, ROF_MULT)
                energyWeaponFluxCostMod.modifyMult(id, FLUX_MULT)
                ballisticRoFMult.modifyMult(id, ROF_MULT)
                ballisticAmmoRegenMult.modifyMult(id, ROF_MULT)
                ballisticWeaponFluxCostMod.modifyMult(id, FLUX_MULT)
                missileRoFMult.modifyMult(id, ROF_MULT)
                missileAmmoRegenMult.modifyMult(id, ROF_MULT)
                missileWeaponFluxCostMod.modifyMult(id, FLUX_MULT)
            }
        }
    }

    override fun unapply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?) {

    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        if (!ship!!.hasListenerOfClass(NSP_absolutionListener::class.java)){
            ship.addListener(NSP_absolutionListener(ship))
        }
    }

    override fun unapplyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
      //  ship!!.removeListenerOfClass(NSP_absolutionListener::class.java)
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
        val dealt = ((AUTOMATED_DMGMULT - 1f)*100f).roundToInt()
        val taken = ((1f - AUTOMATED_DMGTAKENMULT)*100f).roundToInt()
        val rofmult = ((ROF_MULT - 1f)*100f).roundToInt()

        info!!.addPara("Deals $dealt% more damage to and receives $taken% less damage from automated ships", 0f).color = Misc.getHighlightColor()
        info.addPara("Damage taken from ship explosions is inverted", 0f).color = Misc.getHighlightColor()
        info.addPara("If the ship has Ill-Advised Modifications, negates its penalties and instead increases all weapon rate of fire and ammo reload rate by $rofmult% with a corresponding reduction in flux costs", 0f).color = Misc.getHighlightColor()
    }
    class NSP_absolutionListener(var ship: ShipAPI) : DamageTakenModifier, DamageDealtModifier, AdvanceableListener {
        val mult = 1.00f
        val armormult = mult/50f
        var did = false
        init {
            ship.mass *= 1.50f
        }

        // stole from KoL who took it from someone else who took it from someone else
        // reminder that explosions, even those spawned by weapons, have a null projectile spec ID
        override fun modifyDamageTaken(param: Any?, target: CombatEntityAPI?, damage: DamageAPI?, point: Vector2f?, shieldHit: Boolean): String {
            // checking for ship explosions
            if (param == null) return "oopsie"
            var source: ShipAPI? = null
            if (param is DamagingProjectileAPI) source = param.source
            else if (param is BeamAPI) source = param.source
            if (source != null && Misc.isAutomated(source)){
                damage!!.modifier.modifyMult(this.javaClass.name, AUTOMATED_DMGTAKENMULT)
            }
            if (param is DamagingProjectileAPI) {
                // checks if the damage fits the details of a ship explosion
                if ( param.projectileSpecId == null && param.spawnType == ProjectileSpawnType.OTHER) {
                    // i noticed the distance between a ship and its own explosion can be higher than 1, sometimes
                    // sometimes even as high as 8??

                    // seems to be based on how the ship is moving when it explodes, really only comes up if you burn drive into an enemy
                    // it's better to be lenient in these cases instead of having the player feel like they were cheated... explosions are hopefully not spawning inside ships anyway?
                    if (param.damageType == DamageType.HIGH_EXPLOSIVE  && param.source?.isAlive == false  && MathUtils.getDistanceSquared(param.spawnLocation, param.source.location) < 500f) {
                        val originalDamage = damage!!.damage
                        val modifiedDamage = originalDamage*mult
                        if (!shieldHit) { // need to get this here b/c ship armor grid isn't real in refit apparently
                            val armorGrid = ship.armorGrid
                            val grid = ship.armorGrid.grid
                            val max = ship.armorGrid.maxArmorInCell
                            val loc = point
                            val radius = param.collisionRadius
                            ship.hitpoints = (ship.hitpoints + modifiedDamage).coerceAtMost(ship.maxHitpoints)
                            for (x in grid.indices) {
                                for (y in grid[0].indices) {
                                    if (grid[x][y] < max && MathUtils.isPointWithinCircle(
                                            armorGrid.getLocation(x, y),
                                            loc,
                                            radius
                                        )
                                    ) {
                                        val regen = (grid[x][y] + (modifiedDamage * armormult)).coerceAtMost(max)
                                        armorGrid.setArmorValue(x, y, regen)
                                    }
                                }
                            }
                            ship.syncWithArmorGridState()
                            // get radius of explosion, check if armor cells are in radius, if they are then heal them
                        }
                        else {
                            ship.fluxTracker.decreaseFlux(modifiedDamage)
                        }

                        damage.modifier!!.modifyMult(this.javaClass.getName(), 0f) // can't just mult by -1 because damage cannot be negative
                    }
                }

            }
            return this.javaClass.name
        }

        override fun modifyDamageDealt(
            param: Any?, target: CombatEntityAPI?, damage: DamageAPI?, point: Vector2f?, shieldHit: Boolean
        ): String {
            if (target is ShipAPI){
                if (Misc.isAutomated(target)){
                    damage!!.modifier.modifyMult(this.javaClass.name, AUTOMATED_DMGMULT)
                }
            }
            return this.javaClass.name
        }

        override fun advance(amount: Float) {
            if (Global.getCombatEngine().isPaused) return
            if (ship.fullTimeDeployed > 0.25f && !did){
                did = true
                ship.turnOnTravelDrive(10f)
            }
            val ai = ship.shipAI ?: return // ai is null when under manual control
            ship.aiFlags.apply {
                setFlag(ShipwideAIFlags.AIFlags.IGNORES_ORDERS)
                setFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF_EVEN_WHILE_VENTING)
                setFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF)
            }
        }
    }
    companion object{
        val ROF_MULT = 1.25f
        val FLUX_MULT = 1f / ROF_MULT
        val AUTOMATED_DMGMULT = 1.15f
        val AUTOMATED_DMGTAKENMULT = 0.90f
    }
}