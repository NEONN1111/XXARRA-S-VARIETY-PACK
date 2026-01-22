package neon.nsp.data.shipsystems;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import java.awt.Color;

import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.MathUtils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.EmpArcEntityAPI.EmpArcParams;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.FindShipFilter;
import com.fs.starfarer.api.impl.combat.threat.VoltaicDischargeOnFireEffect;
import com.fs.starfarer.api.impl.combat.threat.EnergyLashActivatedSystem;

public class NSP_EnergyLashSystemScript extends BaseShipSystemScript {

	public static float MAX_LASH_RANGE = 1500f;

	public static float DAMAGE = 100;
	public static float EMP_DAMAGE = 2000;

	public static float MIN_COOLDOWN = 2f;
	public static float MAX_COOLDOWN = 10f;
	public static float COOLDOWN_DP_MULT = 0.33f;

	public static float MIN_HIT_ENEMY_COOLDOWN = 2f;
	public static float MAX_HIT_ENEMY_COOLDOWN = 5f;

	public static Color FRINGE_COLOR = new Color(255,175,255,255);
	public static Color CORE_COLOR = new Color(255, 255, 255, 255);
	public static Color FRIENDLY_FRINGE_COLOR = new Color(255,175,255,255);
	public static Color FRIENDLY_CORE_COLOR = new Color(255, 255, 255, 255);
	public static Color JITTER_COLOR = VoltaicDischargeOnFireEffect.EMP_FRINGE_COLOR;

	// New configurable settings for self-lashing arcs
	public static float SELF_LASH_CHANCE = 0.05f; // 5% chance per frame while charging
	public static float SELF_LASH_MAX_DURATION = 1.5f; // Maximum duration for self-lash effects
	public static float SELF_LASH_MIN_DURATION = 0.3f; // Minimum duration for self-lash effects
	public static float SELF_LASH_ARC_LENGTH = 800f; // Maximum length of self-lash arcs
	public static float SELF_LASH_EMP_DAMAGE = 200f; // EMP damage to self
	public static float SELF_LASH_MIN_COOLDOWN = 0.5f; // Minimum time between self-lash attempts
	public static float WEAPON_DISABLE_CHANCE = 0.3f; // 30% chance to disable a weapon when hit
	public static float ENGINE_DISABLE_CHANCE = 0.2f; // 20% chance to disable engines when hit
	public static float WEAPON_RECOIL_MULT = 2.0f; // Recoil multiplier when weapons are affected
	public static float TURRET_TURN_PENALTY = 0.5f; // Turret turn rate penalty when affected
	public static float SELF_LASH_ARC_COUNT = 4; // Number of arcs to spawn at once

	public static class DelayedCombatActionPlugin extends BaseEveryFrameCombatPlugin {
		float elapsed = 0f;
		float delay;
		Runnable r;

		public DelayedCombatActionPlugin(float delay, Runnable r) {
			this.delay = delay;
			this.r = r;
		}

		@Override
		public void advance(float amount, List<InputEventAPI> events) {
			if (Global.getCombatEngine().isPaused()) return;

			elapsed += amount;
			if (elapsed < delay) return;

			r.run();

			CombatEngineAPI engine = Global.getCombatEngine();
			engine.removePlugin(this);
		}
	}

	// Plugin for managing self-lash effects
	public static class SelfLashEffectPlugin extends BaseEveryFrameCombatPlugin {
		private ShipAPI ship;
		private float elapsed = 0f;
		private float duration;
		private boolean weaponDisabled = false;
		private boolean enginesDisabled = false;
		private String weaponId;

		public SelfLashEffectPlugin(ShipAPI ship, float duration, boolean weaponDisabled, boolean enginesDisabled, String weaponId) {
			this.ship = ship;
			this.duration = duration;
			this.weaponDisabled = weaponDisabled;
			this.enginesDisabled = enginesDisabled;
			this.weaponId = weaponId;
		}

