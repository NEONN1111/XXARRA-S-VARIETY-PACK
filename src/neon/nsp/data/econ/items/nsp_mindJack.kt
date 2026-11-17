package neon.nsp.data.econ.items

import com.fs.starfarer.api.ui.TooltipMakerAPI
import neon.nsp.data.econ.items.nsp_baseGeneSplicer

class nsp_mindJack : nsp_baseGeneSplicer() {

    override fun addFluffDesc(tooltip: TooltipMakerAPI, filled: Boolean) {
        tooltip.addPara("Wreathed in all sorts of wires with a vicious-looking prong at its end, with a genuine Exodyne Biotech phial known as a \"gene splicer\" tenuously hooked into it.", 5f)
        tooltip.addPara("When AI development began in earnest, it inevitably brought into speculation the deployment of fully automated task forces - given proper opsec security and fallback protocols, would all wars of the future be entirely carried out by AI? Or does humanity have something that can't be replicated?", 5f)
        tooltip.addPara("This invention is the result of yet another follow-up question; why choose?", 5f)
        tooltip.addPara("Can be deployed to extract a skill from an AI core, destroying it in the process.", 5f).setHighlight("AI core", "destroying")
    }
}