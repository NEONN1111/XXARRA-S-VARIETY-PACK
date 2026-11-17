package neon.nsp.data.scripts.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.ui.*
import com.fs.starfarer.api.util.Misc
import neon.nsp.data.campaign.intel.NSP_multipleLocationIntel
import neon.nsp.data.econ.items.nsp_baseGeneSplicer
import neon.nsp.data.econ.items.nsp_mindJackDialogPlugin
import neon.nsp.data.econ.items.nsp_mindJackDialogPlugin.Companion.selectedSkill
import neon.nsp.data.econ.items.nsp_mindJackDialogPlugin.Companion.splicerHiddenTag
import kotlin.collections.ArrayList

class NSP_splicerCmd: BaseCommandPlugin() {
    // todo
    // once per save gene splicer station
    // feed it an empty splicer or maybe don't, it lets you pick one splicer skill, spits it out
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        val order = params!![0].getString(memoryMap)
        when (order){
            "ISPOSSIBLE" -> {
                val entities = ArrayList<SectorEntityToken>()
                Global.getSector().starSystems.forEach {
                    it.allEntities.filter { entity -> !entity.isExpired && entity.memoryWithoutUpdate.contains("\$nsp_splicer") }.forEach {
                        entities.add(it)
                    }
                }
                if (entities.isEmpty()) return false
                return true
            }
            "ADDINTEL" -> {
                val intel = NSP_multipleLocationIntel("\$nsp_splicer", "nsp_geneSplicer")
                Global.getSector().listenerManager.addListener(intel)
                Global.getSector().intelManager.addIntel(intel)
                Global.getSector().intelManager.addIntelToTextPanel(intel, dialog!!.textPanel)

            }
            "SHOWSKILLSSPECIAL" -> {
                showSplicerFreeSkillSelector(dialog!!)
            }
            "ADDSKILLSPECIAL" -> {
                addSpecialSplicer(dialog!!)
            }
        }
        return true
    }
    fun showSplicerFreeSkillSelector(dialog: InteractionDialogAPI){
        selectedSkill = null
        dialog.showCustomDialog(320f, 440f, object : BaseCustomDialogDelegate(){

            override fun createCustomDialog(
                panel: CustomPanelAPI?, callback: CustomDialogDelegate.CustomDialogCallback?
            ) {
                val width = panel!!.position.width
                val height = panel.position.height
                val boxheight = 32f

                val element = panel.createUIElement(width, height, true)
                element.position.inTL(0f, 0f)

                element.addSpacer(5f)
                var prevbutton: ButtonAPI? = null
                var prevIcon: UIComponentAPI? = null
                val list = ArrayList<SkillSpecAPI>()
                Global.getSettings().skillIds.forEach {
                    val spec = Global.getSettings().getSkillSpec(it)
                    if (spec.hasTag(splicerHiddenTag)){
                        list.add(spec)
                    }
                }
                list.sortBy { it.name }

                for (skill in list) {
                    element.setAreaCheckboxFont(Fonts.ORBITRON_16)
                    val img = element.beginImageWithText(skill.spriteName, boxheight, width/2f, false)
                    img.addTitle(skill.name, Misc.getHighlightColor())
                    val p = Global.getFactory().createPerson()
                    p.stats.setSkillLevel(skill.id, 2f)
                    val icon = element.addSkillPanelOneColumn(p, 0f)
                    //val icon = element.addImageWithText(5f)
                    if (prevIcon != null) {
                        icon.position.belowMid(prevIcon, 5f)
                    } else {
                        icon.position.inTL(5f, 5f)
                    }
                    prevIcon = icon
                    val box = element.addAreaCheckbox(
                        "Select",
                        skill.id,
                        Misc.getBrightPlayerColor(),
                        Misc.getBrightPlayerColor(),
                        Misc.getBrightPlayerColor(),
                        (width)/2.5f,
                        boxheight,
                        5f
                    )
                    box.isChecked = false
                    if (prevbutton != null) {
                        box.position.rightOfMid(icon, -icon.position.width/2.5f)
                    } else {
                        box.position.rightOfMid(icon, -icon.position.width/2.5f)
                    }
                    prevbutton = box
                    element.addTooltipToPrevious(object : BaseTooltipCreator() {
                        override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, tooltipParam: Any?) {
                            tooltip!!.setParaFontOrbitron()
                            tooltip.addPara("${skill.name}", 0f).color = Misc.getHighlightColor()
                            //  recreateSkillDesc(skill, tooltip!!) // todo maybe just add a person skill panel instead for each skill??
                        }
                    }, TooltipMakerAPI.TooltipLocation.RIGHT)
                }
                element.addSpacer(5f)
                panel.addUIElement(element)
                element.position.inTL(0f, 0f)
            }

            override fun hasCancelButton(): Boolean {
                return true
            }

            override fun customDialogCancel() {

            }

            override fun customDialogConfirm() {
                if (selectedSkill == null){
                 //   dialog.dismissAsCancel()
                    return
                }
                dialog.optionPanel.clearOptions()
                val p = Global.getFactory().createPerson()
                p.stats.setSkillLevel(selectedSkill,2f)
                dialog.textPanel.addSkillPanel(p, false)
                dialog.textPanel.addPara("Selected this skill")
                val m = dialog.interactionTarget.memoryWithoutUpdate
                if (m.contains("\$nsp_addedSplicerSkills")){ // notify player if skill already exists on map
                    val list = m.get("\$nsp_addedSplicerSkills") as ArrayList<String>
                    if (list.any { it == selectedSkill }){
                        dialog.textPanel.addPara("The machine lets out a beep as you select this output, notifying you that this has been previously dispensed once.\n" +
                                "Perhaps it's already out there somewhere.").setHighlight("previously dispensed")
                    }
                }
                dialog.textPanel.addPara("Proceeding will create a new Gene Splicer with the selected skill.")
                dialog.textPanel.addPara("Would you like to proceed?")

                dialog.optionPanel.addOption("Use", "nsp_splicerStationCreate")
                dialog.optionPanel.addOption("Cancel", "nsp_splicerStationConsider")
            }
            override fun getCustomPanelPlugin(): CustomUIPanelPlugin {
                return nsp_mindJackDialogPlugin.nsp_splicerOfficerSkillPlugin(dialog)
            }


        })

    }

    fun addSpecialSplicer(dialog: InteractionDialogAPI){
        val special = SpecialItemData("nsp_geneSplicer", selectedSkill + "$" + "2")
        Global.getSector().playerFleet.cargo.addSpecial(special, 1f)
        AddRemoveCommodity.addItemGainText(special, 1, dialog.textPanel)
        nsp_baseGeneSplicer.setHiddenSkillKnown(Global.getSettings().getSkillSpec(selectedSkill))
    }
}