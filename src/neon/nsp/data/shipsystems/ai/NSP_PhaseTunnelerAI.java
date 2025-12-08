package neon.nsp.data.shipsystems.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import java.util.*;

public class NSP_PhaseTunnelerAI implements ShipSystemAIScript {

    private CombatEngineAPI engine;
    private ShipwideAIFlags flags;
    private ShipAPI ship;
    private ShipSystemAPI system;

    // Aggression parameters
    private float aggressionLevel = 1.0f;
    private float lastTeleportTime = 0f;
    private float minTeleportCooldown = 3f;
    private float preferredTeleportRange = 400f;
    private float maxTeleportDistance = 2000f;

    // AI state
    private ShipAPI primaryTarget = null;
    private ShipAPI secondaryTarget = null;
    private ShipAPI emergencyTarget = null;
    private final IntervalUtil decisionInterval = new IntervalUtil(0.3f, 0.5f);
    private final IntervalUtil targetUpdateInterval = new IntervalUtil(0.8f, 1.2f);
    private final IntervalUtil aggressionUpdateInterval = new IntervalUtil(1f, 1.5f);

    // Target priority weights by ship size
    private static final Map<ShipAPI.HullSize, Float> SIZE_PRIORITY = new HashMap<>();
    static {
        SIZE_PRIORITY.put(ShipAPI.HullSize.CAPITAL_SHIP, 5.0f);    // Highest priority
        SIZE_PRIORITY.put(ShipAPI.HullSize.CRUISER, 4.0f);
        SIZE_PRIORITY.put(ShipAPI.HullSize.DESTROYER, 3.0f);
        SIZE_PRIORITY.put(ShipAPI.HullSize.FRIGATE, 2.0f);
        SIZE_PRIORITY.put(ShipAPI.HullSize.FIGHTER, 1.0f);         // Lowest priority
    }

    // Teleport range modifiers by target size
    private static final Map<ShipAPI.HullSize, Float> SIZE_TELEPORT_RANGE = new HashMap<>();
    static {
        SIZE_TELEPORT_RANGE.put(ShipAPI.HullSize.CAPITAL_SHIP, 1.5f);    // Keep distance from capitals
        SIZE_TELEPORT_RANGE.put(ShipAPI.HullSize.CRUISER, 1.2f);
        SIZE_TELEPORT_RANGE.put(ShipAPI.HullSize.DESTROYER, 1.0f);
        SIZE_TELEPORT_RANGE.put(ShipAPI.HullSize.FRIGATE, 0.8f);         // Can get closer to frigates
        SIZE_TELEPORT_RANGE.put(ShipAPI.HullSize.FIGHTER, 0.5f);         // Very close for fighters
    }

    // Damage assessment weights by target size
    private static final Map<ShipAPI.HullSize, Float> SIZE_DAMAGE_PRIORITY = new HashMap<>();
    static {
        SIZE_DAMAGE_PRIORITY.put(ShipAPI.HullSize.CAPITAL_SHIP, 3.0f);    // Capital damage is very valuable
        SIZE_DAMAGE_PRIORITY.put(ShipAPI.HullSize.CRUISER, 2.5f);
        SIZE_DAMAGE_PRIORITY.put(ShipAPI.HullSize.DESTROYER, 2.0f);
        SIZE_DAMAGE_PRIORITY.put(ShipAPI.HullSize.FRIGATE, 1.5f);
        SIZE_DAMAGE_PRIORITY.put(ShipAPI.HullSize.FIGHTER, 0.5f);         // Minimal value for fighter damage
    }

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
        this.flags = flags;
        this.engine = engine;

