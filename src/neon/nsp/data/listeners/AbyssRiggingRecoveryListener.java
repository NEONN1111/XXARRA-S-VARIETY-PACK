package neon.nsp.data.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.listeners.ShipRecoveryListener;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.campaign.fleet.CampaignFleet;

import java.util.List;
import java.util.Objects;

public class AbyssRiggingRecoveryListener implements ShipRecoveryListener {

    ShipHullSpecAPI FABRICATOR_PLAYER_HULLSPEC = Global.getSettings().getHullSpec("nsp_fabricator_unit_recovered");

    @Override
    public void reportShipsRecovered(List<FleetMemberAPI> ships, InteractionDialogAPI dialog) {
        if (ships == null) return;
        if (Global.getSector().getPlayerStats().hasSkill("nsp_threat_auto")) {
            for (FleetMemberAPI ship : ships) {
                if (Objects.equals(ship.getHullId(), "fabricator_unit")) {
                    CampaignFleet pf = (CampaignFleet) Global.getSector().getPlayerFleet();
                    ShipVariantAPI v = ship.getVariant().clone();
                    v.setHullSpecAPI(FABRICATOR_PLAYER_HULLSPEC);
                    pf.removeFleetMember(ship);
                    FleetMemberAPI repl = Global.getFactory().createFleetMember(FleetMemberType.SHIP, v);
                    repl.setShipName(ship.getShipName());
                    repl.setId(ship.getId());
                    repl.getRepairTracker().setCR(ship.getRepairTracker().getCR());
                    repl.getStatus().setHullFraction(ship.getStatus().getHullFraction());
                    pf.addFleetMember(repl);
                }
            }
        }
    }
}
