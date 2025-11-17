package neon.nsp.data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Misc;
import neon.nsp.data.plugins.NSP_InvictaCore;

import java.util.Random;


public class InvictaAssignment extends BaseHullMod {
    public void advanceInCampaign(FleetMemberAPI member, float amount) {

        if (member == null || Global.getSector().getPlayerFleet() == null || Global.getSector().getPlayerPerson() == null) {
            return;
        }
        // captain can be either the "default" personAPI created to fill the slot (the blank portrait is actually a dummy person, the captain isn't null)
        // or can be the player, in which case we don't replace them
        // or can be something else, in which case we set the captain to invicta and set them as unremovable
        // when we create invicta, set them as unremovable to avoid issues with ai core duplication
        // the original SOTF hullmod works fine without this jank because sierra is (probably?) a person under the sector's important people and not an AI core
        if (member.getCaptain() == null ||  member.getCaptain().isDefault() || (!member.getCaptain().getId().equals("nsp_invicta") && !member.getCaptain().isPlayer())){
            PersonAPI invicta = Global.getSector().getImportantPeople().getPerson("nsp_invicta");
            if (invicta != null) {
                member.setCaptain(invicta);
                Misc.setUnremovable(invicta, true); // technically makes it not possible for player to be assigned to ship in campaign, but neural link still works
            } else {
                invicta = new NSP_InvictaCore().createPerson("nsp_invicta_core",Factions.DERELICT,new Random());
                member.setCaptain(invicta);
                Misc.setUnremovable(invicta, true);
            }


        }
    }

    public void advanceInCombat(ShipAPI ship, float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (!Global.getCurrentState().equals(GameState.COMBAT)) {
            return;
        }
        if (engine.isInCampaign() || engine.isInCampaignSim()) {
            // force Sierra as the captain, even if player is piloting
            //
            if (ship.isAlive() && !ship.getCaptain().getId().equals("nsp_invicta_core")) {
                PersonAPI invicta = Global.getSector().getImportantPeople().getPerson("nsp_invicta");
                if (invicta != null) {
                    ship.setCaptain(invicta);
                }
            }
        }
    }

}
