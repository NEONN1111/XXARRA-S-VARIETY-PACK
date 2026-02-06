package neon.nsp.data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.MagicRender;
import neon.nsp.data.scripts.util.NSP_Util;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.Map;



public class NSP_PhaseTunnelerStats extends BaseShipSystemScript {

    private static final String CHARGEUP_SOUND = "nsp_phasetunneleractivate";
    private static final float DAMAGE_MOD_VS_CAPITAL = 0.2f;
    private static final float DAMAGE_MOD_VS_CRUISER = 0.4f;
    private static final float DAMAGE_MOD_VS_DESTROYER = 1f;
    private static final float DAMAGE_MOD_VS_FIGHTER = 0.7f;
    private static final float DAMAGE_MOD_VS_FRIGATE = 0.8f;
    private static final float DISTORTION_BLAST_RADIUS = 1500f;
    private static final Color EXPLOSION_COLOR = new Color(255, 255, 255);
    private static final float EXPLOSION_DAMAGE_AMOUNT = 5000f;
    @SuppressWarnings("SuspiciousNameCombination")
    private static final DamageType EXPLOSION_DAMAGE_TYPE = DamageType.ENERGY;
    private static final float EXPLOSION_DAMAGE_VS_ALLIES_MODIFIER = .25f;
    private static final float EXPLOSION_EMP_DAMAGE_AMOUNT = 5000f;
    private static final float EXPLOSION_EMP_VS_ALLIES_MODIFIER = .25f;
    private static final float EXPLOSION_FORCE_VS_ALLIES_MODIFIER = .3f;
    private static final float EXPLOSION_PUSH_RADIUS = 1000f;
    private static final float EXPLOSION_VISUAL_RADIUS = 1500f;
    private static final float FORCE_VS_ASTEROID = 1500f;
    private static final float FORCE_VS_CAPITAL = 200f;
    private static final float FORCE_VS_CRUISER = 350f;
    private static final float FORCE_VS_DESTROYER = 900f;
    private static final float FORCE_VS_FIGHTER = 1250f;
    private static final float FORCE_VS_FRIGATE = 1000f;
    private static final int MAX_PARTICLES_PER_FRAME = 30;
    private static final Color PARTICLE_COLOR = new Color(243, 225, 255);
    private static final float PARTICLE_OPACITY = 0.5f;
    private static final float PARTICLE_RADIUS = 600f;
    private static final float PARTICLE_SIZE = 10f;
    public static final Color RIPPLE_COLOR = new Color(174, 55, 255, 200);
    public static final float RIPPLE_DURATION = 5f;
    public static final float RIPPLE_MAX_SIZE = 4000;
    public static final Color AFTERIMAGE_COLOR = new Color(255, 196, 19, 20);
    private static final Vector2f ZERO = new Vector2f();

    // AI teleport parameters
    private static final float TELEPORT_SPEED = 4000f;
    private static final float MIN_TELEPORT_DISTANCE = 100f;
    private static final float MAX_TELEPORT_DISTANCE = 4000;
    private static final float TELEPORT_ACCELERATION = 0.2f;

    private final IntervalUtil interval = new IntervalUtil(0.035f, 0.035f);
    private final IntervalUtil interval2 = new IntervalUtil(0.015f, 0.015f);
    private final IntervalUtil teleportInterval = new IntervalUtil(0.016f, 0.016f);
    private boolean isActive = false;
    private StandardLight light = null;
    private Vector2f novaLocation = null;
    private float novaTime = -1f;
    private SoundAPI sound = null;

    // Teleport state
    private Vector2f teleportStartLocation = null;
    private Vector2f teleportTargetLocation = null;
    private Vector2f teleportVelocity = null;
    private float teleportProgress = 0f;
    private boolean teleportInProgress = false;
    private float teleportStartTime = 0f;
    private float teleportDuration = 0f;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (!(stats.getEntity() instanceof ShipAPI)) {
            return;
        }


        ShipAPI ship = (ShipAPI) stats.getEntity();
        CombatEngineAPI engine = Global.getCombatEngine();





