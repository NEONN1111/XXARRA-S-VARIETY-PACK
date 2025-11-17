package neon.nsp.data.shipsystems.scripts;

import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;

public class DeepStrikeStats extends BaseShipSystemScript {

	private static Map mag = new HashMap();
	static {
		mag.put(ShipAPI.HullSize.FIGHTER, 0.33f);
		mag.put(ShipAPI.HullSize.FRIGATE, 0.33f);
		mag.put(ShipAPI.HullSize.DESTROYER, 0.33f);
		mag.put(ShipAPI.HullSize.CRUISER, 0.5f);
		mag.put(ShipAPI.HullSize.CAPITAL_SHIP, 0.5f);
	}
	public static final float ROF_BONUS = 2f;
	public static final float FLUX_REDUCTION = 50f;
	public static final float MASS_SET = 2f;

	public static final float DAMAGEREDUCTION = 0.5f;

	float massBeforeActivation = 0f;
	boolean masscheck = true;
	private CombatEngineAPI Vector2f;


	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (masscheck) {
			massBeforeActivation = stats.getEntity().getMass();
			masscheck = false;
		}
		if (state == ShipSystemStatsScript.State.OUT) {
			stats.getEntity().setMass(massBeforeActivation);
			stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
		} else {
			stats.getMaxSpeed().modifyFlat(id, 200f * effectLevel);
			stats.getAcceleration().modifyFlat(id, 200f * effectLevel);
			stats.getHullDamageTakenMult().modifyMult(id, 1f - (1f - DAMAGEREDUCTION) * effectLevel);
			stats.getArmorDamageTakenMult().modifyMult(id, 1f - (1f - DAMAGEREDUCTION) * effectLevel);
			stats.getEmpDamageTakenMult().modifyMult(id, 1f - (1f - DAMAGEREDUCTION) * effectLevel);
			stats.getEntity().setMass(massBeforeActivation * (MASS_SET * effectLevel));
			//stats.getAcceleration().modifyPercent(id, 200f * effectLevel);
		}
		float mult = 1f + ROF_BONUS * effectLevel;
		stats.getBallisticRoFMult().modifyMult(id, mult);
		stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1f - (FLUX_REDUCTION * 0.01f));

		float mult2 = 1f + MASS_SET * effectLevel;
		stats.getBallisticRoFMult().modifyMult(id, mult2);
		stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1f - (FLUX_REDUCTION * 0.01f));
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		stats.getHullDamageTakenMult().unmodify(id);
		stats.getArmorDamageTakenMult().unmodify(id);
		stats.getEmpDamageTakenMult().unmodify(id);

	}

	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("increased engine power", false);
		}
		    float mult = 1f + ROF_BONUS * effectLevel;
		    float bonusPercent = (int) ((mult - 1f) * 100f);
		    if (index == 0) {
				return new StatusData("ballistic rate of fire +" + (int) bonusPercent + "%", false);
			}
		return null;
	}
}
