package neon.nsp.data.hullmods;

import java.awt.Color;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class NSP_EnergyBoltCoherer extends BaseHullMod {

	public static float RANGE_BONUS = 500;
	public static float CREWED_RANGE_BONUS = 100;

	public static float CREW_CASUALTIES = 50;

	public static float RANGE_THRESHOLD = 750f;

	public static final float COST_REDUCTION_L = 10;
	public static final float COST_REDUCTION_M = 5;

	public static float ENERGY_WEAPON_FLUX_DECREASE = -10f;

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		if (!Misc.isAutomated(stats)) {
			stats.getCrewLossMult().modifyPercent(id, CREW_CASUALTIES);
		}
		stats.getDynamic().getMod(Stats.LARGE_ENERGY_MOD).modifyFlat(id, -COST_REDUCTION_L);
		stats.getDynamic().getMod(Stats.MEDIUM_ENERGY_MOD).modifyFlat(id, -COST_REDUCTION_M);
		stats.getEnergyWeaponFluxCostMod().modifyPercent(id, ENERGY_WEAPON_FLUX_DECREASE);
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ship.addListener(new EnergyBoltCohererRangeModifier());
	}

	public static class EnergyBoltCohererRangeModifier implements WeaponBaseRangeModifier {
		public EnergyBoltCohererRangeModifier() {
		}

		public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
			return 0;
		}
		public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
			return 1f;
		}
		public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
			if (weapon.isBeam()) return 0f;
			if (weapon.getType() == WeaponType.ENERGY || weapon.getType() == WeaponType.HYBRID) {
				if (Misc.isAutomated(ship)) {
					return RANGE_BONUS;
				} else {
					return CREWED_RANGE_BONUS;
				}
			}
			return 0f;
		}
	}

	public static class ScintillationRangeMod implements WeaponBaseRangeModifier {
		public ScintillationRangeMod() {
		}
		public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
			return 0;
		}
		public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
			return 1f;
		}
		public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
			if (weapon.isBeam() || weapon.getType() == WeaponAPI.WeaponType.BALLISTIC) {
				float range = weapon.getSpec().getMaxRange();
				if (range < RANGE_THRESHOLD) return 0;

				float offset = range - RANGE_THRESHOLD;
				return -offset;
			}
			return 0f;
		}
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
		return null;
	}

	@Override
	public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}

	@Override
	public boolean affectsOPCosts() {
		return true;
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float pad = 3f;
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		Color bad = Misc.getNegativeHighlightColor();


		if (!Misc.isAutomated(ship)) {
			tooltip.addPara("Originally designed by the Tri-Tachyon Corporation for use on its combat droneships, "
					+ "the coherence field strength has to be dialed down to allow operation on crewed vessels.", opad);
			tooltip.addPara("Increases the base range of all non-beam Energy and Hybrid weapons by %s.", opad, h,
					"" + (int)CREWED_RANGE_BONUS);
			tooltip.addPara("The coherence field is unstable under combat conditions, with stresses on the hull "
					+ "resulting in spot failures that release bursts of lethal radiation. "
					+ "Crew casualties in combat are increased by %s.", opad, h,
					"" + (int) CREW_CASUALTIES + "%");
		} else {
			tooltip.addPara("Increases the base range of all non-beam Energy and Hybrid weapons by %s.", opad, h,
				"" + (int)RANGE_BONUS);
		}

		tooltip.addSectionHeading("Interactions with other modifiers", Alignment.MID, opad);
		tooltip.addPara("Since the base range is increased, this range modifier"
				+ " - unlike most other flat modifiers in the game - "
				+ "is increased by percentage modifiers from other hullmods and skills.", opad);

		tooltip.addSectionHeading("Energy integration grid", Alignment.MID, opad);
		tooltip.addPara("Large and Medium energy weapon Ordnance Point costs are reduced by %s and %s, respectively.", opad, Color.ORANGE, "10", "5");

		tooltip.addPara("The base range of %s weapons and %s weapons before modifiers cannot exceed %s.", opad, Color.ORANGE, "ballistic", "beam", "750u");

		tooltip.addPara("Energy weapon flux generation is reduced by %s.", opad, Color.ORANGE, "10%");
	}


	
//	@Override
//	public boolean isApplicableToShip(ShipAPI ship) {
//		return getUnapplicableReason(ship) == null;
//	}
//	
//	public String getUnapplicableReason(ShipAPI ship) {
//		if (ship != null && 
//				ship.getHullSize() != HullSize.CAPITAL_SHIP && 
//				ship.getHullSize() != HullSize.DESTROYER && 
//				ship.getHullSize() != HullSize.CRUISER) {
//			return "Can only be installed on destroyer-class hulls and larger";
//		}
//		return null;
//	}
	
}









