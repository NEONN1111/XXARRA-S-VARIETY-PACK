package neon.nsp.data.hullmods;

import java.awt.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.combat.listeners.WeaponOPCostModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import neon.nsp.data.scripts.util.NSP_Tags;
import org.lwjgl.util.vector.Vector2f;

public class NSP_Threat_Automation extends BaseHullMod {

	public static float MAX_CR_PENALTY = 1f;
	public static String THREAT_AUTOMATION_SKILL_ID = "nsp_threat_auto";

	// Vanilla Automated hullmod ID
	public static final String VANILLA_AUTOMATED_HULLMOD = "automated";

	// Core-specific CR penalties (these are ADDITIONAL to the base penalty)
	public static float GAMMA_CORE_CR_PENALTY = 0.15f;  // 15% additional penalty
	public static float BETA_CORE_CR_PENALTY = 0.30f;   // 30% additional penalty
	public static float ALPHA_CORE_CR_PENALTY = 0.50f;  // 50% additional penalty
	public static float THREAT_PROCESSOR_CR_PENALTY = 0f;  // 0% penalty (treated like no core)



	// Map of weapon IDs to their flat OP reduction values
	private static final Map<String, Integer> WEAPON_OP_REDUCTION = new HashMap<>();
	static {
		WEAPON_OP_REDUCTION.put("swarm_launcher", 10);
		WEAPON_OP_REDUCTION.put("seeker_fragment", 3);
		WEAPON_OP_REDUCTION.put("kinetic_fragments", 3);
		WEAPON_OP_REDUCTION.put("unstable_fragment", 3);
		WEAPON_OP_REDUCTION.put("devouring_swarm", 2);
		WEAPON_OP_REDUCTION.put("voltaic_discharge", 1);
		WEAPON_OP_REDUCTION.put("neutron_torpedo", 2);
		WEAPON_OP_REDUCTION.put("light_mass_driver", 2);
		WEAPON_OP_REDUCTION.put("heavy_mass_driver", 3);
		WEAPON_OP_REDUCTION.put("neoferric_quadcoil", 5);
		WEAPON_OP_REDUCTION.put("voltaic_cannon", 3);
		WEAPON_OP_REDUCTION.put("voidblaster", 2);
		WEAPON_OP_REDUCTION.put("nsp_mk9_threat", 8);
		WEAPON_OP_REDUCTION.put("nsp_threatbore", 8);
	}
	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {


		tooltip.addPara("Threat Automated ships suffer a maximum combat readiness penalty of %s. ", 5f, Misc.getNegativeHighlightColor(), "-100%");

		tooltip.addPara("This penalty can be offset by a fleet commander with the %s skill, up to a maximum of %s.", 5f, Color.ORANGE, "Abyss-Rigging", "80%");

		tooltip.addPara("They draw from a %s pool of points to that of standard automated hulls.", 5f, Color.ORANGE, "seperate");

		tooltip.addPara("Owing to their strange design, these hulls are nearly incompatible with most %s AI Cores.", 5f, Color.ORANGE, "standard");

	}

	// Track if we've already removed vanilla hullmod to avoid repeated attempts
	private static final Set<String> VANILLA_REMOVED_SHIPS = new HashSet<>();

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getMinCrewMod().modifyMult(id, 0);
		stats.getMaxCrewMod().modifyMult(id, 0);

		// Get a unique identifier for this specific ship
		String shipId = "";
		FleetMemberAPI member = stats.getFleetMember();
		if (member != null && member.getId() != null) {
			shipId = member.getId();
		} else {
			shipId = id;
		}

		// Use ship-specific modifier keys
		String baseKey = id + "_" + shipId + "_base";
		String coreKey = id + "_" + shipId + "_core";

		// Clear existing modifiers for THIS SHIP only
		stats.getMaxCombatReadiness().unmodifyFlat(baseKey);
		stats.getMaxCombatReadiness().unmodifyFlat(coreKey);

		// Check what core is installed (no special treatment for any core)
		String installedCoreId = null;
		if (member != null) {
			PersonAPI captain = member.getCaptain();
			if (captain != null && !captain.isDefault() && captain.isAICore()) {
				installedCoreId = captain.getAICoreId();
			}
		}

		// Remove the vanilla Automated hullmod if it exists on this ship
		// ONLY do this once per ship to avoid repeated modifications
		if (stats.getVariant() != null && !VANILLA_REMOVED_SHIPS.contains(shipId)) {
			boolean removed = false;

			// Check and remove from regular hullmods - create a copy to avoid CME
			List<String> hullmodsCopy = new ArrayList<>(stats.getVariant().getHullMods());
			for (String mod : hullmodsCopy) {
				if (VANILLA_AUTOMATED_HULLMOD.equals(mod)) {
					stats.getVariant().getHullMods().remove(mod);
					removed = true;
				}
			}

			// Check and remove from built-in hullmods
			if (stats.getVariant().getPermaMods() != null) {
				Set<String> permaCopy = new HashSet<>(stats.getVariant().getPermaMods());
				for (String mod : permaCopy) {
					if (VANILLA_AUTOMATED_HULLMOD.equals(mod)) {
						stats.getVariant().getPermaMods().remove(mod);
						removed = true;
					}
				}
			}

			if (removed && member != null) {
				Global.getLogger(this.getClass()).info("Removed vanilla Automated hullmod from Threat ship: " +
						member.getShipName());
			}

			// Mark as processed to never attempt removal again
			VANILLA_REMOVED_SHIPS.add(shipId);

			// Add tags (these are safe - no iteration happening)
			stats.getVariant().addTag(Tags.AUTOMATED);
			stats.getVariant().addTag(NSP_Tags.THREAT_AUTOMATED);
			stats.getVariant().addTag(NSP_Tags.THREAT_RECOVERABLE);
		}