		@Override
		public void advance(float amount, List<InputEventAPI> events) {
			if (Global.getCombatEngine().isPaused()) return;
			if (ship == null || ship.isHulk()) {
				Global.getCombatEngine().removePlugin(this);
				return;
			}

			elapsed += amount;

			// Apply effects
			if (weaponDisabled && weaponId != null) {
				for (WeaponAPI weapon : ship.getAllWeapons()) {
					if (weapon.getId().contentEquals(weaponId)) {
						weapon.disable();
						weapon.setRemainingCooldownTo(duration - elapsed);
						break;
					}
				}
			}

			if (enginesDisabled) {
				ship.getMutableStats().getMaxSpeed().modifyMult("self_lash_engine_disable", 0f);
				ship.getMutableStats().getAcceleration().modifyMult("self_lash_engine_disable", 0f);
				ship.getMutableStats().getDeceleration().modifyMult("self_lash_engine_disable", 0f);
				ship.getMutableStats().getMaxTurnRate().modifyMult("self_lash_engine_disable", 0f);
				ship.getMutableStats().getTurnAcceleration().modifyMult("self_lash_engine_disable", 0f);
			}

			// Add visual feedback
			if (elapsed < duration * 0.5f) {
				float brightness = elapsed / (duration * 0.5f);
				Color color = new Color(255, 100, 100, (int)(100 * brightness));
				//ship.setJitter(this, color, brightness, 3, 0f, 5f);
			}

			// Remove plugin when duration is over
			if (elapsed >= duration) {
				// Restore engine stats
				if (enginesDisabled) {
					ship.getMutableStats().getMaxSpeed().unmodify("self_lash_engine_disable");
					ship.getMutableStats().getAcceleration().unmodify("self_lash_engine_disable");
					ship.getMutableStats().getDeceleration().unmodify("self_lash_engine_disable");
					ship.getMutableStats().getMaxTurnRate().unmodify("self_lash_engine_disable");
					ship.getMutableStats().getTurnAcceleration().unmodify("self_lash_engine_disable");
				}

				// Weapons will automatically re-enable when their cooldown expires

				Global.getCombatEngine().removePlugin(this);
			}
		}
	}



	protected WeaponSlotAPI mainSlot;
	protected List<WeaponSlotAPI> slots;
	protected boolean readyToFire = true;
	protected float cooldownToSet = -1f;
	protected float sinceLastSelfLash = 0f;
	protected Random random = new Random();

