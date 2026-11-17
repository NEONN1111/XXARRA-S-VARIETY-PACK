package neon.nsp.data.scripts.skills

import com.fs.starfarer.api.characters.FleetStatsSkillEffect
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.ShipSkillEffect
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import kotlin.math.roundToInt

class NSP_packRat {

    //
    //
    // ... The two hundred Vulcans? Yes, I need those too.
    // generally a pretty good skill with a small combat penalty if you travel light
    // it's easy to just carry around a ton of supplies anyway, so... not really a big deal, just flavor


    class NSP_packRat1: FleetStatsSkillEffect, BaseSkillEffectDescription() {
        // basically still exists b/c i'm stupid, there's no reason to do this in here
        override fun hasCustomDescription(): Boolean {
            return true
        }

        override fun createCustomDescription(
            stats: MutableCharacterStatsAPI?, skill: SkillSpecAPI?, info: TooltipMakerAPI?, width: Float
        ) {
        }

        override fun apply(stats: MutableFleetStatsAPI?, id: String?, level: Float) {
            stats!!.dynamic.apply {
                getMod(Stats.ENEMY_WEAPON_RECOVERY_MOD).modifyFlat(id, WEAPON_SALVAGE_BONUS)
                getStat(Stats.SALVAGE_VALUE_MULT_FLEET_INCLUDES_RARE).modifyFlat(id, RARE_SALVAGE_BONUS)
             //   getStat(Stats.SALVAGE_VALUE_MULT_FLEET_NOT_RARE).modifyFlat(id, RARE_SALVAGE_BONUS)
            }
        }

        override fun unapply(stats: MutableFleetStatsAPI?, id: String?) {
            stats!!.dynamic.apply {
                getMod(Stats.ENEMY_WEAPON_RECOVERY_MOD).unmodify(id)
                getStat(Stats.SALVAGE_VALUE_MULT_FLEET_INCLUDES_RARE).unmodify(id)
            }
        }

    }
    class NSP_packRat2: ShipSkillEffect, BaseSkillEffectDescription() {
        override fun apply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
        }

        override fun unapply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?) {
        }
        override fun hasCustomDescription(): Boolean {
            return true
        }

        override fun createCustomDescription(
            stats: MutableCharacterStatsAPI?, skill: SkillSpecAPI?, info: TooltipMakerAPI?, width: Float
        ) {
            val rare = (RARE_SALVAGE_BONUS * 100f).roundToInt()
            val cargo = (CARGO_BONUS * 100f).roundToInt()
            val weapons = (WEAPON_SALVAGE_BONUS * 100f).roundToInt()
            val cargothreshold = (CARGO_THRESHOLD * 100F).roundToInt()
            val pen = (CR_PENALTY * 100f).roundToInt()
            info!!.addPara(
                "+$rare% resources, including rare items such as blueprints, recovered from abandoned stations and other derelicts",
                0f
            ).color = Misc.getHighlightColor()
            info.addPara("+$weapons% weapons recovered from enemies after combat", 0f).color = Misc.getHighlightColor()
            info.addPara("+$cargo% cargo capacity for all ships in fleet", 0f).color = Misc.getHighlightColor()
            info.addPara(
                "If fleet is using less than $cargothreshold% of its cargo capacity, all ships lose $pen% maximum CR",
                0f
            ).color = Misc.getHighlightColor()
            val f = stats?.fleet ?: return
        }
    }
    companion object {
        const val RARE_SALVAGE_BONUS = 0.10f // for comparison, SHU fullerene spool on a capital is 8%
        const val WEAPON_SALVAGE_BONUS = 0.30f
        const val CARGO_BONUS = 0.20f
        const val CARGO_THRESHOLD = 0.60f
        const val CR_PENALTY = 0.10f
    }
}