		// Apply CR penalties to Threat Automated ships in player fleet
		if (isInPlayerFleet(stats)) {

			// Always apply base penalty to all Threat Automated ships
			stats.getMaxCombatReadiness().modifyFlat(baseKey, -MAX_CR_PENALTY, "Threat Automated ship penalty");

			// Additional penalties based on AI core type (all cores treated uniformly)
			if (installedCoreId != null) {
				float corePenalty = 0f;
				String coreName = "";

				if ("gamma_core".equals(installedCoreId)) {
					corePenalty = GAMMA_CORE_CR_PENALTY;
					coreName = "Gamma Core";
				} else if ("beta_core".equals(installedCoreId)) {
					corePenalty = BETA_CORE_CR_PENALTY;
					coreName = "Beta Core";
				} else if ("alpha_core".equals(installedCoreId)) {
					corePenalty = ALPHA_CORE_CR_PENALTY;
					coreName = "Alpha Core";
				} else if ("nsp_threat_processor".equals(installedCoreId)) {
					corePenalty = THREAT_PROCESSOR_CR_PENALTY;
					coreName = "Threat Processor";
				}

				if (corePenalty > 0f) {
					stats.getMaxCombatReadiness().modifyFlat(coreKey, -corePenalty,
							coreName + " penalty");
				}
			}
		}

		// Control recoverability by managing the UNBOARDABLE tag
		if (stats.getVariant() != null) {
			boolean hasSkill = hasThreatAutomationSkill();

			if (hasSkill) {
				stats.getVariant().removeTag(Tags.VARIANT_UNBOARDABLE);
				stats.getVariant().removeTag(Tags.UNRECOVERABLE);
				stats.getVariant().addTag(NSP_Tags.THREAT_RECOVERABLE);
			} else {
				stats.getVariant().addTag(Tags.VARIANT_UNBOARDABLE);
				stats.getVariant().addTag(Tags.UNRECOVERABLE);
			}
		}

		// Add the weapon OP cost modifier listener
		if (!stats.hasListenerOfClass(ThreatWeaponOPCostModifier.class)) {
			stats.addListener(new ThreatWeaponOPCostModifier());
		}
	}

	// Helper method to check if player has the Threat Automation skill
	private boolean hasThreatAutomationSkill() {
		if (Global.getSector() == null || Global.getSector().getCharacterData() == null) return false;
		if (Global.getSector().getCharacterData().getPerson() == null) return false;
		if (Global.getSector().getCharacterData().getPerson().getStats() == null) return false;
		return Global.getSector().getCharacterData().getPerson().getStats().hasSkill(THREAT_AUTOMATION_SKILL_ID);
	}

	// Custom WeaponOPCostModifier listener to reduce OP costs for Threat-specific weapons
	private static class ThreatWeaponOPCostModifier implements WeaponOPCostModifier {
		@Override
		public int getWeaponOPCost(MutableShipStatsAPI stats, WeaponSpecAPI weapon, int currCost) {
			String weaponId = weapon.getWeaponId();
			if (WEAPON_OP_REDUCTION.containsKey(weaponId)) {
				int reduction = WEAPON_OP_REDUCTION.get(weaponId);
				return Math.max(0, currCost - reduction);
			}
			return currCost;
		}
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ship.setInvalidTransferCommandTarget(true);
	}

	public void onRemove(ShipAPI ship) {
		if (ship != null && ship.getMutableStats() != null) {
			ship.getMutableStats().removeListenerOfClass(ThreatWeaponOPCostModifier.class);
		}
		if (ship != null && ship.getVariant() != null) {
			ship.getVariant().removeTag(Tags.UNRECOVERABLE);
			ship.getVariant().removeTag(Tags.VARIANT_UNBOARDABLE);
		}
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		if (ship.getOriginalOwner() == -1) return;
		if (Global.getCombatEngine() == null || Global.getCombatEngine().isCombatOver() ||
				Global.getCurrentState().equals(GameState.TITLE)) return;

		if (!ship.hasListenerOfClass(nsp_threat_dmg_listener.class)) {
			nsp_threat_dmg_listener listener = new nsp_threat_dmg_listener();
			listener.ship = ship;
			ship.addListener(listener);
		}
	}

	static class nsp_threat_dmg_listener implements DamageTakenModifier {
		public ShipAPI ship = null;

		@Override
		public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage,
										Vector2f point, boolean shieldHit) {
			if (this.ship == null || target != this.ship) {
				return null;
			}
			String id = "nsp_threat_automation_dmg_reduction";
			if (damage.getDamage() > 6000f) {
				damage.getModifier().modifyMult(id, 0.10f);
			}
			return id;
		}
	}

	@Override
	public boolean affectsOPCosts() {
		return true;
	}

	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) {
			return "" + (int)Math.round(GAMMA_CORE_CR_PENALTY * 100) + "%";
		}
		if (index == 1) {
			return "" + (int)Math.round(BETA_CORE_CR_PENALTY * 100) + "%";
		}
		if (index == 2) {
			return "" + (int)Math.round(ALPHA_CORE_CR_PENALTY * 100) + "%";
		}
		return null;
	}

}