package neon.nsp.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class NSP_LashReceiver extends BaseHullMod {

    private static final int BASE_CHARGES_RESTORED = 1;

    private static final Set<String> BLOCKED_SYSTEMS = new HashSet<>();

    static {
        BLOCKED_SYSTEMS.add("energy_lash");
        BLOCKED_SYSTEMS.add("nsp_energy_lash");
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {

        if (index == 0) return "Energy Lash";
        if (index == 1) return BASE_CHARGES_RESTORED + " charge(s)";
        return null;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {

        tooltip.addPara("This hullmod %s on vessels that have the corresponding %s power transmitter system.", 3f, Misc.getNegativeHighlightColor(),
                "cannot be installed", "Energy Lash");
    }

    public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "all";
        return null;
    }

    public boolean isApplicableToShip(ShipAPI ship) {
        for (String idStr : BLOCKED_SYSTEMS) {
            if (Objects.equals(ship.getSystem().getId(), idStr)) {
                return false;
            }
        }
        return true;
    }

    public String getUnapplicableReason(ShipAPI ship) {
        for (String idStr : BLOCKED_SYSTEMS) {
            if (Objects.equals(ship.getSystem().getId(), idStr)) {
                return "Cannot be installed on vessels with an Energy Lash power transmitter.";
            }
        }
        return null;
    }
}
