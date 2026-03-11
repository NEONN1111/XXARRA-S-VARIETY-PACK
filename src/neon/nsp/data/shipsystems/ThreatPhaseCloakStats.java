package neon.nsp.data.shipsystems;

import java.awt.Color;
import java.util.List;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.threat.BaseEnergyLashActivatedSystem;

import static com.fs.starfarer.api.impl.combat.PhaseCloakStats.*;

public class ThreatPhaseCloakStats extends BaseEnergyLashActivatedSystem {

	public static Color JITTER_COLOR = new Color(130,155,145,255);
	public static float JITTER_FADE_TIME = 0.5f;

	public static float SHIP_ALPHA_MULT = 0.25f;
	public static float VULNERABLE_FRACTION = 0f;
	public static float INCOMING_DAMAGE_MULT = 0.25f;

	public static float MAX_TIME_MULT = 3f;

	// New constants for timeflow surge
	public static float EXIT_TIME_FLOW_MULT = 3f;
	public static float EXIT_TIME_FLOW_DURATION = 1.5f;

	// Flag to track if we've already vented for this activation
	private boolean hasVentedThisActivation = false;
	// Flag to track if we've applied exit surge
	private boolean hasAppliedExitSurge = false;
	// Track previous state to detect state changes
	private State previousState = State.IDLE;

	protected Object STATUSKEY1 = new Object();
	protected Object STATUSKEY2 = new Object();
	protected Object STATUSKEY3 = new Object();
	protected Object STATUSKEY4 = new Object();
	protected ShipAIConfig origConfig;

	protected void init(ShipAPI ship) {
		super.init(ship);
		if (ship.getShipAI() != null && ship.getShipAI().getConfig() != null) {
			ShipAIConfig config = ship.getShipAI().getConfig();
			origConfig = config.clone();
		}
		// Reset flags on init
		hasVentedThisActivation = false;
		hasAppliedExitSurge = false;
		previousState = State.IDLE;
	}

	public static float getMaxTimeMult(MutableShipStatsAPI stats) {
		return 1f + (MAX_TIME_MULT - 1f) * stats.getDynamic().getValue(Stats.PHASE_TIME_BONUS_MULT);
	}

	protected boolean isDisruptable(ShipSystemAPI cloak) {
		return cloak.getSpecAPI().hasTag(Tags.DISRUPTABLE);
	}

	protected float getDisruptionLevel(ShipAPI ship) {
		// This is kept for jitter effects but no longer affects speed
		if (FLUX_LEVEL_AFFECTS_SPEED) {
			float threshold = ship.getMutableStats().getDynamic().getMod(
					Stats.PHASE_CLOAK_FLUX_LEVEL_FOR_MIN_SPEED_MOD).computeEffective(BASE_FLUX_LEVEL_FOR_MIN_SPEED);
			if (threshold <= 0) return 1f;
			float level = ship.getHardFluxLevel() / threshold;
			if (level > 1f) level = 1f;
			return level;
		}
		return 0f;
	}

	protected void maintainStatus(ShipAPI playerShip, State state, float effectLevel) {
		float level = effectLevel;
		float f = VULNERABLE_FRACTION;

		ShipSystemAPI cloak = playerShip.getPhaseCloak();
		if (cloak == null) cloak = playerShip.getSystem();
		if (cloak == null) return;

		if (level > f) {
			Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY2,
					cloak.getSpecAPI().getIconSpriteName(), cloak.getDisplayName(), "time flow altered", false);
		}

