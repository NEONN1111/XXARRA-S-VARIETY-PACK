package data.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;

public class nsp_onslaughtMK1Listener implements EconomyTickListener {

    // i don't know if there's a way to listen for a ship being recovered, so i did this
    // it's not fantastic, but it works and doesn't crash the game, so...
    // pretty big W
    MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
    String onslaughtHullID = "onslaught_mk1";
    boolean onslaughtDetected = false;
    Long onslaughtDetectionDate = Global.getSector().getClock().getTimestamp();
    Float timer = 40f; // days
    // to reduce lag
    @Override
    public void reportEconomyTick(int iterIndex) {
        if (Global.getSector().getPlayerFleet() != null){
            if (memory.getBoolean("$nsp_foundDominatorMK1") && memory.getBoolean("$nsp_foundLegionMK1")){ // if we found both already, then who cares
                Global.getSector().getListenerManager().removeListener(this);
                return;
            }
            CampaignFleetAPI playerfleet = Global.getSector().getPlayerFleet();
            CampaignClockAPI clock = Global.getSector().getClock();
            for (FleetMemberAPI member : playerfleet.getFleetData().getMembersListCopy()) {
                if (member.getHullSpec().getBaseHullId().equals(onslaughtHullID)) {
                    if (!onslaughtDetected) {
                        onslaughtDetected = true;
                        onslaughtDetectionDate = clock.getTimestamp();
                    } else {
                        if (clock.getElapsedDaysSince(onslaughtDetectionDate) > timer && !Global.getSector().getCampaignUI().isShowingDialog()){
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
