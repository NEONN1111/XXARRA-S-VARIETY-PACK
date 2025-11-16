package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Misc;

public class cutcrew extends BaseHullMod {

	public String getUnapplicableReason(ShipAPI ship) {
		return null;
		//return "Incompatible with Dedicated Targeting Core";
	}

	public static final float MAX_CREW_MULT = 0.25f;

	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		float crewMult = MAX_CREW_MULT;
		stats.getMinCrewMod().modifyMult(id, crewMult);
	}

	public String getDescriptionParam(int index, HullSize hullSize) {

		if (index == 0) return Misc.getRoundedValue(((MAX_CREW_MULT + 1f) - (MAX_CREW_MULT * 2)) * 100f) + "%";
		return null;
	}


}
