package neon.nsp.data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.util.MagicRender;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class NSP_TemporalJaunt extends BaseShipSystemScript {
    public static final Color JITTER_COLOR = new Color(255, 196, 19, 200);
    public static final Color JITTER_UNDER_COLOR = new Color(255, 196, 19, 75);
    public static float JITTER_FADE_TIME = 0.5f;

    public static float SHIP_ALPHA_MULT = 0.25f;
    public static float VULNERABLE_FRACTION = 0f;
    public static float INCOMING_DAMAGE_MULT = 0.25f;

    public static float MAX_TIME_MULT = 3.0f;

    public static boolean FLUX_LEVEL_AFFECTS_SPEED = true;
    public static float MIN_SPEED_MULT = 0.33f;
    public static float BASE_FLUX_LEVEL_FOR_MIN_SPEED = 0.5f;

    public static final float DISSIPATION_MULT = 1.5f;
    public static final Color AFTERIMAGE_COLOR = new Color(255, 196, 19, 20);

    public static final float SPEED_BONUS = 200f;
    public static final float ACCELERATION_BONUS = 400f;
    public static final float TURN_MULT = 2.0f;

    public static final Color RIPPLE_COLOR = new Color(255, 196, 19, 150);
    public static final float RIPPLE_DURATION = 1.5f;
    public static final float RIPPLE_MAX_SIZE = 2000f;

    public static final Color OVERLOAD_COLOR = new Color(229, 21, 21, 135);
    public static final Color INTERDICTION_JITTER_COLOR = new Color(255, 80, 30, 135);
    public static final Color INTERDICTION_JITTER_UNDER_COLOR = new Color(255, 80, 30, 135);
    public static final float DISRUPTION_DUR = 1.5f;
    public static final float MIN_DISRUPTION_RANGE = 800f;

    private IntervalUtil interval = new IntervalUtil(0.2f, 0.2f);
    private IntervalUtil rippleInterval = new IntervalUtil(0.3f, 0.5f);
    private IntervalUtil interdictionInterval = new IntervalUtil(0.5f, 0.5f);
    private boolean isPhased = false;
    private boolean hasTriggeredInitialRipple = false;

    protected Object STATUSKEY1 = new Object();
    protected Object STATUSKEY2 = new Object();

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (system.isActive()) {
            return "ELYSIUM DIVE ACTIVE";
        } else if (system.isCoolingDown()) {
            return "RECHARGING";
        }
        return "READY";
    }

    public void apply(MutableShipStatsAPI stats, String id, ShipSystemStatsScript.State state, float effectLevel) {
        ShipAPI ship = null;
        boolean player = false;
        CombatEngineAPI engine = Global.getCombatEngine();

        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI)stats.getEntity();
            player = (ship == Global.getCombatEngine().getPlayerShip());

            // Add visual effects listener when system is active
            if ((state == ShipSystemStatsScript.State.ACTIVE || state == ShipSystemStatsScript.State.IN) &&
                    !ship.hasListenerOfClass(TemporalJauntVisuals.class)) {
                ship.addListener(new TemporalJauntVisuals(ship));
            }
        } else {
            return;
        }

        if (!(stats.getEntity() instanceof ShipAPI)) {
            return;
        }
        id = id + "_" + ship.getId();
        if (Global.getCombatEngine().isPaused() || !ship.isAlive()) {
            return;
        }

        // Only apply phase effects when system is active
        if (state == State.IN || state == State.ACTIVE || state == State.OUT) {
            float levelForAlpha = effectLevel;
            ShipSystemAPI cloak = ship.getPhaseCloak();
            if (cloak == null) {
                cloak = ship.getSystem();
            }

            if (state == State.IN || state == State.ACTIVE) {
                ship.setPhased(true);
                levelForAlpha = effectLevel;
            }

            // Phase Cloak effects
            ship.setPhased(true);
            isPhased = true;

            // Trigger ripple effect when first entering phase space each activation
            if ((state == ShipSystemStatsScript.State.IN || (state == ShipSystemStatsScript.State.ACTIVE && effectLevel < 0.1f)) &&
                    !hasTriggeredInitialRipple) {
                createRippleEffect(ship);
                hasTriggeredInitialRipple = true;
            }

            // Damage reduction while phased
            if (effectLevel > VULNERABLE_FRACTION) {
                stats.getHullDamageTakenMult().modifyMult(id, INCOMING_DAMAGE_MULT);
                stats.getArmorDamageTakenMult().modifyMult(id, INCOMING_DAMAGE_MULT);
                stats.getShieldDamageTakenMult().modifyMult(id, INCOMING_DAMAGE_MULT);
                stats.getEmpDamageTakenMult().modifyMult(id, INCOMING_DAMAGE_MULT);
            }

            // Flux dissipation bonus
            stats.getFluxDissipation().modifyMult(id, DISSIPATION_MULT);

            // Time flow manipulation
            float timeMult = 1f + (getMaxTimeMult(stats) - 1f) * effectLevel;
            stats.getTimeMult().modifyMult(id, timeMult);

            if (player) {
                Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / timeMult);
            } else {
                Global.getCombatEngine().getTimeMult().unmodify(id);
            }

            // Movement bonuses
            if (state == ShipSystemStatsScript.State.OUT) {
                stats.getMaxSpeed().unmodify(id);
            } else {
                float speedMult = getSpeedMult(ship, effectLevel);
                stats.getMaxSpeed().modifyFlat(id, SPEED_BONUS * speedMult);
                stats.getAcceleration().modifyFlat(id, ACCELERATION_BONUS * speedMult);
                stats.getDeceleration().modifyFlat(id, ACCELERATION_BONUS * speedMult);
                stats.getTurnAcceleration().modifyMult(id, TURN_MULT);
                stats.getMaxTurnRate().modifyMult(id, TURN_MULT);
            }

            // Visual effects - jitter and ship alpha
            float jitterLevel = effectLevel;
            if (jitterLevel > 0) {
                ship.setJitter(this, JITTER_COLOR, jitterLevel, 3, 0f, jitterLevel * 10f);
                ship.setJitterUnder(this, JITTER_UNDER_COLOR, jitterLevel, 15, 0f, jitterLevel * 15f);
            }

            ship.setExtraAlphaMult(1f - (1f - SHIP_ALPHA_MULT) * effectLevel);

            // Afterimage effects
            if (!Global.getCombatEngine().isPaused()) {
                this.interval.advance(Global.getCombatEngine().getElapsedInLastFrame());
                if (this.interval.intervalElapsed()) {
                    createAfterimage(ship);
                }

                // Occasional ripple effects during phase
                this.rippleInterval.advance(Global.getCombatEngine().getElapsedInLastFrame());
                if (this.rippleInterval.intervalElapsed() && effectLevel > 0.5f) {
                    createSecondaryRipple(ship);
                }

                // Phase interdiction check
                this.interdictionInterval.advance(Global.getCombatEngine().getElapsedInLastFrame());
                if (this.interdictionInterval.intervalElapsed() && effectLevel > 0.5f) {
                    applyPhaseInterdiction(ship);
                }
            }
        }

        // Update player status display
        if (player) {
            maintainStatus(ship, state, effectLevel);
        }
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        // Remove all stat modifications
        stats.getFluxDissipation().unmodify(id);

        stats.getHullDamageTakenMult().unmodify(id);
        stats.getArmorDamageTakenMult().unmodify(id);
        stats.getShieldDamageTakenMult().unmodify(id);
        stats.getEmpDamageTakenMult().unmodify(id);

        Global.getCombatEngine().getTimeMult().unmodify(id);
        stats.getTimeMult().unmodify(id);

        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);

        // Remove visual effects and reset ship state
        if (stats.getEntity() instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) stats.getEntity();
            ship.removeListenerOfClass(TemporalJauntVisuals.class);
            ship.setPhased(false);
            ship.setExtraAlphaMult(1f);
            ship.setJitter(this, JITTER_COLOR, 0f, 0, 0f, 0f);
            ship.setJitterUnder(this, JITTER_UNDER_COLOR, 0f, 0, 0f, 0f);
            isPhased = false;
            hasTriggeredInitialRipple = false;
        }
    }

    // Phase Interdiction Methods
    protected void applyPhaseInterdiction(ShipAPI ship) {
        List<ShipAPI> targets = findTargetsInRange(ship);

        for (ShipAPI target : targets) {
            if (target != ship && target.isPhased() && !target.getFluxTracker().isOverloadedOrVenting()) {
                applyEffectToTarget(ship, target);
                createInterdictionEffect(ship, target);
            }
        }
    }

    protected List<ShipAPI> findTargetsInRange(ShipAPI ship) {
        List<ShipAPI> targets = new ArrayList<>();
        float range = getMaxRange(ship);

        for (ShipAPI otherShip : Global.getCombatEngine().getShips()) {
            if (otherShip.getOwner() == ship.getOwner()) continue;
            if (!otherShip.isAlive()) continue;

            float dist = Misc.getDistance(ship.getLocation(), otherShip.getLocation());
            float radSum = ship.getCollisionRadius() + otherShip.getCollisionRadius();

            if (dist <= range + radSum) {
                targets.add(otherShip);
            }
        }

        return targets;
    }

    protected float getMaxRange(ShipAPI ship) {
        return MIN_DISRUPTION_RANGE;
    }

    protected void applyEffectToTarget(final ShipAPI ship, final ShipAPI target) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (target.getFluxTracker().isOverloadedOrVenting()) {
            return;
        }
        if (target == ship) return;

        target.setOverloadColor(OVERLOAD_COLOR);
        target.getFluxTracker().beginOverloadWithTotalBaseDuration(DISRUPTION_DUR);

        if (target.getFluxTracker().showFloaty() ||
                ship == engine.getPlayerShip() ||
                target == engine.getPlayerShip()) {
            target.getFluxTracker().playOverloadSound();
            target.getFluxTracker().showOverloadFloatyIfNeeded("System Disruption!", OVERLOAD_COLOR, 4f, true);
        }

        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            public void advance(float amount) {
                if (!target.getFluxTracker().isOverloadedOrVenting()) {
                    target.resetOverloadColor();
                    Global.getCombatEngine().removePlugin(this);
                }
            }
        });
    }

    protected void createInterdictionEffect(ShipAPI source, ShipAPI target) {
        Vector2f midPoint = new Vector2f(
                (source.getLocation().x + target.getLocation().x) * 0.5f,
                (source.getLocation().y + target.getLocation().y) * 0.5f
        );

        float distance = Misc.getDistance(source.getLocation(), target.getLocation());
        float angle = Misc.getAngleInDegrees(source.getLocation(), target.getLocation());

        SpriteAPI interdictionSprite = Global.getSettings().getSprite("fx", "nsp_enlightenment");
        MagicRender.battlespace(
                interdictionSprite,
                midPoint,
                new Vector2f(0f, 0f),
                new Vector2f(distance * 0.8f, 20f),
                new Vector2f(distance, 40f),
                angle,
                0f,
                INTERDICTION_JITTER_COLOR,
                true,
                0f,
                0f,
                0.1f,
                0.3f,
                0f,
                0.05f,
                0.1f,
                0.2f,
                CombatEngineLayers.ABOVE_SHIPS_LAYER
        );

        target.setJitter(source, INTERDICTION_JITTER_COLOR, 1f, 5, 0f, 10f);
        target.setJitterUnder(source, INTERDICTION_JITTER_UNDER_COLOR, 1f, 15, 0f, 15f);
    }

    private void createRippleEffect(ShipAPI ship) {
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

    private void createSecondaryRipple(ShipAPI ship) {
        SpriteAPI rippleSprite = Global.getSettings().getSprite("fx", "shield_ring");
        Vector2f ripplePos = MathUtils.getPointOnCircumference(
                ship.getLocation(),
                MathUtils.getRandomNumberInRange(0f, ship.getCollisionRadius() * 0.5f),
                MathUtils.getRandomNumberInRange(0f, 360f)
        );

        MagicRender.battlespace(
                rippleSprite,
                ripplePos,
                new Vector2f(0f, 0f),
                new Vector2f(20f, 20f),
                new Vector2f(RIPPLE_MAX_SIZE * 0.3f, RIPPLE_MAX_SIZE * 0.3f),
                MathUtils.getRandomNumberInRange(0f, 360f),
                0f,
                new Color(RIPPLE_COLOR.getRed(), RIPPLE_COLOR.getGreen(), RIPPLE_COLOR.getBlue(), 80),
                true,
                0f,
                0.05f,
                0.15f,
                RIPPLE_DURATION * 0.5f,
                0f,
                0.05f,
                0.1f,
                0.3f,
                CombatEngineLayers.ABOVE_SHIPS_LAYER
        );
    }

    public static float getMaxTimeMult(MutableShipStatsAPI stats) {
        return 1f + (MAX_TIME_MULT - 1f) * stats.getDynamic().getValue(Stats.PHASE_TIME_BONUS_MULT);
    }

    protected float getDisruptionLevel(ShipAPI ship) {
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

    protected void maintainStatus(ShipAPI playerShip, ShipSystemStatsScript.State state, float effectLevel) {
        float level = effectLevel;

        ShipSystemAPI cloak = playerShip.getPhaseCloak();
        if (cloak == null) cloak = playerShip.getSystem();
        if (cloak == null) return;

        if (level > VULNERABLE_FRACTION) {
            Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY1,
                    cloak.getSpecAPI().getIconSpriteName(), cloak.getDisplayName(), "SHOW THEM THE LIGHT", false);
        }

        List<ShipAPI> targets = findTargetsInRange(playerShip);
        int phasedTargets = 0;
        for (ShipAPI target : targets) {
            if (target.isPhased() && !target.getFluxTracker().isOverloadedOrVenting()) {
                phasedTargets++;
            }
        }

        if (phasedTargets > 0) {
            Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY2,
                    "graphics/icons/hullsys/phase_anchor.png",
                    "Phase Interdiction",
                    phasedTargets + " phased target" + (phasedTargets > 1 ? "s" : "") + " in range",
                    false);
        }
    }

    public float getSpeedMult(ShipAPI ship, float effectLevel) {
        if (getDisruptionLevel(ship) <= 0f) return 1f;
        return MIN_SPEED_MULT + (1f - MIN_SPEED_MULT) * (1f - getDisruptionLevel(ship) * effectLevel);
    }

    private void createAfterimage(ShipAPI ship) {
        SpriteAPI sprite = ship.getSpriteAPI();
        float offsetX = sprite.getWidth() / 2.0f - sprite.getCenterX();
        float offsetY = sprite.getHeight() / 2.0f - sprite.getCenterY();
        float trueOffsetX = (float)FastTrig.cos(Math.toRadians((ship.getFacing() - 90.0f))) * offsetX - (float)FastTrig.sin(Math.toRadians((ship.getFacing() - 90.0f))) * offsetY;
        float trueOffsetY = (float)FastTrig.sin(Math.toRadians((ship.getFacing() - 90.0f))) * offsetX + (float)FastTrig.cos(Math.toRadians((ship.getFacing() - 90.0f))) * offsetY;

        MagicRender.battlespace(
                Global.getSettings().getSprite(ship.getHullSpec().getSpriteName()),
                new Vector2f(ship.getLocation().getX() + trueOffsetX, ship.getLocation().getY() + trueOffsetY),
                new Vector2f(0.0f, 0.0f),
                new Vector2f(ship.getSpriteAPI().getWidth(), ship.getSpriteAPI().getHeight()),
                new Vector2f(0.0f, 0.0f),
                ship.getFacing() - 90.0f,
                0.0f,
                AFTERIMAGE_COLOR,
                true,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.1f,
                0.1f,
                1.0f,
                CombatEngineLayers.BELOW_SHIPS_LAYER
        );
    }

    // Updated visual effects listener class
    public static class TemporalJauntVisuals implements AdvanceableListener {
        private ShipAPI ship;
        private SpriteAPI sprite1;
        private Color color;
        private Color color2;
        private boolean started = false;
        private float rotation = 0f;
        private IntervalUtil pulse = new IntervalUtil(2.5f, 2.5f);
        private IntervalUtil distortionInterval = new IntervalUtil(1.0f, 2.0f);

        public TemporalJauntVisuals(ShipAPI ship) {
            this.ship = ship;
            this.sprite1 = Global.getSettings().getSprite("fx", "nsp_enlightenment");
            this.color = new Color(255, 196, 19, 100);
            this.color2 = new Color(255, 196, 19, 140);
        }

        @Override
        public void advance(float amount) {
            if (!ship.isAlive()) {
                ship.removeListener(this);
                return;
            }

            if (!started) {
                Global.getCombatEngine().addFloatingText(
                        new Vector2f(ship.getLocation().x, ship.getLocation().y + ship.getShieldRadiusEvenIfNoShield()),
                        getRandomText(),
                        32f,
                        color2,
                        ship,
                        1f,
                        3f
                );

                MagicRender.objectspace(
                        sprite1,
                        ship,
                        new Vector2f(0f, 0f),
                        new Vector2f(0f, 0f),
                        new Vector2f(1000, 1000f),
                        new Vector2f(),
                        0f,
                        12f,
                        false,
                        color,
                        false,
                        0f,
                        0f,
                        0f,
                        0f,
                        0f,
                        1f,
                        3f,
                        2f,
                        true,
                        CombatEngineLayers.BELOW_SHIPS_LAYER
                );

                started = true;
            }

            if (distortionInterval.intervalElapsed()) {
                createDistortionWave(ship);
            }

            if (pulse.intervalElapsed()) {
                for (int i = 0; i <= 15; i++) {
                    Vector2f point = MathUtils.getPointOnCircumference(
                            ship.getLocation(),
                            ship.getShieldRadiusEvenIfNoShield() * 3f,
                            MathUtils.getRandomNumberInRange(0f, 360f)
                    );
                    Vector2f velocity = new Vector2f();
                    Vector2f.sub(ship.getLocation(), point, velocity);
                    Global.getCombatEngine().addSmoothParticle(
                            point,
                            velocity,
                            MathUtils.getRandomNumberInRange(8f, 20f),
                            MathUtils.getRandomNumberInRange(0.4f, 1.5f),
                            1f,
                            color2
                    );
                }
            }

            rotation += 0.5f;
            if (rotation > 360f) rotation = 0f;
        }

        private void createDistortionWave(ShipAPI ship) {
            SpriteAPI distortionSprite = Global.getSettings().getSprite("fx", "shield_ring");
            Vector2f wavePos = MathUtils.getPointOnCircumference(
                    ship.getLocation(),
                    MathUtils.getRandomNumberInRange(ship.getCollisionRadius() * 0.5f, ship.getCollisionRadius() * 1.5f),
                    MathUtils.getRandomNumberInRange(0f, 360f)
            );

            MagicRender.battlespace(
                    distortionSprite,
                    wavePos,
                    new Vector2f(0f, 0f),
                    new Vector2f(30f, 30f),
                    new Vector2f(800f, 800f),
                    MathUtils.getRandomNumberInRange(0f, 360f),
                    0f,
                    new Color(150, 100, 255, 60),
                    true,
                    0f,
                    0.1f,
                    0.2f,
                    1.0f,
                    0f,
                    0.05f,
                    0.1f,
                    0.4f,
                    CombatEngineLayers.ABOVE_SHIPS_LAYER
            );
        }
    }

    private static String getRandomText() {
        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();
        picker.add("LET SHINE OUR LIGHT");
        picker.add("SHOW THEM THE LIGHT");
        picker.add("OPEN THEIR MINDS");
        picker.add("WE WILL SHOW THEM ALL");
        picker.add("I WILL SHOW THEM ALL");
        picker.add("DELIVERANCE");
        picker.add("SALVATION");
        picker.add("ABSOLUTION");
        picker.add("ENLIGHTENMENT");
        return picker.pick();
    }
}

