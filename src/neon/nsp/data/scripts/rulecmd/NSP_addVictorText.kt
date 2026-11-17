package neon.nsp.data.scripts.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc

class NSP_addVictorText: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        val string = params!![0].getString(memoryMap) ?: return false
        dialog!!.textPanel.setFontVictor()
        dialog.textPanel.addPara(string)
        dialog.textPanel.setFontInsignia()

        return true

    }
}