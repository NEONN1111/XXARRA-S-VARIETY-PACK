//package neon.nsp.data.scripts;
//
//import com.fs.starfarer.api.Global;
//import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
//import com.fs.starfarer.api.fleet.FleetMemberAPI;
//import com.fs.starfarer.api.input.InputEventAPI;
//import com.fs.starfarer.api.util.Misc;
//import java.util.List;
//
//public class NSP_DerelictCombatPlugin extends BaseEveryFrameCombatPlugin {
//    private final float max = 20f;
//
//    @Override
//    public void advance(float amount, List<InputEventAPI> events) {
//        if (!Global.getSector().getMemoryWithoutUpdate().getBoolean("$nsp_MotherShipInteract")) {
//            return;
//        }
//
//        var playerfm = Global.getCombatEngine().getFleetManager(0);
//        var pofficer = Global.getSector().getPlayerFleet().getFleetData().getOfficersCopy();
//        if (pofficer == null || !pofficer.isEmpty()) {
//            return;
//        }
//
//        float bonus = 0f;
//        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
//            if (!member.getCaptain().isDefault() &&
//                    member.getCaptain().isAICore() &&
//                    Misc.isUnremovable(member.getCaptain())) {
//                bonus = Math.min(bonus + (member.getCaptain().getStats().getLevel() / 3f), max);
//            }
//        }
//
//        playerfm.modifyPercentMax("playercores", bonus);
//
//        super.advance(amount, events);
//    }
//}