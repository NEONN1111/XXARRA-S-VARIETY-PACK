package neon.nsp.data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseAssignmentAI;

public class NSP_ChauffeurAI extends BaseAssignmentAI {
    private CampaignFleetAPI fleet;
    private MarketAPI market;

    public NSP_ChauffeurAI(CampaignFleetAPI fleet, MarketAPI market) {
        this.fleet = fleet;
        this.market = market;
    }

    @Override
    public void giveInitialAssignments() {
        fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, Global.getSector().getPlayerFleet(), 120f);
    }

    @Override
    public void pickNext() {
        if (fleet.getContainingLocation() != null &&
                fleet.getContainingLocation().getEntityById(market.getPrimaryEntity().getId()) != null) {
            fleet.removeFirstAssignment();
            fleet.addAssignment(FleetAssignment.ATTACK_LOCATION, market.getPrimaryEntity(), 30f);
        } else if (Global.getSector().getPlayerFleet() != null &&
                fleet.getCurrentAssignment() != null &&
                fleet.getCurrentAssignment().getAssignment() != FleetAssignment.ORBIT_PASSIVE) {
            fleet.removeFirstAssignment();
            fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, Global.getSector().getPlayerFleet(), 30f);
        }
    }

    // Getter methods
    public CampaignFleetAPI getFleet() {
        return fleet;
    }

    public MarketAPI getMarket() {
        return market;
    }
}