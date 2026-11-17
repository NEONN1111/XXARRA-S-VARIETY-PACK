package neon.nsp.data.campaign.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.listeners.ExtraSalvageShownListener
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.IntelUIAPI
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import neon.nsp.data.campaign.intel.NSP_multipleLocationTarget
import org.lwjgl.input.Keyboard
import java.awt.Color

class NSP_multipleLocationIntel(val memoryToCheck: String, val thingID: String): BaseIntelPlugin(), ExtraSalvageShownListener {

    // intel that checks for entities containing a memkey, then displays all of them at once
    // currently only supports special items b/c i'm dumb like that

    val spec = Global.getSettings().getSpecialItemSpec(thingID)
    var thingName =  spec.name
    var thingSprite = spec.iconName
    val thingColor = Misc.getDesignTypeColor(spec.manufacturer)
    init {
        this.isImportant = true
        Global.getSector().listenerManager.addListener(this)
        getPossibleEntities().forEach {
            Misc.makeImportant(it, "revealed")
        }
    }

    override fun getTitleColor(mode: IntelInfoPlugin.ListInfoMode?): Color {
        return thingColor
    }

    override fun createSmallDescription(info: TooltipMakerAPI?, width: Float, height: Float) {
        info!!.addPara("The Historian has recently given you a list of the following systems that should contain a $thingName :", 5f).setHighlight("$thingName")
        val nexii = getPossibleEntities()
        val alreadyAddedSystems = ArrayList<StarSystemAPI>()
        nexii.forEach {
            if (alreadyAddedSystems.contains(it.starSystem)) return@forEach
            alreadyAddedSystems.add(it.starSystem)
            if (it.starSystem.constellation.name.startsWith("The")){
                info.addPara("${it.starSystem.name}, in ${it.starSystem.constellation.name}", 10f).setHighlight(it.starSystem.name)

            } else {
                info.addPara("${it.starSystem.name}, in the ${it.starSystem.constellation.name}", 10f).setHighlight(it.starSystem.name)

            }
        }

        val buttong =  addGenericButton(info, width, "Toggle location display", "nsp_LocationToggle")
        buttong.setShortcut(Keyboard.KEY_T, true)
    }

    override fun createIntelInfo(info: TooltipMakerAPI?, mode: IntelInfoPlugin.ListInfoMode?) {
        info!!.addTitle(this.name)
        info.addPara("A databank containing location markers for odd antiques from before the Collapse.", 5f).color = thingColor
    }

    override fun buttonPressConfirmed(buttonId: Any?, ui: IntelUIAPI) {
        if (buttonId == "nsp_LocationToggle"){
            toggleEntityList(this, ui)
        }
    }
    override fun getIcon(): String {
        return thingSprite
    }

    override fun isHidden(): Boolean {
        return false
    }
    override fun autoAddCampaignMessage(): Boolean {
        return true
    }

    override fun getCommMessageSound(): String {
        return getSoundMajorPosting()
    }
    override fun getListInfoParam(): Any {
        return IntelInfoPlugin.ListInfoMode.MESSAGES
    }


    override fun isImportant(): Boolean {
        return true
    }

    override fun shouldRemoveIntel(): Boolean {
        return false
    }

    override fun getSortTier(): IntelInfoPlugin.IntelSortTier {
        return IntelInfoPlugin.IntelSortTier.TIER_1
    }

    override fun getIntelTags(map: SectorMapAPI?): Set<String>? {
        val tags = super.getIntelTags(map)
        tags.add(Tags.INTEL_ACCEPTED)
        tags.add(Tags.INTEL_MISSIONS)
        return tags
    }


    override fun getName(): String {
        return "Ancient Past : $thingName"
    }
    fun toggleEntityList(curr: IntelInfoPlugin?, ui: IntelUIAPI) {
        val removelist = ArrayList<IntelInfoPlugin>()
        Global.getSector().intelManager.getIntel(NSP_multipleLocationTarget::class.java).forEach {
            removelist.add(it)
        }
        removelist.forEach {
            Global.getSector().intelManager.removeIntel(it)
        }
        if (ui.isShowingCustomIntelSubset) {

            ui.updateIntelList(true)
            ui.updateUIForItem(curr)
            //for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel(LuddicShrineIntel.class)) {
            for (intel in Global.getSector().intelManager.getIntel(NSP_multipleLocationTarget::class.java).filter { (it as NSP_multipleLocationIntel).memoryToCheck == this.memoryToCheck }) {
                ui.selectItem(intel)
                break
            }
        } else {
            getPossibleEntities().forEach {
                Global.getSector().intelManager.addIntel(NSP_multipleLocationTarget(it, thingID), true)
            }
            val show: MutableList<IntelInfoPlugin> = ArrayList()
            for (intel in Global.getSector().intelManager.getIntel(NSP_multipleLocationTarget::class.java)) {
                show.add(intel)
            }
            Global.getSector().intelManager.sortIntel(show)
            ui.updateIntelList(true, show)
            ui.updateUIForItem(curr)
        }
        return
    }
    fun getPossibleEntities(): ArrayList<SectorEntityToken>{
        val entities = ArrayList<SectorEntityToken>()
        Global.getSector().starSystems.forEach {
            it.allEntities.filter { entity -> !entity.isExpired && entity.memoryWithoutUpdate.contains(memoryToCheck) }.forEach {
                entities.add(it)
            }
        }
        return entities
    }

    override fun reportExtraSalvageShown(entity: SectorEntityToken?) {
        if (getPossibleEntities().isEmpty()){
            Global.getSector().listenerManager.removeListener(this)
            this.endImmediately()
        }
    }
}