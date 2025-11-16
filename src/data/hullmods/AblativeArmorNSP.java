package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class AblativeArmorNSP extends BaseHullMod {

	public static float ARMOR_MULT = 0.3f;
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getEffectiveArmorBonus().modifyMult(id, ARMOR_MULT);
		stats.getMinArmorFraction().modifyMult(id, ARMOR_MULT);
	}
	
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) Math.round(ARMOR_MULT * 100f) + "%";
		return null;
	}


}








