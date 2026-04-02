package neon.nsp.data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import neon.nsp.data.plugins.NSP_ExponentCore;
import neon.nsp.data.plugins.NSP_ThreatProcessor;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

import static neon.nsp.data.scripts.NSPPeople.NSP_THREAT_PROCESSOR;

public class NSP_Threat_Automation extends BaseHullMod {

	public static float MAX_CR_PENALTY = 1f;

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getMinCrewMod().modifyMult(id, 0);
		stats.getMaxCrewMod().modifyMult(id, 0);

		boolean hasGestaltCore = false;
		FleetMemberAPI member = stats.getFleetMember();

		if (member != null) {
			PersonAPI captain = member.getCaptain();
			if (captain != null && !captain.isDefault() && captain.isAICore() && "nsp_threat_processor".equals(captain.getAICoreId())) {
				hasGestaltCore = true;
			}

			if (!hasGestaltCore && member.getFleetData() != null) {
				CampaignFleetAPI fleet = member.getFleetData().getFleet();
				if (fleet != null && fleet.getFaction() != null) {
					String memberFleetFactionId = fleet.getFaction().getId();
					if ("threat".equals(memberFleetFactionId)) {
						hasGestaltCore = true;
					}
				}
			}
		}

		if (stats.getVariant() != null) {
			stats.getVariant().addTag(Tags.AUTOMATED);
		}

		if (hasGestaltCore && stats.getVariant() != null) {
			stats.getVariant().addTag(Tags.TAG_AUTOMATED_NO_PENALTY);
		}

		if (isInPlayerFleet(stats) && !isAutomatedNoPenalty(stats)) {
			stats.getMaxCombatReadiness().modifyFlat(id, -MAX_CR_PENALTY, "Automated ship penalty");
		}
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ship.setInvalidTransferCommandTarget(true);
	}

	@Override
	public void advanceInCampaign(FleetMemberAPI member, float amount) {
		if (member.getCaptain() == null || member.getCaptain().isDefault() || (!member.getCaptain().getId().equals(NSP_THREAT_PROCESSOR) && !member.getCaptain().isPlayer())) {
			PersonAPI threatprocessor = Global.getSector().getImportantPeople().getPerson(NSP_THREAT_PROCESSOR);
			if (threatprocessor != null) {
				member.setCaptain(threatprocessor);
				Misc.setUnremovable(threatprocessor, false);
			} else {
				threatprocessor = new NSP_ThreatProcessor().createPerson("nsp_threat_processor", Factions.NEUTRAL, new Random());
				member.setCaptain(threatprocessor);
				Misc.setUnremovable(threatprocessor, true);
			}
		}
	}

	public static boolean isAutomatedNoPenalty(MutableShipStatsAPI stats) {
		if (stats == null) return false;
		FleetMemberAPI member = stats.getFleetMember();
		if (member == null) {
			if (stats.getVariant() != null) {
				return stats.getVariant().hasTag(Tags.TAG_AUTOMATED_NO_PENALTY);
			}
			return false;
		}
		return member.getHullSpec().hasTag(Tags.TAG_AUTOMATED_NO_PENALTY) ||
				(member.getVariant() != null && member.getVariant().hasTag(Tags.TAG_AUTOMATED_NO_PENALTY));
	}

	public static boolean isAutomatedNoPenalty(FleetMemberAPI member) {
		if (member == null) return false;
		return member.getHullSpec().hasTag(Tags.TAG_AUTOMATED_NO_PENALTY) ||
				(member.getVariant() != null && member.getVariant().hasTag(Tags.TAG_AUTOMATED_NO_PENALTY));
	}

	public static boolean isAutomatedNoPenalty(ShipAPI ship) {
		if (ship == null) return false;
		FleetMemberAPI member = ship.getFleetMember();
		if (member == null) return false;
		return member.getHullSpec().hasTag(Tags.TAG_AUTOMATED_NO_PENALTY) ||
				member.getVariant().hasTag(Tags.TAG_AUTOMATED_NO_PENALTY);
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		if (ship.getOriginalOwner() == -1) return;
		if (Global.getCombatEngine() == null || Global.getCombatEngine().isCombatOver() || Global.getCurrentState().equals(GameState.TITLE)) return;

		if (!ship.hasListenerOfClass(nsp_threat_dmg_listener.class)) {
			nsp_threat_dmg_listener listener = new nsp_threat_dmg_listener();
			listener.ship = ship;
			ship.addListener(listener);
		}
	}

	static class nsp_threat_dmg_listener implements DamageTakenModifier {
		public ShipAPI ship = null;

		@Override
		public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
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
		return null;
	}

	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float opad = 10f;
		tooltip.addPara("Automated ships usually require specialized equipment and expertise to maintain, "
						+ "resulting in a maximum combat readiness penalty of %s. "
						+ "This penalty can be offset by a fleet commander skilled in the use of "
						+ "automated ships.", opad, Misc.getHighlightColor(),
				"" + (int)Math.round(MAX_CR_PENALTY * 100f) + "%");

		}
}