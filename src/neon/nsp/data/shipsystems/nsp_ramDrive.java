package neon.nsp.data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import org.lazywizard.lazylib.CollisionUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class nsp_ramDrive extends BaseShipSystemScript {

    private static final float BASE_EXPLOSION_SIZE = 450f;
    private static final float BASE_EXPLOSION_DURATION = 2f;
    private static final float BASE_SOUND_PITCH = 1.0f;
    private static final float BASE_SOUND_VOLUME = 3.0f;
    private static final float BASE_SPEED_BOOST = 750f;
    private static final float BASE_MASS_SCALAR = 1.25f;
    private static final float BASE_TURN_BOOST_MULT = 1.5f;


    private static final float MIN_ENGINE_LENGTH = 0.01f;
    private static final float MAX_ENGINE_LENGTH = 4f;


    public static float SELF_DAMAGE_FLUX_MULT = 0.15f;
    public static float SELF_DAMAGE_MIN_HULL = 0.2f;


    public static float MALFUNCTION_CHANCE = 0.33f;


    public static final Object ENGINE_EFFECTS_KEY = new Object();

    private static final Map<ShipAPI.HullSize, Float> EXPLOSION_SIZE_MAP = new HashMap<>();
    static {
        EXPLOSION_SIZE_MAP.put(ShipAPI.HullSize.FRIGATE, BASE_EXPLOSION_SIZE * 0.4f);
        EXPLOSION_SIZE_MAP.put(ShipAPI.HullSize.DESTROYER, BASE_EXPLOSION_SIZE * 0.6f);
        EXPLOSION_SIZE_MAP.put(ShipAPI.HullSize.CRUISER, BASE_EXPLOSION_SIZE * 0.8f);
        EXPLOSION_SIZE_MAP.put(ShipAPI.HullSize.CAPITAL_SHIP, BASE_EXPLOSION_SIZE);
        EXPLOSION_SIZE_MAP.put(ShipAPI.HullSize.DEFAULT, BASE_EXPLOSION_SIZE * 0.7f);
    }

    private static final Map<ShipAPI.HullSize, Float> SOUND_PITCH_MAP = new HashMap<>();
    static {
        SOUND_PITCH_MAP.put(ShipAPI.HullSize.FRIGATE, BASE_SOUND_PITCH * 1.15f);
        SOUND_PITCH_MAP.put(ShipAPI.HullSize.DESTROYER, BASE_SOUND_PITCH * 1.05f);
        SOUND_PITCH_MAP.put(ShipAPI.HullSize.CRUISER, BASE_SOUND_PITCH * 0.95f);
        SOUND_PITCH_MAP.put(ShipAPI.HullSize.CAPITAL_SHIP, BASE_SOUND_PITCH);
        SOUND_PITCH_MAP.put(ShipAPI.HullSize.DEFAULT, BASE_SOUND_PITCH);
    }

    private static final Map<ShipAPI.HullSize, Float> SOUND_VOLUME_MAP = new HashMap<>();
    static {
        SOUND_VOLUME_MAP.put(ShipAPI.HullSize.FRIGATE, BASE_SOUND_VOLUME * 0.6f);
        SOUND_VOLUME_MAP.put(ShipAPI.HullSize.DESTROYER, BASE_SOUND_VOLUME * 0.7f);
        SOUND_VOLUME_MAP.put(ShipAPI.HullSize.CRUISER, BASE_SOUND_VOLUME * 0.8f);
        SOUND_VOLUME_MAP.put(ShipAPI.HullSize.CAPITAL_SHIP, BASE_SOUND_VOLUME);
        SOUND_VOLUME_MAP.put(ShipAPI.HullSize.DEFAULT, BASE_SOUND_VOLUME * 0.8f);
    }

    private static final Map<ShipAPI.HullSize, Float> SPEED_BOOST_MAP = new HashMap<>();
    static {
        SPEED_BOOST_MAP.put(ShipAPI.HullSize.FRIGATE, BASE_SPEED_BOOST * 1.5f);
        SPEED_BOOST_MAP.put(ShipAPI.HullSize.DESTROYER, BASE_SPEED_BOOST * 1.4f);
        SPEED_BOOST_MAP.put(ShipAPI.HullSize.CRUISER, BASE_SPEED_BOOST * 1.3f);
        SPEED_BOOST_MAP.put(ShipAPI.HullSize.CAPITAL_SHIP, BASE_SPEED_BOOST);
        SPEED_BOOST_MAP.put(ShipAPI.HullSize.DEFAULT, BASE_SPEED_BOOST * 1.15f);
    }

    private static final Map<ShipAPI.HullSize, Float> MASS_SCALAR_MAP = new HashMap<>();
    static {
        MASS_SCALAR_MAP.put(ShipAPI.HullSize.FRIGATE, BASE_MASS_SCALAR * 2f);
        MASS_SCALAR_MAP.put(ShipAPI.HullSize.DESTROYER, BASE_MASS_SCALAR * 1.6f);
        MASS_SCALAR_MAP.put(ShipAPI.HullSize.CRUISER, BASE_MASS_SCALAR * 1.35f);
        MASS_SCALAR_MAP.put(ShipAPI.HullSize.CAPITAL_SHIP, BASE_MASS_SCALAR);
        MASS_SCALAR_MAP.put(ShipAPI.HullSize.DEFAULT, BASE_MASS_SCALAR * 1.2f);
    }

    private static final Map<ShipAPI.HullSize, Float> TURN_BOOST_MULT_MAP = new HashMap<>();
    static {
        TURN_BOOST_MULT_MAP.put(ShipAPI.HullSize.FRIGATE, BASE_TURN_BOOST_MULT * 1.25f);
        TURN_BOOST_MULT_MAP.put(ShipAPI.HullSize.DESTROYER, BASE_TURN_BOOST_MULT * 1.2f);
        TURN_BOOST_MULT_MAP.put(ShipAPI.HullSize.CRUISER, BASE_TURN_BOOST_MULT * 1.15f);
        TURN_BOOST_MULT_MAP.put(ShipAPI.HullSize.CAPITAL_SHIP, BASE_TURN_BOOST_MULT);
        TURN_BOOST_MULT_MAP.put(ShipAPI.HullSize.DEFAULT, BASE_TURN_BOOST_MULT * 1.2f);
    }

    public static final float RESIST_MULT = 0.5f;
    public static final float TURN_REDUCTION = 0.1f;
    public static final float SELF_DAMAGE = 0f;
    public static final float CHARGEUP_MAX_SPEED_MULT = 0.2f;
    public static final float CHARGEUP_ACCEL_MULT = 0.1f;

    public static final String RAM_DRIVE_CHARGE_SOUND_ID = "nsp_rammingdrive_start";
    public static final String RAM_DRIVE_ACTIVE_SOUND_ID = "nsp_rammingdrive_loop";

    private float currentMassScalar = BASE_MASS_SCALAR;
    private boolean primed = false;
    private boolean upMassed = false;
    private boolean hasAppliedMalfunction = false;
    private Random random = new Random();
    private float currentEngineLength = 0f;

    public static Vector2f calculateHeader(float facing) {
        double angle = Math.toRadians(facing);
        Vector2f dir = new Vector2f((float) Math.cos(angle), (float) Math.sin(angle));
        if (dir.lengthSquared() > 0f) dir.normalise();
        return dir;
    }


    protected void updateEngineEffects(ShipAPI ship, float effectLevel, State state) {
        float targetLength = 0f;

        if (state == State.IN) {

            float t = effectLevel;

            float easedT = t * t * (3f - 2f * t);
            targetLength = MIN_ENGINE_LENGTH + (MAX_ENGINE_LENGTH - MIN_ENGINE_LENGTH) * easedT;

        } else if (state == State.ACTIVE) {

            targetLength = MAX_ENGINE_LENGTH;

        } else if (state == State.OUT) {

            float t = effectLevel;

            float easedT = t * t * (3f - 2f * t);
            targetLength = MIN_ENGINE_LENGTH + (MAX_ENGINE_LENGTH - MIN_ENGINE_LENGTH) * easedT;

        } else {

            targetLength = 0f;
        }


        float rampSpeed = 0.15f;
        currentEngineLength = currentEngineLength * (1f - rampSpeed) + targetLength * rampSpeed;


        ship.getEngineController().extendFlame(ENGINE_EFFECTS_KEY, currentEngineLength, 0f, 0f);
    }

    protected void applySelfDamage(ShipAPI ship, float amount, float effectLevel) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        if (ship.getHullLevel() >= SELF_DAMAGE_MIN_HULL) {
            float scaledDamage = amount * SELF_DAMAGE_FLUX_MULT * ship.getVariant().getHullSpec().getFluxCapacity() * effectLevel;

            Vector2f point = new Vector2f(ship.getLocation());
            point.x += ship.getCollisionRadius() * ((float) Math.random() * 2f - 1f);
            point.y += ship.getCollisionRadius() * ((float) Math.random() * 2f - 1f);

            if (!CollisionUtils.isPointWithinBounds(point, ship)) {
                point = CollisionUtils.getNearestPointOnBounds(point, ship);
            }

            engine.applyDamage(ship, point, scaledDamage, DamageType.OTHER, 0f, true, false, null);
        }
    }

    protected void applyMalfunction(ShipAPI ship) {
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.getSlot().isSystemSlot()) continue;
            if (weapon.isDecorative()) continue;
            if (weapon.getType() == WeaponAPI.WeaponType.MISSILE) continue;

            if (random.nextFloat() < MALFUNCTION_CHANCE) {
                weapon.disable();
            }
        }
    }


    protected void fireDecoWeapons(ShipAPI ship) {
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.isDecorative() && w.getSpec().hasTag(Tags.NOVA)) {
                w.setForceFireOneFrame(true);
                w.forceShowBeamGlow();
            }
        }
    }


    protected void createEngineExplosions(ShipAPI ship, float explosionSize, float explosionDuration, Color explosionColor) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        for (ShipEngineControllerAPI.ShipEngineAPI engineSlot : ship.getEngineController().getShipEngines()) {
            Vector2f engineLocation = engineSlot.getLocation();

            float angleRad = (float) Math.toRadians(ship.getFacing() + 180f);
            Vector2f offset = new Vector2f((float) Math.cos(angleRad) * 50f, (float) Math.sin(angleRad) * 50f);

            Vector2f explosionLocation = Vector2f.add(engineLocation, offset, new Vector2f());

            float perEngineSize = explosionSize * 0.5f;

            engine.spawnExplosion(explosionLocation, new Vector2f(), explosionColor, perEngineSize, explosionDuration);
        }
    }

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        ShipAPI.HullSize hullSize = ship.getHullSize();

        float explosionSize = EXPLOSION_SIZE_MAP.getOrDefault(hullSize, EXPLOSION_SIZE_MAP.get(ShipAPI.HullSize.DEFAULT));
        float soundPitch = SOUND_PITCH_MAP.getOrDefault(hullSize, SOUND_PITCH_MAP.get(ShipAPI.HullSize.DEFAULT));
        float soundVolume = SOUND_VOLUME_MAP.getOrDefault(hullSize, SOUND_VOLUME_MAP.get(ShipAPI.HullSize.DEFAULT));
        float speedBoost = SPEED_BOOST_MAP.getOrDefault(hullSize, SPEED_BOOST_MAP.get(ShipAPI.HullSize.DEFAULT));
        currentMassScalar = MASS_SCALAR_MAP.getOrDefault(hullSize, MASS_SCALAR_MAP.get(ShipAPI.HullSize.DEFAULT));
        float turnBoostMult = TURN_BOOST_MULT_MAP.getOrDefault(hullSize, TURN_BOOST_MULT_MAP.get(ShipAPI.HullSize.DEFAULT));

        Color explosionColor = new Color(255, 163, 135, 255);
        Color jitterColor = new Color(255, 84, 84, 64);


        updateEngineEffects(ship, effectLevel, state);

        if (state == State.OUT) {
            stats.getMaxSpeed().unmodify(id);
            stats.getDeceleration().unmodify(id);
            if (ship.getVelocity().length() > stats.getMaxSpeed().getModifiedValue())
                ship.getVelocity().scale(0.95f);

            if (!hasAppliedMalfunction) {
                applyMalfunction(ship);
                hasAppliedMalfunction = true;
            }

        } else if (state == State.ACTIVE) {
            if (primed) {
                primed = false;

                stats.getMaxSpeed().unmodify(id);
                stats.getAcceleration().unmodify(id);
                stats.getMaxTurnRate().unmodify(id);
                stats.getTurnAcceleration().unmodify(id);

                Vector2f header = calculateHeader(ship.getFacing());
                ship.getVelocity().set(header);
                ship.getVelocity().scale(speedBoost);

                float baseMaxSpeed = stats.getMaxSpeed().getBaseValue();
                stats.getMaxSpeed().modifyFlat(id, speedBoost - baseMaxSpeed);


                fireDecoWeapons(ship);


                createEngineExplosions(ship, explosionSize, BASE_EXPLOSION_DURATION, explosionColor);

                Vector2f behindShip = new Vector2f(header);
                behindShip.scale(-180f);
                Vector2f damageLocation = Vector2f.add(ship.getLocation(), behindShip, new Vector2f());

                ship.setMass(ship.getMass() * currentMassScalar);
                for (ShipAPI s : ship.getChildModulesCopy()) {
                    if (s.isAlive()) s.setMass(s.getMass() * currentMassScalar);
                }

                Global.getSoundPlayer().playSound(RAM_DRIVE_ACTIVE_SOUND_ID, soundPitch, soundVolume, ship.getLocation(), ship.getVelocity());

                stats.getEngineDamageTakenMult().modifyMult(id, 0f);
                stats.getArmorDamageTakenMult().unmodify(id);
                stats.getHullDamageTakenMult().unmodify(id);
                for (ShipAPI module : ship.getChildModulesCopy()) {
                    if (module.isAlive()) {
                        module.getMutableStats().getArmorDamageTakenMult().unmodify(id);
                        module.getMutableStats().getHullDamageTakenMult().unmodify(id);
                    }
                }

                Global.getCombatEngine().applyDamage(ship, damageLocation, SELF_DAMAGE, DamageType.HIGH_EXPLOSIVE, 0f, true, false, null);

                stats.getEngineDamageTakenMult().unmodify(id);
                stats.getArmorDamageTakenMult().modifyMult(id, RESIST_MULT);
                stats.getHullDamageTakenMult().modifyMult(id, RESIST_MULT);
                for (ShipAPI module : ship.getChildModulesCopy()) {
                    if (module.isAlive()) {
                        module.getMutableStats().getArmorDamageTakenMult().modifyMult(id, RESIST_MULT);
                        module.getMutableStats().getHullDamageTakenMult().modifyMult(id, RESIST_MULT);
                    }
                }
                upMassed = true;
            }

            stats.getDeceleration().modifyMult(id, 0f);
            stats.getMaxTurnRate().modifyMult(id, TURN_REDUCTION);
            stats.getTurnAcceleration().modifyMult(id, TURN_REDUCTION);
            ship.giveCommand(ShipCommand.ACCELERATE, null, 0);

            float amount = Global.getCombatEngine().getElapsedInLastFrame();
            applySelfDamage(ship, amount, effectLevel);

            hasAppliedMalfunction = false;

        } else if (state == State.IN) {
            if (!primed) {
                Global.getSoundPlayer().playSound(RAM_DRIVE_CHARGE_SOUND_ID, soundPitch * 0.9f, soundVolume * 0.8f, ship.getLocation(), ship.getVelocity());
            }
            primed = true;

            stats.getMaxSpeed().modifyMult(id, CHARGEUP_MAX_SPEED_MULT);
            stats.getAcceleration().modifyMult(id, CHARGEUP_ACCEL_MULT);

            stats.getMaxTurnRate().modifyMult(id, turnBoostMult);
            stats.getTurnAcceleration().modifyMult(id, turnBoostMult * 2f);

            stats.getArmorDamageTakenMult().modifyMult(id, RESIST_MULT);
            stats.getHullDamageTakenMult().modifyMult(id, RESIST_MULT);
            for (ShipAPI module : ship.getChildModulesCopy()) {
                if (module.isAlive()) {
                    module.getMutableStats().getArmorDamageTakenMult().modifyMult(id, RESIST_MULT);
                    module.getMutableStats().getHullDamageTakenMult().modifyMult(id, RESIST_MULT);
                }
            }

            ship.setJitter(id, jitterColor, effectLevel, 5, 50f * effectLevel);
            for (ShipAPI s : ship.getChildModulesCopy()) {
                if (s.isAlive()) s.setJitter(id + s.getId(), jitterColor, effectLevel, 5, 50f * effectLevel);
            }

            float amount = Global.getCombatEngine().getElapsedInLastFrame();
            applySelfDamage(ship, amount, effectLevel);
        }

        if (state != State.IN) {
            stats.getMissileMaxSpeedBonus().modifyFlat(id, ship.getVelocity().length());
        }
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) return;

        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getEngineDamageTakenMult().unmodify(id);

        stats.getArmorDamageTakenMult().unmodify(id);
        stats.getHullDamageTakenMult().unmodify(id);
        for (ShipAPI module : ship.getChildModulesCopy()) {
            module.getMutableStats().getArmorDamageTakenMult().unmodify(id);
            module.getMutableStats().getHullDamageTakenMult().unmodify(id);
        }

        stats.getMissileMaxSpeedBonus().unmodify(id);

        if (upMassed) {
            float massUnscalar = 1f / currentMassScalar;
            ship.setMass(ship.getMass() * massUnscalar);
            for (ShipAPI s : ship.getChildModulesCopy()) {
                if (s.isAlive() && s.getShipTarget() == null && s.getParentStation() == ship) {
                    s.setMass(s.getMass() * massUnscalar);
                }
            }
            upMassed = false;
        }


        if (ship != null) {
            currentEngineLength = 0f;
            ship.getEngineController().extendFlame(ENGINE_EFFECTS_KEY, 0f, 0f, 0f);
        }

        hasAppliedMalfunction = false;
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        return null;
    }
}