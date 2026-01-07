package neon.nsp.data.hullmods;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.impl.combat.dweller.ShroudedLensHullmod;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import neon.nsp.data.plugins.NSP_ExponentCore;
import neon.nsp.data.scripts.util.NSPSandevistan2;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class NSPSandyEffect2 extends BaseHullMod {
    public static float SHIELD_BONUS_TURN = 120f;
    public static float SHIELD_BONUS_UNFOLD = 120f;
    public static float MISSILE_ROF_MULT = 0.3f;
    public static float ARMOR_DAMAGE_MULT = 3f;
    public static float HULL_DAMAGE_MULT = 3f;
    public static float EMP_DAMAGE_MULT = 0.5f;
    public static float ENERGY_WEAPON_FLUX_INCREASE = 50f;
    public static final float RESISTANCE = 0.35f;
    public static final float RESIST_TIME = 0.1f;
    public static float AMMO_BONUS = 50f;
    public static float REGEN_BONUS = 100f;
    public static float RANGE_BONUS = 200;
    public static float CREWED_RANGE_BONUS = 200;
    public static float COST_REDUCTION1 = 5f;
    public static float COST_REDUCTION2 = 7f;

    // Explosion system constants
    public static float MAX_RANGE = 1000f;
    public static float RADIUS = 100f;
    public static float REFIRE_DELAY = 0.8f;
    public static float FLUX_PER_DAMAGE = 1f;
    public static float DAMAGE = 90f;
    //public static float MIN_ROF_MULT = 1f;
    //public static float MAX_ROF_MULT = 4f;

    // Visual/Audio effects
    public static float MAX_JITTER_INTENSITY = 1.0f;
    public static float JITTER_DURATION = 0.1f;
    public static Color JITTER_COLOR = new Color(255, 196, 19, 75);
    public static String AUDIO_FILE = "nsp_sandevistan_loop";
    public static float MIN_VOLUME = 0f;
    public static float MAX_VOLUME = 3f;
    public static float VOLUME_RAMP_SPEED = 10f;

    // Weapon flux reduction system
    // public static float MAX_FLUX_REDUCTION = 50f;
    // public static float FLUX_REDUCTION_PER_KILL = 12.5f;

    // Fire rate bonus system
    //public static float MAX_FIRE_RATE_BONUS = 25f;
    //public static float FIRE_RATE_PER_KILL = 6.25f;

    // Flux dissipation bonus system
    //public static float MAX_FLUX_DISSIPATION_BONUS = 50f;
    //public static float FLUX_DISSIPATION_PER_KILL = 12.5f;

    // Data key for explosion system
    public static String EXPLOSION_DATA_KEY = "nsp_ShroudedLensHullmod_data_key";

    // Mote System Constants
    public static final int MAX_MOTES = 12;
    public static final float MOTE_SPAWN_INTERVAL = 0.75f;
    public static final Color MOTE_JITTER_COLOR = new Color(255, 196, 19, 175);
    public static final String MOTE_DATA_KEY = "nsp_sandy_mote_data";

    private static final Set<String> BLOCKED_HULLMODS = new HashSet<>();
    private Set<ShipAPI> nearbyShips = new HashSet<>();
    private float resisting = 0f;

    static {
        BLOCKED_HULLMODS.add("safetyoverrides");
        BLOCKED_HULLMODS.add("shield_shunt");
        BLOCKED_HULLMODS.add("heavyarmor");
        BLOCKED_HULLMODS.add("advancedshieldemitter");
        BLOCKED_HULLMODS.add("magazines");
    }

    // Data class for explosion system
    public static class ExplosionSystemData {
        public float untilAttack = 0f;
        public float sinceAttack = 1000f;
    }

    // Mote System Data
    public static class MoteSystemData {
        public List<MissileAPI> motes = new ArrayList<>();
        public float elapsed = 0f;
        public CombatEntityAPI lastBeamHit = null;
        public Vector2f lastHitLocation = null;
        public float lastHitTime = 0f;
        public IntervalUtil moteSpawnInterval = new IntervalUtil(MOTE_SPAWN_INTERVAL, MOTE_SPAWN_INTERVAL * 1.5f);
    }

    // Shared Mote AI Data
    public static class SharedMoteAIData {
        public List<MissileAPI> motes = new ArrayList<>();
        public float elapsed = 0f;
        public ShipAPI attractorLock = null;
        public Vector2f attractorTarget = null;
        public float attractorRemaining = 0f;
    }

    // Weapon slot management for motes
    protected WeightedRandomPicker<WeaponSlotAPI> launchSlots = new WeightedRandomPicker<WeaponSlotAPI>();
    protected WeaponSlotAPI attractor = null;

    // Get mote system data
    public static MoteSystemData getMoteData(ShipAPI source) {
        CombatEngineAPI engine = Global.getCombatEngine();
        String key = MOTE_DATA_KEY + "_" + source.getId();
        MoteSystemData data = (MoteSystemData) engine.getCustomData().get(key);
        if (data == null) {
            data = new MoteSystemData();
            engine.getCustomData().put(key, data);
        }
        return data;
    }

    // Get shared mote AI data
    public static SharedMoteAIData getSharedMoteData(ShipAPI source) {
        String key = source.getId() + "_temporal_motes_shared";
        SharedMoteAIData data = (SharedMoteAIData) Global.getCombatEngine().getCustomData().get(key);
        if (data == null) {
            data = new SharedMoteAIData();
            Global.getCombatEngine().getCustomData().put(key, data);
        }
        return data;
    }

    // Target finding that ONLY targets ships (no fighters or missiles)
    public CombatEntityAPI findBestTarget(ShipAPI ship) {
        CombatEngineAPI engine = Global.getCombatEngine();
        CombatEntityAPI bestTarget = null;
        float bestDistance = Float.MAX_VALUE;

        // Only target enemy ships (excluding fighters)
        for (ShipAPI targetShip : engine.getShips()) {
            if (isValidShipTarget(ship, targetShip) && targetShip.getHullSize() != ShipAPI.HullSize.FIGHTER) {
                float distance = Misc.getDistance(ship.getLocation(), targetShip.getLocation());

                // Aggressive targeting: prioritize closer targets and be more lenient with range
                if (distance < bestDistance && distance <= MAX_RANGE * 1.5f) {
                    bestTarget = targetShip;
                    bestDistance = distance;
                }
            }
        }

        return bestTarget;
    }

    // More aggressive ship target validation
    private boolean isValidShipTarget(ShipAPI source, ShipAPI target) {
        return target != null &&
                target.isAlive() &&
                !target.isHulk() &&
                target.getOwner() != source.getOwner() &&
                !target.isPhased() &&
                target.isTargetable() &&
                target.getCollisionClass() != CollisionClass.NONE &&
                target.getHullSize() != ShipAPI.HullSize.FIGHTER;
    }

    // Enhanced aggressive mote target updating
    protected void updateMoteTarget(ShipAPI ship, MoteSystemData moteData) {
        SharedMoteAIData sharedData = getSharedMoteData(ship);

        // If we have a recent beam hit on a SHIP, use that as the target (highest priority)
        if (moteData.lastBeamHit != null && moteData.lastHitTime > 0f &&
                moteData.lastBeamHit instanceof ShipAPI && isValidShipTarget(ship, (ShipAPI)moteData.lastBeamHit)) {

            float timeSinceLastHit = Global.getCombatEngine().getTotalElapsedTime(false) - moteData.lastHitTime;

            // Target remains valid for 15 seconds after last hit (increased for aggression)
            if (timeSinceLastHit < 60f && isEntityAlive(moteData.lastBeamHit)) {
                // Set the attractor to the last beam hit target
                sharedData.attractorLock = (ShipAPI) moteData.lastBeamHit;
                sharedData.attractorTarget = moteData.lastHitLocation;
                sharedData.attractorRemaining = 15f - timeSinceLastHit;
                return;
            } else {
                // Clear expired target
                moteData.lastBeamHit = null;
                moteData.lastHitLocation = null;
            }
        }

        // If no recent beam hit, find the best available SHIP target
        CombatEntityAPI bestTarget = findBestTarget(ship);

        if (bestTarget != null && bestTarget instanceof ShipAPI) {
            // Set the attractor to the best found SHIP target
            sharedData.attractorLock = (ShipAPI) bestTarget;
            sharedData.attractorTarget = bestTarget.getLocation();
            sharedData.attractorRemaining = 20f;
        } else {
            // If no ship targets found, clear the target (don't target fighters or missiles)
            sharedData.attractorLock = null;
            sharedData.attractorTarget = null;
            sharedData.attractorRemaining = 0f;
        }
    }

    // Find weapon slots for mote spawning
    protected void findSlots(ShipAPI ship) {
        if (!launchSlots.isEmpty() && attractor != null) return;

        launchSlots.clear();
        for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
            if (slot.isSystemSlot()) {
                if (slot.getSlotSize() == WeaponAPI.WeaponSize.SMALL) {
                    launchSlots.add(slot);
                }
                if (slot.getSlotSize() == WeaponAPI.WeaponSize.MEDIUM) {
                    attractor = slot;
                }
            }
        }

        if (launchSlots.isEmpty()) {
            for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
                if (slot.getSlotSize() == WeaponAPI.WeaponSize.SMALL && slot.getWeaponType() == WeaponAPI.WeaponType.ENERGY) {
                    launchSlots.add(slot);
                }
            }
        }
    }

    // Mote spawning method - UPDATED: Use modified MoteAIScript with priority targeting
    protected void spawnTemporalMote(ShipAPI ship, MoteSystemData moteData) {
        CombatEngineAPI engine = Global.getCombatEngine();

        findSlots(ship);
        if (launchSlots.isEmpty()) return;

        WeaponSlotAPI slot = launchSlots.pick();
        Vector2f loc = slot.computePosition(ship);
        float dir = slot.computeMidArcAngle(ship);
        float arc = slot.getArc();
        dir += arc * (float) Math.random() - arc / 2f;

        String weaponId = "motelauncher";
        MissileAPI mote = (MissileAPI) engine.spawnProjectile(ship, null,
                weaponId,
                loc, dir, null);

        // UPDATED: Use the modified MoteAIScript with priority targeting
        mote.setWeaponSpec(weaponId);
        mote.setMissileAI(new neon.nsp.data.shipsystems.NSP_ExponentMoteControl(mote));
        mote.getActiveLayers().remove(CombatEngineLayers.FF_INDICATORS_LAYER);
        mote.setEmpResistance(10000);

        moteData.motes.add(mote);

        engine.spawnMuzzleFlashOrSmoke(ship, slot, mote.getWeaponSpec(), 0, dir);
        Global.getSoundPlayer().playSound("mote_attractor_launch_mote", 1f, 0.25f, loc, new Vector2f());
    }

    // Mote management
    protected void manageMotes(ShipAPI ship) {
        MoteSystemData moteData = getMoteData(ship);
        CombatEngineAPI engine = Global.getCombatEngine();

        List<MissileAPI> aliveMotes = new ArrayList<MissileAPI>();
        for (MissileAPI mote : moteData.motes) {
            if (engine.isEntityInPlay(mote) && !mote.isExpired() && !mote.didDamage()) {
                aliveMotes.add(mote);
            }
        }
        moteData.motes = aliveMotes;

        if (moteData.motes.size() < MAX_MOTES && !ship.getFluxTracker().isOverloadedOrVenting()) {
            spawnTemporalMote(ship, moteData);
        }
    }

    // Check if entity is alive
    private boolean isEntityAlive(CombatEntityAPI entity) {
        if (entity instanceof ShipAPI) {
            return ((ShipAPI) entity).isAlive();
        } else if (entity instanceof MissileAPI) {
            return !((MissileAPI) entity).isExpired() && !((MissileAPI) entity).didDamage();
        }
        return Global.getCombatEngine().isEntityInPlay(entity);
    }

    // Record beam hit for mote targeting - ONLY records ship hits
    public static void recordBeamHit(ShipAPI source, CombatEntityAPI target, Vector2f hitLocation) {
        // Only record hits on ships (not fighters or missiles)
        if (target instanceof ShipAPI && ((ShipAPI)target).getHullSize() != ShipAPI.HullSize.FIGHTER) {
            MoteSystemData moteData = getMoteData(source);
            moteData.lastBeamHit = target;
            moteData.lastHitLocation = new Vector2f(hitLocation);
            moteData.lastHitTime = Global.getCombatEngine().getTotalElapsedTime(false);
        }
    }

    // Flux deduction method
    private boolean deductFlux(ShipAPI ship, float amount) {
        if (ship.getFluxTracker().getCurrFlux() + amount <= ship.getFluxTracker().getMaxFlux()) {
            ship.getFluxTracker().increaseFlux(amount, false);
            return true;
        }
        return false;
    }

    // Explosion system methods
    public static ExplosionSystemData getExplosionData(ShipAPI ship) {
        CombatEngineAPI engine = Global.getCombatEngine();
        String key = EXPLOSION_DATA_KEY + "_" + ship.getId();
        ExplosionSystemData data = (ExplosionSystemData) engine.getCustomData().get(key);
        if (data == null) {
            data = new ExplosionSystemData();
            engine.getCustomData().put(key, data);
        }
        return data;
    }

    public static float getPowerMult(ShipAPI.HullSize size) {
        switch (size) {
            case CAPITAL_SHIP:
                return 1f;
            case CRUISER:
                return 0.6666666667f;
            case DESTROYER:
                return 0.3333333333f;
            case FIGHTER:
            case FRIGATE:
                return 0f;
        }
        return 1f;
    }

    public static float getFluxCost(ShipAPI.HullSize size) {
        return DAMAGE * FLUX_PER_DAMAGE;
    }

    public static float getDamage(ShipAPI.HullSize size) {
        return DAMAGE;
    }

    // Explosion spawning method
    public void spawnExplosion(ShipAPI ship, CombatEntityAPI target) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        float angle = Misc.getAngleInDegrees(target.getLocation(), ship.getLocation());
        angle += 45f - 90f * (float) Math.random();
        Vector2f from = Misc.getUnitVectorAtDegreeAngle(angle);
        from.scale(10000f);

        float targetRadius = Misc.getTargetingRadius(from, target, false);
        Vector2f point = Misc.getUnitVector(target.getLocation(), from);
        point.scale(targetRadius * (0.8f + (float) Math.random() * 0.4f));
        Vector2f.add(target.getLocation(), point, point);

        float damage = getDamage(ship.getHullSize());

        if (FLUX_PER_DAMAGE > 0f) {
            float fluxCost = getFluxCost(ship.getHullSize());
            if (!deductFlux(ship, fluxCost)) {
                return;
            }
        }

        Vector2f shipCenter = new Vector2f(ship.getLocation());
        Color color = new Color(255, 196, 19, 255);
        float explosionRadius = RADIUS;

        DamagingExplosionSpec spec = new DamagingExplosionSpec(
                0.1f,
                explosionRadius,
                explosionRadius * 0.5f,
                damage,
                damage,
                CollisionClass.PROJECTILE_NO_FF,
                CollisionClass.PROJECTILE_FIGHTER,
                3f,
                3f,
                0.5f,
                0,
                new Color(255,255,255,0),
                new Color(255,196,19,0)
        );

        spec.setDamageType(DamageType.ENERGY);
        spec.setUseDetailedExplosion(false);
        spec.setSoundSetId("abyssal_glare_explosion");
        spec.setSoundVolume(0.33f);

        DamagingProjectileAPI explosion = engine.spawnDamagingExplosion(spec, ship, point);

        // Record this beam hit for mote targeting (only if it's a ship)
        recordBeamHit(ship, target, point);

        float baseSize = 7f;
        NegativeExplosionVisual.NEParams p = RiftCascadeMineExplosion.createStandardRiftParams(
                color, baseSize);
        p.noiseMult = 6f;
        p.thickness = 25f;
        p.fadeOut = 0.5f;
        p.spawnHitGlowAt = 1f;
        p.additiveBlend = true;
        p.blackColor = Color.white;
        p.underglow = null;
        p.withNegativeParticles = false;
        p.withHitGlow = false;
        p.fadeIn = 0f;

        RiftCascadeMineExplosion.spawnStandardRift(explosion, p);

        Vector2f preArcTarget = new Vector2f(point);
        preArcTarget.x += (float) (Math.random() - 0.5) * 50f;
        preArcTarget.y += (float) (Math.random() - 0.5) * 50f;

        EmpArcEntityAPI.EmpArcParams preParams = new EmpArcEntityAPI.EmpArcParams();
        preParams.segmentLengthMult = 6f;
        preParams.maxZigZagMult = 0.15f;
        preParams.zigZagReductionFactor = 1f;
        preParams.flickerRateMult = 0.8f + 0.4f * (float) Math.random();
        preParams.fadeOutDist = 100f;
        preParams.minFadeOutMult = 3f;
        preParams.glowSizeMult = 0.3f;

        Color preArcColor = new Color(255, 220, 100, 200);
        Color preArcCore = new Color(255, 255, 200, 255);

        EmpArcEntityAPI preArc = engine.spawnEmpArcVisual(shipCenter, ship, preArcTarget, ship, 15f, preArcColor, preArcCore, preParams);
        if (preArc != null) {
            preArc.setCoreWidthOverride(8f);
            preArc.setSingleFlickerMode();
            preArc.setRenderGlowAtStart(true);
        }

        float thickness = 25f;
        Color coreColor = new Color(255, 255, 200, 255);

        EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
        params.segmentLengthMult = 8f;
        params.maxZigZagMult = 0.1f;
        params.zigZagReductionFactor = 1f;
        params.flickerRateMult = 0.9f + 0.2f * (float) Math.random();
        params.fadeOutDist = 200f;
        params.minFadeOutMult = 4f;
        params.glowSizeMult = 0.6f;

        EmpArcEntityAPI arc = engine.spawnEmpArcVisual(shipCenter, ship, point, explosion, thickness, color, coreColor, params);
        if (arc != null) {
            arc.setCoreWidthOverride(thickness * 0.8f);
            arc.setSingleFlickerMode();
            arc.setRenderGlowAtStart(true);
            arc.setWarping(0.1f);
        }

        for (int i = 0; i < 2; i++) {
            Vector2f secondaryTarget = new Vector2f(point);
            secondaryTarget.x += (float) (Math.random() - 0.5) * 80f;
            secondaryTarget.y += (float) (Math.random() - 0.5) * 80f;

            EmpArcEntityAPI.EmpArcParams secondaryParams = new EmpArcEntityAPI.EmpArcParams();
            secondaryParams.segmentLengthMult = 4f;
            secondaryParams.maxZigZagMult = 0.2f;
            secondaryParams.zigZagReductionFactor = 1f;
            secondaryParams.flickerRateMult = 0.7f + 0.3f * (float) Math.random();
            secondaryParams.fadeOutDist = 120f;
            secondaryParams.minFadeOutMult = 2f;
            secondaryParams.glowSizeMult = 0.2f;

            Color secondaryColor = new Color(255, 180, 50, 150);
            Color secondaryCore = new Color(255, 230, 150, 200);

            EmpArcEntityAPI secondaryArc = engine.spawnEmpArcVisual(shipCenter, ship, secondaryTarget, explosion, 8f, secondaryColor, secondaryCore, secondaryParams);
            if (secondaryArc != null) {
                secondaryArc.setCoreWidthOverride(4f);
                secondaryArc.setSingleFlickerMode();
            }
        }
    }

    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {
        if (member.getCaptain() == null || member.getCaptain().isDefault() || (!member.getCaptain().getId().equals("exponent_core") && !member.getCaptain().isPlayer())) {
            PersonAPI exponent = Global.getSector().getImportantPeople().getPerson("exponent_core");
            if (exponent != null) {
                member.setCaptain(exponent);
                Misc.setUnremovable(exponent, false);
            } else {
                exponent = new NSP_ExponentCore().createPerson("nsp_exponent_core", Factions.NEUTRAL, new Random());
                member.setCaptain(exponent);
                Misc.setUnremovable(exponent, false);
            }
        }
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    public static class EnlightenedRangeBonusModifier implements WeaponBaseRangeModifier {
        public EnlightenedRangeBonusModifier() {
        }

        public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
            return 0;
        }

        public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
            return 1f;
        }

        public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
            if (weapon.isBeam()) return 0f;
            if (weapon.getType() == WeaponAPI.WeaponType.ENERGY || weapon.getType() == WeaponAPI.WeaponType.HYBRID) {
                if (Misc.isAutomated(ship)) {
                    return RANGE_BONUS;
                } else {
                    return CREWED_RANGE_BONUS;
                }
            }
            return 0f;
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {

        tooltip.addPara("The %s we weave together.", 5f, Color.ORANGE, "song");


        // UPDATED: Mote system info with aggressive ship-only targeting
    }

    public String getHullmodName(String id) {
        return Global.getSettings().getHullModSpec(id).getDisplayName();
    }

    public void applyEffectsAfterShipCreated(ShipAPI ship, String id) {
        ship.addListener(new SandyListener(ship));
    }

    @Override
    public boolean affectsOPCosts() {
        return true;
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getMissileRoFMult().modifyMult(id, MISSILE_ROF_MULT);
        stats.getEnergyWeaponFluxCostMod().modifyPercent(id, ENERGY_WEAPON_FLUX_INCREASE);
        stats.getShieldTurnRateMult().modifyPercent(id, SHIELD_BONUS_TURN);
        stats.getShieldUnfoldRateMult().modifyPercent(id, SHIELD_BONUS_UNFOLD);
        stats.getBallisticAmmoBonus().modifyPercent(id, AMMO_BONUS);
        stats.getEnergyAmmoBonus().modifyPercent(id, AMMO_BONUS);
        stats.getBallisticAmmoRegenMult().modifyPercent(id, REGEN_BONUS);
        stats.getEnergyAmmoRegenMult().modifyPercent(id, REGEN_BONUS);
        stats.getDynamic().getMod(Stats.MEDIUM_ENERGY_MOD).modifyFlat(id, -COST_REDUCTION1);
        stats.getDynamic().getMod(Stats.LARGE_ENERGY_MOD).modifyFlat(id, -COST_REDUCTION2);
    }

    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        for (String tmp : BLOCKED_HULLMODS) {
            if (ship.getVariant().getHullMods().contains(tmp)) {
                ship.getVariant().removeMod(tmp);
                String ERROR = "nsp_incompatible";
                ship.getVariant().addMod(ERROR);
            }
        }
    }

    @Override
    public void applyEffectsAfterShipAddedToCombatEngine(ShipAPI ship, String id) {
        ship.addListener(new SandyListener(ship));
        ship.getMutableStats().getTimeMult().modifyMult(id, SandyListener.PASSIVE_TIMEFLOW);
        ship.getMutableStats().getArmorDamageTakenMult().modifyMult(id, ARMOR_DAMAGE_MULT);
        ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, HULL_DAMAGE_MULT);
        ship.getMutableStats().getEmpDamageTakenMult().modifyMult(id, EMP_DAMAGE_MULT);
    }

    private static final Object STATUS_KEY1 = new Object();
    private static final Object STATUS_KEY2 = new Object();

    class SandyListener implements AdvanceableListener, DamageDealtModifier {
        String id = "nsp_sandy_effect";
        ShipAPI ship;
        float timer = 0.05f;
        float duration = 0.05f;
        Color color = Color.GRAY;

        int kills = 0;
        int maxKillsBonus = 4;
        float bonusPerKill = 0.7f;
        float killBonusDuration = 60f;

        float currentBonus = 1f;
        float maxBonus = 5f + (maxKillsBonus * bonusPerKill);
        float minBonus = 1f;
        float decayRate = 0.0001f;
        public static final float PASSIVE_TIMEFLOW = 1.2f;
        public static final float DAMAGE_REDUCTION = 0.12f;
        public static final Color AFTERIMAGE_COLOR = new Color(255, 196, 19, 90);

        float currentFluxReduction = 0f;
        float currentFireRateBonus = 0f;
        float currentFluxDissipationBonus = 0f;

        private Object loopSound;
        private float currentVolume = 0f;
        private float targetVolume = 0f;

        float[] killBonuses = new float[maxKillsBonus];
        float[] killTimers = new float[maxKillsBonus];

        private ExplosionSystemData explosionData;
        private IntervalUtil interval = new IntervalUtil(timer, timer);

        private MoteSystemData moteData;

        public SandyListener(ShipAPI attachedShip) {
            this.ship = attachedShip;
            this.interval = new IntervalUtil(timer, timer);
            this.explosionData = getExplosionData(ship);
            this.moteData = getMoteData(ship);
            if (this.explosionData != null) {
                this.explosionData.untilAttack = REFIRE_DELAY;
            }
        }

        private void updateJitterEffect(float amount) {
            if (ship == null || ship.isHulk()) return;

            float jitterIntensity = 0f;
            int activeKillCount = 0;

            for (int i = 0; i < kills; i++) {
                if (killBonuses[i] > 0 && killTimers[i] > 0) {
                    activeKillCount++;
                    jitterIntensity += (killBonuses[i] / bonusPerKill);
                }
            }

            if (activeKillCount > 0) {
                jitterIntensity = jitterIntensity / maxKillsBonus;
                jitterIntensity = Math.min(jitterIntensity, MAX_JITTER_INTENSITY);

                if (jitterIntensity > 0.1f) {
                    float maxJitterRange = 15f * jitterIntensity;
                    ship.setJitter(this, JITTER_COLOR, jitterIntensity, 3, 0f, maxJitterRange);
                    ship.setJitterUnder(this, JITTER_COLOR, jitterIntensity, 3, 0f, maxJitterRange);
                }
            } else {
                ship.setJitter(this, JITTER_COLOR, 0f, 0, 0f, 0f);
                ship.setJitterUnder(this, JITTER_COLOR, 0f, 0, 0f, 0f);
            }
        }

        private void updateAudioEffect(float amount) {
            if (ship == null || ship.isHulk()) return;

            float intensity = 0f;
            int activeKillCount = 0;

            for (int i = 0; i < kills; i++) {
                if (killBonuses[i] > 0 && killTimers[i] > 0) {
                    activeKillCount++;
                    intensity += (killBonuses[i] / bonusPerKill);
                }
            }

            if (activeKillCount > 0) {
                targetVolume = (intensity / maxKillsBonus) * (MAX_VOLUME - MIN_VOLUME) + MIN_VOLUME;
                targetVolume = Math.min(targetVolume, MAX_VOLUME);
            } else {
                targetVolume = 0f;
            }

            if (currentVolume < targetVolume) {
                currentVolume = Math.min(currentVolume + VOLUME_RAMP_SPEED * amount, targetVolume);
            } else if (currentVolume > targetVolume) {
                currentVolume = Math.max(currentVolume - VOLUME_RAMP_SPEED * amount, targetVolume);
            }

            Global.getSoundPlayer().playLoop(
                    AUDIO_FILE,
                    loopSound,
                    1.0f,
                    currentVolume,
                    ship.getLocation(),
                    Misc.ZERO
            );
        }

        private void updateWeaponBonuses() {
            int activeKillCount = 0;
            for (int i = 0; i < kills; i++) {
                if (killBonuses[i] > 0 && killTimers[i] > 0) {
                    activeKillCount++;
                }
            }

            if (currentFluxDissipationBonus > 0) {
                ship.getMutableStats().getFluxDissipation().modifyPercent(id + "_dissipation", currentFluxDissipationBonus);
            } else {
                ship.getMutableStats().getFluxDissipation().unmodify(id + "_dissipation");
            }
        }

        // UPDATED: Mote system update with aggressive ship-only targeting
        private void updateMoteSystem(float amount) {
            if (moteData == null) return;

            moteData.elapsed += amount;
            moteData.moteSpawnInterval.advance(amount);

            // Update mote targets with aggressive ship-only targeting
            updateMoteTarget(ship, moteData);

            if (moteData.moteSpawnInterval.intervalElapsed()) {
                manageMotes(ship);
            }
        }

        // UPDATED: Explosion system with ship-only targeting
        private void updateExplosionSystem(float amount) {
            if (explosionData == null) return;

            explosionData.untilAttack -= amount;
            if (explosionData.untilAttack <= 0f) {
                // Use the aggressive ship-only targeting for explosions
                CombatEntityAPI target = findBestTarget(ship);
                if (target != null) {
                    spawnExplosion(ship, target);
                }
                explosionData.untilAttack = REFIRE_DELAY;
            }
        }

        @Override
        public void advance(float amount) {
            if (Global.getCombatEngine().isPaused() || ship == null || ship.isHulk()) return;

            updateExplosionSystem(amount);
            updateMoteSystem(amount);

            float totalActiveBonus = 0f;
            for (int i = 0; i < kills; i++) {
                if (killBonuses[i] > 0) {
                    killTimers[i] -= amount;
                    if (killTimers[i] <= 0) {
                        killBonuses[i] = 0;
                    } else {
                        killBonuses[i] = Math.max(0, killBonuses[i] - (decayRate * amount));
                        totalActiveBonus += killBonuses[i];
                    }
                }
            }

            currentBonus = minBonus + totalActiveBonus;
            ship.getMutableStats().getTimeMult().modifyMult(id, currentBonus);

            updateWeaponBonuses();

            updateJitterEffect(amount);
            updateAudioEffect(amount);

            if (ship == Global.getCombatEngine().getPlayerShip()) {
                int activeKillCount = 0;
                for (int i = 0; i < kills; i++) {
                    if (killBonuses[i] > 0 && killTimers[i] > 0) {
                        activeKillCount++;
                    }
                }

                String bonusText = Misc.getRoundedValue(currentBonus) + "x Speed";
                if (activeKillCount > 0) {
                    bonusText += " | " + activeKillCount + "/4 Kills Active";
                }

                if (moteData != null && !moteData.motes.isEmpty()) {
                    bonusText += " | " + moteData.motes.size() + " Motes";
                }

                Global.getCombatEngine().maintainStatusForPlayerShip(
                        NSPSandyEffect2.STATUS_KEY1,
                        Global.getSettings().getSpriteName("ui", "icon_op"),
                        "SHOW THEM THE LIGHT",
                        bonusText,
                        false
                );
            }

            interval.advance(amount);
            if (interval.intervalElapsed()) {
                NSPSandevistan2.afterimage(ship, AFTERIMAGE_COLOR, duration, duration, duration);
            }
        }

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (target instanceof ShipAPI) {
                ShipAPI targetShip = (ShipAPI) target;
                if (targetShip.getOwner() != ship.getOwner()
                        && !targetShip.isHulk()
                        && targetShip.getHullSize() != ShipAPI.HullSize.FIGHTER
                        && !targetShip.isStationModule()
                        && !targetShip.hasListener(new KillTracker(ship))) {
                    targetShip.addListener(new KillTracker(ship));
                    return "added_kill_tracker";
                }
            }
            return null;
        }

        public void addKillBonus() {
            if (kills < maxKillsBonus) {
                killBonuses[kills] = bonusPerKill;
                killTimers[kills] = killBonusDuration;
                kills++;
            } else {
                int oldestIndex = 0;
                for (int i = 1; i < maxKillsBonus; i++) {
                    if (killTimers[i] < killTimers[oldestIndex]) {
                        oldestIndex = i;
                    }
                }
                killBonuses[oldestIndex] = bonusPerKill;
                killTimers[oldestIndex] = killBonusDuration;
            }
        }
    }

    // KillTracker class
    class KillTracker implements HullDamageAboutToBeTakenListener {
        String key = "$nsp_kill_tracker_key";
        ShipAPI dealer;

        public KillTracker(ShipAPI ship) {
            dealer = ship;
        }

        @Override
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
            if (damageAmount >= ship.getHitpoints()) {
                if (param instanceof ShipAPI) {
                    if (param != dealer) return false;
                    if (ship.getCustomData().containsKey(key)) return false;

                    ship.setCustomData(key, true);

                    if (dealer.getListenerManager() != null) {
                        Optional<SandyListener> maybelistener = dealer.getListenerManager()
                                .getListeners(SandyListener.class).stream().findFirst();

                        if (maybelistener.isPresent()) {
                            SandyListener listener = maybelistener.get();
                            listener.addKillBonus();
                        }
                    }
                }
            }
            return false;
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (Global.getCombatEngine() == null) return;

        List<ShipAPI> shipPlusModules = ship.getChildModulesCopy();
        shipPlusModules.add(ship);

        resisting -= amount;
        for (ShipAPI s : Global.getCombatEngine().getShips()) {
            if (s != ship && s.getHullSize() != ShipAPI.HullSize.FIGHTER && s.isAlive()) {
                float distance = Vector2f.sub(ship.getLocation(), s.getLocation(), new Vector2f()).length() - ship.getCollisionRadius();
                for (ShipAPI child : ship.getChildModulesCopy()) {
                    float newDistance = Vector2f.sub(child.getLocation(), s.getLocation(), new Vector2f()).length() - child.getCollisionRadius();
                    distance = Math.min(distance, newDistance);
                }
                float mult = s.getMutableStats().getDynamic().getValue(Stats.EXPLOSION_RADIUS_MULT);
                float radius = s.getCollisionRadius() + Math.min(200f, s.getCollisionRadius()) * mult;
                if (distance <= radius) {
                    nearbyShips.add(s);
                } else {
                    nearbyShips.remove(s);
                }
            }
        }

        Iterator<ShipAPI> iter = nearbyShips.iterator();
        while (iter.hasNext()) {
            ShipAPI t = iter.next();
            if (t == null || !t.isAlive()) {
                iter.remove();
                for (ShipAPI s : shipPlusModules) {
                    s.getMutableStats().getHighExplosiveDamageTakenMult().modifyMult("KT_blastdampeners", RESISTANCE);
                    s.getMutableStats().getHighExplosiveShieldDamageTakenMult().modifyMult("KT_blastdampeners", RESISTANCE);
                }
                resisting = RESIST_TIME;
            }
        }

        if (resisting <= 0f) {
            for (ShipAPI s : shipPlusModules) {
                s.getMutableStats().getHighExplosiveDamageTakenMult().unmodify("KT_blastdampeners");
                s.getMutableStats().getHighExplosiveShieldDamageTakenMult().unmodify("KT_blastdampeners");
            }
        }
    }

    private ShroudedLensHullmod.ShroudedLensHullmodData getData(ShipAPI ship) {
        return null;
    }
}