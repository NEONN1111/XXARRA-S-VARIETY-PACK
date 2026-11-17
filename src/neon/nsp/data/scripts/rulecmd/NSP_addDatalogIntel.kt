package neon.nsp.data.scripts.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import neon.nsp.data.campaign.intel.NSP_baseDatalogIntel

class NSP_addDatalogIntel: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        val name = dialog!!.interactionTarget.name
        val desc = params!![0].getString(memoryMap)
        val buttonID = params[1].getString(memoryMap)
        val intel = NSP_baseDatalogIntel(name, desc, buttonID)
        Global.getSector().intelManager.addIntel(intel, false, dialog.textPanel)
        return true
    }
}