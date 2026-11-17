package neon.nsp.data.campaign.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI

class NSP_multipleLocationTarget(var entity: SectorEntityToken, var thingID: String): BaseIntelPlugin() {

    val thingName = Global.getSettings().getSpecialItemSpec(thingID).name

    override fun getIcon(): String {
        return Global.getSettings().getSpecialItemSpec(thingID).iconName
    }

    override fun getMapLocation(map: SectorMapAPI?): SectorEntityToken {
        return entity
    }

    override fun getName(): String {
        return thingName
    }

    override fun createSmallDescription(info: TooltipMakerAPI?, width: Float, height: Float) {
        info!!.addPara("A $name is located in the ${entity.starSystem.name}.", 5f)
    }

    override fun setNew(isNew: Boolean) {
        setNew(false)
    }

    override fun isNew(): Boolean {
        return false
    }

    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String>? {
        return null
    }


}