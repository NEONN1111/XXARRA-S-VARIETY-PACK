package neon.nsp.data.econ.items

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI
import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.campaign.SpecialItemPlugin
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.ui.impl.StandardTooltipV2
import neon.nsp.data.econ.items.nsp_mindJackDialogPlugin.Companion.splicerHiddenTag
import neon.nsp.data.scripts.util.ReflectionUtils
import neon.nsp.data.scripts.util.ReflectionUtils_Crab
import java.awt.Color

open class nsp_baseGeneSplicer: BaseSpecialItemPlugin() {

    // special item that opens dialog on right click
    // if empty, just display the standard description
    // right click opens dialog to remove a skill from an officer, filtered by if they contain a memkey denoting they've already been selected
    // picking officer opens a secondary menu showing all of their skills, pick again to select the skill then confirm to remove it
    // on removal, grab our stack and assign the data of the skill ID to it
    // the skill icon will now be rendered in the BR of the item icon, and expanding the tip shows the skill info
    // unless the skill spec has a specific tag, then it shows something like :
    // "This specimen already contains a sequence within, which is impossible to identify and may have unforeseen effects."

    // when right clicking a filled splicer, show officer menu again filtered by memkey
    // confirm to add skill to that person and set memkey

    override fun shouldRemoveOnRightClickAction(): Boolean {
        return false
    }

    override fun init(stack: CargoStackAPI?) {
        super.init(stack)
        val d = this.stack.specialDataIfSpecial.data
        val split = getSplitSplicerSkill(d)
        val skillID = split?.keys?.firstOrNull()
        if (skillID == null || Global.getSettings().getSkillSpec(skillID) == null){
            this.stack.specialDataIfSpecial.data = null
            return
            // remove invalid entries if they exist somehow
        }
    }

    override fun hasRightClickAction(): Boolean {
        return (Global.getSector().playerFleet != null && stack?.isInPlayerCargo == true)
    }

    override fun render(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        alphaMult: Float,
        glowMult: Float,
        renderer: SpecialItemPlugin.SpecialItemRendererAPI?
    ) {
        val d = this.stack.specialDataIfSpecial.data ?: return
        val skillID = getSplitSplicerSkill(d)?.keys?.firstOrNull() ?: return
        val skill = Global.getSettings().getSkillSpec(skillID)
        if (skill != null){
            var s = Global.getSettings().getSprite(skill.spriteName)
            if (!shouldShowSplicerSkill(skill)) s = Global.getSettings().getSprite("icons", "thankyoumayu")
            s.setSize(32f,32f)
            s.render(x, y)
        }
    }

    override fun createTooltip(
        tooltip: TooltipMakerAPI?,
        expanded: Boolean,
        transferHandler: CargoTransferHandlerAPI?,
        stackSource: Any?
    ) {
        val d = this.stack.specialDataIfSpecial.data
        val skillID = getSplitSplicerSkill(d)?.keys?.firstOrNull()
        val skillLvl = getSplitSplicerSkill(d)?.values?.firstOrNull()
        val s = Global.getSettings().getSkillSpec(skillID)
        val filled = skillID != null && s != null && skillLvl != null
        val elite = (skillLvl != null && skillLvl > 1)
        var extra = ""
        if (filled){
            if (!shouldShowSplicerSkill(s)) {
                extra = "  [Unknown]"
            }
            else {
                extra = "  [" + s.name + "]"
                if (elite) extra += " - ELITE"
            }
        }
        ReflectionUtils_Crab.invoke("setRecreateEveryFrame", tooltip!!, false)
        if (filled){
            if (expanded) {
                val p = Global.getFactory().createPerson()
                p.stats.setSkillLevel(s.id, skillLvl!!.toFloat())
                val tt = ReflectionUtils_Crab.invokeStatic(8, "createSkillTooltip", StandardTooltipV2::class.java,
                    s,
                    p.stats,
                    tooltipWidth,
                    tooltipWidth / 2f,
                    true,
                    false,
                    0,
                    null) ?: return
                ReflectionUtils_Crab.invoke("setSize", tt, tooltipWidth, tooltipWidth/3f)
                ReflectionUtils_Crab.invoke("makeNonExpandable", tt)
                ReflectionUtils_Crab.invoke("updateSize", tt)
                ReflectionUtils_Crab.invoke("beforeShown", tt)
                val contents = ReflectionUtils_Crab.invoke("getContents", tt) as UIComponentAPI
                val custom = Global.getSettings().createCustom(tooltipWidth, tooltipWidth/3f, null)
                custom.addComponent(contents).inTL(0f,0f)
                tooltip.addCustom(custom, 0f).position.inTL(0f,0f)
                tooltip.addSpacer(custom.position.height/5f)
                return
            }
        }
        tooltip.addTitle(this.spec.name + extra)
        val design = designType
        Misc.addDesignTypePara(tooltip, design, 5f)
        addFluffDesc(tooltip, filled)
        addCostLabel(tooltip, 5f, transferHandler, stackSource)

    }


