package neon.nsp.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.hullmods.ShieldShunt;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class HardenedArmorNSP extends BaseHullMod {

	public static float ARMOR_MULT = 1.75f;
	private static final Set<String> BLOCKED_HULLMODS = new HashSet<>();

	static {
		// These hullmods will automatically be removed
		// This prevents unexplained hullmod blocking
		BLOCKED_HULLMODS.add("heavyarmor");
		BLOCKED_HULLMODS.add("apex_armor");
		BLOCKED_HULLMODS.add("apex_cryo_armor");
		BLOCKED_HULLMODS.add("CHM_apex");
		BLOCKED_HULLMODS.add("DEX_ablative");
		BLOCKED_HULLMODS.add("TADA_lightArmor");
		BLOCKED_HULLMODS.add("TADA_reactiveArmor");
		BLOCKED_HULLMODS.add("mhmods_hullfoam");
		BLOCKED_HULLMODS.add("mhmods_integratedarmor");
		BLOCKED_HULLMODS.add("ash_front_loaded_armor");
		BLOCKED_HULLMODS.add("specialsphmod_soilnanites_upgrades");
		BLOCKED_HULLMODS.add("BT_antiNuke");

	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {

		tooltip.addPara("Increases effective armor strength by %s.", 5f, Color.ORANGE, "75%");
		tooltip.addSectionHeading("Incompatibilites", Alignment.MID, 5f);
		tooltip.addPara("Due to the nature of this armor type, it is incompatible with %s.", 5f, Misc.getNegativeHighlightColor(),
				getHullmodName("heavyarmor"));
		tooltip.addSectionHeading("Effects On Other Hullmods", Alignment.MID, 5f);
		tooltip.addPara("%s hullmods which add armor will have no effect on the hull's armor.", 5f, Color.ORANGE, "Most");
	}

	public String getHullmodName(String id) {
		return Global.getSettings().getHullModSpec(id).getDisplayName();
	}

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getEffectiveArmorBonus().modifyMult(id, ARMOR_MULT);
		stats.getMinArmorFraction().modifyMult(id, ARMOR_MULT);
		ShipVariantAPI variant = stats.getVariant();
		if (variant == null) return;
		if (!variant.hasHullMod(HullMods.SHIELD_SHUNT)) return;
		boolean sMod = new ShieldShunt().isSMod(stats);
		float armorPenalty = ShieldShunt.ARMOR_BONUS;
		if (sMod) armorPenalty += ShieldShunt.SMOD_ARMOR_BONUS;
		stats.getArmorBonus().modifyPercent(id, -armorPenalty);

	}

	public void applyEffectsAfterShipCreation(ShipAPI ship, String id, MutableShipStatsAPI stats) {
		for (String tmp : BLOCKED_HULLMODS) {
			if (ship.getVariant().getHullMods().contains(tmp)) {
				ship.getVariant().removeMod(tmp);
				String ERROR = "nsp_incompatible";
				ship.getVariant().addMod(ERROR);
			}
		}

	}

	public void advanceInCombat(ShipAPI ship, MutableShipStatsAPI stats, float amount) {

	}
	public void advanceInCampaign(FleetMemberAPI member, MutableShipStatsAPI stats, float amount) {
		stats.getArmorBonus().unmodify(HullMods.SHIELD_SHUNT);
		stats.getArmorBonus().unmodify(HullMods.ARMOREDWEAPONS);
	}

}








