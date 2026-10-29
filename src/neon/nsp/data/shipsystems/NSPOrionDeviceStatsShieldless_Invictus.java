package neon.nsp.data.shipsystems;

import java.awt.Color;
import java.util.*;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.combat.LidarArrayStats;
import org.lazywizard.lazylib.CollisionUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

/**
 *  The way to provide custom params is to have a derived class that sets p = <params> in its constructor.
 *
 * @author Alex
 *
 * Copyright 2022 Fractal Softworks, LLC
 */
public class NSPOrionDeviceStatsShieldless_Invictus extends BaseShipSystemScript {


	public static String LIDAR_WINDUP = "lidar_windup";

	public static Color WEAPON_GLOW = new Color(255,50,50,155);

	public static float SELF_DAMAGE_FLUX_MULT = 0.20f;
	public static float SELF_DAMAGE_MIN_HULL = 0.2f;


	public static float MALFUNCTION_CHANCE = 0.22f;
	public static float MALFUNCTION_DURATION = 1.5f;


	public static float SELF_DAMAGE_MIN = 100f;
	public static float SELF_DAMAGE_MAX = 9000f;

	public static float CONTINUOUS_FIRE_INTERVAL = 0.5f;

	protected Random random = new Random();
	protected boolean hasAppliedMalfunction = false;
	protected boolean hasAppliedSelfDamage = false;

	public static class LidarDishData {
		public float turnDir;
		public float turnRate;
		public float angle;
		public float phase;
		public float count;
		public WeaponAPI w;
	}

	protected List<LidarArrayStats.LidarDishData> dishData = new ArrayList<LidarArrayStats.LidarDishData>();
	protected boolean needsUnapply = false;
	protected boolean playedWindup = false;

	protected boolean inited = false;
	public void init(ShipAPI ship) {
		if (inited) return;
		inited = true;

		needsUnapply = true;

		int turnDir = 1;
		float index = 0f;
		float count = 0f;
		List<WeaponAPI> lidar = new ArrayList<WeaponAPI>();
		for (WeaponAPI w : ship.getAllWeapons()) {
			if (w.isDecorative() && w.getSpec().hasTag(Tags.LIDAR)) {
				lidar.add(w);
				count++;
			}
		}

		if (count == 0f) {
			for (WeaponAPI w : ship.getAllWeapons()) {
				if (w.isDecorative() && w.getSlot().isSystemSlot()) {
					lidar.add(w);
					count++;
				}
			}
		}

		System.out.println("[NSPOrion] LIDAR dishes found: " + count);

		Collections.sort(lidar, new Comparator<WeaponAPI>() {
			public int compare(WeaponAPI o1, WeaponAPI o2) {
				return (int) Math.signum(o1.getSlot().getLocation().x - o2.getSlot().getLocation().x);
			}
		});
		for (WeaponAPI w : lidar) {
			w.setSuspendAutomaticTurning(true);
			LidarArrayStats.LidarDishData data = new LidarArrayStats.LidarDishData();
			data.turnDir = Math.signum(turnDir);
			data.turnRate = 0.1f;
			data.w = w;
			data.angle = 0f;
			data.phase = index / count;
			data.count = count;
			dishData.add(data);
			turnDir = -turnDir;
			index++;
		}
	}

	public void rotateLidarDishes(boolean active, float effectLevel) {
		float amount = Global.getCombatEngine().getElapsedInLastFrame();

		float turnRateMult = 1f;
		if (active) {
			turnRateMult = 20f;
		}
		for (LidarArrayStats.LidarDishData data : dishData) {
			float arc = data.w.getArc();
			float useTurnDir = data.turnDir;
			if (active) {
				useTurnDir = Misc.getClosestTurnDirection(data.angle, 0f);
			}
			float delta = useTurnDir * amount * data.turnRate * turnRateMult * arc;
			if (active && effectLevel > 0f && Math.abs(data.angle) < Math.abs(delta * 1.5f)) {
				data.angle = 0f;
			} else {
				data.angle += delta;
				data.phase += 1f * amount;
				if (arc < 360f) {
					if (data.angle > arc/2f && data.turnDir > 0f) {
						data.angle = arc/2f;
						data.turnDir = -1f;
					}
					if (data.angle < -arc/2f && data.turnDir < 0f) {
						data.angle = -arc/2f;
						data.turnDir = 1f;
					}
				} else {
					data.angle = data.angle % 360f;
				}
			}


			float facing = data.angle + data.w.getArcFacing() + data.w.getShip().getFacing();
			data.w.setFacing(facing);
			data.w.updateBeamFromPoints();
		}
	}

