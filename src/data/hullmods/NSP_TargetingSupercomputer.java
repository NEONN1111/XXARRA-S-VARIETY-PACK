package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class NSP_TargetingSupercomputer extends BaseHullMod {

	public static final float RECOIL_BONUS = 50f;
	public static final float PROJ_SPEED_BONUS = 100f;
	public static final float MISSILE_SPEED_BONUS = 15f;
	public static final float MISSILE_TURN_RATE = 50f;
	
	public static float RANGE_BONUS = 150f;
	public static float VISION_BONUS = 2000f;
	public static float AUTOFIRE_AIM = 0.5f;
	private static final Set<String> BLOCKED_HULLMODS = new HashSet<>();

	static {
		// These hullmods will automatically be removed
		// This prevents unexplained hullmod blocking
		BLOCKED_HULLMODS.add("targetingunit");
		BLOCKED_HULLMODS.add("dedicated_targeting_core");
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		tooltip.addPara("Incompatible with %s and %s", 3f, Misc.getNegativeHighlightColor(),
	            getHullmodName("targetingunit"), getHullmodName("dedicated_targeting_core"));
		tooltip.addPara("Extends the range of all weapons by %s.", 5f, Color.ORANGE, "150%");
	}
	public String getHullmodName(String id) {
		return Global.getSettings().getHullModSpec(id).getDisplayName();
	}



	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
//		if (stats.getFleetMember() != null) {
//			System.out.println("FData [" + stats.getVariant().getHullVariantId() + "]: " + stats.getFleetMember().getFleetData());
//		}
		
		stats.getBallisticWeaponRangeBonus().modifyPercent(id, RANGE_BONUS);
		stats.getEnergyWeaponRangeBonus().modifyPercent(id, RANGE_BONUS);
		//stats.getMissileMaxSpeedBonus().modifyPercent(id, MISSILE_SPEED_BONUS);
		//stats.getMissileMaxTurnRateBonus().modifyPercent(id, MISSILE_TURN_RATE);
		//stats.getMissileGuidance().modifyPercent(id, MISSILE_TURN_RATE);

		
		
		stats.getSightRadiusMod().modifyFlat(id, VISION_BONUS);
		stats.getAutofireAimAccuracy().modifyFlat(id, AUTOFIRE_AIM);
		
		
		
		
		stats.getMaxRecoilMult().modifyMult(id, 1f - (0.01f * RECOIL_BONUS));
		stats.getRecoilPerShotMult().modifyMult(id, 1f - (0.01f * RECOIL_BONUS));
		//stats.getProjectileSpeedMult().modifyPercent(id, PROJ_SPEED_BONUS);
	}
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		for (String tmp : BLOCKED_HULLMODS) {
			if (ship.getVariant().getHullMods().contains(tmp)) {
				ship.getVariant().removeMod(tmp);
				String ERROR = "nsp_incompatible";
				ship.getVariant().addMod(ERROR);
			}
		}
	}


	
}