        // Initialize aggression based on ship status
        updateAggressionLevel();
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine.isPaused() || ship.getOriginalOwner() == -1) return;

        decisionInterval.advance(amount);
        targetUpdateInterval.advance(amount);
        aggressionUpdateInterval.advance(amount);

        // Update aggression level periodically
        if (aggressionUpdateInterval.intervalElapsed()) {
            updateAggressionLevel();
        }

        // Update target selection with size-based priority
        if (targetUpdateInterval.intervalElapsed()) {
            updateTargetsBySize();
        }

        // Make teleport decisions
        if (decisionInterval.intervalElapsed()) {
            evaluateTeleport();
        }

        // Manage AI flags for aggressive behavior
        manageAggressiveFlags();
    }

    private void updateAggressionLevel() {
        float hullRatio = ship.getHullLevel();
        float fluxRatio = ship.getFluxLevel();
        float timeSinceLastTeleport = engine.getTotalElapsedTime(false) - lastTeleportTime;

        // Start with base aggression
        aggressionLevel = 0.5f;

        // Increase aggression if we're healthy
        if (hullRatio > 0.7f) aggressionLevel += 0.2f;
        if (fluxRatio < 0.3f) aggressionLevel += 0.15f;

        // Increase aggression if we haven't teleported recently
        if (timeSinceLastTeleport > minTeleportCooldown * 2) {
            aggressionLevel += 0.25f;
        }

        // Decrease aggression if we're badly damaged
        if (hullRatio < 0.3f) aggressionLevel -= 0.3f;
        if (fluxRatio > 0.85f) aggressionLevel -= 0.2f;

        // Clamp to 0-1 range
        aggressionLevel = Math.max(0.1f, Math.min(1.0f, aggressionLevel));

        // Adjust teleport parameters based on aggression
        minTeleportCooldown = 5f - (aggressionLevel * 2f);
        preferredTeleportRange = 300f + (aggressionLevel * 200f);
    }

    private void updateTargetsBySize() {
        primaryTarget = null;
        secondaryTarget = null;
        emergencyTarget = null;

        List<ShipAPI> allTargets = new ArrayList<>();
        Map<ShipAPI, Float> targetScores = new HashMap<>();

        // First pass: score all enemies by size priority and other factors
        for (ShipAPI enemy : engine.getShips()) {
            if (enemy.getOwner() == ship.getOwner()) continue;
            if (!enemy.isAlive()) continue;

            float score = calculateSizeBasedTargetScore(enemy);
            targetScores.put(enemy, score);
            allTargets.add(enemy);
        }

        if (allTargets.isEmpty()) return;

        // Sort targets by score (highest first)
        allTargets.sort((t1, t2) -> Float.compare(targetScores.get(t2), targetScores.get(t1)));

        // Assign targets based on priority
        for (ShipAPI enemy : allTargets) {
            // Primary target: highest score overall
            if (primaryTarget == null) {
                primaryTarget = enemy;
                continue;
            }

            // Secondary target: different size class than primary, or high-value backup
            if (secondaryTarget == null &&
                    (enemy.getHullSize() != primaryTarget.getHullSize() ||
                            targetScores.get(enemy) > targetScores.get(primaryTarget) * 0.7f)) {
                secondaryTarget = enemy;
                continue;
            }

            // Emergency target: low health and close range, regardless of size
            if (emergencyTarget == null &&
                    enemy.getHullLevel() < 0.3f &&
                    MathUtils.getDistance(ship.getLocation(), enemy.getLocation()) < 800f) {
                emergencyTarget = enemy;
                break;
            }
        }
    }

    private float calculateSizeBasedTargetScore(ShipAPI target) {
        float score = 0f;
        float distance = MathUtils.getDistance(ship.getLocation(), target.getLocation());

        // SIZE-BASED PRIORITY (most important factor)
        Float sizePriority = SIZE_PRIORITY.get(target.getHullSize());
        if (sizePriority != null) {
            score += sizePriority * 3.0f; // Triple weight for size priority
        }

        // Distance score (prefer closer targets, but weighted by size)
        float distanceScore = 1f - Math.min(1f, distance / 2000f);
        Float sizeRangeMod = SIZE_TELEPORT_RANGE.get(target.getHullSize());
        if (sizeRangeMod != null) {
            distanceScore *= sizeRangeMod; // Adjust distance preference by target size
        }
        score += distanceScore * 2f;

        // Vulnerability score (weighted by size damage priority)
        float healthScore = 1f - target.getHullLevel();
        float fluxScore = target.getFluxLevel();
        Float damagePriority = SIZE_DAMAGE_PRIORITY.get(target.getHullSize());
        if (damagePriority != null) {
            score += healthScore * 1.5f * damagePriority;
            score += fluxScore * 1.2f * damagePriority;
        } else {
            score += healthScore * 1.5f;
            score += fluxScore * 1.2f;
        }

        // Isolation score (more important for larger targets)
        float isolationScore = calculateIsolationScore(target);
        if (target.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) {
            score += isolationScore * 1.5f; // Capitals get isolation bonus
        } else if (target.getHullSize() == ShipAPI.HullSize.CRUISER) {
            score += isolationScore * 1.2f; // Cruisers get moderate isolation bonus
        } else {
            score += isolationScore * 1.0f;
        }

        // Facing score (prefer targets facing away from us)
        float angleToTarget = VectorUtils.getAngle(ship.getLocation(), target.getLocation());
        float targetFacing = target.getFacing();
        float angleDiff = Math.abs(MathUtils.getShortestRotation(targetFacing, angleToTarget + 180f));
        float rearScore = 1f - Math.min(1f, angleDiff / 180f);
        score += rearScore * 1.0f;

        // Bonus for targets that are attacking us or allies
        if (isTargetEngaging(target)) {
            score += 0.5f;
        }

        // Penalty for targets with strong PD (for our teleporter safety)
        if (hasStrongPointDefense(target)) {
            score -= 0.3f;
        }

        return score;
    }

    private boolean isTargetEngaging(ShipAPI target) {
        // Check if target is actively engaging us
        if (target.getShipTarget() == ship) return true;

        // Check if target is firing weapons that could hit us
        for (WeaponAPI weapon : target.getAllWeapons()) {
            if (weapon.isFiring()) {
                // Check weapon arc and range to see if it could hit us
                float weaponRange = weapon.getRange();
                float distanceToUs = MathUtils.getDistance(target.getLocation(), ship.getLocation());

                // If we're within range, check if weapon is aimed in our direction
                if (distanceToUs <= weaponRange + ship.getCollisionRadius()) {
                    // For beam weapons, check if beam is hitting us
                    if (weapon.isBeam()) {
                        BeamAPI beam = (BeamAPI) weapon.getBeams();
                        if (beam != null) {
                            // Check if beam is colliding with our ship
                            if (MathUtils.isWithinRange(beam.getFrom(), ship.getLocation(), ship.getCollisionRadius()) ||
                                    MathUtils.isWithinRange(beam.getTo(), ship.getLocation(), ship.getCollisionRadius())) {
                                return true;
                            }
                        }
                    }
                    // For projectile weapons, check firing direction
                    else {
                        float weaponFacing = weapon.getCurrAngle();
                        float angleToUs = VectorUtils.getAngle(target.getLocation(), ship.getLocation());
                        float angleDiff = Math.abs(MathUtils.getShortestRotation(weaponFacing, angleToUs));

                        // If weapon is firing roughly in our direction (within 45 degrees)
                        if (angleDiff < 45f) {
                            return true;
                        }
                    }
                }
            }
        }

        // Check nearby allies (within 500 units)
        for (ShipAPI ally : engine.getShips()) {
            if (ally.getOwner() != ship.getOwner()) continue;
            if (!ally.isAlive()) continue;
            if (ally == ship) continue;

            float allyDistance = MathUtils.getDistance(ally.getLocation(), target.getLocation());
            if (allyDistance < 500f) {
                // Check if target is aiming at this ally
                if (target.getShipTarget() == ally) {
                    return true;
                }

                // Check target's facing toward ally
                float targetFacing = target.getFacing();
                float angleToAlly = VectorUtils.getAngle(target.getLocation(), ally.getLocation());
                float angleDiff = Math.abs(MathUtils.getShortestRotation(targetFacing, angleToAlly));

                // If target is facing toward ally (within 90 degrees) and close
                if (angleDiff < 90f && allyDistance < 800f) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasStrongPointDefense(ShipAPI target) {
        // Check if target has significant point defense weapons
        int pdWeapons = 0;
        for (WeaponAPI weapon : target.getAllWeapons()) {
            if (weapon.hasAIHint(WeaponAPI.AIHints.PD) ||
                    weapon.hasAIHint(WeaponAPI.AIHints.PD_ALSO)) {
                pdWeapons++;
                if (pdWeapons >= 3) return true; // Consider strong PD if 3+ PD weapons
            }
        }
        return false;
    }

    private float calculateIsolationScore(ShipAPI target) {
        int nearbyAllies = 0;
        float maxDistance = 800f;

        for (ShipAPI other : engine.getShips()) {
            if (other.getOwner() != target.getOwner()) continue;
            if (other == target) continue;

            float distance = MathUtils.getDistance(target.getLocation(), other.getLocation());
            if (distance < maxDistance) {
                nearbyAllies++;
            }
        }
        return 1f - Math.min(1f, nearbyAllies / 5f);
    }

    private void evaluateTeleport() {
        if (!canTeleport()) return;

        // Try emergency target first (low health, close range)
        if (emergencyTarget != null && emergencyTarget.getHullLevel() < 0.3f) {
            Vector2f emergencyLocation = findEmergencyTeleportLocation(emergencyTarget);
            if (emergencyLocation != null && evaluateLocationScore(emergencyLocation, emergencyTarget) > 0.4f) {
                setTeleportTarget(emergencyLocation);
                executeTeleport();
                return;
            }
        }

        // Try primary target (size-based priority)
        if (primaryTarget != null && aggressionLevel > 0.4f) {
            Vector2f primaryLocation = findSizeBasedTeleportLocation(primaryTarget);
            if (primaryLocation != null && evaluateLocationScore(primaryLocation, primaryTarget) > 0.6f) {
                setTeleportTarget(primaryLocation);
                executeTeleport();
                return;
            }
        }

        // Try secondary target (backup target)
        if (secondaryTarget != null && aggressionLevel > 0.6f) {
            Vector2f secondaryLocation = findSizeBasedTeleportLocation(secondaryTarget);
            if (secondaryLocation != null && evaluateLocationScore(secondaryLocation, secondaryTarget) > 0.5f) {
                setTeleportTarget(secondaryLocation);
                executeTeleport();
                return;
            }
        }

        // Try flanking teleport against any capital or cruiser
        if (aggressionLevel > 0.7f) {
            Vector2f flankingLocation = findFlankingTeleportLocation();
            if (flankingLocation != null && evaluateLocationScore(flankingLocation, null) > 0.5f) {
                setTeleportTarget(flankingLocation);
                executeTeleport();
                return;
            }
        }

        // Emergency retreat if very damaged and not aggressive
        if (ship.getHullLevel() < 0.25f && aggressionLevel < 0.4f) {
            Vector2f retreatLocation = findRetreatLocation();
            if (retreatLocation != null) {
                setTeleportTarget(retreatLocation);
                executeTeleport();
                return;
            }
        }
    }

    private Vector2f findSizeBasedTeleportLocation(ShipAPI target) {
        float targetFacing = target.getFacing();
        float rangeModifier = 1.0f;

        // Adjust teleport range based on target size
        Float sizeRangeMod = SIZE_TELEPORT_RANGE.get(target.getHullSize());
        if (sizeRangeMod != null) {
            rangeModifier = sizeRangeMod;
        }

        float adjustedRange = preferredTeleportRange * rangeModifier;

        // For capital ships, try to stay at optimal distance
        if (target.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) {
            // Generate multiple positions around capital
            Vector2f[] capitalPositions = {
                    MathUtils.getPointOnCircumference(target.getLocation(), adjustedRange, targetFacing + 180f), // Directly behind
                    MathUtils.getPointOnCircumference(target.getLocation(), adjustedRange * 1.2f, targetFacing + 150f), // Slightly to side
                    MathUtils.getPointOnCircumference(target.getLocation(), adjustedRange * 1.2f, targetFacing + 210f), // Other side
                    MathUtils.getPointOnCircumference(target.getLocation(), adjustedRange * 0.8f, targetFacing + 90f), // Close flank
                    MathUtils.getPointOnCircumference(target.getLocation(), adjustedRange * 0.8f, targetFacing + 270f)  // Other close flank
            };

            return findBestPosition(capitalPositions, target);
        }
        // For cruisers, focus on rear and flanks
        else if (target.getHullSize() == ShipAPI.HullSize.CRUISER) {
            Vector2f[] cruiserPositions = {
                    MathUtils.getPointOnCircumference(target.getLocation(), adjustedRange, targetFacing + 180f),
                    MathUtils.getPointOnCircumference(target.getLocation(), adjustedRange * 0.9f, targetFacing + 135f),
                    MathUtils.getPointOnCircumference(target.getLocation(), adjustedRange * 0.9f, targetFacing + 225f),
                    MathUtils.getPointOnCircumference(target.getLocation(), adjustedRange * 0.7f, targetFacing + 90f),
                    MathUtils.getPointOnCircumference(target.getLocation(), adjustedRange * 0.7f, targetFacing + 270f)
            };

            return findBestPosition(cruiserPositions, target);
        }
        // For destroyers and smaller, can be more aggressive
        else {
            return findOffensiveTeleportLocation(target);
        }
    }

    private Vector2f findEmergencyTeleportLocation(ShipAPI target) {
        // For emergency low-health targets, get very close for the kill
        float targetFacing = target.getFacing();
        float closeRange = 200f; // Very close range

        Vector2f[] emergencyPositions = {
                MathUtils.getPointOnCircumference(target.getLocation(), closeRange, targetFacing + 180f), // Directly behind
                MathUtils.getPointOnCircumference(target.getLocation(), closeRange, targetFacing + 135f), // Rear quarter
                MathUtils.getPointOnCircumference(target.getLocation(), closeRange, targetFacing + 225f), // Other rear quarter
                MathUtils.getPointOnCircumference(target.getLocation(), closeRange * 1.5f, targetFacing + 90f), // Close flank
                MathUtils.getPointOnCircumference(target.getLocation(), closeRange * 1.5f, targetFacing + 270f)  // Other close flank
        };

        return findBestPosition(emergencyPositions, target);
    }

    private Vector2f findOffensiveTeleportLocation(ShipAPI target) {
        float targetFacing = target.getFacing();

        Vector2f[] offensivePositions = {
                MathUtils.getPointOnCircumference(target.getLocation(), preferredTeleportRange, targetFacing + 180f),
                MathUtils.getPointOnCircumference(target.getLocation(), preferredTeleportRange * 0.8f, targetFacing + 150f),
                MathUtils.getPointOnCircumference(target.getLocation(), preferredTeleportRange * 1.2f, targetFacing + 210f),
                MathUtils.getPointOnCircumference(target.getLocation(), preferredTeleportRange * 0.9f, targetFacing + 90f),
                MathUtils.getPointOnCircumference(target.getLocation(), preferredTeleportRange * 0.9f, targetFacing + 270f)
        };

        return findBestPosition(offensivePositions, target);
    }

    private Vector2f findBestPosition(Vector2f[] positions, ShipAPI target) {
        Vector2f bestPos = null;
        float bestScore = 0f;

        for (Vector2f testPos : positions) {
            testPos = ensureWithinBounds(testPos);
            float score = evaluateLocationScore(testPos, target);

            if (score > bestScore) {
                bestScore = score;
                bestPos = testPos;
            }
        }

        return bestPos;
    }

    private Vector2f findFlankingTeleportLocation() {
        // Find flanking positions against capital ships and cruisers first
        Vector2f bestPos = null;
        float bestScore = 0f;

        for (ShipAPI enemy : engine.getShips()) {
            if (enemy.getOwner() == ship.getOwner()) continue;
            if (!enemy.isAlive()) continue;

            // Prioritize capital ships and cruisers for flanking
            if (enemy.getHullSize() != ShipAPI.HullSize.CAPITAL_SHIP &&
                    enemy.getHullSize() != ShipAPI.HullSize.CRUISER) {
                continue;
            }

            // Check 8 positions around enemy
            for (int i = 0; i < 8; i++) {
                float angle = enemy.getFacing() + (i * 45f);
                Vector2f testPos = MathUtils.getPointOnCircumference(
                        enemy.getLocation(),
                        preferredTeleportRange * 0.7f,
                        angle
                );

                testPos = ensureWithinBounds(testPos);

                // Check if this is a flanking position
                float angleToEnemy = VectorUtils.getAngle(testPos, enemy.getLocation());
                float angleDiff = Math.abs(MathUtils.getShortestRotation(enemy.getFacing(), angleToEnemy));

                if (angleDiff > 45f && angleDiff < 135f) {
                    float score = evaluateLocationScore(testPos, enemy);

                    // Bonus for flanking capital ships
                    if (enemy.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) {
                        score += 0.3f;
                    }

                    // Safety considerations
                    int nearbyEnemies = countNearbyEnemies(testPos, 600f);
                    score -= nearbyEnemies * 0.15f;

                    int nearbyAllies = countNearbyAllies(testPos, 800f);
                    score += nearbyAllies * 0.05f;

                    if (score > bestScore) {
                        bestScore = score;
                        bestPos = testPos;
                    }
                }
            }
        }

        return bestPos;
    }

    private Vector2f findRetreatLocation() {
        Vector2f avgEnemyPos = new Vector2f();
        int enemyCount = 0;

        for (ShipAPI enemy : engine.getShips()) {
            if (enemy.getOwner() == ship.getOwner()) continue;
            if (!enemy.isAlive()) continue;

            avgEnemyPos.x += enemy.getLocation().x;
            avgEnemyPos.y += enemy.getLocation().y;
            enemyCount++;
        }

        if (enemyCount > 0) {
            avgEnemyPos.x /= enemyCount;
            avgEnemyPos.y /= enemyCount;

            float retreatAngle = VectorUtils.getAngle(avgEnemyPos, ship.getLocation());
            return MathUtils.getPointOnCircumference(ship.getLocation(), 1000f, retreatAngle);
        }

        return new Vector2f(engine.getMapWidth() / 2f, engine.getMapHeight() / 2f);
    }

    private float evaluateLocationScore(Vector2f location, ShipAPI target) {
        float score = 0.5f;

        if (target != null) {
            float distance = MathUtils.getDistance(location, target.getLocation());

            // Adjust optimal distance based on target size
            float optimalDistance = preferredTeleportRange;
            Float sizeRangeMod = SIZE_TELEPORT_RANGE.get(target.getHullSize());
            if (sizeRangeMod != null) {
                optimalDistance *= sizeRangeMod;
            }

            // Score peaks at optimal distance for the target size
            float distanceScore = 1f - Math.abs(distance - optimalDistance) / optimalDistance;
            score += Math.max(0f, distanceScore) * 0.3f;

            // Target vulnerability bonus (weighted by size)
            if (target.getFluxLevel() > 0.8f) score += 0.2f;
            if (target.getHullLevel() < 0.4f) score += 0.3f;

            // Size-based target value bonus
            Float sizePriority = SIZE_PRIORITY.get(target.getHullSize());
            if (sizePriority != null) {
                score += (sizePriority - 2f) * 0.1f; // Bonus for larger targets
            }
        }

        // Safety considerations
        int nearbyEnemies = countNearbyEnemies(location, 600f);
        score -= nearbyEnemies * 0.15f;

        int nearbyAllies = countNearbyAllies(location, 800f);
        score += nearbyAllies * 0.1f;

        // Aggression modifier
        score *= aggressionLevel;

        // Ensure within teleport range
        float distanceFromShip = MathUtils.getDistance(ship.getLocation(), location);
        if (distanceFromShip > maxTeleportDistance) {
            score *= 0.5f;
        }

        return score;
    }

    private boolean canTeleport() {
        if (system.isCoolingDown()) return false;
        if (system.isActive()) return false;
        if (ship.getFluxTracker().isOverloadedOrVenting()) return false;

        if (ship.getFluxLevel() > 0.9f - (aggressionLevel * 0.3f)) return false;

        float timeSinceLastTeleport = engine.getTotalElapsedTime(false) - lastTeleportTime;
        if (timeSinceLastTeleport < minTeleportCooldown) return false;

        return AIUtils.canUseSystemThisFrame(ship);
    }

    private void setTeleportTarget(Vector2f location) {
        location = ensureWithinBounds(location);

        Map<String, Object> teleportData = new HashMap<>();
        teleportData.put("targetX", location.x);
        teleportData.put("targetY", location.y);
        teleportData.put("facing", ship.getFacing());

        ship.setCustomData("nsp_teleport_data", teleportData);
    }

    private void executeTeleport() {
        ship.useSystem();
        lastTeleportTime = engine.getTotalElapsedTime(false);

        flags.setFlag(ShipwideAIFlags.AIFlags.PHASE_ATTACK_RUN, 2f);
        flags.setFlag(ShipwideAIFlags.AIFlags.PURSUING, 2f);
    }

    private void manageAggressiveFlags() {
        if (aggressionLevel > 0.7f) {
            flags.setFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF, 1f);
            flags.unsetFlag(ShipwideAIFlags.AIFlags.RUN_QUICKLY);

            if (primaryTarget != null && primaryTarget.getHullSize().ordinal() >= ShipAPI.HullSize.CRUISER.ordinal()) {
                // Extra aggression against capitals and cruisers
                float distance = MathUtils.getDistance(ship.getLocation(), primaryTarget.getLocation());
                if (distance < 1200f) {
                    flags.setFlag(ShipwideAIFlags.AIFlags.PHASE_ATTACK_RUN, 1f);
                }
            }
        }

        if (aggressionLevel > 0.5f && ship.getFluxLevel() < 0.9f) {
            flags.setFlag(ShipwideAIFlags.AIFlags.DO_NOT_VENT, 1f);
        }
    }

    private int countNearbyEnemies(Vector2f location, float radius) {
        int count = 0;
        for (ShipAPI enemy : engine.getShips()) {
            if (enemy.getOwner() == ship.getOwner()) continue;
            if (!enemy.isAlive()) continue;

            float distance = MathUtils.getDistance(location, enemy.getLocation());
            if (distance < radius) {
                count++;
            }
        }
        return count;
    }

    private int countNearbyAllies(Vector2f location, float radius) {
        int count = 0;
        for (ShipAPI ally : engine.getShips()) {
            if (ally.getOwner() != ship.getOwner()) continue;
            if (!ally.isAlive()) continue;
            if (ally == ship) continue;

            float distance = MathUtils.getDistance(location, ally.getLocation());
            if (distance < radius) {
                count++;
            }
        }
        return count;
    }

    private Vector2f ensureWithinBounds(Vector2f pos) {
        float margin = 200f;

        pos.x = Math.max(margin, Math.min(engine.getMapWidth() - margin, pos.x));
        pos.y = Math.max(margin, Math.min(engine.getMapHeight() - margin, pos.y));

        return pos;
    }
}