package neon.nsp.data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.MagicSubsystem;

import java.awt.*;

public class NSP_PlasmaJetsSubsystem extends MagicSubsystem {

    private static final float SPEED_BONUS = 90f;
    private static final float ACC_BONUS = 125f;
    private static final float TURN_BONUS = 20f;
    private static final float ENGINE_FLAME_EXTEND = 2f;

    // Jitter effect
    private static final Color JITTER_COLOR = new Color(255, 100, 43, 120);
    private static final Color JITTER_UNDER_COLOR = new Color(255, 40, 79, 40);
    private static final float MAX_JITTER = 0.5f;

    // AI behavior states
    private enum AIState {
        DEFENSIVE,      // Evading threats
        OFFENSIVE,      // Closing distance/attacking
        REPOSITIONING,  // Moving to better position
        IDLE           // Not actively using system
    }

    private Color jetColor = new Color(100, 255, 100, 255);
    private float effectLevel = 0f;
    private IntervalUtil aiEvaluationInterval = new IntervalUtil(0.5f, 0.8f);
    private IntervalUtil aiStateCheckInterval = new IntervalUtil(1f, 1.5f); // Less frequent state changes

    // AI decision variables
    private float timeSinceLastUse = 0f;
    private static final float MIN_REUSE_TIME = 15f; // Minimum time between uses
    private float activeTime = 0f;

    // AI state tracking
    private AIState currentAIState = AIState.IDLE;
    private float stateDuration = 0f;
    private ShipAPI primaryTarget = null;
    private Vector2f desiredPosition = null;

    // Sound management
    private String loopSoundId = null;
    private boolean soundStarted = false;

    public NSP_PlasmaJetsSubsystem(ShipAPI ship) {
        super(ship);
    }

    @Override
    public float getBaseActiveDuration() {
        return 10f; // Active for 3 seconds
    }

    @Override
    public float getBaseCooldownDuration() {
        return 30f; // 10 second cooldown
    }

    @Override
    public boolean shouldActivateAI(float amount) {
        timeSinceLastUse += amount;

        // Don't activate too frequently
        if (timeSinceLastUse < MIN_REUSE_TIME) {
            return false;
        }

        // Use interval to reduce AI calculations
        aiEvaluationInterval.advance(amount);
        if (!aiEvaluationInterval.intervalElapsed()) {
            return false;
        }

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || ship == null) return false;

        // Update AI state
        updateAIState(amount);