	public static class PusherPlateImpulse {
		public float force;
		public float dur;
		public float elapsed;
	}
	public static class PusherPlateState {
		public float compression;
		public float vel;
		public List<PusherPlateImpulse> impulses = new ArrayList<PusherPlateImpulse>();

		public void addImpulse(float force, float dur) {
			PusherPlateImpulse ppi = new PusherPlateImpulse();
			ppi.force = force;
			ppi.dur = dur;
			impulses.add(ppi);

		}
		public void advance(float amount) {
			List<PusherPlateImpulse> remove = new ArrayList<PusherPlateImpulse>();
			float totalForce = 0f;
			for (PusherPlateImpulse curr : impulses) {
				totalForce += curr.force;
				curr.elapsed += amount;
				if (curr.elapsed >= curr.dur) {
					remove.add(curr);
				}
			}
			impulses.removeAll(remove);

			float springForce = compression;
			float netForce = totalForce - springForce;

			vel += netForce * amount;
			compression += vel * amount;

			if (compression > 1f) {
				compression = 1f;
				vel = 0f;
			}
			float min = 0f;
			if (compression < min) {
				compression = min;
				vel = 0f;
			}

		}
	}




	public static class WeaponMalfunctionData {
		IntervalUtil interval = new IntervalUtil(100f, 100f);
		public WeaponMalfunctionData(float interval) {
			this.interval = new IntervalUtil(interval, interval);
		}
	}

	public static class OrionDeviceParams {
		public float bombFadeInTime = 0.15f;
		public float bombLiveTime = 0.25f;
		public float bombSpeed = 9f;
		public float bombInheritedVelocityFraction = 0.5f;

		public float shapedExplosionOffset = 50f;
		public float shapedExplosionEndSizeMin = 1f;
		public float shapedExplosionEndSizeMax = 2f;
		public Color shapedExplosionColor = new Color(200,125,255,255);
		public int shapedExplosionNumParticles = 200;
		public float shapedExplosionMinParticleSize = 150;
		public float shapedExplosionMaxParticleSize = 250;
		public float shapedExplosionScatter = 100f;
		public float shapedExplosionMinParticleVel = 150;
		public float shapedExplosionMaxParticleVel = 400f;
		public float shapedExplosionMinParticleDur = 2f;
		public float shapedExplosionMaxParticleDur = 3f;
		public float shapedExplosionArc = 90f;
		public Color jitterColor = new Color(200,125,255,55);
		public float maxJitterDur = 6f;

		public float pusherPlateMaxOffset = 14f;
		public float pusherPlateImpulseForce = 10f;
		public float pusherPlateImpulseDuration = 1.5f;

		public float impactAccel = 15000f;
		public float impactRateMult = 0.5f;

		public boolean recolorTowardsEngineColor = false;

		public String bombWeaponId = "nsp_od_bomblauncher_lp";
	}


	protected OrionDeviceParams p = new OrionDeviceParams();
	protected PusherPlateState pusherState = new PusherPlateState();

	public NSPOrionDeviceStatsShieldless_Invictus() {
		p = new OrionDeviceParams();

	}


	protected Color orig = null;
	protected void recolor(ShipAPI ship) {
		if (ship == null) return;
		if (!p.recolorTowardsEngineColor) return;

		if (orig == null) orig = p.shapedExplosionColor;

		Color curr = ship.getEngineController().getFlameColorShifter().getCurr();

		p.shapedExplosionColor = Misc.interpolateColor(orig, curr, 0.75f);
		p.shapedExplosionColor = Misc.setAlpha(p.shapedExplosionColor, orig.getAlpha());
	}