        // Handle teleport movement during charge-up
        if (state == State.IN) {
            handleTeleportMovement(ship, engine, effectLevel);

            // Only show visual effects if we're actually charging (not teleporting yet)
            if (!teleportInProgress || teleportProgress < 0.5f) {
                if (!isActive) {
                    isActive = true;
                    sound = Global.getSoundPlayer().playSound(CHARGEUP_SOUND, 1f, 2f, ship.getLocation(), ship.getVelocity());

                    light = new StandardLight();
                    light.setIntensity(1.25f);
                    light.setSize(EXPLOSION_VISUAL_RADIUS);
                    light.setColor(PARTICLE_COLOR);
                    light.fadeIn(1.95f);
                    light.setLifetime(0.1f);
                    light.setAutoFadeOutTime(0.17f);
                    LightShader.addLight(light);
                }

                Vector2f loc = new Vector2f(ship.getLocation());
                loc.x -= 70f * FastTrig.cos(ship.getFacing() * Math.PI / 180f);
                loc.y -= 70f * FastTrig.sin(ship.getFacing() * Math.PI / 180f);
                if (light != null) {
                    light.setLocation(loc);
                }

                interval2.advance(engine.getElapsedInLastFrame());
                if (interval2.intervalElapsed()) {
                    Vector2f particlePos, particleVel;
                    int numParticlesThisFrame = Math.round(effectLevel * MAX_PARTICLES_PER_FRAME);
                    for (int x = 0; x < numParticlesThisFrame; x++) {
                        particlePos = MathUtils.getRandomPointOnCircumference(ship.getLocation(), PARTICLE_RADIUS);
                        particleVel = Vector2f.sub(ship.getLocation(), particlePos, null);
                        engine.addSmokeParticle(particlePos, particleVel, PARTICLE_SIZE, PARTICLE_OPACITY,
                                1f, PARTICLE_COLOR);
                    }
                }
            }
        }
        // Handle teleport completion
        else if (state == State.OUT) {
            completeTeleport(ship, engine);

            if (isActive) {
                engine.spawnExplosion(ship.getLocation(), ship.getVelocity(), EXPLOSION_COLOR, EXPLOSION_VISUAL_RADIUS,
                        0.2f);
                engine.spawnExplosion(ship.getLocation(), ship.getVelocity(), EXPLOSION_COLOR, EXPLOSION_VISUAL_RADIUS
                        / 2f, 0.2f);

                Vector2f loc = new Vector2f(ship.getLocation());
                loc.x -= 70f * FastTrig.cos(ship.getFacing() * Math.PI / 180f);
                loc.y -= 70f * FastTrig.sin(ship.getFacing() * Math.PI / 180f);

                light = new StandardLight(loc, ZERO, ZERO, null);
                light.setIntensity(2f);
                light.setSize(EXPLOSION_VISUAL_RADIUS * 3f);
                light.setColor(EXPLOSION_COLOR);
                light.fadeOut(2.35f);
                LightShader.addLight(light);

                final WaveDistortion wave = new WaveDistortion();
                wave.setLocation(loc);
                wave.setSize(1200.0f);
                wave.setIntensity(85.0f);
                wave.fadeInSize(1.2f);
                wave.fadeOutIntensity(0.9f);
                wave.setSize(262.5f);
                DistortionShader.addDistortion(wave);

                final StandardLight light = new StandardLight();
                light.setLocation(loc);
                light.setIntensity(0.35f);
                light.setSize(950.0f);
                light.fadeOut(1.0f);
                LightShader.addLight(light);

                novaLocation = loc;
                novaTime = 0f;
                engine.addHitParticle(loc, ZERO, 500f, 1f, 0.3f, EXPLOSION_COLOR);
                engine.spawnExplosion(loc, ZERO, EXPLOSION_COLOR, 1000f, 0.09f);
                Global.getSoundPlayer().playSound("nsp_phasetunnelerblast", 1f, 2f, loc, ZERO);

                try {
                    if (sound != null) {
                        sound.setLocation(ship.getLocation().x, ship.getLocation().y);
                    }
                } catch (Exception ex) {
                    Global.getSoundPlayer().playSound(CHARGEUP_SOUND, 2f, 2f, ship.getLocation(), ship.getVelocity());
                }

                // Apply explosion damage and force
                applyExplosionEffects(ship, engine);
                ship.getFluxTracker().decreaseFlux(ship.getMaxFlux()/4);

                isActive = false;
            }
        }