        // Check if we should activate based on current state
        return shouldActivateForCurrentState();
    }

    private void updateAIState(float amount) {
        aiStateCheckInterval.advance(amount);
        if (!aiStateCheckInterval.intervalElapsed()) {
            return;
        }

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        // Get current target
        ShipAPI currentTarget = ship.getShipTarget();
        primaryTarget = currentTarget;

        // Calculate threat scores
        float defensiveThreatScore = calculateDefensiveThreatScore();
        float offensiveOpportunityScore = calculateOffensiveOpportunityScore(currentTarget);
        float repositioningScore = calculateRepositioningScore(currentTarget);

        // Determine best state
        AIState newState = AIState.IDLE;
        float highestScore = 0f;

        if (defensiveThreatScore > highestScore) {
            highestScore = defensiveThreatScore;
            newState = AIState.DEFENSIVE;
        }

        if (offensiveOpportunityScore > highestScore) {
            highestScore = offensiveOpportunityScore;
            newState = AIState.OFFENSIVE;
        }

        if (repositioningScore > highestScore) {
            highestScore = repositioningScore;
            newState = AIState.REPOSITIONING;
        }

        // Change state if needed
        if (newState != currentAIState) {
            currentAIState = newState;
            stateDuration = 0f;

            // Set desired position for new state
            updateDesiredPositionForState(newState, currentTarget);
        } else {
            stateDuration += aiStateCheckInterval.getIntervalDuration();
        }

        // Random factor to prevent predictable behavior
        if (Math.random() < 0.1f && highestScore > 0) {
            // Occasionally force activation even if not optimal
            currentAIState = newState;
            updateDesiredPositionForState(newState, currentTarget);
        }
    }

    private float calculateDefensiveThreatScore() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return 0f;

        float threatScore = 0f;

        // 1. Check for incoming projectiles
        int incomingProjectiles = 0;
        float closestProjectileDist = Float.MAX_VALUE;

        for (DamagingProjectileAPI proj : engine.getProjectiles()) {
            if (proj.getOwner() != ship.getOwner() && !proj.isFading()) {
                float dist = Misc.getDistance(ship.getLocation(), proj.getLocation());

                // Check if projectile is heading toward us
                Vector2f toShip = Vector2f.sub(ship.getLocation(), proj.getLocation(), new Vector2f());
                Vector2f projVel = proj.getVelocity();

                float dot = Vector2f.dot(
                        Misc.normalise(new Vector2f(toShip)),
                        Misc.normalise(new Vector2f(projVel))
                );

                if (dist < 1000f && dot > 0.6f) { // Heading toward us
                    incomingProjectiles++;
                    closestProjectileDist = Math.min(closestProjectileDist, dist);

                    // Score based on distance and type
                    if (dist < 300f) threatScore += 12f; // Very close
                    else if (dist < 500f) threatScore += 8f;
                    else if (dist < 700f) threatScore += 5f;
                    else threatScore += 3f;
                }
            }
        }

        // 2. Check for nearby fighters/attack ships
        for (ShipAPI other : engine.getShips()) {
            if (other.getOwner() != ship.getOwner() && other.isAlive()) {
                float dist = Misc.getDistance(ship.getLocation(), other.getLocation());

                if (other.isFighter() && dist < 500f) {
                    threatScore += 6f; // Fighter threat
                } else if (dist < 400f) {
                    // Any enemy ship very close
                    threatScore += 8f;

                    // Additional threat if they're facing us
                    float angleDiff = Math.abs(Misc.getAngleDiff(ship.getFacing(), other.getFacing() + 180f));
                    if (angleDiff < 45f) {
                        threatScore += 4f; // They're pointing at us
                    }
                }
            }
        }

        // 3. Check ship status
        if (ship.getFluxTracker().isOverloadedOrVenting()) {
            threatScore += 10f; // Critical need to evade
        } else if (ship.getFluxLevel() > 0.8f) {
            threatScore += 6f; // High flux - need to maneuver
        }

        if (ship.getHullLevel() < 0.3f) {
            threatScore += 12f; // Low hull - desperate evasion
        } else if (ship.getHullLevel() < 0.6f) {
            threatScore += 6f;
        }

        // 4. Check if we're currently being targeted
        if (incomingProjectiles > 3) {
            threatScore += incomingProjectiles * 2f;
        }

        return threatScore;
    }

    private float calculateOffensiveOpportunityScore(ShipAPI target) {
        if (target == null || !target.isAlive()) return 0f;

        float opportunityScore = 0f;
        float dist = Misc.getDistance(ship.getLocation(), target.getLocation());

        // 1. Target vulnerability
        if (target.getFluxTracker().isOverloadedOrVenting()) {
            opportunityScore += 15f; // Perfect time to attack
        } else if (target.getFluxLevel() > 0.7f) {
            opportunityScore += 8f; // Target under pressure
        }

        if (target.getHullLevel() < 0.3f) {
            opportunityScore += 12f; // Finish them off
        } else if (target.getHullLevel() < 0.6f) {
            opportunityScore += 6f;
        }

        // 2. Positioning opportunity
        float optimalRange = aiData.getAverageWeaponRange(false);

        if (dist > optimalRange * 1.5f) {
            opportunityScore += 10f; // Too far - need to close distance
        } else if (dist < optimalRange * 0.8f && dist > optimalRange * 0.4f) {
            opportunityScore += 8f; // Good attack range
        }

        // 3. Flanking opportunity
        // Check if we can get to target's rear/side
        float angleToTarget = Misc.getAngleInDegrees(ship.getLocation(), target.getLocation());
        float targetFacing = target.getFacing();
        float angleDiff = Math.abs(Misc.getAngleDiff(angleToTarget, targetFacing + 180f));

        if (angleDiff > 90f) {
            opportunityScore += 6f; // We're in front of target - good position
        }

        // 4. Target is isolated
        int nearbyAllies = 0;
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null) {
            for (ShipAPI other : engine.getShips()) {
                if (other.getOwner() == target.getOwner() && other != target && other.isAlive()) {
                    float allyDist = Misc.getDistance(target.getLocation(), other.getLocation());
                    if (allyDist < 600f) {
                        nearbyAllies++;
                    }
                }
            }
        }

        if (nearbyAllies == 0) {
            opportunityScore += 5f; // Target is isolated
        }

        return opportunityScore;
    }

    private float calculateRepositioningScore(ShipAPI target) {
        float repositionScore = 0f;

        // 1. Current position is bad
        int enemiesTooClose = 0;
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null) {
            for (ShipAPI other : engine.getShips()) {
                if (other.getOwner() != ship.getOwner() && other.isAlive()) {
                    float dist = Misc.getDistance(ship.getLocation(), other.getLocation());
                    if (dist < 300f) {
                        enemiesTooClose++;
                    }
                }
            }
        }

        if (enemiesTooClose > 1) {
            repositionScore += enemiesTooClose * 4f; // Too many enemies close
        }

        // 2. We're out of position relative to allies
        int nearbyAllies = 0;
        float averageAllyDist = 0f;

        if (engine != null) {
            for (ShipAPI other : engine.getShips()) {
                if (other.getOwner() == ship.getOwner() && other != ship && other.isAlive()) {
                    float dist = Misc.getDistance(ship.getLocation(), other.getLocation());
                    if (dist < 800f) {
                        nearbyAllies++;
                        averageAllyDist += dist;
                    }
                }
            }
        }

        if (nearbyAllies > 0) {
            averageAllyDist /= nearbyAllies;
            if (averageAllyDist > 600f) {
                repositionScore += 8f; // Too far from allies
            } else if (averageAllyDist < 200f) {
                repositionScore += 6f; // Too close to allies (bunching up)
            }
        }

        // 3. We're in a bad firing position
        if (target != null) {
            float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
            float optimalRange = aiData.getAverageWeaponRange(false);

            // Check firing arcs
            boolean canFireAtTarget = false;
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (weapon.isDisabled() || weapon.getType() == WeaponAPI.WeaponType.DECORATIVE) continue;

                float arc = weapon.getArc();
                float angleToTarget = Misc.getAngleInDegrees(ship.getLocation(), target.getLocation());
                float weaponAngle = ship.getFacing() + weapon.getSlot().getAngle();

                if (Math.abs(Misc.getAngleDiff(angleToTarget, weaponAngle)) <= arc / 2f) {
                    canFireAtTarget = true;
                    break;
                }
            }

            if (!canFireAtTarget && dist < optimalRange * 1.2f) {
                repositionScore += 10f; // Can't fire at target but should be able to
            }
        }

        return repositionScore;
    }

    private void updateDesiredPositionForState(AIState state, ShipAPI target) {
        desiredPosition = null;

        if (state == AIState.DEFENSIVE) {
            // Calculate evasion vector
            desiredPosition = calculateEvasionPosition();
        } else if (state == AIState.OFFENSIVE && target != null) {
            // Calculate attack position
            desiredPosition = calculateAttackPosition(target);
        } else if (state == AIState.REPOSITIONING) {
            // Calculate better tactical position
            desiredPosition = calculateRepositionPosition(target);
        }
    }

    private Vector2f calculateEvasionPosition() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return null;

        Vector2f currentPos = ship.getLocation();
        Vector2f evasionVector = new Vector2f();

        // Collect threat directions
        int threatCount = 0;

        for (DamagingProjectileAPI proj : engine.getProjectiles()) {
            if (proj.getOwner() != ship.getOwner() && !proj.isFading()) {
                float dist = Misc.getDistance(currentPos, proj.getLocation());
                if (dist < 600f) {
                    // Move perpendicular to threat
                    Vector2f threatDir = Vector2f.sub(proj.getLocation(), currentPos, new Vector2f());
                    threatDir.normalise();

                    // Perpendicular vector (rotate 90 degrees)
                    Vector2f evadeDir = new Vector2f(-threatDir.y, threatDir.x);

                    // Randomize direction sometimes
                    if (Math.random() < 0.3f) {
                        evadeDir.scale(-1f);
                    }

                    Vector2f.add(evasionVector, evadeDir, evasionVector);
                    threatCount++;
                }
            }
        }

        if (threatCount > 0) {
            evasionVector.scale(1f / threatCount);
            evasionVector.normalise();
            evasionVector.scale(500f); // Evade 500 units in calculated direction

            return new Vector2f(currentPos.x + evasionVector.x, currentPos.y + evasionVector.y);
        }

        // Default: move away from nearest enemy
        ShipAPI nearestEnemy = findNearestEnemy();
        if (nearestEnemy != null) {
            Vector2f awayDir = Vector2f.sub(currentPos, nearestEnemy.getLocation(), new Vector2f());
            awayDir.normalise();
            awayDir.scale(400f);
            return new Vector2f(currentPos.x + awayDir.x, currentPos.y + awayDir.y);
        }

        return null;
    }

    private Vector2f calculateAttackPosition(ShipAPI target) {
        Vector2f currentPos = ship.getLocation();
        Vector2f targetPos = target.getLocation();

        float dist = Misc.getDistance(currentPos, targetPos);
        float optimalRange = aiData.getAverageWeaponRange(false);

        // Vector from us to target
        Vector2f toTarget = Vector2f.sub(targetPos, currentPos, new Vector2f());
        toTarget.normalise();

        // Try to get to optimal range
        float desiredDist = optimalRange * 0.8f; // Slightly inside optimal range

        if (dist > desiredDist) {
            // Move toward target
            Vector2f moveDir = new Vector2f(toTarget);
            moveDir.scale(Math.min(400f, dist - desiredDist));
            return new Vector2f(currentPos.x + moveDir.x, currentPos.y + moveDir.y);
        } else if (dist < desiredDist * 0.5f) {
            // Too close - back off
            Vector2f moveDir = new Vector2f(toTarget);
            moveDir.scale(-300f); // Back away
            return new Vector2f(currentPos.x + moveDir.x, currentPos.y + moveDir.y);
        } else {
            // Good range, try to flank
            // Pick a direction perpendicular to target's facing
            float flankAngle = target.getFacing() + (Math.random() > 0.5f ? 90f : -90f);
            Vector2f flankDir = Misc.getUnitVectorAtDegreeAngle(flankAngle);
            flankDir.scale(200f);
            return new Vector2f(currentPos.x + flankDir.x, currentPos.y + flankDir.y);
        }
    }

    private Vector2f calculateRepositionPosition(ShipAPI target) {
        Vector2f currentPos = ship.getLocation();

        // Try to find a position near allies but not too close
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return null;

        Vector2f averageAllyPos = new Vector2f();
        int allyCount = 0;

        for (ShipAPI other : engine.getShips()) {
            if (other.getOwner() == ship.getOwner() && other != ship && other.isAlive()) {
                float dist = Misc.getDistance(currentPos, other.getLocation());
                if (dist < 1000f) {
                    Vector2f.add(averageAllyPos, other.getLocation(), averageAllyPos);
                    allyCount++;
                }
            }
        }

        if (allyCount > 0) {
            averageAllyPos.scale(1f / allyCount);

            // Move toward average ally position, but maintain some distance
            Vector2f toAllies = Vector2f.sub(averageAllyPos, currentPos, new Vector2f());
            float distToAllies = toAllies.length();

            if (distToAllies > 400f) {
                // Too far from allies
                toAllies.normalise();
                toAllies.scale(Math.min(300f, distToAllies - 300f));
                return new Vector2f(currentPos.x + toAllies.x, currentPos.y + toAllies.y);
            } else if (distToAllies < 150f) {
                // Too close to allies
                toAllies.normalise();
                toAllies.scale(-200f); // Move away
                return new Vector2f(currentPos.x + toAllies.x, currentPos.y + toAllies.y);
            }
        }

        // Default: move toward target if exists, otherwise random direction
        if (target != null) {
            return calculateAttackPosition(target);
        } else {
            // Random direction
            float randomAngle = (float)(Math.random() * 360f);
            Vector2f randomDir = Misc.getUnitVectorAtDegreeAngle(randomAngle);
            randomDir.scale(300f);
            return new Vector2f(currentPos.x + randomDir.x, currentPos.y + randomDir.y);
        }
    }

    private ShipAPI findNearestEnemy() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return null;

        ShipAPI nearest = null;
        float nearestDist = Float.MAX_VALUE;

        for (ShipAPI other : engine.getShips()) {
            if (other.getOwner() != ship.getOwner() && other.isAlive()) {
                float dist = Misc.getDistance(ship.getLocation(), other.getLocation());
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = other;
                }
            }
        }

        return nearest;
    }

    private boolean shouldActivateForCurrentState() {
        switch (currentAIState) {
            case DEFENSIVE:
                // Activate if under significant threat
                return calculateDefensiveThreatScore() > 15f;

            case OFFENSIVE:
                // Activate if good attack opportunity
                return calculateOffensiveOpportunityScore(primaryTarget) > 18f;

            case REPOSITIONING:
                // Activate if significantly out of position
                return calculateRepositioningScore(primaryTarget) > 12f;

            case IDLE:
            default:
                return false;
        }
    }

    @Override
    public void onActivate() {
        timeSinceLastUse = 0f;
        activeTime = 0f;
        effectLevel = 0f;
        soundStarted = false;

        // Apply initial stats
        stats.getMaxSpeed().modifyFlat(getDisplayText(), SPEED_BONUS);
        stats.getMaxTurnRate().modifyFlat(getDisplayText(), 15f);
        stats.getMaxTurnRate().modifyPercent(getDisplayText(), 100f);
        stats.getAcceleration().modifyFlat(getDisplayText(), ACC_BONUS);

        // Play activation sound
        playActivationSound();

        // Apply visual activation effects to main ship and modules
        applyActivationEffects();

        // Issue movement command based on AI state if desired position exists
        if (desiredPosition != null) {
            issueMovementCommand(desiredPosition);
        }
    }

    private void playActivationSound() {
        try {
            // Play activation sound
            Global.getSoundPlayer().playSound(
                    "system_plasma_jets",
                    1f,
                    1f,
                    ship.getLocation(),
                    ship.getVelocity()
            );

            // Start loop sound with unique ID
            loopSoundId = "plasma_jets_loop_" + ship.getId() + "_" + System.currentTimeMillis();
            Global.getSoundPlayer().playLoop(
                    loopSoundId,
                    "system_plasma_jets_loop",
                    1f,
                    1f,
                    ship.getLocation(),
                    ship.getVelocity()
            );
            soundStarted = true;
        } catch (Exception e) {
            // Try alternative sounds
            try {
                Global.getSoundPlayer().playSound(
                        "system_travel_drive_deactivate",
                        1f,
                        1.2f,
                        ship.getLocation(),
                        ship.getVelocity()
                );
            } catch (Exception e2) {
                // No sound - just continue silently
            }
        }
    }

    private void applyActivationEffects() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        // Apply effects to main ship
        applyEngineEffectsToShip(ship, 1f);

        // Apply effects to all child modules
        for (ShipAPI module : ship.getChildModulesCopy()) {
            if (module.isAlive() && !module.isFighter() && !module.isDrone()) {
                applyEngineEffectsToShip(module, 0.7f); // Slightly reduced intensity for modules
            }
        }

        // Initial jitter effect
        float jitterIntensity = 0.3f;
        ship.setJitter(this, JITTER_COLOR, jitterIntensity, 3, 0, 3f);
        ship.setJitterUnder(this, JITTER_UNDER_COLOR, jitterIntensity, 15, 0f, 5f);

        // Jitter for modules
        for (ShipAPI module : ship.getChildModulesCopy()) {
            if (module.isAlive()) {
                module.setJitter(this, JITTER_COLOR, jitterIntensity * 0.6f, 2, 0, 2f);
                module.setJitterUnder(this, JITTER_UNDER_COLOR, jitterIntensity * 0.6f, 10, 0f, 3f);
            }
        }
    }

    private void applyEngineEffectsToShip(ShipAPI targetShip, float intensity) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || targetShip == null) return;

        // Clamp intensity to ensure alpha is within valid range
        float clampedIntensity = Math.max(0f, Math.min(1f, intensity));

        // Engine flare effect
        for (ShipEngineControllerAPI.ShipEngineAPI engineSlot : targetShip.getEngineController().getShipEngines()) {
            if (engineSlot.isActive()) {
                Vector2f engineLoc = engineSlot.getLocation();

                // Main engine flare
                engine.addHitParticle(
                        engineLoc,
                        new Vector2f(),
                        15f * clampedIntensity,
                        1f,
                        0.3f,
                        new Color(
                                jetColor.getRed(),
                                jetColor.getGreen(),
                                jetColor.getBlue(),
                                (int)(jetColor.getAlpha() * clampedIntensity)
                        )
                );

                // Trail effect
                Vector2f dir = Misc.getUnitVectorAtDegreeAngle(targetShip.getFacing() + engineSlot.getEngineSlot().getAngle());
                dir.scale(-1f); // Opposite direction of ship facing

                for (int i = 0; i < 5; i++) {
                    Vector2f offset = new Vector2f(dir);
                    offset.scale(i * 20f * clampedIntensity);
                    Vector2f particlePos = Vector2f.add(engineLoc, offset, new Vector2f());

                    engine.addHitParticle(
                            particlePos,
                            new Vector2f(),
                            8f - i * 1f,
                            0.8f * clampedIntensity,
                            0.5f,
                            new Color(100, 255, 100, (int)((150 - i * 20) * clampedIntensity))
                    );
                }
            }
        }

        // Apply engine controller effects
        targetShip.getEngineController().fadeToOtherColor(
                this,
                new Color(
                        jetColor.getRed(),
                        jetColor.getGreen(),
                        jetColor.getBlue(),
                        (int)(jetColor.getAlpha() * clampedIntensity)
                ),
                new Color(0, 0, 0, 0),
                clampedIntensity,
                0.67f
        );

        targetShip.getEngineController().extendFlame(
                this,
                ENGINE_FLAME_EXTEND * clampedIntensity,
                0f,
                0f
        );
    }

    private void issueMovementCommand(Vector2f targetPosition) {
        // Calculate angle to desired position
        float angleToTarget = Misc.getAngleInDegrees(ship.getLocation(), targetPosition);
        float angleDiff = Misc.getAngleDiff(ship.getFacing(), angleToTarget);

        // Issue turn command if needed
        if (Math.abs(angleDiff) > 30f) {
            if (angleDiff > 0) {
                ship.giveCommand(ShipCommand.TURN_LEFT, null, 0);
            } else {
                ship.giveCommand(ShipCommand.TURN_RIGHT, null, 0);
            }
        }

        // Always accelerate when using plasma jets
        ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
    }

    @Override
    public void advance(float amount, boolean isPaused) {
        if (isPaused || !isActive()) return;

        activeTime += amount;

        // Gradually increase effect level
        if (effectLevel < 1f) {
            effectLevel = Math.min(1f, effectLevel + amount * 2f);
        }

        // Update stats based on effect level
        float activeDuration = getBaseActiveDuration();
        float remainingRatio = 1f - (activeTime / activeDuration);

        // Ramp down effect near end
        float currentEffectLevel = effectLevel;
        if (remainingRatio < 0.3f) {
            currentEffectLevel = effectLevel * (remainingRatio / 0.3f);
        }

        // Clamp effect level to ensure alpha values stay within range
        currentEffectLevel = Math.max(0f, Math.min(1f, currentEffectLevel));

        // Apply dynamic stats
        stats.getAcceleration().modifyPercent(getDisplayText(), SPEED_BONUS * 3f * currentEffectLevel);
        stats.getDeceleration().modifyPercent(getDisplayText(), SPEED_BONUS * 3f * currentEffectLevel);
        stats.getTurnAcceleration().modifyFlat(getDisplayText(), TURN_BONUS * currentEffectLevel);
        stats.getTurnAcceleration().modifyPercent(getDisplayText(), TURN_BONUS * 5f * currentEffectLevel);

        // Update loop sound position and volume
        updateLoopSound(currentEffectLevel);

        // Apply continuous visual effects
        applyContinuousEffects(currentEffectLevel);
    }

    private void updateLoopSound(float effectLevel) {
        if (soundStarted && loopSoundId != null) {
            try {
                // Update sound position and volume based on effect level
                Global.getSoundPlayer().playLoop(
                        loopSoundId,
                        "system_plasma_jets_loop",
                        effectLevel * 0.8f, // Volume fades with effect
                        0.8f + effectLevel * 0.4f, // Pitch variation
                        ship.getLocation(),
                        ship.getVelocity()
                );
            } catch (Exception e) {
                // Sound doesn't exist or failed to play
            }
        }
    }

    private void applyContinuousEffects(float currentEffectLevel) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        // Apply jitter effect based on current effect level
        float jitterIntensity = currentEffectLevel * MAX_JITTER;

        // Jitter for main ship
        ship.setJitter(this, JITTER_COLOR, jitterIntensity, 3, 0, 2f + jitterIntensity * 3f);
        ship.setJitterUnder(this, JITTER_UNDER_COLOR, jitterIntensity, 15, 0f, 4f + jitterIntensity * 3f);

        // Jitter for child modules (reduced intensity)
        for (ShipAPI module : ship.getChildModulesCopy()) {
            if (module.isAlive()) {
                float moduleJitter = jitterIntensity * 0.5f;
                module.setJitter(this, JITTER_COLOR, moduleJitter, 2, 0, 1f + moduleJitter * 2f);
                module.setJitterUnder(this, JITTER_UNDER_COLOR, moduleJitter, 10, 0f, 2f + moduleJitter * 2f);
            }
        }

        // Engine effects for main ship
        applyContinuousEngineEffects(ship, currentEffectLevel);

        // Engine effects for modules
        for (ShipAPI module : ship.getChildModulesCopy()) {
            if (module.isAlive() && !module.isFighter() && !module.isDrone()) {
                applyContinuousEngineEffects(module, currentEffectLevel * 0.6f);
            }
        }

        // Occasional plasma particles
        if (Math.random() < currentEffectLevel * 0.3f) {
            createPlasmaParticles(currentEffectLevel);
        }
    }

    private void applyContinuousEngineEffects(ShipAPI targetShip, float intensity) {
        // Clamp intensity to ensure alpha is within valid range
        float clampedIntensity = Math.max(0f, Math.min(1f, intensity));

        // Engine glow effect
        targetShip.getEngineController().fadeToOtherColor(
                this,
                new Color(
                        jetColor.getRed(),
                        jetColor.getGreen(),
                        jetColor.getBlue(),
                        (int)(jetColor.getAlpha() * clampedIntensity)
                ),
                new Color(0, 0, 0, 0),
                clampedIntensity,
                0.67f
        );

        // Extended flames
        targetShip.getEngineController().extendFlame(
                this,
                ENGINE_FLAME_EXTEND * clampedIntensity,
                0f,
                0f
        );
    }

    private void createPlasmaParticles(float intensity) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        // Create particles for main ship
        createPlasmaParticlesForShip(ship, intensity);

        // Create particles for modules
        for (ShipAPI module : ship.getChildModulesCopy()) {
            if (module.isAlive() && !module.isFighter() && !module.isDrone()) {
                createPlasmaParticlesForShip(module, intensity * 0.5f);
            }
        }
    }

    private void createPlasmaParticlesForShip(ShipAPI targetShip, float intensity) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        // Clamp intensity to ensure alpha is within valid range
        float clampedIntensity = Math.max(0f, Math.min(1f, intensity));

        for (ShipEngineControllerAPI.ShipEngineAPI engineSlot : targetShip.getEngineController().getShipEngines()) {
            if (engineSlot.isActive() && Math.random() < 0.4f) {
                Vector2f loc = engineSlot.getLocation();
                Vector2f dir = Misc.getUnitVectorAtDegreeAngle(targetShip.getFacing() + engineSlot.getEngineSlot().getAngle());
                dir.scale(-1f);

                // Create plasma particle
                Vector2f vel = new Vector2f(dir);
                vel.scale(100f + (float) Math.random() * 200f);
                Vector2f.add(vel, targetShip.getVelocity(), vel);

                engine.addHitParticle(
                        loc,
                        vel,
                        6f + (float) Math.random() * 4f,
                        0.8f * clampedIntensity,
                        0.7f,
                        new Color(
                                100 + (int)(Math.random() * 50),
                                255,
                                100 + (int)(Math.random() * 50),
                                (int)(180 * clampedIntensity)
                        )
                );
            }
        }
    }

    @Override
    public void onFinished() {
        // Stop loop sound by playing it with 0 volume
        if (loopSoundId != null) {
            try {
                // Play the loop sound with 0 volume to effectively stop it
                Global.getSoundPlayer().playLoop(
                        loopSoundId,
                        "system_plasma_jets_loop",
                        0f, // 0 volume stops the sound
                        1f,
                        ship.getLocation(),
                        ship.getVelocity()
                );
            } catch (Exception e) {
                // Ignore sound errors
            }
        }

        // Play deactivation sound
        try {
            Global.getSoundPlayer().playSound(
                    "engine_stop",
                    1f,
                    0.8f,
                    ship.getLocation(),
                    ship.getVelocity()
            );
        } catch (Exception e) {
            // Ignore sound errors
        }

        // Remove all stat modifications
        stats.getMaxSpeed().unmodify(getDisplayText());
        stats.getMaxTurnRate().unmodify(getDisplayText());
        stats.getTurnAcceleration().unmodify(getDisplayText());
        stats.getAcceleration().unmodify(getDisplayText());
        stats.getDeceleration().unmodify(getDisplayText());

        // Fade out effects for main ship
        ship.getEngineController().fadeToOtherColor(this, jetColor, new Color(0, 0, 0, 0), 0f, 0.5f);
        ship.getEngineController().extendFlame(this, 0f, 0f, 0f);

        // Fade out effects for modules
        for (ShipAPI module : ship.getChildModulesCopy()) {
            if (module.isAlive()) {
                module.getEngineController().fadeToOtherColor(this, jetColor, new Color(0, 0, 0, 0), 0f, 0.5f);
                module.getEngineController().extendFlame(this, 0f, 0f, 0f);
            }
        }

        effectLevel = 0f;
        activeTime = 0f;
        soundStarted = false;
        loopSoundId = null;
    }

    @Override
    public String getDisplayText() {
        return "Plasma Jets";
    }
}