	protected List<MissileAPI> heldBombs = new ArrayList<MissileAPI>();
	protected IntervalUtil continuousFireInterval = new IntervalUtil(CONTINUOUS_FIRE_INTERVAL, CONTINUOUS_FIRE_INTERVAL);
	protected WeaponSlotAPI bombSlot = null;
	protected boolean windupFiring = false;
	protected boolean bombsDetonated = false;

	protected boolean wasIdle = false;
	protected boolean deployedBomb = false;
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;

		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else {
			return;
		}

		recolor(ship);
		init(ship);

		boolean active = state == State.IN || state == State.ACTIVE || state == State.OUT;

		rotateLidarDishes(active, effectLevel);

		if (bombSlot == null) {
			for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
				if (slot.isSystemSlot() && slot.getSlotSize() == WeaponAPI.WeaponSize.MEDIUM) {
					bombSlot = slot;
					break;
				}
			}
		}

		float amount = Global.getCombatEngine().getElapsedInLastFrame();

		if (state == State.IN && bombSlot != null) {
			if (!windupFiring) {
				windupFiring = true;
				bombsDetonated = false;
				continuousFireInterval.setElapsed(0f);
			}
			continuousFireInterval.advance(amount);
			if (continuousFireInterval.intervalElapsed()) {
				MissileAPI bomb = spawnHeldBomb(ship, bombSlot);
				if (bomb != null) {
					heldBombs.add(bomb);
				}
			}
		}

		if (!bombsDetonated && (state == State.ACTIVE || (state == State.IN && effectLevel >= 1f))) {
			detonateHeldBombs(ship);
			bombsDetonated = true;
			deployedBomb = true;

			hasAppliedMalfunction = false;
			hasAppliedSelfDamage = false;
		} else if (state == State.COOLDOWN) {
			deployedBomb = false;
			windupFiring = false;
			bombsDetonated = false;

			if (!hasAppliedMalfunction) {
				applyMalfunction(ship);
				hasAppliedMalfunction = true;
			}
		}

		pusherState.advance(amount);

		for (WeaponAPI w : ship.getAllWeapons()) {
			Vector2f offset = new Vector2f(p.pusherPlateMaxOffset, 0f);

			offset.scale(pusherState.compression);
			if (w.getSpec().hasTag("pusherplate")) {
				w.setRenderOffsetForDecorativeBeamWeaponsOnly(offset);
			}
		}


		if (deployedBomb && effectLevel >= 1f && !hasAppliedSelfDamage) {
			applySelfDamage(ship);
			hasAppliedSelfDamage = true;
		}


		if (state == State.COOLDOWN || state == State.IDLE) {
			updateMalfunctionTimers(ship, amount);
		}

		advanceImpl(amount, ship, state, effectLevel);

		for (WeaponAPI w : ship.getAllWeapons()) {
			if (w.getSlot().isSystemSlot()) continue;
			if (w.getType() == WeaponType.MISSILE) continue;
			if (state == State.IN) {
				if (!(w.isDecorative() && w.getSpec().hasTag(Tags.LIDAR))) {
					w.setForceNoFireOneFrame(true);
				}
			} else {
				boolean isLidar = w.isDecorative() && w.getSpec().hasTag(Tags.LIDAR);
				if (isLidar) {
					w.setForceNoFireOneFrame(true);
				}
			}
		}

		Color glowColor = WEAPON_GLOW;

		float lidarRange = 500;
		for (WeaponAPI w : ship.getAllWeapons()) {
			if (!w.isDecorative() && w.getSlot().isHardpoint() && !w.isBeam() &&
					(w.getType() == WeaponType.BALLISTIC || w.getType() == WeaponType.ENERGY)) {
				lidarRange = Math.max(lidarRange, w.getRange());
				w.setGlowAmount(effectLevel, glowColor);
			}
		}
		lidarRange += 100f;
		stats.getBeamWeaponRangeBonus().modifyFlat("lidararray", lidarRange);

		float fireThreshold = 0.25f / 3.25f;
		fireThreshold += 0.02f;
		for (LidarArrayStats.LidarDishData data : dishData) {
			if (data.w == null) continue;
			if (!(data.w.isDecorative() && data.w.getSpec().hasTag(Tags.LIDAR))) {
				if (!data.w.isDecorative()) continue;
			}
			if (state == State.IN && Math.abs(data.angle) < 5f && effectLevel >= fireThreshold) {
				data.w.setForceFireOneFrame(true);
			}
		}

		if (((state == State.IN && effectLevel > 0.67f) || state == State.ACTIVE) && !playedWindup) {
			Global.getSoundPlayer().playSound(LIDAR_WINDUP, 1f, 1f, ship.getLocation(), ship.getVelocity());
			playedWindup = true;
		}

	}

	protected void advanceImpl(float amount, ShipAPI ship, State state, float effectLevel) {

	}


	public void unapply(MutableShipStatsAPI stats, String id) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine != null) {
			ShipAPI ship = (ShipAPI)stats.getEntity();
			if (ship != null) {
				String key = "orion_malfunction" + "_" + ship.getId();
				engine.getCustomData().remove(key);
			}
		}
		for (MissileAPI bomb : heldBombs) {
			if (bomb != null && !bomb.isFading() && !bomb.isExpired()) {
				bomb.fadeOutThenIn(0.05f);
			}
		}
		heldBombs.clear();

		hasAppliedMalfunction = false;
		hasAppliedSelfDamage = false;
		playedWindup = false;
		deployedBomb = false;
		windupFiring = false;
		bombsDetonated = false;
	}

	protected MissileAPI spawnHeldBomb(ShipAPI source, WeaponSlotAPI slot) {
		CombatEngineAPI engine = Global.getCombatEngine();
		Vector2f loc = slot.computePosition(source);
		float angle = slot.computeMidArcAngle(source);

		if (pusherState.compression > 0) {
			Vector2f offset = new Vector2f(p.pusherPlateMaxOffset, 0f);
			offset.scale(pusherState.compression);
			offset = Misc.rotateAroundOrigin(offset, source.getFacing());
			Vector2f.add(loc, offset, loc);
		}

		MissileAPI bomb = (MissileAPI) engine.spawnProjectile(source, null,
				p.bombWeaponId,
				loc,
				angle, source.getVelocity());
		if (bomb == null) return null;

		if (source != null) {
			Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(
					source, WeaponType.MISSILE, false, bomb.getDamage());
		}

		float fadeInTime = p.bombFadeInTime;
		Vector2f inheritedVel = new Vector2f(source.getVelocity());
		inheritedVel.scale(p.bombInheritedVelocityFraction);
		float speed = p.bombSpeed;

		Vector2f vel = Misc.getUnitVectorAtDegreeAngle(angle);
		vel.scale(speed);
		Vector2f.add(vel, inheritedVel, vel);
		bomb.getVelocity().set(vel);
		bomb.fadeOutThenIn(fadeInTime);

		bomb.setCollisionClass(CollisionClass.NONE);
		bomb.setEmpResistance(1000);
		bomb.setEccmChanceOverride(1f);

		bomb.setMaxFlightTime(Float.MAX_VALUE);

		Global.getCombatEngine().addPlugin(createBombImpactPlugin(source, slot, bomb, loc, angle));

		return bomb;
	}

	protected void detonateHeldBombs(ShipAPI ship) {
		if (heldBombs.isEmpty()) return;

		for (MissileAPI bomb : heldBombs) {
			if (bomb == null || bomb.isFading() || bomb.isExpired()) continue;

			bomb.setMaxFlightTime(p.bombLiveTime);
		}
		heldBombs.clear();
	}

	public void spawnBomb(ShipAPI source, WeaponSlotAPI slot) {
		CombatEngineAPI engine = Global.getCombatEngine();
		Vector2f loc = slot.computePosition(source);
		float angle = slot.computeMidArcAngle(source);

		if (pusherState.compression > 0) {
			Vector2f offset = new Vector2f(p.pusherPlateMaxOffset, 0f);
			offset.scale(pusherState.compression);
			offset = Misc.rotateAroundOrigin(offset, source.getFacing());
			Vector2f.add(loc, offset, loc);
		}

		MissileAPI bomb = (MissileAPI) engine.spawnProjectile(source, null,
				p.bombWeaponId,
				loc,
				angle, source.getVelocity());
		if (source != null) {
			Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(
					source, WeaponType.MISSILE, false, bomb.getDamage());
		}

		float fadeInTime = p.bombFadeInTime;
		Vector2f inheritedVel = new Vector2f(source.getVelocity());
		inheritedVel.scale(p.bombInheritedVelocityFraction);
		float speed = p.bombSpeed;

		Vector2f vel = Misc.getUnitVectorAtDegreeAngle(angle);
		vel.scale(speed);
		Vector2f.add(vel, inheritedVel, vel);
		bomb.getVelocity().set(vel);
		bomb.fadeOutThenIn(fadeInTime);

		bomb.setCollisionClass(CollisionClass.NONE);
		bomb.setEmpResistance(1000);
		bomb.setEccmChanceOverride(1f);


		float liveTime = p.bombLiveTime;
		bomb.setMaxFlightTime(liveTime);


		Global.getCombatEngine().addPlugin(createBombImpactPlugin(source, slot, bomb, loc, angle));
	}

	protected void notifySpawnedExplosionParticles(Vector2f bombLoc) {

	}

	protected EveryFrameCombatPlugin createBombImpactPlugin(final ShipAPI ship, final WeaponSlotAPI launchSlot,
															final MissileAPI bomb, final Vector2f launchLoc, final float launchAngle) {

		return new BaseEveryFrameCombatPlugin() {
			float elapsed = 0f;
			float impactTime = 0f;
			float brakingTime = 0f;
			float forceAngle;
			float jitterTime = 0f;
			boolean triggered = false;
			boolean braking = false;
			boolean done = false;

			@Override
			public void advance(float amount, List<InputEventAPI> events) {
				if (Global.getCombatEngine().isPaused()) return;

				elapsed += amount;

				String impactCounterId = "od_system_counter";

				if (bomb.isFizzling()) {
					if (!triggered) {

						pusherState.addImpulse(p.pusherPlateImpulseForce, p.pusherPlateImpulseDuration);

						forceAngle = Misc.getAngleInDegrees(bomb.getLocation(), launchSlot.computePosition(ship));
						float angleToShip = Misc.getAngleInDegrees(bomb.getLocation(), ship.getLocation());
						if (Misc.getAngleDiff(angleToShip, forceAngle) > 90f) {
							forceAngle += 180f;
						}

						float diff = Misc.getAngleDiff(angleToShip, forceAngle);
						float turnDir = Misc.getClosestTurnDirection(angleToShip, forceAngle);
						forceAngle = angleToShip + diff * turnDir * 0.2f;

						triggered = true;
						ship.getMutableStats().getDynamic().getMod(impactCounterId).modifyFlat("od_launch_" + launchLoc, 1f);

						if (Global.getCombatEngine().getViewport().isNearViewport(ship.getLocation(), 800f)) {
							float angle = forceAngle + 180f;

							int numParticles = p.shapedExplosionNumParticles;
							float minSize = p.shapedExplosionMinParticleSize;
							float maxSize = p.shapedExplosionMaxParticleSize;
							Color pc = p.shapedExplosionColor;

							float minDur = p.shapedExplosionMinParticleDur;
							float maxDur = p.shapedExplosionMaxParticleDur;

							float arc = p.shapedExplosionArc;
							float scatter = p.shapedExplosionScatter;
							float minVel = p.shapedExplosionMinParticleVel;
							float maxVel = p.shapedExplosionMaxParticleVel;

							float launchOffset = p.shapedExplosionOffset;
							float endSizeMin = p.shapedExplosionEndSizeMin;
							float endSizeMax = p.shapedExplosionEndSizeMax;

							Vector2f spawnPoint = Misc.getUnitVectorAtDegreeAngle(forceAngle);
							spawnPoint.scale(launchOffset);
							Vector2f.add(bomb.getLocation(), spawnPoint, spawnPoint);
							for (int i = 0; i < numParticles; i++) {
								float angleOffset = (float) Math.random();
								if (angleOffset > 0.2f) {
									angleOffset *= angleOffset;
								}
								float speedMult = 1f - angleOffset;
								speedMult = 0.5f + speedMult * 0.5f;
								angleOffset *= Math.signum((float) Math.random() - 0.5f);
								angleOffset *= arc/2f;
								float theta = (float) Math.toRadians(angle + angleOffset);
								float r = (float) (Math.random() * Math.random() * scatter);
								float x = (float)Math.cos(theta) * r;
								float y = (float)Math.sin(theta) * r;
								Vector2f pLoc = new Vector2f(spawnPoint.x + x, spawnPoint.y + y);

								float speed = minVel + (maxVel - minVel) * (float) Math.random();
								speed *= speedMult;

								Vector2f pVel = Misc.getUnitVectorAtDegreeAngle((float) Math.toDegrees(theta));
								pVel.scale(speed);

								float pSize = minSize + (maxSize - minSize) * (float) Math.random();
								float pDur = minDur + (maxDur - minDur) * (float) Math.random();
								float endSize = endSizeMin + (endSizeMax - endSizeMin) * (float) Math.random();
								Global.getCombatEngine().addNebulaParticle(pLoc, pVel, pSize, endSize, 0.1f, 0.5f, pDur, pc);
							}


							notifySpawnedExplosionParticles(bomb.getLocation());
						}
					}
				}

				boolean multipleImpacts = ship.getMutableStats().getDynamic().getMod(impactCounterId).computeEffective(0) > 1;

				String id = "od_system_mod";
				ship.getMutableStats().getMaxSpeed().unmodifyFlat(id);

				float maxSpeedWithoutBonus = ship.getMutableStats().getMaxSpeed().getModifiedValue();



				if (triggered) {
					jitterTime += amount;
					float intensity = bomb.getFlightTime() / bomb.getMaxFlightTime();
					if (intensity > 1f) intensity = 1f;
					if (triggered) intensity = 1f;
					if (braking) {
						intensity = 1f - brakingTime * 2f;
						if (intensity < 0) intensity = 0;
					}
					float alt = 1f - (jitterTime / p.maxJitterDur);
					if (alt < intensity) {
						intensity = Math.max(alt, 0f);
					}
					Color jc = p.jitterColor;
					ship.setJitter(this, jc, intensity, 3, 0f, 0f);
				}

				if (triggered && !braking) {
					impactTime += amount * p.impactRateMult;

					float mag = (1f - impactTime) * (1f - impactTime);


					if (mag > 0) mag = (float) Math.sqrt(mag);

					Vector2f forcePoint = launchSlot.computePosition(ship);

					float dirToCenter = Misc.getAngleInDegrees(forcePoint, ship.getLocation());
					float angleDiff = Misc.getAngleDiff(forceAngle, dirToCenter);


					float totalAccel = p.impactAccel;
					float portionAppliedToAngularVelocity = angleDiff * 1f / 90f;
					if (portionAppliedToAngularVelocity > 1f) portionAppliedToAngularVelocity = 1f;

					Vector2f acc = Misc.getUnitVectorAtDegreeAngle(forceAngle);
					acc.scale(totalAccel * (1f - portionAppliedToAngularVelocity * 0.2f));
					acc.scale(mag * amount);

					Vector2f.add(ship.getVelocity(), acc, ship.getVelocity());

					float angVelChange = portionAppliedToAngularVelocity * ship.getMaxTurnRate() * 0.25f;
					angVelChange *= mag;
					angVelChange *= Misc.getClosestTurnDirection(forceAngle, dirToCenter);
					ship.setAngularVelocity(ship.getAngularVelocity() + angVelChange * amount);

					float maxSpeedBoost = 1000f * Math.max(0f, (1f - portionAppliedToAngularVelocity) * 0.5f);

					if (maxSpeedBoost > 0) {
						ship.getMutableStats().getMaxSpeed().modifyFlat(id, maxSpeedBoost);
					} else {
						ship.getMutableStats().getMaxSpeed().unmodifyFlat(id);
					}
					ship.getMutableStats().getDeceleration().modifyFlat(id, 1f * Math.max(maxSpeedWithoutBonus, ship.getVelocity().length() - maxSpeedWithoutBonus));

					ship.blockCommandForOneFrame(ShipCommand.ACCELERATE);
					ship.blockCommandForOneFrame(ShipCommand.ACCELERATE_BACKWARDS);
					ship.giveCommand(ShipCommand.DECELERATE, null, 0);

					if (impactTime >= 1f) {
						braking = true;

						ship.getMutableStats().getMaxSpeed().unmodify(id);
					}
				}

				if (braking) {
					if (!multipleImpacts) {
						ship.getMutableStats().getDeceleration().modifyFlat(id, 2f * Math.max(maxSpeedWithoutBonus, ship.getVelocity().length() - maxSpeedWithoutBonus));

						ship.blockCommandForOneFrame(ShipCommand.ACCELERATE);
						ship.blockCommandForOneFrame(ShipCommand.ACCELERATE_BACKWARDS);
						ship.giveCommand(ShipCommand.DECELERATE, null, 0);

					}
					brakingTime += amount;
					float threshold = 3f;
					if (multipleImpacts) threshold = 0.1f;
					if (brakingTime >= threshold || ship.getVelocity().length() <= maxSpeedWithoutBonus) {
						done = true;
					}
				}


				if ((!triggered && elapsed > 1f) || done) {
					Global.getCombatEngine().removePlugin(this);

					ship.getMutableStats().getDeceleration().unmodify(id);

					ship.getMutableStats().getMaxSpeed().unmodify(id);

					ship.getMutableStats().getDynamic().getMod(impactCounterId).unmodifyFlat("od_launch_" + launchLoc);
				}
			}
		};
	}


	protected void applySelfDamage(ShipAPI ship) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine == null) return;


		if (ship.getHullLevel() >= SELF_DAMAGE_MIN_HULL) {
			float damage = SELF_DAMAGE_MIN + (float) Math.random() * (SELF_DAMAGE_MAX - SELF_DAMAGE_MIN);


			Vector2f point = new Vector2f(ship.getLocation());
			point.x += ship.getCollisionRadius() * ((float) Math.random() * 2f - 1f);
			point.y += ship.getCollisionRadius() * ((float) Math.random() * 2f - 1f);

			if (!CollisionUtils.isPointWithinBounds(point, ship)) {
				point = CollisionUtils.getNearestPointOnBounds(point, ship);
			}


			engine.applyDamage(ship, point, damage, DamageType.OTHER, 0f, true, false, null);
		}
	}


	protected void updateMalfunctionTimers(ShipAPI ship, float amount) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine == null) return;

		String key = "orion_malfunction" + "_" + ship.getId();
		WeaponMalfunctionData data = (WeaponMalfunctionData) engine.getCustomData().get(key);

		if (data == null) return;

		data.interval.advance(amount);


		if (data.interval.intervalElapsed()) {

			for (WeaponAPI weapon : ship.getAllWeapons()) {
				if (weapon.getSlot().isSystemSlot()) continue;
				weapon.repair();
			}

			engine.getCustomData().remove(key);
			ship.syncWeaponDecalsWithArmorDamage();
		}

	}

	protected void applyMalfunction(ShipAPI ship) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine == null) return;

		String key = "orion_malfunction" + "_" + ship.getId();
		WeaponMalfunctionData data = (WeaponMalfunctionData) engine.getCustomData().get(key);

		if (data == null) {
			data = new WeaponMalfunctionData(MALFUNCTION_DURATION);
			engine.getCustomData().put(key, data);
		}


		for (WeaponAPI weapon : ship.getAllWeapons()) {

			if (weapon.getSlot().isSystemSlot()) continue;
			if (weapon.isDecorative()) continue;
			if (weapon.getType() == WeaponType.MISSILE) continue;


			if (random.nextFloat() < MALFUNCTION_CHANCE) {
				weapon.disable();

			}
		}


		data.interval.setElapsed(0f);
	}

	@Override
	public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
		if (ship.getEngineController().isFlamedOut() || ship.getEngineController().isFlamingOut()) {
			return false;
		}
		return super.isUsable(system, ship);
	}
}