		if (FLUX_LEVEL_AFFECTS_SPEED) {
			if (level > f) {
				if (getDisruptionLevel(playerShip) <= 0f) {
					Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY3,
							cloak.getSpecAPI().getIconSpriteName(), "phase coils stable", "top speed at 100%", false);
				} else {
					// REMOVED: Speed loss indicator - now always shows 100% speed
					Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY3,
							cloak.getSpecAPI().getIconSpriteName(),
							"phase coil stress",
							"no speed penalty", false);
				}
			}
		}
	}

	public float getSpeedMult(ShipAPI ship, float effectLevel) {
		// REMOVED: Speed loss from phase coil stress - always return 1f (100% speed)
		return 1f;
	}

	public void applyImpl(ShipAPI ship, MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		// Check for state changes to trigger effects
		if (state != previousState) {
			// State transition detected
			if (state == State.ACTIVE || state == State.IN) {
				// Entering phase - vent 90% of flux (only once per activation)
				if (!hasVentedThisActivation && ship.getFluxTracker().getCurrFlux() > 0) {
					float currentFlux = ship.getFluxTracker().getCurrFlux();
					float fluxToVent = currentFlux * 0.9f;
					ship.getFluxTracker().decreaseFlux(fluxToVent);

					// Visual effect for venting
					Global.getCombatEngine().addSmoothParticle(
							ship.getLocation(),
							ship.getVelocity(),
							ship.getCollisionRadius() * 0.5f,
							1f,
							0.5f,
							new Color(100, 200, 255, 100)
					);

					hasVentedThisActivation = true;
				}
			} else if (previousState == State.ACTIVE && state == State.OUT) {
				// Exiting phase - prepare for timeflow surge
				hasAppliedExitSurge = false;
			}
		}

		// Apply exit timeflow surge when fully exiting phase
		if (previousState == State.ACTIVE && state == State.OUT && effectLevel <= 0.1f && !hasAppliedExitSurge) {
			// Apply brief timeflow multiplier to the ship
			stats.getTimeMult().modifyMult(id + "_exit_surge", EXIT_TIME_FLOW_MULT);

			// Add visual effect for the surge
			Global.getCombatEngine().addSmoothParticle(
					ship.getLocation(),
					ship.getVelocity(),
					ship.getCollisionRadius() * 0.8f,
					2f,
					0.3f,
					new Color(150, 200, 255, 150)
			);

			// Schedule removal of the timeflow bonus after duration
			final String surgeId = id + "_exit_surge";
			final MutableShipStatsAPI finalStats = stats;
			Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
				float elapsed = 0f;
				@Override
				public void advance(float amount, List<InputEventAPI> events) {
					elapsed += amount;
					if (elapsed >= EXIT_TIME_FLOW_DURATION) {
						finalStats.getTimeMult().unmodifyMult(surgeId);
						Global.getCombatEngine().removePlugin(this);
					}
				}
			});

			hasAppliedExitSurge = true;
		}

		// Reset flags if system is no longer active
		if (state == State.IDLE || state == State.COOLDOWN) {
			hasVentedThisActivation = false;
			hasAppliedExitSurge = false;
			stats.getTimeMult().unmodifyMult(id + "_exit_surge");
		}

		if (ship.getShipAI() != null && ship.getShipAI().getConfig() != null) {
			ShipAIConfig config = ship.getShipAI().getConfig();
			if (effectLevel > 0) {
				config.personalityOverride = Personalities.RECKLESS;
				config.alwaysStrafeOffensively = true;
				config.backingOffWhileNotVentingAllowed = false;
				config.turnToFaceWithUndamagedArmor = false;
				config.burnDriveIgnoreEnemies = true;
			} else {
				config.copyFrom(origConfig);
			}
		}

		if (effectLevel <= 0f) return;

		ship.getEngineController().extendFlame(ship.getSystem(), 1f * effectLevel, 0f * effectLevel, 0.5f * effectLevel);

		ship.getAIFlags().setFlag(AIFlags.DO_NOT_BACK_OFF, 1f);
		ship.getAIFlags().setFlag(AIFlags.DO_NOT_VENT, 1f);
		ship.getAIFlags().setFlag(AIFlags.IGNORES_ORDERS, 1f);

		makeAllGroupsAutofireOneFrame(ship);

		setStandardJitter(ship, state, effectLevel);

		// Update previous state for next frame
		previousState = state;
	}

	@Override
	public float getCurrentUsefulnessLevel(ShipAPI overseer, ShipAPI ship) {
		// This determines if the Energy Lash system should target this ship
		// Return high value when the ship would benefit from being phase-activated

		if (ship.getSystem().isActive() || ship.getSystem().isChargedown() ||
				ship.getSystem().isChargeup() || ship.getSystem().isCoolingDown()) {
			return 0f;
		}

		// Check if ship is in combat and has high flux (would benefit from venting)
		Object test = ship.getAIFlags().getCustom(AIFlags.MANEUVER_TARGET);
		if (test instanceof ShipAPI) {
			ShipAPI target = (ShipAPI) test;

			float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
			dist -= ship.getCollisionRadius() + target.getCollisionRadius();

			float range = getNonMissileWeaponRange(ship);
			float extra = 750f;
			if (dist < range + extra) {
				float distToOverseer = Misc.getDistance(ship.getLocation(), overseer.getLocation());
				distToOverseer -= ship.getCollisionRadius() + overseer.getCollisionRadius();
				float overseerDistFactor = 0f;
				if (distToOverseer < 1000f) {
					float min = 500f;
					overseerDistFactor = (1f - Math.max(0f, distToOverseer - min) / (1000f - min)) * 0.25f;
				}

				// Higher usefulness when flux is high (to utilize the venting effect)
				float fluxUsefulness = Math.min(0.7f, ship.getFluxLevel() * 1.2f);

				return Math.min(1f, 0.3f + fluxUsefulness + overseerDistFactor);
			}
		}

		return 0f;
	}

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

		if (player) {
			maintainStatus(ship, state, effectLevel);
		}

		if (Global.getCombatEngine().isPaused()) {
			return;
		}

		ShipSystemAPI cloak = ship.getPhaseCloak();
		if (cloak == null) cloak = ship.getSystem();
		if (cloak == null) return;

		// REMOVED: Speed modification from flux level - now handled by getSpeedMult returning 1f
		// But keep jitter effect for visual feedback
		if (FLUX_LEVEL_AFFECTS_SPEED) {
			if (state == State.ACTIVE || state == State.OUT || state == State.IN) {
				// Still update jitter level based on disruption for visual effect
				((PhaseCloakSystemAPI)cloak).setMinCoilJitterLevel(getDisruptionLevel(ship));
			}
		}

		if (state == State.COOLDOWN || state == State.IDLE) {
			unapply(stats, id);
			return;
		}

		float speedPercentMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_SPEED_MOD).computeEffective(0f);
		float accelPercentMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_ACCEL_MOD).computeEffective(0f);
		stats.getMaxSpeed().modifyPercent(id, speedPercentMod * effectLevel);
		stats.getAcceleration().modifyPercent(id, accelPercentMod * effectLevel);
		stats.getDeceleration().modifyPercent(id, accelPercentMod * effectLevel);

		float speedMultMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_SPEED_MOD).getMult();
		float accelMultMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_ACCEL_MOD).getMult();
		stats.getMaxSpeed().modifyMult(id, speedMultMod * effectLevel);
		stats.getAcceleration().modifyMult(id, accelMultMod * effectLevel);
		stats.getDeceleration().modifyMult(id, accelMultMod * effectLevel);

		float level = effectLevel;

		float jitterLevel = 0f;
		float jitterRangeBonus = 0f;
		float levelForAlpha = level;

		if (state == State.IN || state == State.ACTIVE) {
			ship.setPhased(true);
			levelForAlpha = level;
		} else if (state == State.OUT) {
			if (level > 0.5f) {
				ship.setPhased(true);
			} else {
				ship.setPhased(false);
			}
			levelForAlpha = level;
		}

		ship.setExtraAlphaMult(1f - (1f - SHIP_ALPHA_MULT) * levelForAlpha);
		ship.setApplyExtraAlphaToEngines(true);

		float extra = 0f;
		float shipTimeMult = 1f + (getMaxTimeMult(stats) - 1f) * levelForAlpha * (1f - extra);
		stats.getTimeMult().modifyMult(id, shipTimeMult);
		if (player) {
			Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / shipTimeMult);
		} else {
			Global.getCombatEngine().getTimeMult().unmodify(id);
		}
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else {
			return;
		}

		Global.getCombatEngine().getTimeMult().unmodify(id);
		stats.getTimeMult().unmodify(id);
		stats.getTimeMult().unmodifyMult(id + "_exit_surge");

		stats.getMaxSpeed().unmodify(id);
		stats.getMaxSpeed().unmodifyMult(id + "_2");
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);

		ship.setPhased(false);
		ship.setExtraAlphaMult(1f);

		ShipSystemAPI cloak = ship.getPhaseCloak();
		if (cloak == null) cloak = ship.getSystem();
		if (cloak != null) {
			((PhaseCloakSystemAPI)cloak).setMinCoilJitterLevel(0f);
		}

		// Reset flags
		hasVentedThisActivation = false;
		hasAppliedExitSurge = false;
		previousState = State.IDLE;
	}

	public static float getNonMissileWeaponRange(ShipAPI ship) {
		float max = 0f;
		for (WeaponAPI w : ship.getAllWeapons()) {
			if (w.isDecorative()) continue;
			if (w.getType() == WeaponAPI.WeaponType.MISSILE) continue;
			max = Math.max(max, w.getRange());
		}
		return max;
	}

	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0 && state == State.ACTIVE) {
			return new StatusData("venting 90% flux", false);
		} else if (index == 0 && state == State.OUT && effectLevel < 0.5f) {
			return new StatusData("timeflow surge", false);
		}
		return null;
	}
}