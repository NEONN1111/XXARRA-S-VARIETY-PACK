package neon.nsp.data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.MagicSubsystem;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class NSP_FlareSubsystem extends MagicSubsystem {

    private IntervalUtil flareInterval = new IntervalUtil(0.02f, 0.05f);
    private int flaresToLaunch = 15;
    private int flaresLaunched = 0;
    private List<Vector2f> flareLaunchPositions = new ArrayList<>();
    private List<Float> flareLaunchAngles = new ArrayList<>();
    private int currentPositionIndex = 0;

    public NSP_FlareSubsystem(ShipAPI ship) {
        super(ship);
        findFlareLaunchPositions();
    }

    private void findFlareLaunchPositions() {
        flareLaunchPositions.clear();
        flareLaunchAngles.clear();

        if (ship == null) return;

        // Look for small system slots on the ship
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            // Check if this is a small system slot
            if (weapon.getSlot().getSlotSize() == WeaponAPI.WeaponSize.SMALL &&
                    weapon.getSlot().isSystemSlot()) {

                // Get the weapon's location and facing
                Vector2f location = weapon.getLocation();
                float angle = weapon.getCurrAngle();

                flareLaunchPositions.add(location);
                flareLaunchAngles.add(angle);
            }
        }

        // If no system slots found, create positions around the ship
        if (flareLaunchPositions.isEmpty()) {
            createDefaultFlarePositions();
        }
    }

    private void createDefaultFlarePositions() {
        // Create 8 positions around the ship
        int numPositions = 8;

        for (int i = 0; i < numPositions; i++) {
            float angle = 360f * i / numPositions;
            Vector2f direction = Misc.getUnitVectorAtDegreeAngle(angle);
            float distance = ship.getCollisionRadius() * 0.9f;

            Vector2f position = new Vector2f(
                    ship.getLocation().x + direction.x * distance,
                    ship.getLocation().y + direction.y * distance
            );

            flareLaunchPositions.add(position);
            flareLaunchAngles.add(angle);
        }
    }

    @Override
    public float getBaseActiveDuration() {
        return 1.5f;
    }

    @Override
    public float getBaseCooldownDuration() {
        return 7f;
    }

    @Override
    public boolean shouldActivateAI(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return false;

        int nearbyMissiles = 0;
        float closestMissileDist = Float.MAX_VALUE;

        // Check for incoming missiles
        for (MissileAPI missile : engine.getMissiles()) {
            if (missile.getOwner() != ship.getOwner()) {
                float dist = Misc.getDistance(ship.getLocation(), missile.getLocation());
                if (dist < 1200f) {
                    nearbyMissiles++;
                    closestMissileDist = Math.min(closestMissileDist, dist);
                }
            }
        }

        // Also check fighter missiles
        for (ShipAPI other : engine.getShips()) {
            if (other.getOwner() != ship.getOwner() && other.isFighter() && !other.isDrone()) {
                float dist = Misc.getDistance(ship.getLocation(), other.getLocation());
                if (dist < 800f) {
                    nearbyMissiles++;
                }
            }
        }

        float threatScore = 0f;

        // Missile threat
        if (nearbyMissiles > 0) {
            threatScore += nearbyMissiles * 3f;
            if (closestMissileDist < 600f) {
                threatScore += 5f * (1f - closestMissileDist / 600f);
            }
        }

        // Flux pressure
        float fluxLevel = ship.getFluxTracker().getFluxLevel();
        if (fluxLevel > 0.6f) {
            threatScore += fluxLevel * 4f;
        }

        // Defensive situation
        if (ship.getFluxTracker().isOverloadedOrVenting()) {
            threatScore += 8f;
        }

        // Low hull
        if (ship.getHullLevel() < 0.4f) {
            threatScore += 6f;
        }

        // Add some randomness
        threatScore += (float)Math.random() * 2f;

        return threatScore > 6f;
    }

    @Override
    public void onActivate() {
        flaresLaunched = 0;
        currentPositionIndex = 0;
        flareInterval.setElapsed(0);

        // Update positions each activation
        findFlareLaunchPositions();

        // Visual activation effect at launch positions
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null && !flareLaunchPositions.isEmpty()) {
            for (Vector2f position : flareLaunchPositions) {
                engine.addHitParticle(
                        position,
                        new Vector2f(),
                        6f + (float)Math.random() * 4f,
                        1f,
                        0.4f,
                        new Color(255, 200, 100, 200)
                );
            }
        }
    }

    @Override
    public void advance(float amount, boolean isPaused) {
        if (isPaused || !isActive()) return;

        flareInterval.advance(amount);

        if (flareInterval.intervalElapsed() && flaresLaunched < flaresToLaunch) {
            launchFlareFromPosition();
            flaresLaunched++;
        }

        // Disrupt nearby missiles while active
        if (isActive()) {
            disruptMissiles(amount);
        }
    }

    private void launchFlareFromPosition() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || flareLaunchPositions.isEmpty()) return;

        // Get current position and advance to next
        Vector2f launchPoint = flareLaunchPositions.get(currentPositionIndex);
        float flareAngle = flareLaunchAngles.get(currentPositionIndex);
        currentPositionIndex = (currentPositionIndex + 1) % flareLaunchPositions.size();

        // Calculate launch velocity - outward from position
        Vector2f direction = Misc.getUnitVectorAtDegreeAngle(flareAngle);
        Vector2f velocity = new Vector2f(direction);
        velocity.scale(100f + (float)Math.random() * 50f);
        Vector2f.add(velocity, ship.getVelocity(), velocity);

        // Try to spawn flare
        try {
            engine.spawnProjectile(
                    ship,
                    null,
                    "flarelauncher1",
                    launchPoint,
                    flareAngle,
                    velocity
            );
        } catch (Exception e) {
            // Fallback to visual effect
            createVisualFlare(launchPoint, velocity, flareAngle);
        }

        // Launch effects
        createLaunchEffects(launchPoint, direction);
    }

    private void createVisualFlare(Vector2f position, Vector2f velocity, float angle) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        // Main flare particle
        Color flareColor = new Color(
                255,
                180 + (int)(Math.random() * 40),
                50 + (int)(Math.random() * 50),
                220
        );

        engine.addHitParticle(
                position,
                velocity,
                12f + (float)Math.random() * 6f,
                1f,
                2f + (float)Math.random() * 1f,
                flareColor
        );

        // Bright core
        engine.addHitParticle(
                position,
                new Vector2f(),
                8f,
                1f,
                0.3f,
                Color.white
        );

        // Spawn EMP effect to simulate missile disruption
        engine.spawnEmpArc(
                ship,
                position,
                ship,
                ship,
                DamageType.OTHER,
                0f,
                60f + (float)Math.random() * 120f,
                300f,
                null,
                4f,
                new Color(255, 200, 100, 100),
                new Color(255, 255, 200, 50)
        );
    }

    private void createLaunchEffects(Vector2f launchPoint, Vector2f direction) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        // Muzzle flash
        engine.addHitParticle(
                launchPoint,
                new Vector2f(),
                8f + (float)Math.random() * 4f,
                1f,
                0.2f,
                new Color(255, 240, 200, 255)
        );

        // Smoke puff
        Vector2f smokeVel = new Vector2f(
                direction.x * -15f + (float)Math.random() * 10f - 5f,
                direction.y * -15f + (float)Math.random() * 10f - 5f
        );

        engine.addSmokeParticle(
                launchPoint,
                smokeVel,
                10f + (float)Math.random() * 6f,
                0.7f,
                1.5f,
                new Color(120, 120, 120, 100)
        );

        // Play launch sound occasionally
        if (flaresLaunched % 4 == 0) {
            try {
                Global.getSoundPlayer().playSound(
                        "flare_launcher_passive_oneshot",
                        0.6f,
                        0.9f + (float)Math.random() * 0.2f,
                        launchPoint,
                        new Vector2f()
                );
            } catch (Exception e) {
                // Ignore sound errors
            }
        }
    }

    private void disruptMissiles(float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        for (MissileAPI missile : engine.getMissiles()) {
            if (missile.getOwner() != ship.getOwner() && !missile.isFading()) {
                float dist = Misc.getDistance(ship.getLocation(), missile.getLocation());

                // Disrupt missiles within flare radius
                if (dist < 500f) {
                    float disruptChance = 1f - (dist / 500f);

                    if (Math.random() < disruptChance * 0.4f * amount) {
                        disruptMissile(missile);
                    }
                }
            }
        }
    }

    private void disruptMissile(MissileAPI missile) {
        if (missile.getMissileAI() instanceof GuidedMissileAI) {
            GuidedMissileAI ai = (GuidedMissileAI) missile.getMissileAI();

            // 40% chance to make missile lose target
            if (Math.random() < 0.4f) {
                ai.setTarget(null);
            }

            // Add random turning
            missile.giveCommand(Math.random() > 0.5f ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT);

            // Visual EMP effect on missile
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine != null) {
                engine.spawnEmpArc(
                        ship,
                        missile.getLocation(),
                        missile,
                        missile,
                        DamageType.ENERGY,
                        0f,
                        30f,
                        150f,
                        null,
                        3f,
                        new Color(255, 200, 100, 100),
                        new Color(255, 255, 200, 60)
                );
            }
        }
    }

    @Override
    public void onFinished() {
        // Final effects at launch positions
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null && !flareLaunchPositions.isEmpty()) {
            for (Vector2f position : flareLaunchPositions) {
                engine.addHitParticle(
                        position,
                        new Vector2f(),
                        4f + (float)Math.random() * 3f,
                        0.8f,
                        0.6f,
                        new Color(255, 150, 50, 150)
                );
            }
        }
    }

    @Override
    public String getDisplayText() {
        return "Flare Launcher";
    }
}