    override fun getTooltipWidth(): Float {
        return 800f // same width as normal skill box, i think?
    }

    open fun addFluffDesc(tooltip: TooltipMakerAPI, filled: Boolean){
    }

    override fun isTooltipExpandable(): Boolean {
        return getSplitSplicerSkill(this.stack.specialDataIfSpecial.data) != null
    }

    override fun performRightClickAction(helper: SpecialItemPlugin.RightClickActionHelper?) {
        val pcargo = Global.getSector().playerFleet?.cargo ?: return
        if (pcargo.getQuantity(this.stack.type, this.stack.data) <= 0f) return // stop usage if we run out
        Global.getSoundPlayer().playUISound(getSpec().soundId, 1f, 1f)
        val plugin = nsp_mindJackDialogPlugin(this.stack)
        Global.getSector().campaignUI.showInteractionDialogFromCargo(
            plugin, Global.getSector().playerFleet
        ) {}
    }
    companion object{
        val knownSkillList = "\$nsp_knownSplicerSkillList"
        fun shouldShowSplicerSkill(skill: SkillSpecAPI): Boolean{
            return !skill.hasTag(splicerHiddenTag) || isHiddenSkillKnown(skill)

        }
        fun isHiddenSkillKnown(skill: SkillSpecAPI): Boolean{
            return getSkillList().contains(skill.id)

        }
        fun setHiddenSkillKnown(skill: SkillSpecAPI){
            val l = getSkillList()
            l.add(skill.id)
            Global.getSector().playerMemoryWithoutUpdate.set(knownSkillList, l)

        }
        fun getSkillList(): ArrayList<String>{
            val pmem = Global.getSector().playerMemoryWithoutUpdate
            if (!pmem.contains(knownSkillList)){
                pmem.set(knownSkillList, ArrayList<String>())
            }
            return pmem.get(knownSkillList) as ArrayList<String>
        }
        fun getSplitSplicerSkill(data: String?): HashMap<String, Int>?{
            if (data.isNullOrBlank()){
                return null
            }
            val split = data.split("$")
            val skillID = split.getOrNull(0)
            var skillLevel = split.getOrNull(1)?.toIntOrNull()

           // logger.info("split splicer to $skillID and level $skillLevel")
            if (skillLevel == null) skillLevel = 1
            if (skillID == null || Global.getSettings().getSkillSpec(skillID) == null){
                return null
            }
            val map = HashMap<String, Int>()
            map.put(skillID, skillLevel)
            return map
            /*
            attempts to parse the string data on a splicer into a split string and int,
            representing skill ID and level respectively

             */
        }
        fun isCurrentUsageValid(): Boolean{
            /*
            added fleet dialog check for extra jank
            because of issues with item duping
             */
            val currDialog = Global.getSector().campaignUI?.currentInteractionDialog
            val plugin = currDialog?.plugin
            if ((plugin != null && plugin !is FleetInteractionDialogPluginImpl) || Global.getSector().currentlyOpenMarket != null){
                Global.getSector().campaignUI.messageDisplay.addMessage("Leave the current dialog first to use this!", Color.RED)
                return false
            }
            if (plugin !is FleetInteractionDialogPluginImpl) Global.getSector().campaignUI.showCoreUITab(CoreUITabId.CARGO)
            return true
        }
    }


}