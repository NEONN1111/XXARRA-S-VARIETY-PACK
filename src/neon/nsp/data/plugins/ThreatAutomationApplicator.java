package neon.nsp.data.plugins;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import java.util.HashSet;
import java.util.Set;

public class ThreatAutomationApplicator implements EveryFrameScript {

    private static final String THREAT_AUTOMATION_HULLMOD = "nsp_threat_automation";
    private static final String THREAT_DESIGN_TYPE = "Threat";

    private final IntervalUtil interval = new IntervalUtil(0.5f, 1.0f);
    private final Set<String> processedShips = new HashSet<>();

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        if (Global.getSector() == null) return;

        interval.advance(amount);
        if (!interval.intervalElapsed()) return;


        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet != null && playerFleet.getFleetData() != null) {
            processFleet(playerFleet);
        }

        for (LocationAPI location : Global.getSector().getAllLocations()) {
            for (SectorEntityToken entity : location.getAllEntities()) {
                if (entity instanceof CampaignFleetAPI) {
                    CampaignFleetAPI fleet = (CampaignFleetAPI) entity;
                    if (fleet != playerFleet && fleet.getFleetData() != null) {
                        processFleet(fleet);
                    }
                }
            }
        }
    }

    private void processFleet(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getFleetData() == null) return;

        FleetDataAPI fleetData = fleet.getFleetData();


        Set<FleetMemberAPI> membersToProcess = new HashSet<>(fleetData.getMembersListCopy());

        for (FleetMemberAPI member : membersToProcess) {
            if (shouldHaveThreatAutomation(member)) {
                applyThreatAutomation(member, fleet);
            }
        }
    }

    private boolean shouldHaveThreatAutomation(FleetMemberAPI member) {
        if (member == null || member.getVariant() == null) return false;

        if (member.getVariant().getHullMods().contains(THREAT_AUTOMATION_HULLMOD)) {
            return false;
        }


        if (member.getHullSpec() != null &&
                member.getHullSpec().getManufacturer() != null &&
                THREAT_DESIGN_TYPE.equals(member.getHullSpec().getManufacturer())) {
            return true;
        }


        if (member.getVariant().hasTag("threat_auto")) {
            return true;
        }

        if (member.getHullSpec() != null && member.getHullSpec().hasTag("threat_auto")) {
            return true;
        }

        return false;
    }

    private void applyThreatAutomation(FleetMemberAPI member, CampaignFleetAPI fleet) {
        if (member == null || member.getVariant() == null) return;

        String shipId = member.getId();


        if (processedShips.contains(shipId)) return;


        if (!member.getVariant().getHullMods().contains(THREAT_AUTOMATION_HULLMOD)) {
            try {
                member.getVariant().addMod(THREAT_AUTOMATION_HULLMOD);


                boolean isPlayerFleet = (fleet == Global.getSector().getPlayerFleet());
                String fleetType = isPlayerFleet ? "player" : "AI";
                Global.getLogger(this.getClass()).info("Applied " + THREAT_AUTOMATION_HULLMOD +
                        " to " + member.getShipName() + " (" + member.getHullSpec().getHullId() +
                        ") in " + fleetType + " fleet");


                processedShips.add(shipId);
            } catch (Exception e) {
                Global.getLogger(this.getClass()).warn("Failed to apply " + THREAT_AUTOMATION_HULLMOD +
                        " to " + member.getShipName() + ": " + e.getMessage());
            }
        }
    }


    public void clearCache() {
        processedShips.clear();
    }
}