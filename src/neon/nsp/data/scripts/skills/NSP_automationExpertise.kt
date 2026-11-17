package neon.nsp.data.scripts.skills

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.ShipSkillEffect
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription
import com.fs.starfarer.api.impl.combat.CRPluginImpl
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import kotlin.math.roundToInt

class NSP_automationExpertise: BaseSkillEffectDescription(), ShipSkillEffect {
    override fun apply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {

    }

    override fun unapply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?) {
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
        val cent = (AUTO_CR_BONUS *100f).roundToInt()
        val red = ((1f - AUTO_MAINT_MULT)*100f).roundToInt()
        val start = (CRPluginImpl.MALFUNCTION_START*100f).roundToInt()
        val new = (CRPluginImpl.MALFUNCTION_START* AUTO_MAINT_MULT *100f).roundToInt()
        info!!.addPara("+$cent% maximum combat readiness", 0f).color = Misc.getHighlightColor()
        info.addPara("CR threshold at which malfunctions begin to occur is lowered by $red%", 0f).color = Misc.getHighlightColor()
        info.addPara("  - The base threshold is $start%, which is reduced to $new%", 0f).color = Misc.getHighlightColor()
    }
    companion object{
        val AUTO_CR_BONUS = 0.05f
        val AUTO_MAINT_MULT = 0.70f
    }
}