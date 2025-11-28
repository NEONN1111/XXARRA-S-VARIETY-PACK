package neon.nsp.data.scripts.campaign.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseAssignmentAI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

/**
 * Exponent Mission - Custom assignment script for the Luddic Church escort fleet
 */
public class ExponentMissionLCFleetEscort extends BaseAssignmentAI implements FleetEventListener {

    protected StarSystemAPI system;

    public static final float ESCORT_DURATION = 999f;
    public static final float ORDER_DURATION = 10f;

    protected float escortTimeLeft;

    protected float jumpCooldown = 2f;


    public ExponentMissionLCFleetEscort(CampaignFleetAPI fleet) {
        super();
        this.fleet = fleet;
        this.system = system;
        
        escortTimeLeft = ESCORT_DURATION;
        fleet.addEventListener(this);
        giveInitialAssignments();
    }

    public void advance(float amount) {
        if (Global.getSector().isPaused()) return;
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();

        jumpCooldown -= Global.getSector().getClock().convertToDays(amount);


        if (playerFleet != null) {

            // Jump to player (in-system > hyperspace)
            if ( (!playerFleet.isInHyperspace() && playerFleet.isInHyperspaceTransition() && fleet.getContainingLocation() == playerFleet.getContainingLocation()) ||
                    (!playerFleet.isInHyperspace() && playerFleet.hasAbility(Abilities.TRANSVERSE_JUMP) && playerFleet.getAbility(Abilities.TRANSVERSE_JUMP).isActiveOrInProgress())
                            && jumpCooldown < 0 && !fleet.isInHyperspaceTransition()) {

                fleet.getAbility(Abilities.TRANSVERSE_JUMP).activate();

                jumpCooldown = 2f;

            }
            // Jump to player (hyperspace > in-system)
            else if (fleet.isInHyperspace() && !playerFleet.isInHyperspace() && !fleet.isInHyperspaceTransition() && jumpCooldown < 0) {

                Vector2f loc  = Misc.getPointAtRadius(playerFleet.getLocation(), 300f + fleet.getRadius());
                SectorEntityToken token = playerFleet.getContainingLocation().createToken(loc.x, loc.y);
                JumpPointAPI.JumpDestination dest = new JumpPointAPI.JumpDestination(token, null);
                Global.getSector().doHyperspaceTransition(fleet, null, dest);

                jumpCooldown = 2f;
            }
            // otherwise follow them around
            else {
                fleet.clearAssignments();
                fleet.addAssignmentAtStart(FleetAssignment.ORBIT_AGGRESSIVE, Global.getSector().getPlayerFleet(), ORDER_DURATION, "escorting your fleet", null);
            }

        }

    }

    @Override
    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {

    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {

    }

    @Override
    protected void giveInitialAssignments() {
        fleet.addAssignmentAtStart(FleetAssignment.ORBIT_AGGRESSIVE, Global.getSector().getPlayerFleet(), ORDER_DURATION, "escorting your fleet", null);
    }

    @Override
    protected void pickNext() {
        fleet.clearAssignments();
        Misc.giveStandardReturnToSourceAssignments(fleet);
        fleet.removeScript(this);
    }
}
