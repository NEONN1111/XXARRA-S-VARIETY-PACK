package data.shipsystems;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import data.scripts.util.MagicFakeBeam;
import org.lwjgl.util.vector.Vector2f;

public class DeepStrikeRampager extends BaseShipSystemScript {

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
	public static final float MAX_TIME_MULT = 3f;
	public static final Color JITTER_COLOR = new Color(255, 100, 43, 40);
	public static final Color JITTER_UNDER_COLOR = new Color(255, 40, 79, 100);

	public static final float DAMAGEREDUCTION = 0.5f;

	float massBeforeActivation = 0f;
	boolean masscheck = true;
	private CombatEngineAPI Vector2f;


	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;
		boolean player = false;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
			player = ship == Global.getCombatEngine().getPlayerShip();
			id = id + "_" + ship.getId();
		} else {
			return;
		}
		float jitterLevel = effectLevel;
		float jitterRangeBonus = 0;
		float maxRangeBonus = 10f;
		if (state == State.IN) {
			jitterLevel = effectLevel / (1f / ship.getSystem().getChargeUpDur());
			if (jitterLevel > 1) {
				jitterLevel = 1f;
			}
			jitterRangeBonus = jitterLevel * maxRangeBonus;
		} else if (state == State.ACTIVE) {
			jitterLevel = 1f;
			jitterRangeBonus = maxRangeBonus;
		} else if (state == State.OUT) {
			jitterRangeBonus = jitterLevel * maxRangeBonus;
		}
		jitterLevel = (float) Math.sqrt(jitterLevel);
		effectLevel *= effectLevel;

		ship.setJitter(this, JITTER_COLOR, jitterLevel, 3, 0, 0 + jitterRangeBonus);
		ship.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, 25, 0f, 7f + jitterRangeBonus);
		float shipTimeMult = 1f + (MAX_TIME_MULT - 1f) * effectLevel;
		stats.getTimeMult().modifyMult(id, shipTimeMult);
		if (player) {
			Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / shipTimeMult);
//			if (ship.areAnyEnemiesInRange()) {
//				Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / shipTimeMult);
//			} else {
//				Global.getCombatEngine().getTimeMult().modifyMult(id, 2f / shipTimeMult);
//			}
		} else {
			Global.getCombatEngine().getTimeMult().unmodify(id);
		}
		ship.getEngineController().fadeToOtherColor(this, JITTER_COLOR, new Color(0,0,0,0), effectLevel, 0.5f);
		ship.getEngineController().extendFlame(this, -0.25f, -0.25f, -0.25f);


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
		Global.getCombatEngine().getTimeMult().unmodify(id);
		stats.getTimeMult().unmodify(id);

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
