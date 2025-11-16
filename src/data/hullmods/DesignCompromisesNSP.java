package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class DesignCompromisesNSP extends BaseHullMod {

	public static float RANGE_MULT = 0.8f;
	public static float FLUX_MULT = 0.75f;
	public static float ENERGY_WEAPON_FLUX_INCREASE = 15f;
	public static float MISSILE_ROF_MULT = 0.5f;
	public static float BALLISTIC_RANGE_MULT = 0.85f;
	public static final float COST_INCREASE  = 10;
	public static final float COST_REDUCTION  = 7;


	@Override
	public boolean affectsOPCosts() {
		return true;
	}
		public void applyEffectsBeforeShipCreation (HullSize hullSize, MutableShipStatsAPI stats, String id){
			stats.getBallisticWeaponRangeBonus().modifyMult(id, BALLISTIC_RANGE_MULT);
			stats.getMissileRoFMult().modifyMult(id, MISSILE_ROF_MULT);
			stats.getEnergyWeaponFluxCostMod().modifyPercent(id, ENERGY_WEAPON_FLUX_INCREASE);
			stats.getDynamic().getMod(Stats.LARGE_MISSILE_MOD).modifyFlat(id, COST_INCREASE);
			stats.getDynamic().getMod(Stats.LARGE_ENERGY_MOD).modifyFlat(id, -COST_REDUCTION);

			stats.getFluxDissipation().modifyMult(id, FLUX_MULT);
			stats.getFluxCapacity().modifyMult(id, FLUX_MULT);
			stats.getSystemFluxCostBonus().modifyMult(id, FLUX_MULT);
		}


		public String getDescriptionParam ( int index, HullSize hullSize){
			if (index == 0) return "" + (int) Math.round((1f - FLUX_MULT) * 100f) + "%";
			if (index == 1) return "" + (int) Math.round((1f - BALLISTIC_RANGE_MULT) * 100f) + "%";
			if (index == 2) return "" + (int) Math.round((1f - MISSILE_ROF_MULT) * 100f) + "%";
			if (index == 3) return "" + (int) Math.round(ENERGY_WEAPON_FLUX_INCREASE) + "%";
			if (index == 4) return "" + (int)Math.round(COST_INCREASE) + "";
			if (index == 5) return "" + (int)Math.round(COST_REDUCTION) + "";

			return null;
		}
}









