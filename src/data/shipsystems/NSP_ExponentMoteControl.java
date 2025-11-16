package data.shipsystems;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

public class NSP_ExponentMoteControl implements MissileAIPlugin {

	public static float MAX_FLOCK_RANGE = 500;
	public static float MAX_HARD_AVOID_RANGE = 200;
	public static float AVOID_RANGE = 50;
	public static float COHESION_RANGE = 100;

	public static float ATTRACTOR_LOCK_STOP_FLOCKING_ADD = 300f;

	protected MissileAPI missile;

	protected IntervalUtil tracker = new IntervalUtil(0.05f, 0.1f);

	protected IntervalUtil updateListTracker = new IntervalUtil(0.05f, 0.1f);
	protected List<MissileAPI> missileList = new ArrayList<MissileAPI>();
	protected List<CombatEntityAPI> hardAvoidList = new ArrayList<CombatEntityAPI>();

	protected float r;

	protected CombatEntityAPI target;
	protected Object data; // Changed to Object to avoid class casting issues

	public NSP_ExponentMoteControl(MissileAPI missile) {
		this.missile = missile;
		r = (float) Math.random();
		elapsed = -(float) Math.random() * 0.5f;

		data = NSP_ExponentMoteControl.getSharedData(missile.getSource());

		updateHardAvoidList();
	}

	// FIXED: Use the hullmod's SharedMoteAIData class
	private static Object getSharedData(ShipAPI source) {
		if (source == null) return null;

		String key = source.getId() + "_temporal_motes_shared";
		return Global.getCombatEngine().getCustomData().get(key);
	}

	// Helper methods to safely access the shared data
	private ShipAPI getAttractorLock() {
		if (data == null) return null;
		try {
			// Use reflection to access the fields to avoid class casting issues
			java.lang.reflect.Field attractorLockField = data.getClass().getField("attractorLock");
			return (ShipAPI) attractorLockField.get(data);
		} catch (Exception e) {
			return null;
		}
	}

	private Vector2f getAttractorTarget() {
		if (data == null) return null;
		try {
			java.lang.reflect.Field attractorTargetField = data.getClass().getField("attractorTarget");
			return (Vector2f) attractorTargetField.get(data);
		} catch (Exception e) {
			return null;
		}
	}

	private float getAttractorRemaining() {
		if (data == null) return 0f;
		try {
			java.lang.reflect.Field attractorRemainingField = data.getClass().getField("attractorRemaining");
			return attractorRemainingField.getFloat(data);
		} catch (Exception e) {
			return 0f;
		}
	}

	@SuppressWarnings("unchecked")
	private List<MissileAPI> getMotes() {
		if (data == null) return new ArrayList<MissileAPI>();
		try {
			java.lang.reflect.Field motesField = data.getClass().getField("motes");
			return (List<MissileAPI>) motesField.get(data);
		} catch (Exception e) {
			return new ArrayList<MissileAPI>();
		}
	}

	private float getDataElapsed() {
		if (data == null) return 0f;
		try {
			java.lang.reflect.Field elapsedField = data.getClass().getField("elapsed");
			return elapsedField.getFloat(data);
		} catch (Exception e) {
			return 0f;
		}
	}

	public void updateHardAvoidList() {
		hardAvoidList.clear();

		CollisionGridAPI grid = Global.getCombatEngine().getAiGridShips();
		Iterator<Object> iter = grid.getCheckIterator(missile.getLocation(), MAX_HARD_AVOID_RANGE * 2f, MAX_HARD_AVOID_RANGE * 2f);
		while (iter.hasNext()) {
			Object o = iter.next();
			if (!(o instanceof ShipAPI)) continue;

			ShipAPI ship = (ShipAPI) o;

			if (ship.isFighter()) continue;
			hardAvoidList.add(ship);
		}

		grid = Global.getCombatEngine().getAiGridAsteroids();
		iter = grid.getCheckIterator(missile.getLocation(), MAX_HARD_AVOID_RANGE * 2f, MAX_HARD_AVOID_RANGE * 2f);
		while (iter.hasNext()) {
			Object o = iter.next();
			if (!(o instanceof CombatEntityAPI)) continue;

			CombatEntityAPI asteroid = (CombatEntityAPI) o;
			hardAvoidList.add(asteroid);
		}
	}

