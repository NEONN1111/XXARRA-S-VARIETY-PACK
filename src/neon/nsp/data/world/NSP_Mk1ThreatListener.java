package neon.nsp.data.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;

public class NSP_Mk1ThreatListener implements EconomyTickListener {


    MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
    String threat_hullmod = "threat_hullmod";
    boolean ThreatDetected = false;
    Long ThreatDetectionDate = Global.getSector().getClock().getTimestamp();
    Float timer = 40f; // days
    // to reduce lag
    @Override
    public void reportEconomyTick(int iterIndex) {
        if (Global.getSector().getPlayerFleet() != null){
            CampaignFleetAPI playerfleet = Global.getSector().getPlayerFleet();
            CampaignClockAPI clock = Global.getSector().getClock();
            for (FleetMemberAPI member : playerfleet.getFleetData().getMembersListCopy()) {
                if (member.getVariant().hasHullMod(threat_hullmod)) {
                    if (!ThreatDetected) {
                        ThreatDetected = true;
                        ThreatDetectionDate = clock.getTimestamp();
                    } else {
                        if (clock.getElapsedDaysSince(ThreatDetectionDate) > timer && !Global.getSector().getCampaignUI().isShowingDialog()){
                            Misc.showRuleDialog(playerfleet, "nsp_deepSpaceSignalsPopup");
                            Global.getSector().getListenerManager().removeListener(this);
                            return;
                        }
                    }
                }
            }
        }

    }

    @Override
    public void reportEconomyMonthEnd() {

    }
}
