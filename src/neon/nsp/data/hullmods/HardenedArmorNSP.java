package neon.nsp.data.hullmods;

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

public class HardenedArmorNSP extends BaseHullMod {

	public static float ARMOR_MULT = 1.8f;
	private static final Set<String> BLOCKED_HULLMODS = new HashSet<>();

	static {
		// These hullmods will automatically be removed
		// This prevents unexplained hullmod blocking
		BLOCKED_HULLMODS.add("heavyarmor");
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {

		tooltip.addPara("Increases effective armor strength by %s." ,5f, Color.ORANGE, "80%");

		tooltip.addPara("Due to the nature of this armor type, it is incompatible with %s.", 5f, Misc.getNegativeHighlightColor(),
				getHullmodName("heavyarmor"));
	}
	public String getHullmodName(String id) {
		return Global.getSettings().getHullModSpec(id).getDisplayName();
	}

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getEffectiveArmorBonus().modifyMult(id, ARMOR_MULT);
		stats.getMinArmorFraction().modifyMult(id, ARMOR_MULT);
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