	public void doFlocking() {
		if (missile.getSource() == null) return;

		ShipAPI source = missile.getSource();
		CombatEngineAPI engine = Global.getCombatEngine();

		float avoidRange = AVOID_RANGE;
		float cohesionRange = COHESION_RANGE;

		float sourceRejoin = source.getCollisionRadius() + 200f;

		float sourceRepel = source.getCollisionRadius() + 50f;
		float sourceCohesion = source.getCollisionRadius() + 600f;

		float sin = (float) Math.sin(getDataElapsed() * 1f);
		float mult = 1f + sin * 0.25f;
		avoidRange *= mult;

		Vector2f total = new Vector2f();
		Vector2f attractor = getAttractorLoc();

		if (attractor != null) {
			float dist = Misc.getDistance(missile.getLocation(), attractor);
			Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(missile.getLocation(), attractor));
			float f = dist / 200f;
			if (f > 1f) f = 1f;
			dir.scale(f * 3f);
			Vector2f.add(total, dir, total);

			avoidRange *= 3f;
		}

		boolean hardAvoiding = false;
		for (CombatEntityAPI other : hardAvoidList) {
			float dist = Misc.getDistance(missile.getLocation(), other.getLocation());
			float hardAvoidRange = other.getCollisionRadius() + avoidRange + 50f;
			if (dist < hardAvoidRange) {
				Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(other.getLocation(), missile.getLocation()));
				float f = 1f - dist / (hardAvoidRange);
				dir.scale(f * 5f);
				Vector2f.add(total, dir, total);
				hardAvoiding = f > 0.5f;
			}
		}

		List<MissileAPI> motes = getMotes();
		for (MissileAPI otherMissile : motes) {
			if (otherMissile == missile) continue;

			float dist = Misc.getDistance(missile.getLocation(), otherMissile.getLocation());

			float w = otherMissile.getMaxHitpoints();
			w = 1f;

			float currCohesionRange = cohesionRange;

			if (dist < avoidRange && otherMissile != missile && !hardAvoiding) {
				Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(otherMissile.getLocation(), missile.getLocation()));
				float f = 1f - dist / avoidRange;
				dir.scale(f * w);
				Vector2f.add(total, dir, total);
			}

			if (dist < currCohesionRange) {
				Vector2f dir = new Vector2f(otherMissile.getVelocity());
				Misc.normalise(dir);
				float f = 1f - dist / currCohesionRange;
				dir.scale(f * w);
				Vector2f.add(total, dir, total);
			}
		}

		if (missile.getSource() != null) {
			float dist = Misc.getDistance(missile.getLocation(), source.getLocation());
			if (dist > sourceRejoin) {
				Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(missile.getLocation(), source.getLocation()));
				float f = dist / (sourceRejoin  + 400f) - 1f;
				dir.scale(f * 0.5f);

				Vector2f.add(total, dir, total);
			}

			if (dist < sourceRepel) {
				Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(source.getLocation(), missile.getLocation()));
				float f = 1f - dist / sourceRepel;
				dir.scale(f * 5f);
				Vector2f.add(total, dir, total);
			}

			if (dist < sourceCohesion && source.getVelocity().length() > 20f) {
				Vector2f dir = new Vector2f(source.getVelocity());
				Misc.normalise(dir);
				float f = 1f - dist / sourceCohesion;
				dir.scale(f * 1f);
				Vector2f.add(total, dir, total);
			}

			// if not strongly going anywhere, circle the source ship; only kicks in for lone motes
			if (total.length() <= 0.05f) {
				float offset = r > 0.5f ? 90f : -90f;
				Vector2f dir = Misc.getUnitVectorAtDegreeAngle(
						Misc.getAngleInDegrees(missile.getLocation(), source.getLocation()) + offset);
				float f = 1f;
				dir.scale(f * 1f);
				Vector2f.add(total, dir, total);
			}
		}

		if (total.length() > 0) {
			float dir = Misc.getAngleInDegrees(total);
			engine.headInDirectionWithoutTurning(missile, dir, 10000);

			if (r > 0.5f) {
				missile.giveCommand(ShipCommand.TURN_LEFT);
			} else {
				missile.giveCommand(ShipCommand.TURN_RIGHT);
			}
			missile.getEngineController().forceShowAccelerating();
		}
	}

	//public void accumulate(FlockingData data, Vector2f )


	protected IntervalUtil flutterCheck = new IntervalUtil(2f, 4f);
	protected FaderUtil currFlutter = null;
	protected float flutterRemaining = 0f;

	protected float elapsed = 0f;
	public void advance(float amount) {
		if (missile.isFizzling()) return;
		if (missile.getSource() ==  null) return;

		elapsed += amount;

		updateListTracker.advance(amount);
		if (updateListTracker.intervalElapsed()) {
			updateHardAvoidList();
		}

		// NEW: Prioritize target selection in this order:
		// 1. Ship's current selected target (highest priority)
		// 2. Shared attractor target
		// 3. Closest valid enemy ship
		ShipAPI source = missile.getSource();
		ShipAPI shipTarget = source.getShipTarget();

		// Check if ship's current target is valid
		if (shipTarget != null && shipTarget.isAlive() && !shipTarget.isPhased() &&
				!shipTarget.isHulk() && shipTarget.getOwner() != source.getOwner() &&
				shipTarget.getHullSize() != ShipAPI.HullSize.FIGHTER) {
			target = shipTarget;
		}
		// Otherwise use shared attractor target (with null check)
		else {
			ShipAPI attractorLock = getAttractorLock();
			if (attractorLock != null && attractorLock.isAlive() && !attractorLock.isPhased()) {
				target = attractorLock;
			}
		}

		if (elapsed >= 0.5f) {

			boolean wantToFlock = !isTargetValid();
			ShipAPI attractorLock = getAttractorLock();
			if (attractorLock != null) {
				float dist = Misc.getDistance(missile.getLocation(), attractorLock.getLocation());
				if (dist > attractorLock.getCollisionRadius() + ATTRACTOR_LOCK_STOP_FLOCKING_ADD) {
					wantToFlock = true;
				}
			}

			if (wantToFlock) {
				doFlocking();
			} else {
				CombatEngineAPI engine = Global.getCombatEngine();
				Vector2f targetLoc = engine.getAimPointWithLeadForAutofire(missile, 1.5f, target, 50);
				engine.headInDirectionWithoutTurning(missile,
						Misc.getAngleInDegrees(missile.getLocation(), targetLoc),
						10000);
				if (r > 0.5f) {
					missile.giveCommand(ShipCommand.TURN_LEFT);
				} else {
					missile.giveCommand(ShipCommand.TURN_RIGHT);
				}
				missile.getEngineController().forceShowAccelerating();
			}
		}

		tracker.advance(amount);
		if (tracker.intervalElapsed()) {
			if (elapsed >= 0.5f) {
				acquireNewTargetIfNeeded();
			}
		}
	}


	@SuppressWarnings("unchecked")
	protected boolean isTargetValid() {
		if (target == null || (target instanceof ShipAPI && ((ShipAPI)target).isPhased())) {
			return false;
		}
		CombatEngineAPI engine = Global.getCombatEngine();

		if (target != null && target instanceof ShipAPI && ((ShipAPI)target).isHulk()) return false;

		List list = null;
		if (target instanceof ShipAPI) {
			list = engine.getShips();
		} else {
			list = engine.getMissiles();
		}
		return target != null && list.contains(target) && target.getOwner() != missile.getOwner();
	}

	// NEW: Modified to prioritize ship's current target, then shared attractor, then closest enemy ship
	protected void acquireNewTargetIfNeeded() {
		ShipAPI source = missile.getSource();

		// Priority 1: Ship's current selected target
		ShipAPI shipTarget = source.getShipTarget();
		if (shipTarget != null && shipTarget.isAlive() && !shipTarget.isPhased() &&
				!shipTarget.isHulk() && shipTarget.getOwner() != source.getOwner() &&
				shipTarget.getHullSize() != ShipAPI.HullSize.FIGHTER) {
			target = shipTarget;
			return;
		}

		// Priority 2: Shared attractor target
		ShipAPI attractorLock = getAttractorLock();
		if (attractorLock != null && attractorLock.isAlive() && !attractorLock.isPhased()) {
			target = attractorLock;
			return;
		}

		// Priority 3: Find closest valid enemy ship
		CombatEngineAPI engine = Global.getCombatEngine();

		int owner = missile.getOwner();

		int maxMotesPerTarget = 6; // Increased since we're only targeting ships now
		float maxDistFromSourceShip = 2500f; // Increased range for more aggressive behavior
		float maxDistFromAttractor = 2500f; // Increased range

		float minDist = Float.MAX_VALUE;
		CombatEntityAPI closest = null;

		// Only look for enemy ships (excluding fighters)
		for (ShipAPI other : engine.getShips()) {
			if (other.getOwner() == owner) continue;
			if (other.getOwner() == 100) continue;
			if (other.isFighter()) continue; // Explicitly exclude fighters
			if (other.isPhased()) continue;
			if (other.isHulk()) continue;

			float distToTarget = Misc.getDistance(missile.getLocation(), other.getLocation());

			if (distToTarget > minDist) continue;
			if (distToTarget > 3000 && !engine.isAwareOf(owner, other)) continue;

			float distFromAttractor = Float.MAX_VALUE;
			Vector2f attractorTarget = getAttractorTarget();
			if (attractorTarget != null) {
				distFromAttractor = Misc.getDistance(other.getLocation(), attractorTarget);
			}
			float distFromSource = Misc.getDistance(other.getLocation(), missile.getSource().getLocation());
			if (distFromSource > maxDistFromSourceShip &&
					distFromAttractor > maxDistFromAttractor) continue;

			if (getNumMotesTargeting(other) >= maxMotesPerTarget) continue;
			if (distToTarget < minDist) {
				closest = other;
				minDist = distToTarget;
			}
		}

		// Removed missile and fighter targeting completely

		target = closest;
	}

	protected int getNumMotesTargeting(CombatEntityAPI other) {
		List<MissileAPI> motes = getMotes();
		int count = 0;
		for (MissileAPI mote : motes) {
			if (mote == missile) continue;
			if (mote.getUnwrappedMissileAI() instanceof NSP_ExponentMoteControl) {
				NSP_ExponentMoteControl ai = (NSP_ExponentMoteControl) mote.getUnwrappedMissileAI();
				if (ai.getTarget() == other) {
					count++;
				}
			}
		}
		return count;
	}

	public Vector2f getAttractorLoc() {
		Vector2f attractorTarget = getAttractorTarget();
		ShipAPI attractorLock = getAttractorLock();

		Vector2f attractor = null;
		if (attractorTarget != null) {
			attractor = attractorTarget;
			if (attractorLock != null) {
				attractor = attractorLock.getLocation();
			}
		}
		return attractor;
	}

	public CombatEntityAPI getTarget() {
		return target;
	}

	public void setTarget(CombatEntityAPI target) {
		this.target = target;
	}
	public void render() {

	}
}