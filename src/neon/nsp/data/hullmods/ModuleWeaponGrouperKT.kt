package neon.nsp.data.hullmods

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.combat.systems.WeaponGroup
import neon.nsp.data.scripts.util.NSP_ReflectionUtilsT

class ModuleWeaponGrouperKT: BaseHullMod() {


    override fun applyEffectsAfterShipAddedToCombatEngine(ship: ShipAPI?, id: String?) {
        /*
        uses reflection to create a new group and adds the child's groups to that
        then adds the final group to the parent
        you could also just add the child's groups directly to the parent, but you need to make sure they're removed at some point
        weapon groups on multiple ships will advance several times as fast (they do stuff faster)
        note that adding an 8th group will cause the ui to break
         */
        val new = NSP_ReflectionUtilsT.instantiate(WeaponGroup::class.java) as WeaponGroupAPI
        ship!!.childModulesCopy.forEach { child ->
            val groups = child.weaponGroupsCopy
            groups.forEach {
                for (weapon in it.weaponsCopy){
                    new.addWeaponAPI(weapon)
                    child.removeWeaponFromGroups(weapon)
                }
            }
        }
        NSP_ReflectionUtilsT.invoke("addGroup", ship, new)
    }
}