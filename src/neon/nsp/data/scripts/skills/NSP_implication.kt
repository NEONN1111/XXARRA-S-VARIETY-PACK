package neon.nsp.data.scripts.skills

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MonthlyReport
import com.fs.starfarer.api.campaign.econ.MonthlyReport.ADMIN
import com.fs.starfarer.api.campaign.econ.MonthlyReport.FDNode
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.characters.ShipSkillEffect
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.shared.SharedData
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import kotlin.math.roundToInt

class NSP_implication: BaseSkillEffectDescription(), ShipSkillEffect {
    override fun apply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
        if (!Global.getSector().listenerManager.hasListenerOfClass(NSP_superiorNegotiationListener::class.java)){
            Global.getSector().listenerManager.addListener(NSP_superiorNegotiationListener(), true)
        }
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
        val crewcent = ((1f - CREW_MULT)*100f).roundToInt()
        val offcent = ((1f - OFFICERADMIN_MULT)*100f).roundToInt()
        info!!.addPara("Through ancient knowledge of superior negotiation tactics, your crew are willing to take a marginal hit to their pay", 0f).color = Misc.getHighlightColor()
        info.addPara("Reduces salaries for crew by $crewcent%, and officers and admins by $offcent%",0f).color = Misc.getHighlightColor()
    }
    companion object{
        val CREW_MULT = 0.50f
        val OFFICERADMIN_MULT = 0.67f
    }
    class NSP_superiorNegotiationListener: EconomyTickListener {
        override fun reportEconomyTick(iterIndex: Int) {

            val lastIterInMonth = Global.getSettings().getFloat("economyIterPerMonth").toInt() - 1
            if (iterIndex != lastIterInMonth) { // only run once on last economy tick in the month
                return
            }
            val playerFleet = Global.getSector().playerFleet ?: return
            val offs = playerFleet.fleetData.officersCopy.map { it.person }
            val ppl = ArrayList<PersonAPI>()
            ppl.add(Global.getSector().playerPerson)
            ppl.addAll(offs)
            if (!ppl.any { it.stats.hasSkill("nsp_implication") }) {
                Global.getSector().listenerManager.removeListener(this)
                return
            }
            val report = SharedData.getData().getCurrentReport();
            val fleetNode: FDNode = report.getNode(MonthlyReport.FLEET)
            val crewNode = report.getNode(fleetNode, MonthlyReport.CREW)
            crewNode.upkeep *= CREW_MULT


            //FDNode officersNode = report.getNode(MonthlyReport.OFFICERS);
            val officersNode = report.getNode(fleetNode, MonthlyReport.OFFICERS)
            val adminNode = report.getNode(fleetNode, ADMIN)

            for (od in playerFleet.fleetData.officersCopy) {
                val oNode = report.getNode(officersNode, od.person.id)
                if (oNode != null) oNode.upkeep *= OFFICERADMIN_MULT
            }

            for (admin in Global.getSector().characterData.admins){
                val aNode = report.getNode(adminNode, admin.person.id)
                if (aNode != null) aNode.upkeep *= OFFICERADMIN_MULT
            }

        }

        override fun reportEconomyMonthEnd() {
        }
    }
}