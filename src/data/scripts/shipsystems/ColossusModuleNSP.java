package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class ColossusModuleNSP extends BaseShipSystemScript {
    public static final boolean USE_SHARD_EVERYFRAME = true;

    public static boolean detachShardsIfNeeded(ShipAPI ship, boolean fromSystemUse) {
        if (!ship.isAlive()) {
            return true;
        }
            CombatEngineAPI engine = Global.getCombatEngine();
            for (ShipAPI module : ship.getChildModulesCopy()) {
                module.setStationSlot(null);
            }
            return true;
        }
    }
