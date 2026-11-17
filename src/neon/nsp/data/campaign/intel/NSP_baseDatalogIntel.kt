package neon.nsp.data.campaign.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.IntelUIAPI
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI

class NSP_baseDatalogIntel(var intelName: String, var desc: String, var buttonID: String): BaseIntelPlugin() {


    val pad = 5f
    override fun getIcon(): String {
        return "graphics/icons/intel/fleet_log3.png"
    }
    override fun createSmallDescription(info: TooltipMakerAPI?, width: Float, height: Float) {
        info!!.addTitle(this.name)
        info.addPara(this.desc, pad)
        info.addButton("View data log", buttonID, 128f,32f,pad).position.inBMid(0f)
        addDeleteButton(info, width)
    }
    override fun createIntelInfo(info: TooltipMakerAPI?, mode: IntelInfoPlugin.ListInfoMode?) {
        val maxLength = 45
        info!!.addTitle(this.name)
        val toDrop = (this.desc.length - maxLength).coerceAtLeast(0)
        info.addPara(this.desc.dropLast(toDrop) + "...", pad)

    }

    override fun buttonPressConfirmed(buttonId: Any?, ui: IntelUIAPI?) {
        super.buttonPressConfirmed(buttonId, ui)
        if (buttonId == this.buttonID){
            Global.getSector().campaignUI.showInteractionDialogFromCargo(RuleBasedInteractionDialogPluginImpl(this.buttonID), Global.getSector().playerFleet ){}
        }
    }

    override fun getName(): String {
        return intelName
    }
    override fun getListInfoParam(): Any {
        return IntelInfoPlugin.ListInfoMode.MESSAGES
    }

    override fun getIntelTags(map: SectorMapAPI?): Set<String>? {
        val tags = super.getIntelTags(map)
        tags.add(Tags.INTEL_STORY)
        tags.add(Tags.INTEL_MISSIONS)
        return tags
    }

    override fun getSortTier(): IntelInfoPlugin.IntelSortTier {
        return IntelInfoPlugin.IntelSortTier.TIER_4
    }

}