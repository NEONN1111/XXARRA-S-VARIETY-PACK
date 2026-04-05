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


	public static final String VANILLA_AUTOMATED_HULLMOD = "automated";


	public static float GAMMA_CORE_CR_PENALTY = 0.15f;
	public static float BETA_CORE_CR_PENALTY = 0.30f;
	public static float ALPHA_CORE_CR_PENALTY = 0.50f;
	public static float THREAT_PROCESSOR_CR_BONUS = 0.20f;

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

		tooltip.addPara("This penalty can be offset by a fleet commander with the %s skill, up to a maximum of %s.", 5f, Color.ORANGE, "Abyss-Rigging", "+80%");

		tooltip.addPara("They draw from a %s point pool than standard automated hulls.", 5f, Color.ORANGE, "seperate");

		tooltip.addPara("%s type weapons have %s OP Costs on these hulls.", 5f, Color.ORANGE, "Threat", "reduced");
	}



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


		String baseKey = id + "_" + shipId + "_base";
		String coreKey = id + "_" + shipId + "_core";


		stats.getMaxCombatReadiness().unmodifyFlat(baseKey);
		stats.getMaxCombatReadiness().unmodifyFlat(coreKey);


		String installedCoreId = null;
		if (member != null) {
			PersonAPI captain = member.getCaptain();
			if (captain != null && !captain.isDefault() && captain.isAICore()) {
				installedCoreId = captain.getAICoreId();
			}
		}

		if (stats.getVariant() != null && !VANILLA_REMOVED_SHIPS.contains(shipId)) {
			boolean removed = false;

			List<String> hullmodsCopy = new ArrayList<>(stats.getVariant().getHullMods());
			for (String mod : hullmodsCopy) {
				if (VANILLA_AUTOMATED_HULLMOD.equals(mod)) {
					stats.getVariant().getHullMods().remove(mod);
					removed = true;
				}
			}

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

			VANILLA_REMOVED_SHIPS.add(shipId);

			stats.getVariant().addTag(Tags.AUTOMATED);
			stats.getVariant().addTag(NSP_Tags.THREAT_AUTOMATED);
			stats.getVariant().addTag(NSP_Tags.THREAT_RECOVERABLE);
		}


		if (isInPlayerFleet(stats)) {


			stats.getMaxCombatReadiness().modifyFlat(baseKey, -MAX_CR_PENALTY, "Threat Automated ship penalty");


			// Core-specific modifications (penalties OR bonuses)
			if (installedCoreId != null) {
				float coreMod = 0f;
				String coreName = "";

				if ("gamma_core".equals(installedCoreId)) {
					coreMod = -GAMMA_CORE_CR_PENALTY;  // -0.15 = 15% penalty
					coreName = "Gamma Core";
				} else if ("beta_core".equals(installedCoreId)) {
					coreMod = -BETA_CORE_CR_PENALTY;   // -0.30 = 30% penalty
					coreName = "Beta Core";
				} else if ("alpha_core".equals(installedCoreId)) {
					coreMod = -ALPHA_CORE_CR_PENALTY;  // -0.50 = 50% penalty
					coreName = "Alpha Core";
				} else if ("nsp_threat_processor".equals(installedCoreId)) {
					coreMod = +0.20f;  // +0.20 = 20% BONUS (positive value)
					coreName = "Threat Processor";
				}

				if (coreMod != 0f) {
					String description = coreMod < 0 ? coreName + " penalty" : coreName + " bonus";
					stats.getMaxCombatReadiness().modifyFlat(coreKey, coreMod, description);
				}
			}
		}


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


		if (!stats.hasListenerOfClass(ThreatWeaponOPCostModifier.class)) {
			stats.addListener(new ThreatWeaponOPCostModifier());
		}
	}


	private boolean hasThreatAutomationSkill() {
		if (Global.getSector() == null || Global.getSector().getCharacterData() == null) return false;
		if (Global.getSector().getCharacterData().getPerson() == null) return false;
		if (Global.getSector().getCharacterData().getPerson().getStats() == null) return false;
		return Global.getSector().getCharacterData().getPerson().getStats().hasSkill(THREAT_AUTOMATION_SKILL_ID);
	}


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

		// Remove vanilla Automated hullmod every time the ship is loaded/created
		// This prevents it from being re-added when loading saves (like NSPSandyEffect does)
		if (ship.getVariant() != null) {
			// Remove from regular hullmods
			if (ship.getVariant().getHullMods().contains(VANILLA_AUTOMATED_HULLMOD)) {
				ship.getVariant().getHullMods().remove(VANILLA_AUTOMATED_HULLMOD);
				Global.getLogger(this.getClass()).info("Removed vanilla Automated hullmod from Threat ship in afterShipCreation: " + ship.getName());
			}

			// Remove from built-in hullmods
			if (ship.getVariant().getPermaMods() != null &&
					ship.getVariant().getPermaMods().contains(VANILLA_AUTOMATED_HULLMOD)) {
				ship.getVariant().getPermaMods().remove(VANILLA_AUTOMATED_HULLMOD);
				Global.getLogger(this.getClass()).info("Removed built-in vanilla Automated hullmod from Threat ship in afterShipCreation: " + ship.getName());
			}

			// Ensure tags are present
			ship.getVariant().addTag(Tags.AUTOMATED);
			ship.getVariant().addTag(NSP_Tags.THREAT_AUTOMATED);
			ship.getVariant().addTag(NSP_Tags.THREAT_RECOVERABLE);
		}
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

		// Safety check - remove vanilla Automated hullmod if it somehow reappears during combat
		if (ship.getVariant() != null && ship.getVariant().getHullMods().contains(VANILLA_AUTOMATED_HULLMOD)) {
			ship.getVariant().getHullMods().remove(VANILLA_AUTOMATED_HULLMOD);
			Global.getLogger(this.getClass()).info("Removed vanilla Automated hullmod from Threat ship in combat: " + ship.getName());
		}

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