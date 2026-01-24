package neon.nsp.data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.MagicSubsystem;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NSP_FlareSubsystem extends MagicSubsystem {

    private IntervalUtil flareInterval = new IntervalUtil(0.02f, 0.05f);
    private int flaresToLaunch = 15;
    private int flaresLaunched = 0;
    private List<Vector2f> flareLaunchPositions = new ArrayList<>();
    private List<Float> flareLaunchAngles = new ArrayList<>();
    private int currentPositionIndex = 0;
    private Map<DamagingProjectileAPI, FlareData> activeFlares = new HashMap<>();
    private static final float FLARE_LIFETIME = 20f;
    private static final float DISRUPTION_RADIUS = 400f; //
    private FlareManagerPlugin flareManager;

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
        return 6f;
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
        activeFlares.clear(); // Clear old flares

        // Create and add flare manager plugin
        flareManager = new FlareManagerPlugin();
        Global.getCombatEngine().addPlugin(flareManager);

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
        velocity.scale(80f + (float)Math.random() * 40f); // Slightly slower for better lingering
        Vector2f.add(velocity, ship.getVelocity(), velocity);

        // Try to spawn flare with longer lifetime
        DamagingProjectileAPI flare = null;
        try {
            flare = (DamagingProjectileAPI) engine.spawnProjectile(
                    ship,
                    null,
                    "flarelauncher1",
                    launchPoint,
                    flareAngle,
                    velocity
            );
        } catch (Exception e) {
            // Fallback to visual effect only
            createVisualFlare(launchPoint, velocity, flareAngle);
            return;
        }

        if (flare != null) {
            // Make flare last longer
            flare.setHitpoints(999999f);
            flare.setDamageAmount(0f); // No damage
            flare.setCollisionClass(CollisionClass.NONE);

            // Add flare to active list with data
            activeFlares.put(flare, new FlareData());
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
                        1f,
                        1.5f + (float)Math.random() * 0.2f,
                        launchPoint,
                        new Vector2f()
                );
            } catch (Exception e) {
                // Ignore sound errors
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

        // Clean up the flare manager when system is done
        if (flareManager != null) {
            flareManager.markDone();
        }
    }

    @Override
    public String getDisplayText() {
        return "Flare Launcher";
    }

    // Helper class to store flare data
    private static class FlareData {
        float elapsed = 0f;
        IntervalUtil pulseInterval = new IntervalUtil(0.5f, 0.8f);
    }

    // Internal class to manage flares
    private class FlareManagerPlugin implements EveryFrameCombatPlugin {
        private boolean done = false;

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (Global.getCombatEngine().isPaused() || done) return;

            // Update all active flares
            updateFlares(amount);

            // Disrupt missiles based on all active flares
            disruptMissilesFromFlares(amount);

            // Clean up expired flares and plugin if done
            cleanUpExpiredFlares();

            // Remove plugin if no flares are active and system is not active
            if (activeFlares.isEmpty() && !isActive()) {
                done = true;
            }
        }

        private void updateFlares(float amount) {
            List<DamagingProjectileAPI> toRemove = new ArrayList<>();

            for (Map.Entry<DamagingProjectileAPI, FlareData> entry : activeFlares.entrySet()) {
                DamagingProjectileAPI flare = entry.getKey();
                FlareData data = entry.getValue();

                if (flare == null || flare.didDamage() || flare.isExpired()) {
                    toRemove.add(flare);
                    continue;
                }

                data.elapsed += amount;

                // Fade out effect in last 2 seconds
                if (data.elapsed > FLARE_LIFETIME - 2f) {
                    float fadeTime = FLARE_LIFETIME - 2f;
                    float alpha = 1f - ((data.elapsed - fadeTime) / 2f);
                    // Create fade visual effect
                    createFadeEffect(flare.getLocation(), alpha);
                }

                // Remove flare after duration
                if (data.elapsed >= FLARE_LIFETIME) {
                    flare.setHitpoints(0f);
                    toRemove.add(flare);
                    continue;
                }

                // Create periodic disruption pulses
                data.pulseInterval.advance(amount);
                if (data.pulseInterval.intervalElapsed()) {
                    createDisruptionPulse(flare.getLocation());
                }

                // Subtle continuous visual effect
                createContinuousEffect(flare.getLocation(), data.elapsed);
            }

            // Remove expired flares
            for (DamagingProjectileAPI flare : toRemove) {
                activeFlares.remove(flare);
            }
        }

        private void cleanUpExpiredFlares() {
            List<DamagingProjectileAPI> toRemove = new ArrayList<>();
            for (DamagingProjectileAPI flare : activeFlares.keySet()) {
                if (flare == null || flare.didDamage() || flare.isExpired()) {
                    toRemove.add(flare);
                }
            }
            for (DamagingProjectileAPI flare : toRemove) {
                activeFlares.remove(flare);
            }
        }

        private void disruptMissilesFromFlares(float amount) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || activeFlares.isEmpty()) return;

            for (MissileAPI missile : engine.getMissiles()) {
                if (missile.getOwner() != ship.getOwner() && !missile.isFading()) {
                    // Check distance to each flare
                    for (DamagingProjectileAPI flare : activeFlares.keySet()) {
                        if (flare == null || flare.didDamage() || flare.isExpired()) continue;

                        float dist = Misc.getDistance(flare.getLocation(), missile.getLocation());

                        // Disrupt missiles within flare's disruption radius
                        if (dist < DISRUPTION_RADIUS) {
                            float disruptChance = 1f - (dist / DISRUPTION_RADIUS);

                            // Higher chance when closer to flare
                            if (Math.random() < disruptChance * 0.6f * amount) {
                                disruptMissile(missile, flare.getLocation());
                                break; // Only disrupt once per missile per frame
                            }
                        }
                    }
                }
            }
        }

        private void disruptMissile(MissileAPI missile, Vector2f flareLocation) {
            if (missile.getMissileAI() instanceof GuidedMissileAI) {
                GuidedMissileAI ai = (GuidedMissileAI) missile.getMissileAI();

                // Higher chance to lose target when near flare
                float loseTargetChance = 0.6f; // 60% chance
                if (Math.random() < loseTargetChance) {
                    ai.setTarget(null);
                    // Make missile drift toward flare
                    Vector2f toFlare = Vector2f.sub(flareLocation, missile.getLocation(), new Vector2f());
                    toFlare.normalise();
                    toFlare.scale(100f); // Gentle pull toward flare
                    Vector2f.add(missile.getVelocity(), toFlare, missile.getVelocity());
                }

                // Add random turning
                if (Math.random() < 0.7f) { // 70% chance for turning disruption
                    missile.giveCommand(Math.random() > 0.5f ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT);

                    // Occasionally reverse turn
                    if (Math.random() < 0.3f) {
                        missile.giveCommand(Math.random() > 0.5f ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT);
                    }
                }

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
                            40f,
                            200f,
                            null,
                            3f,
                            new Color(255, 200, 100, 120),
                            new Color(255, 255, 200, 80)
                    );

                    // Add visual connection to flare
                    engine.addHitParticle(
                            missile.getLocation(),
                            new Vector2f(),
                            5f,
                            0.8f,
                            0.3f,
                            new Color(255, 150, 50, 150)
                    );
                }
            }
        }

        private void createFadeEffect(Vector2f location, float alpha) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null) return;

            // Create fade particle
            engine.addHitParticle(
                    location,
                    new Vector2f(),
                    6f * alpha,
                    0.7f * alpha,
                    0.4f,
                    new Color(255, 150, 50, (int)(150 * alpha))
            );
        }

        private void createDisruptionPulse(Vector2f location) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null) return;

            // Create visual pulse
            for (int i = 0; i < 8; i++) {
                float angle = 45f * i;
                Vector2f offset = Misc.getUnitVectorAtDegreeAngle(angle);
                offset.scale(200f + (float)Math.random() * 200f);

                Vector2f particlePos = Vector2f.add(location, offset, new Vector2f());

                engine.addHitParticle(
                        particlePos,
                        new Vector2f(),
                        3f + (float)Math.random() * 2f,
                        0.6f,
                        0.8f,
                        new Color(255, 200, 100, 60)
                );
            }
        }

        private void createContinuousEffect(Vector2f location, float elapsed) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null) return;

            // Subtle glow around flare
            float pulse = (float)(0.7f + 0.3f * Math.sin(elapsed * 3f));
            engine.addHitParticle(
                    location,
                    new Vector2f(),
                    8f * pulse,
                    0.7f * pulse,
                    0.2f,
                    new Color(255, 180, 50, (int)(40 * pulse))
            );

            // Occasional sparkle
            if (Math.random() < 0.3f) {
                engine.addHitParticle(
                        location,
                        new Vector2f(),
                        4f + (float)Math.random() * 3f,
                        0.9f,
                        0.1f,
                        Color.WHITE
                );
            }
        }

        @Override
        public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

        }

        @Override
        public void renderInWorldCoords(ViewportAPI viewport) {
            // Not needed
        }

        @Override
        public void renderInUICoords(ViewportAPI viewport) {
            // Not needed
        }

        @Override
        public void init(CombatEngineAPI engine) {
            // Not needed
        }

        public void markDone() {
            done = true;
        }

        public boolean isDone() {
            return done;
        }
    }
}