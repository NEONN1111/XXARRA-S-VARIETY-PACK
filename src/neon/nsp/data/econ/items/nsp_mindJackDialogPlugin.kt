package neon.nsp.data.econ.items

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.*
import com.fs.starfarer.api.util.Misc
import neon.nsp.data.nsp_referenceSheet
import neon.nsp.data.econ.items.nsp_baseGeneSplicer.Companion.isHiddenSkillKnown
import neon.nsp.data.econ.items.nsp_baseGeneSplicer.Companion.shouldShowSplicerSkill
import neon.nsp.data.scripts.NSP_lunaStuff
import org.apache.log4j.Logger
import kotlin.math.roundToInt

class nsp_mindJackDialogPlugin(var stack: CargoStackAPI): InteractionDialogPlugin {
    /*
    used for splicers and mind jack
    uses item id to determine person pool
    if data is null on person selected, go to skill pool. otherwise, skip to adding skill
    basically just rewrote all the old dogshit code
    still sucks, but marginally less so
     */
    var dialog: InteractionDialogAPI? = null
    val splicerID = "nsp_geneSplicer"
    val jackID = "nsp_mindJack"
    val ID = stack.specialDataIfSpecial.id
    val cargo = Global.getSector().playerFleet.cargo

    var skill: String? = null
    val commToSpecialMap = HashMap<String, String>()
    val commToDataMap = HashMap<String, String>()