	protected void findSlots(ShipAPI ship) {
		if (slots != null) return;
		slots = new ArrayList<>();
		for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
			if (slot.isSystemSlot()) {
				slots.add(slot);
				if (slot.getSlotSize() == WeaponSize.MEDIUM) {
					mainSlot = slot;
				}
			}
		}
	}

	protected void spawnSelfLashArc(ShipAPI ship, CombatEngineAPI engine, Vector2f startPoint, Vector2f endPoint) {
		// Use SKR_empStats simple EMP arc method for self-lash arcs
		// Use the same colors as the main energy lash
		Global.getCombatEngine().spawnEmpArc(
				ship,
				startPoint,
				ship,
				ship,
				DamageType.ENERGY,
				0f, // No regular damage
				SELF_LASH_EMP_DAMAGE, // EMP damage to self
				100000f, // max range
				"system_emp_emitter_impact", // Using the same sound as SKR_empStats
				MathUtils.getRandomNumberInRange(8, 16), // Thicker arcs than SKR_empStats
				FRINGE_COLOR, // Use main lash fringe color
				CORE_COLOR // Use main lash core color
		);

		// Check for weapon/engine disruption
		checkSelfLashEffects(ship);
	}

	protected void checkSelfLashEffects(ShipAPI ship) {
		// Chance to disable a random weapon
		if (random.nextFloat() < WEAPON_DISABLE_CHANCE && !ship.getAllWeapons().isEmpty()) {
			List<WeaponAPI> weapons = ship.getAllWeapons();
			WeaponAPI weapon = weapons.get(random.nextInt(weapons.size()));
			String weaponId = weapon.getId();

			// Apply temporary weapon effects
			float duration = SELF_LASH_MIN_DURATION + random.nextFloat() * (SELF_LASH_MAX_DURATION - SELF_LASH_MIN_DURATION);

			// Apply recoil penalty to the specific weapon (applies to all weapons with this mod)
			ship.getMutableStats().getRecoilPerShotMult().modifyMult("self_lash_weapon_recoil", WEAPON_RECOIL_MULT);
			ship.getMutableStats().getRecoilDecayMult().modifyMult("self_lash_weapon_recoil", 0.5f);

			// For turrets, apply turn rate penalty using general weapon stats
			if (weapon.getSlot() != null && weapon.getSlot().isTurret()) {
				ship.getMutableStats().getBallisticWeaponRangeBonus().modifyMult("self_lash_turret_turn", TURRET_TURN_PENALTY);
				ship.getMutableStats().getEnergyWeaponRangeBonus().modifyMult("self_lash_turret_turn", TURRET_TURN_PENALTY);
			}

			Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin() {
				float elapsed = 0f;
				@Override
				public void advance(float amount, List<InputEventAPI> events) {
					if (Global.getCombatEngine().isPaused()) return;
					if (ship == null || ship.isHulk()) {
						Global.getCombatEngine().removePlugin(this);
						return;
					}

					elapsed += amount;
					if (elapsed >= duration) {
						ship.getMutableStats().getRecoilPerShotMult().unmodify("self_lash_weapon_recoil");
						ship.getMutableStats().getRecoilDecayMult().unmodify("self_lash_weapon_recoil");
						ship.getMutableStats().getBallisticWeaponRangeBonus().unmodify("self_lash_turret_turn");
						ship.getMutableStats().getEnergyWeaponRangeBonus().unmodify("self_lash_turret_turn");
						Global.getCombatEngine().removePlugin(this);
					}
				}
			});

			// Add the main effect plugin for visual feedback
			Global.getCombatEngine().addPlugin(new SelfLashEffectPlugin(ship, duration, true, false, weaponId));
		}

		// Chance to disable engines
		if (random.nextFloat() < ENGINE_DISABLE_CHANCE) {
			float duration = SELF_LASH_MIN_DURATION + random.nextFloat() * (SELF_LASH_MAX_DURATION - SELF_LASH_MIN_DURATION);
			Global.getCombatEngine().addPlugin(new SelfLashEffectPlugin(ship, duration, false, true, null));
		}
	}

	protected void spawnRandomSelfLash(ShipAPI ship, CombatEngineAPI engine) {
		if (sinceLastSelfLash < SELF_LASH_MIN_COOLDOWN) return;

		// Random chance to spawn self-lash arcs
		if (random.nextFloat() < SELF_LASH_CHANCE) {
			// Get the central medium mount position
			findSlots(ship);
			if (mainSlot == null) return;

			Vector2f centerPoint = mainSlot.computePosition(ship);

			// Spawn multiple arcs in a circular pattern
			for (int i = 0; i < SELF_LASH_ARC_COUNT; i++) {
				// Generate random angle for the arc
				float angle = random.nextFloat() * 360f;

				// Calculate end point at a random distance within arc length
				float endDistance = SELF_LASH_ARC_LENGTH * 0.3f + random.nextFloat() * SELF_LASH_ARC_LENGTH * 0.7f;

				Vector2f endPoint = new Vector2f(
						centerPoint.x + (float)Math.cos(Math.toRadians(angle)) * endDistance,
						centerPoint.y + (float)Math.sin(Math.toRadians(angle)) * endDistance
				);

				spawnSelfLashArc(ship, engine, centerPoint, endPoint);
			}

			sinceLastSelfLash = 0f;
		}
	}

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else {
			return;
		}

		CombatEngineAPI engine = Global.getCombatEngine();
		float amount = Global.getCombatEngine().getElapsedInLastFrame();

		sinceLastSelfLash += amount;

		if ((state == State.COOLDOWN || state == State.IDLE) && cooldownToSet >= 0f) {
			ship.getSystem().setCooldown(cooldownToSet);
			ship.getSystem().setCooldownRemaining(cooldownToSet);
			cooldownToSet = -1f;
		}

		if (state == State.IDLE || state == State.COOLDOWN || effectLevel <= 0f) {
			readyToFire = true;
		}

		if (state == State.IN || state == State.OUT) {
			float jitterLevel = effectLevel;

			float maxRangeBonus = 150f;
			float jitterRangeBonus = (1f - effectLevel * effectLevel) * maxRangeBonus;

			float brightness = 0f;
			float threshold = 0.1f;
			if (effectLevel < threshold) {
				brightness = effectLevel / threshold;
			} else {
				brightness = 1f - (effectLevel - threshold) / (1f - threshold);
			}
			if (brightness < 0) brightness = 0;
			if (brightness > 1) brightness = 1;
			if (state == State.OUT) {
				jitterRangeBonus = 0f;
				brightness = effectLevel * effectLevel;
			}
			Color color = JITTER_COLOR;
			//ship.setJitter(this, color, jitterLevel, 5, 0f, 3f + jitterRangeBonus);

			// Spawn random self-lash arcs during charging (State.IN) using SKR_empStats method
			if (state == State.IN && effectLevel > 0.1f) {
				spawnRandomSelfLash(ship, engine);
			}
		}

		if (effectLevel == 1 && readyToFire) {
			ShipAPI target = findTarget(ship);
			readyToFire = false;
			if (target != null) {
				findSlots(ship);

				Vector2f slotLoc = mainSlot.computePosition(ship);

				EmpArcParams params = new EmpArcParams();
				params.segmentLengthMult = 8f;
				params.zigZagReductionFactor = 0.15f;
				params.fadeOutDist = 500f;
				params.minFadeOutMult = 2f;
				params.flickerRateMult = 0.7f;

				if (ship.getOwner() == target.getOwner()) {
					params.flickerRateMult = 0.3f;

					Color color = FRIENDLY_FRINGE_COLOR;
					float emp = 0;
					float dam = 0;
					EmpArcEntityAPI arc = (EmpArcEntityAPI)engine.spawnEmpArcPierceShields(ship, slotLoc, ship, target,
							DamageType.ENERGY,
							dam,
							emp,
							100000f,
							"energy_lash_friendly_impact",
							100f,
							color,
							new Color(255,255,255,255),
							params
					);
					arc.setTargetToShipCenter(slotLoc, target);
					arc.setCoreWidthOverride(50f);

					arc.setSingleFlickerMode(true);
					Global.getSoundPlayer().playSound("energy_lash_fire", 1f, 1f, ship.getLocation(), ship.getVelocity());
				} else {
					params.flickerRateMult = 0.4f;

					int numArcs = slots.size();

					float emp = EMP_DAMAGE;
					float dam = DAMAGE;

					for (int i = 0; i < numArcs; i++) {
						float delay = 0.03f * i;

						int index = i;
						ShipAPI ship2 = ship;
						Runnable r = new Runnable() {
							@Override
							public void run() {
								Vector2f slotLoc = slots.get(index).computePosition(ship2);
								Color color = FRINGE_COLOR;
								Color core = CORE_COLOR;
								EmpArcEntityAPI arc = (EmpArcEntityAPI)engine.spawnEmpArc(ship2, slotLoc, ship2, target,
										DamageType.ENERGY,
										dam,
										emp,
										100000f,
										"energy_lash_enemy_impact",
										60f,
										color,
										core,
										params
								);
								arc.setCoreWidthOverride(40f);
								arc.setSingleFlickerMode(true);
							}
						};
						if (delay <= 0f) {
							r.run();
						} else {
							Global.getCombatEngine().addPlugin(new DelayedCombatActionPlugin(delay, r));
						}

						Global.getSoundPlayer().playSound("energy_lash_fire_at_enemy", 1f, 1f, ship.getLocation(), ship.getVelocity());
					}
				}

				applyEffectToTarget(ship, target);
			}
		}
	}




	protected void applyEffectToTarget(ShipAPI ship, ShipAPI target) {
		if (target == null || target.getSystem() == null || target.isHulk()) return;
		if (ship == null || ship.getSystem() == null || ship.isHulk()) return;

		if (ship.getOwner() == target.getOwner()) {
			if (target.getSystem() != null && target.getSystem().getScript() instanceof EnergyLashActivatedSystem) {
				EnergyLashActivatedSystem script = (EnergyLashActivatedSystem) target.getSystem().getScript();
				script.hitWithEnergyLash(ship, target);
			}

			float cooldown = target.getHullSpec().getSuppliesToRecover();

			cooldown = MIN_COOLDOWN + cooldown * COOLDOWN_DP_MULT;
			if (cooldown > MAX_COOLDOWN) cooldown = MAX_COOLDOWN;
			if (target.isFighter()) cooldown = MIN_COOLDOWN;
			cooldownToSet = cooldown;
		} else {
			float cooldown = MIN_HIT_ENEMY_COOLDOWN +
					(MAX_HIT_ENEMY_COOLDOWN - MIN_HIT_ENEMY_COOLDOWN) * (float) Math.random();
			if (cooldown > MAX_COOLDOWN) cooldown = MAX_COOLDOWN;
			cooldownToSet = cooldown;
		}
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		// Clean up any remaining self-lash effects
		if (stats.getEntity() instanceof ShipAPI) {
			ShipAPI ship = (ShipAPI) stats.getEntity();
			ship.getMutableStats().getRecoilPerShotMult().unmodify("self_lash_weapon_recoil");
			ship.getMutableStats().getRecoilDecayMult().unmodify("self_lash_weapon_recoil");
			ship.getMutableStats().getBallisticWeaponRangeBonus().unmodify("self_lash_turret_turn");
			ship.getMutableStats().getEnergyWeaponRangeBonus().unmodify("self_lash_turret_turn");
			ship.getMutableStats().getMaxSpeed().unmodify("self_lash_engine_disable");
			ship.getMutableStats().getAcceleration().unmodify("self_lash_engine_disable");
			ship.getMutableStats().getDeceleration().unmodify("self_lash_engine_disable");
			ship.getMutableStats().getMaxTurnRate().unmodify("self_lash_engine_disable");
			ship.getMutableStats().getTurnAcceleration().unmodify("self_lash_engine_disable");
		}
	}

	public StatusData getStatusData(int index, State state, float effectLevel) {
		return null;
	}

	@Override
	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isOutOfAmmo()) return null;
		if (system.getState() != SystemState.IDLE) return null;

		ShipAPI target = findTarget(ship);
		if (target != null && target != ship) {
			return "READY";
		}
		if ((target == null || target == ship) && ship.getShipTarget() != null) {
			return "OUT OF RANGE";
		}
		return "NO TARGET";
	}

	public boolean isInRange(ShipAPI ship, ShipAPI target) {
		float range = getRange(ship);
		float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
		float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
		return dist <= range + radSum;
	}

	public boolean isValidLashTarget(ShipAPI ship, ShipAPI other) {
		if (other == null) return false;
		if (other.isHulk() || other.getOwner() == 100) return false;
		if (other.isShuttlePod()) return false;

		if (other.isFighter()) return false;
		if (other.getOwner() == ship.getOwner()) {
			if (other.getSystem() == null) return false;
			if (!(other.getSystem().getScript() instanceof EnergyLashActivatedSystem)) return false;
			if (other.getSystem().getCooldownRemaining() > 0) return false;
			if (other.getSystem().isActive()) return false;
			if (other.getFluxTracker().isOverloadedOrVenting()) return false;
		}
		return true;
	}


	protected ShipAPI findTarget(ShipAPI ship) {
		float range = getRange(ship);
		boolean player = ship == Global.getCombatEngine().getPlayerShip();
		ShipAPI target = ship.getShipTarget();

		float extraRange = 0f;
		if (ship.getShipAI() != null && ship.getAIFlags().hasFlag(AIFlags.CUSTOM1)){
			target = (ShipAPI) ship.getAIFlags().getCustom(AIFlags.CUSTOM1);
			extraRange += 500f;
		}


		if (target != null) {
			float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
			float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
			if (dist > range + radSum + extraRange) target = null;
		} else {
			FindShipFilter filter = s -> isValidLashTarget(ship, s);

			if (target == null || target.getOwner() == ship.getOwner()) {
				if (player) {
					target = Misc.findClosestShipTo(ship, ship.getMouseTarget(), HullSize.FIGHTER, range, true, false, filter);
				} else {
					Object test = ship.getAIFlags().getCustom(AIFlags.MANEUVER_TARGET);
					if (test instanceof ShipAPI) {
						target = (ShipAPI) test;
						float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
						float radSum = ship.getCollisionRadius() + target.getCollisionRadius();
						if (dist > range + radSum) target = null;
					}
				}
			}
			if (target == null) {
				target = Misc.findClosestShipTo(ship, ship.getLocation(), HullSize.FIGHTER, range, true, false, filter);
			}
		}

		return target;
	}

	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		ShipAPI target = findTarget(ship);
		return target != null && target != ship;
	}

	public static float getRange(ShipAPI ship) {
		if (ship == null) return MAX_LASH_RANGE;
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(MAX_LASH_RANGE);
	}

}