        // Handle nova effects
        handleNovaEffects(ship, engine);
    }

    private void handleTeleportMovement(ShipAPI ship, CombatEngineAPI engine, float effectLevel) {
        // Check if AI has set a teleport target using custom data
        if (!teleportInProgress) {
            Object customData = ship.getCustomData();
            if (customData instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) customData;
                Object teleportData = dataMap.get("nsp_teleport_data");

                if (teleportData instanceof Map) {
                    Map<String, Object> teleportMap = (Map<String, Object>) teleportData;
                    Float targetX = (Float) teleportMap.get("targetX");
                    Float targetY = (Float) teleportMap.get("targetY");

                    if (targetX != null && targetY != null) {
                        teleportTargetLocation = new Vector2f(targetX, targetY);
                        teleportStartLocation = new Vector2f(ship.getLocation());

                        // Calculate teleport parameters
                        float distance = MathUtils.getDistance(teleportStartLocation, teleportTargetLocation);

                        // Clamp distance to valid range
                        distance = MathUtils.clamp(distance, MIN_TELEPORT_DISTANCE, MAX_TELEPORT_DISTANCE);

                        // Calculate teleport velocity direction
                        teleportVelocity = Vector2f.sub(teleportTargetLocation, teleportStartLocation, null);
                        if (teleportVelocity.length() > 0) {
                            teleportVelocity.normalise();
                            teleportVelocity.scale(TELEPORT_SPEED);
                        }

                        teleportProgress = 0f;
                        teleportInProgress = true;
                        teleportStartTime = engine.getTotalElapsedTime(false);
                        teleportDuration = distance / TELEPORT_SPEED;

                        // Remove teleport data to prevent repeated use
                        dataMap.remove("nsp_teleport_data");

                        {
                            SpriteAPI rippleSprite = Global.getSettings().getSprite("fx", "shield_ring");
                            MagicRender.battlespace(
                                    rippleSprite,
                                    ship.getLocation(),
                                    new Vector2f(0f, 0f),
                                    new Vector2f(50f, 50f),
                                    new Vector2f(RIPPLE_MAX_SIZE, RIPPLE_MAX_SIZE),
                                    ship.getFacing() - 90f,
                                    0f,
                                    RIPPLE_COLOR,
                                    true,
                                    0f,
                                    0.1f,
                                    0.3f,
                                    RIPPLE_DURATION,
                                    0f,
                                    0.1f,
                                    0.2f,
                                    0.5f,
                                    CombatEngineLayers.ABOVE_SHIPS_LAYER
                            );

                            for (int i = 0; i < 25; i++) {
                                Vector2f particlePos = MathUtils.getPointOnCircumference(
                                        ship.getLocation(),
                                        MathUtils.getRandomNumberInRange(0f, ship.getCollisionRadius()),
                                        MathUtils.getRandomNumberInRange(0f, 360f)
                                );
                                Vector2f particleVel = MathUtils.getRandomPointInCircle(new Vector2f(), 50f);

                                final WaveDistortion wave = new WaveDistortion();
                                final Vector2f loc = new Vector2f(ship.getLocation());
                                wave.setLocation(loc);
                                wave.setSize(950.0f);
                                wave.setIntensity(85.0f);
                                wave.fadeInSize(1.2f);
                                wave.fadeOutIntensity(0.9f);
                                wave.setSize(262.5f);
                                DistortionShader.addDistortion(wave);

                                final StandardLight light = new StandardLight();
                                light.setLocation(loc);
                                light.setIntensity(0.35f);
                                light.setSize(950.0f);
                                light.setColor(AFTERIMAGE_COLOR);
                                light.fadeOut(1.0f);
                                LightShader.addLight(light);

                                Global.getCombatEngine().addSmoothParticle(
                                        particlePos,
                                        particleVel,
                                        MathUtils.getRandomNumberInRange(5f, 15f),
                                        0.8f,
                                        MathUtils.getRandomNumberInRange(0.5f, 1.5f),
                                        RIPPLE_COLOR
                                );

                            }
                        }
                    }
                }
            }
        }


        // Execute teleport movement if in progress
        if (teleportInProgress) {
            teleportInterval.advance(engine.getElapsedInLastFrame());

            // Calculate movement for this frame
            if (teleportInterval.intervalElapsed() && teleportVelocity != null) {
                // Accelerate at start, decelerate at end
                float progressRatio = teleportProgress;
                float accelerationFactor = 1f;

                if (progressRatio < 0.3f) {
                    // Acceleration phase
                    accelerationFactor = progressRatio / 0.3f;
                } else if (progressRatio > 0.7f) {
                    // Deceleration phase
                    accelerationFactor = 1f - ((progressRatio - 0.7f) / 0.3f);
                }

                // Apply smoothed movement
                Vector2f frameVelocity = new Vector2f(teleportVelocity);
                frameVelocity.scale(accelerationFactor * engine.getElapsedInLastFrame());

                // Move ship
                Vector2f.add(ship.getLocation(), frameVelocity, ship.getLocation());

                // Update progress
                float distanceTraveled = frameVelocity.length();
                float totalDistance = MathUtils.getDistance(teleportStartLocation, teleportTargetLocation);
                teleportProgress = MathUtils.clamp(teleportProgress + (distanceTraveled / totalDistance), 0f, 1f);

                // Create teleport trail particles
                createTeleportTrail(ship, engine, accelerationFactor);

                // If teleport complete, trigger early transition to OUT state
                if (teleportProgress >= 0.95f) {
                    // Snap to exact target location
                    ship.getLocation().set(teleportTargetLocation);
                    teleportProgress = 1f;
                }
            }

            // Update ship facing if specified by AI
            Object customData = ship.getCustomData();
            if (customData instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) customData;
                Object teleportData = dataMap.get("nsp_teleport_data");

                if (teleportData instanceof Map) {
                    Map<String, Object> teleportMap = (Map<String, Object>) teleportData;
                    Float targetFacing = (Float) teleportMap.get("facing");
                    if (targetFacing != null) {
                        float facingDiff = MathUtils.getShortestRotation(ship.getFacing(), targetFacing);
                        ship.setFacing(ship.getFacing() + facingDiff * 0.1f);
                    }
                }
            }
        }
    }

    private void createTeleportTrail(ShipAPI ship, CombatEngineAPI engine, float intensity) {
        // Create particle trail behind ship
        Vector2f trailPos = MathUtils.getPointOnCircumference(
                ship.getLocation(),
                -ship.getCollisionRadius() * 0.8f,
                ship.getFacing() + 180f
        );

        // Main trail particle
        engine.addSmoothParticle(
                trailPos,
                new Vector2f(),
                PARTICLE_SIZE * 2f * intensity,
                0.8f * intensity,
                0.5f,
                PARTICLE_COLOR
        );

        // Side particles
        for (int i = 0; i < 3; i++) {
            float angleOffset = (float) Math.random() * 60f - 30f;
            Vector2f sidePos = MathUtils.getPointOnCircumference(
                    trailPos,
                    ship.getCollisionRadius() * 0.3f,
                    ship.getFacing() + 90f + angleOffset
            );

            engine.addSmoothParticle(
                    sidePos,
                    new Vector2f(),
                    PARTICLE_SIZE * intensity,
                    0.6f * intensity,
                    0.3f,
                    PARTICLE_COLOR
            );
        }
    }

    private void completeTeleport(ShipAPI ship, CombatEngineAPI engine) {
        // Clean up teleport state
        if (teleportInProgress) {
            // Ensure ship is at target location
            if (teleportTargetLocation != null) {
                ship.getLocation().set(teleportTargetLocation);
            }

            // Clear teleport state
            teleportInProgress = false;
            teleportStartLocation = null;
            teleportTargetLocation = null;
            teleportVelocity = null;
            teleportProgress = 0f;

            // Clear AI custom data
            Object customData = ship.getCustomData();
            if (customData instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) customData;
                dataMap.remove("nsp_teleport_data");
            }
        }
    }

    private void applyExplosionEffects(ShipAPI ship, CombatEngineAPI engine) {
        ShipAPI victim;
        Vector2f dir;
        float force, damage, emp, mod;

        for (CombatEntityAPI tmp : NSP_Util.getEntitiesWithinRange(ship.getLocation(), EXPLOSION_PUSH_RADIUS)) {
            if ((tmp == ship) || (tmp == null)) {
                continue;
            }

            mod = 1f - (MathUtils.getDistance(ship, tmp) / EXPLOSION_PUSH_RADIUS);
            force = FORCE_VS_ASTEROID * mod;
            damage = EXPLOSION_DAMAGE_AMOUNT * mod;
            emp = EXPLOSION_EMP_DAMAGE_AMOUNT * mod;

            if (tmp instanceof ShipAPI) {
                victim = (ShipAPI) tmp;

                if (null != victim.getHullSize()) {
                    switch (victim.getHullSize()) {
                        case FIGHTER:
                            force = FORCE_VS_FIGHTER * mod;
                            damage /= DAMAGE_MOD_VS_FIGHTER;
                            break;
                        case FRIGATE:
                            force = FORCE_VS_FRIGATE * mod;
                            damage /= DAMAGE_MOD_VS_FRIGATE;
                            break;
                        case DESTROYER:
                            force = FORCE_VS_DESTROYER * mod;
                            damage /= DAMAGE_MOD_VS_DESTROYER;
                            break;
                        case CRUISER:
                            force = FORCE_VS_CRUISER * mod;
                            damage /= DAMAGE_MOD_VS_CRUISER;
                            break;
                        case CAPITAL_SHIP:
                            force = FORCE_VS_CAPITAL * mod;
                            damage /= DAMAGE_MOD_VS_CAPITAL;
                            break;
                        default:
                            break;
                    }
                }

                if (victim.getOwner() == ship.getOwner()) {
                    damage *= EXPLOSION_DAMAGE_VS_ALLIES_MODIFIER;
                    emp *= EXPLOSION_EMP_VS_ALLIES_MODIFIER;
                    force *= EXPLOSION_FORCE_VS_ALLIES_MODIFIER;
                }

                float shipRadius = NSP_Util.effectiveRadius(victim);

                if (victim.getShield() != null && victim.getShield().isOn() && victim.getShield().isWithinArc(
                        ship.getLocation())) {
                    victim.getFluxTracker().increaseFlux(damage * 2, true);
                } else {
                    for (int x = 0; x < 5; x++) {
                        engine.spawnEmpArc(ship, MathUtils.getRandomPointInCircle(victim.getLocation(),
                                        shipRadius),
                                victim, victim, EXPLOSION_DAMAGE_TYPE, damage / 5,
                                emp / 5, EXPLOSION_PUSH_RADIUS, null, 2f,
                                EXPLOSION_COLOR, EXPLOSION_COLOR);
                    }
                }
            }

            if (tmp instanceof DamagingProjectileAPI) {
                DamagingProjectileAPI proj = (DamagingProjectileAPI) tmp;
                if (proj.getBaseDamageAmount() <= 0) {
                    continue;
                }
            }

            dir = VectorUtils.getDirectionalVector(ship.getLocation(), tmp.getLocation());
            dir.scale(force);

            Vector2f.add(tmp.getVelocity(), dir, tmp.getVelocity());
        }
    }

    private void handleNovaEffects(ShipAPI ship, CombatEngineAPI engine) {
        if (novaTime >= 0f) {
            novaTime += engine.getElapsedInLastFrame() * engine.getTimeMult().getModifiedValue();
            interval.advance(engine.getElapsedInLastFrame() * engine.getTimeMult().getModifiedValue());

            if (interval.intervalElapsed()) {
                float offset = (float) Math.random() * 360f;
                for (int i = 0; i < (int) (novaTime * 5f) + 4; i++) {
                    float angle = i / ((novaTime * 5f) + 4f) * 360f + offset;
                    if (angle >= 360f) {
                        angle -= 360f;
                    }
                    float distance = (float) Math.random() * 100f + novaTime * 1500f;
                    Vector2f point1 = MathUtils.getPointOnCircumference(novaLocation, distance, angle);
                    Vector2f point2 = MathUtils.getPointOnCircumference(novaLocation, distance, angle + 360f
                            / ((novaTime * 5f) + 4f)
                            * ((float) Math.random()
                            + 1f));
                    engine.spawnEmpArc(ship, point1, new SimpleEntity(point1),
                            new SimpleEntity(point2), DamageType.ENERGY, 0f, 0f, 10000f,
                            null, 40f, EXPLOSION_COLOR, EXPLOSION_COLOR);
                }

                List<ShipAPI> targets = NSP_Util.getShipsWithinRange(novaLocation, novaTime * 1500f + 25f);
                for (ShipAPI target : targets) {
                    if (target == ship) {
                        continue;
                    }

                    float dist = MathUtils.getDistance(novaLocation, target.getLocation());
                    float dist2 = novaTime * 1500f + 50f;
                    if (dist - target.getCollisionRadius() <= dist2 && dist + target.getCollisionRadius() >= dist2) {
                        if (target.getOwner() == ship.getOwner()) {
                            engine.applyDamage(target, target.getLocation(), 300f,
                                    DamageType.ENERGY, 150f, false, false, ship, false);
                        } else {
                            engine.applyDamage(target, target.getLocation(), 3000f,
                                    DamageType.ENERGY, 1500f, false, false, ship, false);
                        }
                    }
                }
            }

            if (novaTime >= 1f) {
                novaTime = -1f;
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        // Clean up any remaining state
        if (teleportInProgress) {
            teleportInProgress = false;
            teleportStartLocation = null;
            teleportTargetLocation = null;
            teleportVelocity = null;
            teleportProgress = 0f;
        }

        // Ensure ship custom data is cleared
        if (stats.getEntity() instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) stats.getEntity();
            Object customData = ship.getCustomData();
            if (customData instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) customData;
                dataMap.remove("nsp_teleport_data");
            }
        }
    }
}