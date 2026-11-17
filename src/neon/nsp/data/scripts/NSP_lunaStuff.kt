package neon.nsp.data.scripts

import com.fs.starfarer.api.Global
//import data.crabshack.intel.stelnetBoard.cs_stelnetBoard
import lunalib.lunaSettings.LunaSettings
import lunalib.lunaSettings.LunaSettingsListener


object NSP_lunaStuff {

    class nsp_lunaSettingsListener: LunaSettingsListener {
        override fun settingsChanged(modID: String) {
            if (modID == "NSP"){
                val cData = Global.getSector().characterData
              //  if (getLunaBool("nsp_experimentalMods") && cData != null && !cData.abilities.contains("cs_disassemble")) {
                    Global.getSector().characterData.addAbility("cs_disassemble")
                    Global.getSettings().allHullModSpecs.filter { it.hasTag("cs_experimental") }.forEach {
                        Global.getSector().characterData.addHullMod(it.id)
                    }
             //   }
               // cs_stelnetBoard.CURR_TOGGLE_KEY = cs_stelnetBoard.getToggleKey() // very minor performance opt to only check keybind once and then update it here instead
            }
        }
    }
    fun attachSettingsListener(): Boolean{
        if (LunaSettings.hasSettingsListenerOfClass(nsp_lunaSettingsListener::class.java)){
            return false
        }
        LunaSettings.addSettingsListener(nsp_lunaSettingsListener())
        return true
    }

    fun getLunaInt(id: String): Int {
      return  LunaSettings.getInt("NSP", id)!!
    }
    fun getLunaBool(id: String): Boolean{
        return LunaSettings.getBoolean("NSP", id)!!
    }
    fun getLunaFloat(id: String): Float{
        return LunaSettings.getFloat("NSP", id)!!
    }
}