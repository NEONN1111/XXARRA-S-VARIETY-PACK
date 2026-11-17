package neon.nsp.data.econ.items

import com.fs.starfarer.api.ui.TooltipMakerAPI
import neon.nsp.data.econ.items.nsp_baseGeneSplicer

class nsp_geneSplicer: nsp_baseGeneSplicer() {
    override fun addFluffDesc(tooltip: TooltipMakerAPI, filled: Boolean) {
        tooltip.addPara("A phial designed to extract a person's skills in a base form and transplant them onto another, using unclear applications of genetic splicing.", 5f)
        tooltip.addPara("To avoid contamination, subjects are advised to avoid thinking of anything but the property to be extracted, else the future recipient may become privy of confidential knowledge and embarrassing past life events.", 5f)
        tooltip.addPara("It also has the capability of holding standard genemod sequences, but as these were rather expensive to produce, using them on such a mundane application was very uncommon outside of selling them to noble families at extravagant prices.", 5f)
        if (!filled){
            tooltip.addPara("This one lacks an inscribed sequence, and may be deployed to extract one.", 10f)
        }
        else {
            tooltip.addPara("This one currently contains an inscribed sequence. To avoid 'genetic contamination', it will disable itself after being used. A rather non-subtle form of obsolescence.", 10f)
            tooltip.addPara("It's rather plausible that override codes for this function exist, with Domain aristocrats repeatedly using these to extract knowledge from \"stock\" and become educated in every topic under the stars, but if any such codes exist they're long forgotten now.", 5f)

        }
    }
}