    private enum class Option{
        STORE_SKILL, ADD_SKILL, SHOW_SKILLS, LEAVE

    }
    fun consume(){
        if (selectedPerson == null) return
      //  val logger = org.apache.log4j.Logger.getLogger(this::class.java)

        cargo.removeItems(CargoAPI.CargoItemType.SPECIAL, stack.specialDataIfSpecial, 1f)
        cargo.removeEmptyStacks()
        AddRemoveCommodity.addItemLossText(stack.specialDataIfSpecial, 1, dialog!!.textPanel)
        if (stack.specialDataIfSpecial.data == null) {
            if (selectedPerson!!.aiCoreId != null) {
             //   logger.info("checking ai core id ${selectedPerson!!.aiCoreId}")
                if (commToSpecialMap.containsKey(selectedPerson!!.aiCoreId)){
                    val specialID = commToSpecialMap.getValue(selectedPerson!!.aiCoreId)
                 //   logger.info("checking special ID $specialID")
                    val data: String?
                    if (commToDataMap.containsKey(selectedPerson!!.aiCoreId)) {
                        data = commToDataMap.getValue(selectedPerson!!.aiCoreId)
                //        logger.info("data is $data")
                    }
                    else data = null
                    cargo.removeItems(CargoAPI.CargoItemType.SPECIAL, SpecialItemData(specialID, data), 1f)
                    AddRemoveCommodity.addItemLossText(SpecialItemData(specialID, data), 1, dialog!!.textPanel)
                }
                else {
                    cargo.removeCommodity(selectedPerson!!.aiCoreId, 1f)
                    AddRemoveCommodity.addCommodityGainText(selectedPerson!!.aiCoreId, 1, dialog!!.textPanel)

                }

            }
        }
        Global.getSector().campaignUI.showCoreUITab(CoreUITabId.CARGO) // for some reason we can sometimes get a visual bug where stacks overlap
        // unless we do this. idk man.
    }
    fun showSplicerOfficerPicker(dialog: InteractionDialogAPI){
        selectedPerson = null
        selectedSkill = null
        dialog.showCustomDialog(320f, 440f, object : nsp_officerPickerDialog(dialog){
            /*
            epic duplicate code
             */
            override fun createCustomDialog(
                panel: CustomPanelAPI?,
                callback: CustomDialogDelegate.CustomDialogCallback?
            ) {
                val width = panel!!.position.width
                val height = panel.position.height
                val boxheight = 64f

                val element = panel.createUIElement(width, height, true)
                element.position.inTL(0f, 0f)

                element.addSpacer(5f)
                var prevbutton: ButtonAPI? = null
                var prevIcon: UIPanelAPI? = null
                val list = getAvailablePersons()


                for (officer in list) {
                    val isAICore = officer.isAICore
                    val numSlotsUsed = getNumUses(officer)
                    val cap = getCap(officer)
                    val available = cap - numSlotsUsed // num slots can be negative if we took more skills that we gave, so it'll even out eventually
                    val overLimit = available <= 0
                    val isSkillRemoval = stack.specialDataIfSpecial.data == null
                    element.setAreaCheckboxFont(Fonts.ORBITRON_16)
                    val img = element.beginImageWithText(officer.portraitSprite, boxheight, width / 1.5f, false)
                    img.addTitle(officer.name.fullName)
                    img.addPara("level ${officer.stats.level}", 5f)
                    if (!isAICore) {
                        img.addPara("gene slots : $available", 0f).setHighlight("$available")
                        if (overLimit && !isSkillRemoval){
                            img.addPara("at limit!", 0f).color = Misc.getNegativeHighlightColor()
                        }
                    }
                    val icon = element.addImageWithText(5f)
                    if (prevIcon != null) {
                        icon.position.belowMid(prevIcon, 5f)
                    } else {
                        icon.position.inTL(5f, 5f)
                    }
                    val color = if (overLimit && !isSkillRemoval) Misc.getNegativeHighlightColor() else Misc.getBrightPlayerColor()
                    prevIcon = icon
                    val box = element.addAreaCheckbox(
                        "Select",
                        officer.id,
                        Misc.getBrightPlayerColor(),
                        Misc.getBrightPlayerColor(),
                        color,
                        width - icon.position.width,
                        boxheight,
                        5f
                    )
                    box.isChecked = false
                    if (overLimit && !isSkillRemoval){
                        box.setClickable(false)
                    }
                    if (prevbutton != null) {
                        box.position.belowMid(prevbutton, 5f)
                    } else {
                        box.position.inTR(5f, 5f)
                    }
                    prevbutton = box
                    element.addTooltipToPrevious(object : BaseTooltipCreator() {
                        override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, tooltipParam: Any?) {
                            tooltip!!.addSkillPanel(officer, 0f)
                        }
                    }, TooltipMakerAPI.TooltipLocation.RIGHT)
                }
                element.addSpacer(5f)
                panel.addUIElement(element)
                element.position.inTL(0f, 0f)
            }
            override fun getAvailablePersons(): List<PersonAPI> {
                if (ID == jackID){
                    val ppl = ArrayList<PersonAPI>()
                    /*
                    try to get all cores in player cargo
                    check commodities for non-null plugins,
                    then also check special items for non-null plugins
                     */
                 //   val logger = org.apache.log4j.Logger.getLogger(this::class.java)

                    cargo.stacksCopy.forEach {
                        if (it.isCommodityStack){
                          //  logger.info("checking comm stack ${it.displayName}")
                            val plugin = Misc.getAICoreOfficerPlugin(it.commodityId) ?: return@forEach
                            val person = plugin.createPerson(it.commodityId, Factions.PLAYER, Misc.random)
                            if (person != null){
                                person.id = it.commodityId
                                if (ppl.any { it.aiCoreId == person.aiCoreId }) return@forEach
                                ppl.add(person)
                            }
                        }
                        else if (it.isSpecialStack){
                          //  logger.info("checking special stack ${it.displayName}")
                            val id = it.specialDataIfSpecial.id
                            val data = it.specialDataIfSpecial.data
                         //   logger.info("id is $id, data is ${data.toString()}")
                            val allSpecs = Global.getSettings().allCommoditySpecs
                            var matchingComm = allSpecs.firstOrNull { comm -> id.equals(comm.id, true) && Misc.getAICoreOfficerPlugin(comm.id) != null}
                            if (matchingComm == null && data != null){
                                /*
                                have to use .equals here, as using .contains causes us to pick ourselves up...
                                which THEN causes us to overwrite the position in the map with a fake item
                                 */
                             //   logger.info("didn't find a match, but has non-null data")
                                matchingComm = allSpecs.firstOrNull { comm -> data.equals(comm.id, true) && Misc.getAICoreOfficerPlugin(comm.id) != null } ?: return@forEach
                                commToDataMap.put(matchingComm.id, data)
                            //    logger.info("found a match for ${data.toString()}")

                            }
                            if (matchingComm != null){
                                val plugin = Misc.getAICoreOfficerPlugin(matchingComm.id)
                                val person = plugin.createPerson(matchingComm.id, Factions.PLAYER, Misc.random)
                                person.id = matchingComm.id // for easier tracking
                                if (ppl.any { it.aiCoreId == person.aiCoreId }) return@forEach
                                ppl.add(person)
                                commToSpecialMap.put(matchingComm.id, id)
                             //   logger.info("found a match for $id")
                             //   logger.info("mapping ${matchingComm.id} to $id")
                            }
                        }
                    }
                    return ppl

                }
                else {
                    return super.getAvailablePersons()
                }
            }

            override fun customDialogCancel() {
                selectedSkill = null
                selectedPerson = null
                dialog.dismissAsCancel()
            }
            override fun customDialogConfirm() {
                val officer = selectedPerson
                if (officer == null) {
                    dialog.dismissAsCancel()
                    return
                }
                dialog.visualPanel.showPersonInfo(selectedPerson, true, false)
                dialog.optionPanel.clearOptions()
                if (stack.specialDataIfSpecial.data == null) { // if data is null, we must be trying to remove a skill, so...
                    showSplicerSkillPicker(officer, dialog)
                    dialog.optionPanel.addOption("Select a skill", Option.SHOW_SKILLS)
                    dialog.optionPanel.addOption("Leave", Option.LEAVE)
                    return
                }
                else { // if not null, we must be trying to add a skill
                    val tip = dialog.textPanel.beginTooltip()
                    val img = tip.beginImageWithText(officer.portraitSprite, 64f, dialog.textWidth, false)
                    img.addTitle(officer.name.fullName)
                    img.addPara("Selected this officer", 5f)
                    tip.addImageWithText(5f)
                    dialog.textPanel.addTooltip()
                    val s = nsp_baseGeneSplicer.getSplitSplicerSkill(stack.specialDataIfSpecial.data)
                    val skillID = s!!.keys.first()
                    val skillLevel = s.values.first()
                    val skill = Global.getSettings().getSkillSpec(skillID)
                    if (!shouldShowSplicerSkill(skill)){
                        dialog.textPanel.addPara("The splicer was previously filled with a now unidentified sequence.")
                        dialog.textPanel.addPara("The contents could be very dangerous - or very useful, with some luck.")
                    }
                    else {
                        val p = Global.getFactory().createPerson()
                        p.stats.setSkillLevel(skillID, skillLevel.toFloat())
                        dialog.textPanel.addSkillPanel(p, false)
                    }

                    if (officer.stats.hasSkill(skill.id)){
                        dialog.textPanel.addPara("Officer already knows this skill. Cannot be used.")
                        dialog.optionPanel.addOption("Cancel", Option.LEAVE)
                        return
                    }
                    val currAvailable = getCap(officer) - getNumUses(officer)
                    dialog.textPanel.addPara("Proceeding will consume the splicer and add the skill to this officer.")
                    dialog.textPanel.addPara("This will consume a vacant gene slot. There are currently $currAvailable vacant slots.").setHighlight("consume", "$currAvailable")
                    dialog.textPanel.addPara("You can free up slots by using empty Gene Splicers to remove other skills.").setHighlight("empty", "remove")
                    dialog.textPanel.addPara("Would you like to proceed?")

                    dialog.optionPanel.addOption("Use", Option.ADD_SKILL)
                    dialog.optionPanel.addOption("Cancel", Option.LEAVE)

                }
            }
        })
    }
    /*
     todo
     add support for storing and taking out genes
     storing:
     when using an empty splicer, instead of showing a skill list, show list of all stored genes and the skills on each
     confirming storage will grab our intel, creating it if it doesn't exist (should never happen, but idk), then remove the gene and throw it in the splicer
     as for storing what's actually in it...
     1. store via regex (split something like person Name$skillID1$skillID2$geneType
     2. create a unique identifier for the gene that is stored in an array somewhere permanently, and use the id to retrieve it
     (e.g. baseGene data class with an ID field, splicer uses its UID and looks for a matching gene)
     potentially has a memory leak if genes are "lost" somehow (like by spacing or vending them)

     for adding genes, work backwards and grab the gene using the storage method
     if a splicer has a gene in it, only allow usage on the player

     geneMode true if taking skill off officer
     */
    fun isInGeneMode(officer: PersonAPI): Boolean{
        return officer.isPlayer && officer.stats.hasSkill(nsp_referenceSheet.PREDATORY_EVOLUTION)
    }
    fun addSplicerSkill(officer: PersonAPI, dialog: InteractionDialogAPI){
        val skill = nsp_baseGeneSplicer.getSplitSplicerSkill(stack.specialDataIfSpecial.data)
        val skillID = skill!!.keys.first()
        val lvl = skill.values.first()
        val s = Global.getSettings().getSkillSpec(skillID)
        officer.stats.setSkillLevel(skillID, lvl.toFloat())
        consume()
        if (!officer.isAICore) advanceOneUsage(officer)
        dialog.textPanel.setFontVictor()
        dialog.textPanel.addPara("${officer.name.first} now knows level ${lvl} ${s.name}.").color = Misc.getHighlightColor()
        dialog.textPanel.setFontInsignia()
        dialog.optionPanel.clearOptions()
        dialog.optionPanel.addOption("Leave", Option.LEAVE)
        if (!shouldShowSplicerSkill(s) && !isHiddenSkillKnown(s)){
            dialog.textPanel.setFontSmallInsignia()
            dialog.textPanel.addPara("Learned to identify ${s.name}.").color = Misc.getHighlightColor()
            dialog.textPanel.setFontInsignia()
            nsp_baseGeneSplicer.setHiddenSkillKnown(s)
     //   }
      //  Global.getSector().listenerManager.getListeners(nsp_splicerUsedListener::class.java).forEach {
     //       it.onSplicerUsed(officer, skillID, false)
        }
    }
    fun storeSplicerSkill(officer: PersonAPI, dialog: InteractionDialogAPI){
        val data = selectedSkill + "$" + "${officer.stats.getSkillLevel(selectedSkill).roundToInt().toString()}"
        val special = SpecialItemData("nsp_geneSplicer", data)
        val logger = Logger.getLogger(this::class.java)
        logger.info("adding data $data to splicer")
        consume()
        if (!officer.isAICore) removeOneUsage(officer)
        cargo.addSpecial(special, 1f)
        officer.stats.setSkillLevel(selectedSkill, 0f)
        dialog.textPanel.setFontVictor()
        dialog.textPanel.addPara("Skill has been stored inside the splicer.").color = Misc.getHighlightColor()
        dialog.textPanel.setFontInsignia()
        dialog.optionPanel.clearOptions()
        dialog.optionPanel.addOption("Leave", Option.LEAVE)
     //   Global.getSector().listenerManager.getListeners(nsp_splicerUsedListener::class.java).forEach {
         //   it.onSplicerUsed(officer, selectedSkill, true)
       // }
    }
    fun showSplicerSkillPicker(officer: PersonAPI, dialog: InteractionDialogAPI){
        dialog.showCustomDialog(320f, 440f, object: BaseCustomDialogDelegate() {
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
                val list = officer.stats.skillsCopy.filter { it.skill.isCombatOfficerSkill && !it.skill.isAptitudeEffect && it.level > 0f && !it.skill.hasTag("nsp_noSplice") }.map { it.skill }

                for (skill in list) {
                    element.setAreaCheckboxFont(Fonts.ORBITRON_16)
                    val img = element.beginImageWithText(skill.spriteName, boxheight, width/2f, false)
                    img.addTitle(skill.name, Misc.getHighlightColor())
                    val p = Global.getFactory().createPerson()
                    p.stats.setSkillLevel(skill.id, officer.stats.getSkillLevel(skill.id))
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
                dialog.dismissAsCancel()
            }

            override fun customDialogConfirm() {
                if (selectedSkill == null){
                    dialog.dismissAsCancel()
                    return
                }
                dialog.optionPanel.clearOptions()

                val tip = dialog.textPanel.beginTooltip()
                val img = tip.beginImageWithText(officer.portraitSprite, 64f, dialog.textWidth, false)
                img.addTitle(officer.name.fullName)
                img.addPara("Selected this officer", 5f)
                tip.addImageWithText(5f)
                dialog.textPanel.addTooltip()
                val p = Global.getFactory().createPerson()
                p.stats.setSkillLevel(selectedSkill, officer.stats.getSkillLevel(selectedSkill))
                dialog.textPanel.addSkillPanel(p, false)
                if (selectedPerson!!.isAICore) {
                    val p1 = dialog.textPanel.addPara("Proceeding will destroy this AI core and store the skill in the splicer.")
                    p1.setHighlightColor(Misc.getNegativeHighlightColor())
                    p1.setHighlight("destroy")
                    dialog.textPanel.addPara("This cannot be undone!").color = Misc.getNegativeHighlightColor()
                }
                else {
                    val new = (getCap(officer) - getNumUses(officer)) + 1
                    dialog.textPanel.addPara("Proceeding will remove the skill from this officer and store it in the splicer.")
                    dialog.textPanel.addPara("Increases the number of vacant gene slots to $new.").setHighlight("$new")
                }
                dialog.textPanel.addPara("Would you like to proceed?")

                dialog.optionPanel.addOption("Use", Option.STORE_SKILL)
                dialog.optionPanel.addOption("Cancel", Option.LEAVE)

            }

            override fun getCustomPanelPlugin(): CustomUIPanelPlugin {
                return nsp_splicerOfficerSkillPlugin(dialog)
            }
        })
    }
    override fun init(dialog: InteractionDialogAPI?) {
        selectedSkill = null
        selectedPerson = null
        this.dialog = dialog
        showSplicerOfficerPicker(dialog!!)
    }

    override fun optionSelected(optionText: String?, optionData: Any?) {
        if (optionData is Option){
            when (optionData){
                Option.LEAVE -> {
                    selectedPerson = null
                    selectedSkill = null
                    dialog!!.dismissAsCancel()
                }
                Option.ADD_SKILL -> {
                    addSplicerSkill(selectedPerson!!, dialog!!)
                }
                Option.STORE_SKILL -> {
                    storeSplicerSkill(selectedPerson!!, dialog!!)
                }
                Option.SHOW_SKILLS -> {
                    showSplicerSkillPicker(selectedPerson!!, dialog!!)
                }

            }

        }
    }

    override fun optionMousedOver(optionText: String?, optionData: Any?) {
    }

    override fun advance(amount: Float) {
    }

    override fun backFromEngagement(battleResult: EngagementResultAPI?) {
    }

    override fun getContext(): Any? {
        return null
    }

    override fun getMemoryMap(): MutableMap<String, MemoryAPI>? {
        return null
    }

    open class nsp_officerPickerDialog(var dialog: InteractionDialogAPI): BaseCustomDialogDelegate() {
        open fun getAvailablePersons(): List<PersonAPI>{
            val list = ArrayList<PersonAPI>()
            list.add(Global.getSector().playerPerson)
            list.addAll(Global.getSector().playerFleet.fleetData.officersCopy.map { it.person })
            return list
        }
        override fun createCustomDialog(
            panel: CustomPanelAPI?, callback: CustomDialogDelegate.CustomDialogCallback?
        ) {
            val width = panel!!.position.width
            val height = panel.position.height
            val boxheight = 64f

            val element = panel.createUIElement(width, height, true)
            element.position.inTL(0f, 0f)

            element.addSpacer(5f)
            var prevbutton: ButtonAPI? = null
            var prevIcon: UIPanelAPI? = null
            val list = getAvailablePersons()


            for (officer in list) {
                element.setAreaCheckboxFont(Fonts.ORBITRON_16)
                val img = element.beginImageWithText(officer.portraitSprite, boxheight, width / 1.5f, false)
                img.addTitle(officer.name.fullName)
                img.addPara("level ${officer.stats.level.toString()}", 5f)
                val icon = element.addImageWithText(5f)
                if (prevIcon != null) {
                    icon.position.belowMid(prevIcon, 5f)
                } else {
                    icon.position.inTL(5f, 5f)
                }
                prevIcon = icon
                val box = element.addAreaCheckbox(
                    "Select",
                    officer.id,
                    Misc.getBrightPlayerColor(),
                    Misc.getBrightPlayerColor(),
                    Misc.getBrightPlayerColor(),
                    width - icon.position.width,
                    boxheight,
                    5f
                )
                box.isChecked = false
                if (prevbutton != null) {
                    box.position.belowMid(prevbutton, 5f)
                } else {
                    box.position.inTR(5f, 5f)
                }
                prevbutton = box
                element.addTooltipToPrevious(object : BaseTooltipCreator() {
                    override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, tooltipParam: Any?) {
                        tooltip!!.addSkillPanel(officer, 0f)
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
            dialog.dismissAsCancel()
        }

        override fun customDialogConfirm() {
        }

        override fun getCustomPanelPlugin(): CustomUIPanelPlugin {
            return nsp_splicerOfficerPlugin(dialog)
        }
    }

    class nsp_splicerOfficerPlugin(var dialog: InteractionDialogAPI): CustomUIPanelPlugin {
        override fun positionChanged(position: PositionAPI?) {
        }

        override fun renderBelow(alphaMult: Float) {
        }

        override fun render(alphaMult: Float) {
        }

        override fun advance(amount: Float) {
        }

        override fun processInput(events: MutableList<InputEventAPI>?) {
        }

        override fun buttonPressed(buttonId: Any?) {
            //    dialog.textPanel.addPara(buttonId.toString())
            if (selectedPerson != null && selectedPerson!!.id == buttonId){
                selectedPerson = null
                return
            }
            val player = Global.getSector().playerFleet.fleetData
            if (buttonId == Global.getSector().playerPerson.id){
                selectedPerson = Global.getSector().playerPerson
                return
            }
            else selectedPerson = player.officersCopy.firstOrNull { it.person.id == buttonId }?.person
            if (selectedPerson == null && buttonId is String){
                selectedPerson = Misc.getAICoreOfficerPlugin(buttonId)?.createPerson(buttonId, Factions.PLAYER, Misc.random)
            }
        }
    }
    class nsp_splicerOfficerSkillPlugin(var dialog: InteractionDialogAPI): CustomUIPanelPlugin {
        override fun positionChanged(position: PositionAPI?) {
        }

        override fun renderBelow(alphaMult: Float) {
        }

        override fun render(alphaMult: Float) {
        }

        override fun advance(amount: Float) {
        }

        override fun processInput(events: MutableList<InputEventAPI>?) {
        }

        override fun buttonPressed(buttonId: Any?) {
            if (selectedSkill == buttonId.toString()){
                selectedSkill = null // deselect
                return
            }
            selectedSkill = buttonId.toString()
        }
    }

    companion object{
        val splicerNumUsedKey = "\$nsp_numSplicerSkillsAdded"
        val splicerBonusUsesMod = "nsp_splicerGeneCompatibility"
        val defaultCap = 1
        val splicerCryoLevelPenalty = 8 // subtract level from this to determine number of attempts to add bonus slots
        val splicerCryoBonusProb = 0.30f
        val splicerCryoSpecialSkillProb = 0.25f // x% chance for officer to have random splicer skill, can be good or bad
        val splicerHiddenTag = "nsp_splicerSkill"
        val splicerNegativeTag = "nsp_splicerBad"
        val splicerPositiveTag = "nsp_splicerGood"
        val allSplicerSkills = Global.getSettings().skillIds.filter { Global.getSettings().getSkillSpec(it).hasTag(
            splicerHiddenTag

        ) }
     //   @Transient var selectedGene: cs_genetics.cs_baseGene? = null
        @Transient var selectedSkill: String? = null
        @Transient var selectedPerson: PersonAPI? = null
        fun getCap(officer: PersonAPI): Int{
            val liberal = Global.getSettings().modManager.isModEnabled("lunalib")
            val base = if (liberal) NSP_lunaStuff.getLunaInt("nsp_splicerSkillLimit")
            else defaultCap
            return (base + getOfficerBonusCap(officer)).coerceAtLeast(0)
        }
        fun getOfficerBonusCap(officer: PersonAPI): Int{
            return officer.stats.dynamic.getMod(splicerBonusUsesMod).computeEffective(0f).roundToInt()
        }
        fun getNumUses(officer: PersonAPI): Int{
            return officer.memoryWithoutUpdate.getInt(splicerNumUsedKey)
        }
        fun advanceOneUsage(officer: PersonAPI){
            val curr = getNumUses(officer)
            officer.memoryWithoutUpdate.set(splicerNumUsedKey, curr + 1)
        }
        fun removeOneUsage(officer: PersonAPI){
            val curr = getNumUses(officer)
            officer.memoryWithoutUpdate.set(splicerNumUsedKey, curr - 1)
        }
        fun getCryoBonusSlotProb(): Float{
            val liberal = Global.getSettings().modManager.isModEnabled("lunalib")
            if (liberal) return NSP_lunaStuff.getLunaFloat("nsp_cryoBonusSlotProb")
            else return splicerCryoBonusProb
        }
        fun getCryoBonusSkillProb(): Float{
            val liberal = Global.getSettings().modManager.isModEnabled("lunalib")
            if (liberal) return NSP_lunaStuff.getLunaFloat("nsp_cryoBonusSkillProb")
            else return splicerCryoSpecialSkillProb
        }
    }
}