package neon.nsp.data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import java.awt.Color;

public class NSP_FluxSpinner implements EveryFrameWeaponEffectPlugin {

    private float angle = 0;
    private float baseTurnRate = 0;
    private boolean runOnce = true;
    private final float MIN_SPIN_MULTIPLIER = 1f;    // Minimum speed at 0% flux
    private final float MAX_SPIN_MULTIPLIER = 3f;    // Maximum speed at 100% flux

    // Opacity control
    private float currentOpacity = 0f;
    private final float MAX_OPACITY = 1f;           // Fully visible at 100% flux
    private final float MIN_OPACITY = 0f;           // Fully transparent at 0% flux
    private final float OPACITY_CHANGE_RATE = 2.5f; // How quickly opacity changes

    // Jitter effect - now just visual, no position offset
    private float jitterLevel = 0f;
    private final float MAX_JITTER = 0.6f;          // Maximum jitter intensity
    private final Color JITTER_COLOR = new Color(150, 180, 255, 100);
    private final Color JITTER_UNDER_COLOR = new Color(100, 150, 255, 150);

    // Store engine reference
    private CombatEngineAPI engine = null;

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) return;
        if (!weapon.getShip().isAlive()) return;

        // Store engine reference
        this.engine = engine;

        if (runOnce) {
            baseTurnRate = weapon.getSpec().getTurnRate();
            runOnce = false;
        }

        // Get ship's current flux level (0.0 to 1.0)
        ShipAPI ship = weapon.getShip();
        float fluxRatio = ship.getFluxTracker().getFluxLevel();

        // === SPINNING LOGIC ===
        // Calculate spin multiplier based on flux level (instant response)
        float spinMultiplier = MIN_SPIN_MULTIPLIER +
                (MAX_SPIN_MULTIPLIER - MIN_SPIN_MULTIPLIER) * fluxRatio;

        // Update angle based on flux-enhanced turn rate
        float effectiveTurnRate = baseTurnRate * spinMultiplier;
        angle += effectiveTurnRate * amount;

        // Keep angle within 0-360 degrees
        if (angle > 360f) {
            angle -= 360f;
        } else if (angle < 0f) {
            angle += 360f;
        }

        // Apply rotation (KEEP WEAPON CENTERED ON MOUNT)
        weapon.setCurrAngle(weapon.getShip().getFacing() + weapon.getSlot().getAngle() + angle);

        // === OPACITY LOGIC ===
        // Calculate target opacity based on flux level
        // At 0% flux: 0 opacity (invisible)
        // At 100% flux: 1 opacity (fully visible)
        float targetOpacity = MIN_OPACITY + (MAX_OPACITY - MIN_OPACITY) * fluxRatio;

        // Smoothly interpolate current opacity toward target opacity
        if (currentOpacity < targetOpacity) {
            currentOpacity = Math.min(targetOpacity,
                    currentOpacity + OPACITY_CHANGE_RATE * amount);
        } else if (currentOpacity > targetOpacity) {
            currentOpacity = Math.max(targetOpacity,
                    currentOpacity - OPACITY_CHANGE_RATE * amount);
        }

        // Ensure opacity stays within bounds
        currentOpacity = Math.max(MIN_OPACITY, Math.min(MAX_OPACITY, currentOpacity));

        // === JITTER LOGIC (visual only, no position offset) ===
        // Jitter intensity follows flux level
        float targetJitterLevel = fluxRatio * MAX_JITTER;

        // Smooth jitter transitions
        if (jitterLevel < targetJitterLevel) {
            jitterLevel = Math.min(targetJitterLevel, jitterLevel + 3f * amount);
        } else if (jitterLevel > targetJitterLevel) {
            jitterLevel = Math.max(targetJitterLevel, jitterLevel - 3f * amount);
        }

        // Apply opacity to weapon sprite (MAKE INVISIBLE AT 0 FLUX)
        applyOpacityToWeapon(weapon, currentOpacity);

        // Apply visual jitter effects (particles, but NO position offset)
        if (jitterLevel > 0.01f) {
            createVisualJitterEffects(weapon, jitterLevel);
        }

        // Create flux-based visual effects
        if (currentOpacity > 0.1f) {
            createFluxVisualEffects(weapon, fluxRatio, currentOpacity);
        }
    }

    private void applyOpacityToWeapon(WeaponAPI weapon, float opacity) {
        // CRITICAL: Make weapon invisible at 0 opacity
        // DO NOT CHANGE WEAPON POSITION - keep it centered on mount

        if (weapon.getSprite() != null) {
            // Only adjust opacity, not position
            weapon.getSprite().setAlphaMult(opacity);

            // Also set the weapon's render alpha directly
            // This ensures the weapon is truly invisible when opacity is 0
            if (opacity <= 0.01f) {
                weapon.getSprite().setColor(new Color(255, 255, 255, 0));
            } else {
                weapon.getSprite().setColor(new Color(255, 255, 255, (int)(opacity * 255)));
            }
        }

        // Apply to glow sprite too
        if (weapon.getGlowSpriteAPI() != null) {
            weapon.getGlowSpriteAPI().setAlphaMult(opacity);
            if (opacity <= 0.01f) {
                weapon.getGlowSpriteAPI().setColor(new Color(255, 255, 255, 0));
            }
        }
    }

    private void createVisualJitterEffects(WeaponAPI weapon, float jitterIntensity) {
        // Create visual jitter effects WITHOUT moving the weapon sprite
        // This creates the illusion of jitter through particles and overlays

        if (engine == null) return;

        float time = engine.getTotalElapsedTime(false);

        // Create jitter particles around the weapon (but don't move the weapon)
        if (Math.random() < jitterIntensity * 0.3f) {
            // Calculate weapon position (center of mount)
            Vector2f weaponPos = weapon.getLocation();

            // Create particles in a ring around the weapon
            for (int i = 0; i < 3; i++) {
                float particleAngle = (float)(Math.random() * Math.PI * 2);
                float distance = 5f + jitterIntensity * 10f;

                Vector2f particlePos = new Vector2f(
                        weaponPos.x + (float)Math.cos(particleAngle) * distance,
                        weaponPos.y + (float)Math.sin(particleAngle) * distance
                );

                // Velocity away from weapon center
                Vector2f particleVel = new Vector2f(
                        (float)Math.cos(particleAngle) * 20f * jitterIntensity,
                        (float)Math.sin(particleAngle) * 20f * jitterIntensity
                );

                engine.addHitParticle(
                        particlePos,
                        particleVel,
                        2f + jitterIntensity * 2f,
                        0.7f,
                        0.3f,
                        new Color(
                                JITTER_COLOR.getRed(),
                                JITTER_COLOR.getGreen(),
                                JITTER_COLOR.getBlue(),
                                (int)(jitterIntensity * 80)
                        )
                );
            }
        }

        // Create "jitter overlay" effect - visual distortion without moving weapon
        if (jitterIntensity > 0.3f && Math.random() < 0.2f) {
            createJitterOverlay(weapon, jitterIntensity);
        }
    }

    private void createJitterOverlay(WeaponAPI weapon, float intensity) {
        // Create a visual overlay that simulates jitter without moving the weapon

        // Add a bright flash at the weapon location
        engine.addHitParticle(
                weapon.getLocation(),
                new Vector2f(0, 0),
                10f * intensity,
                0.9f,
                0.15f,
                new Color(200, 220, 255, (int)(intensity * 100))
        );

        // Add radial burst effect
        for (int i = 0; i < 6; i++) {
            float angle = (float)i * 60f;
            Vector2f dir = Misc.getUnitVectorAtDegreeAngle(angle);

            Vector2f particlePos = new Vector2f(
                    weapon.getLocation().x + dir.x * 5f,
                    weapon.getLocation().y + dir.y * 5f
            );

            Vector2f particleVel = new Vector2f(dir.x * 50f * intensity, dir.y * 50f * intensity);

            engine.addHitParticle(
                    particlePos,
                    particleVel,
                    3f * intensity,
                    0.8f,
                    0.2f,
                    new Color(150, 180, 255, (int)(intensity * 120))
            );
        }
    }

    private void createFluxVisualEffects(WeaponAPI weapon, float fluxRatio, float opacity) {
        // Create visual effects based on flux level

        if (engine == null) return;

        // Particle effects increase with flux
        float particleChance = 0.05f * fluxRatio * opacity;

        if (Math.random() < particleChance) {
            // Calculate position at weapon tip
            float particleAngle = (float)Math.toRadians(weapon.getCurrAngle());
            float particleDistance = weapon.getSprite().getWidth() * 0.4f;

            float xOffset = (float)Math.cos(particleAngle) * particleDistance;
            float yOffset = (float)Math.sin(particleAngle) * particleDistance;

            Vector2f particlePos = new Vector2f(
                    weapon.getLocation().x + xOffset,
                    weapon.getLocation().y + yOffset
            );

            // Velocity in direction weapon is facing
            Vector2f particleVel = new Vector2f(xOffset, yOffset);
            particleVel.normalise();
            particleVel.scale(50f + fluxRatio * 100f);

            // Color based on flux (blue to purple)
            Color particleColor = new Color(
                    100 + (int)(fluxRatio * 100),
                    150,
                    255,
                    (int)(opacity * 150)
            );

            engine.addHitParticle(
                    particlePos,
                    particleVel,
                    3f + fluxRatio * 3f,
                    opacity * 0.9f,
                    0.25f,
                    particleColor
            );
        }

        // EMP arcs at very high flux
        if (fluxRatio > 0.8f && opacity > 0.8f && Math.random() < 0.1f) {
            createEmpArc(weapon, fluxRatio);
        }
    }

    private void createEmpArc(WeaponAPI weapon, float fluxRatio) {
        // Create EMP arc from weapon to nearby target or just visual effect

        ShipAPI ship = weapon.getShip();

        // Find a random nearby enemy
        for (ShipAPI target : engine.getShips()) {
            if (target.getOwner() != ship.getOwner() &&
                    target.isAlive() &&
                    Misc.getDistance(weapon.getLocation(), target.getLocation()) < 400f) {

                engine.spawnEmpArc(
                        ship,
                        weapon.getLocation(),
                        ship,
                        target,
                        DamageType.ENERGY,
                        0f, // no damage
                        fluxRatio * 25f, // emp strength
                        300f, // range
                        null,
                        2f, // thickness
                        new Color(100, 150, 255, (int)(fluxRatio * 100)),
                        new Color(255, 255, 255, (int)(fluxRatio * 50))
                );
                return;
            }
        }

        // If no target, create a self-contained EMP effect
        engine.spawnEmpArc(
                ship,
                weapon.getLocation(),
                ship,
                ship,
                DamageType.ENERGY,
                0f,
                fluxRatio * 15f,
                150f,
                null,
                1.5f,
                new Color(100, 150, 255, (int)(fluxRatio * 80)),
                new Color(255, 255, 255, (int)(fluxRatio * 30))
        );
    }
}