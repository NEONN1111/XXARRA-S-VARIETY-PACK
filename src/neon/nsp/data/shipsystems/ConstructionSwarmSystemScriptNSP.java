package neon.nsp.data.shipsystems;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BoundsAPI.SegmentAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.threat.ConstructionSwarmSystemScript;
import com.fs.starfarer.api.impl.combat.threat.FragmentSwarmHullmod;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.impl.combat.threat.SwarmLauncherEffect;
import com.fs.starfarer.api.impl.combat.threat.ThreatShipConstructionScript;
import com.fs.starfarer.api.impl.combat.threat.VoltaicDischargeOnFireEffect;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.CountingMap;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

public class ConstructionSwarmSystemScriptNSP extends ConstructionSwarmSystemScript {


    public static final int MAX_PLAYER_CONSTRUCTIONS = 4;
    public static final int MAX_PLAYER_FRIGATES = 2;
    public static final int MAX_PLAYER_DESTROYERS = 2;


    public static final float CONSTRUCTED_SHIP_DP_MULT = 0.5f;


    private static final java.util.Map<String, Float> ORIGINAL_DP_VALUES = new java.util.HashMap<>();

    @Override
    protected void launchSwarm(ShipAPI ship) {
        findSlots(ship);

        String wingId = SwarmLauncherEffect.CONSTRUCTION_SWARM_WING;

        CombatEngineAPI engine = Global.getCombatEngine();
        CombatFleetManagerAPI manager = engine.getFleetManager(ship.getOwner());
        manager.setSuppressDeploymentMessages(true);

        WeaponSlotAPI slot = slots.pick();

        Vector2f loc = slot.computePosition(ship);
        float facing = slot.computeMidArcAngle(ship);

        ShipAPI fighter = manager.spawnShipOrWing(wingId, loc, facing, 0f, null);
        fighter.getWing().setSourceShip(ship);

        manager.setSuppressDeploymentMessages(false);

        fighter.getMutableStats().getMaxSpeed().modifyMult("construction_swarm", CONSTRUCTION_SWARM_SPEED_MULT);

        Vector2f takeoffVel = Misc.getUnitVectorAtDegreeAngle(facing);
        takeoffVel.scale(fighter.getMaxSpeed() * 1f);

        fighter.setDoNotRender(true);
        fighter.setExplosionScale(0f);
        fighter.setHulkChanceOverride(0f);
        fighter.setImpactVolumeMult(SwarmLauncherEffect.IMPACT_VOLUME_MULT);
        fighter.getArmorGrid().clearComponentMap();
        Vector2f.add(fighter.getVelocity(), takeoffVel, fighter.getVelocity());

        RoilingSwarmEffect sourceSwarm = RoilingSwarmEffect.getSwarmFor(ship);
        if (sourceSwarm == null) return;

        RoilingSwarmEffect swarm = FragmentSwarmHullmod.createSwarmFor(fighter);
        swarm.getParams().flashFringeColor = VoltaicDischargeOnFireEffect.EMP_FRINGE_COLOR;
        RoilingSwarmEffect.getFlockingMap().remove(swarm.getParams().flockingClass, swarm);
        swarm.getParams().flockingClass = FragmentSwarmHullmod.CONSTRUCTION_SWARM_FLOCKING_CLASS;
        RoilingSwarmEffect.getFlockingMap().add(swarm.getParams().flockingClass, swarm);

        SwarmConstructableVariant pick = pickVariantWithRestrictions(ship);
        if (pick == null) {
            fighter.getLocation().set(100000f, 100000f);
            return;
        }


        if (isInPlayerFleet(ship) && hasReachedConstructionLimit(ship)) {
            fighter.getLocation().set(100000f, 100000f);
            return;
        }


        if (isInPlayerFleet(ship) && !isVariantAllowedForPlayer(pick)) {
            fighter.getLocation().set(100000f, 100000f);
            return;
        }

        String variantId = pick.variantId;

        ShipVariantAPI variant = Global.getSettings().getVariant(variantId);
        if (variant == null) return;


        float originalDp;
        if (ORIGINAL_DP_VALUES.containsKey(variantId)) {
            originalDp = ORIGINAL_DP_VALUES.get(variantId);
        } else {
            originalDp = variant.getHullSpec().getSuppliesToRecover();
            ORIGINAL_DP_VALUES.put(variantId, originalDp);
        }


        float halvedDp = Math.max(1f, originalDp * CONSTRUCTED_SHIP_DP_MULT);


        variant.getHullSpec().setSuppliesToRecover(halvedDp);

        ship.setCurrentCR(ship.getCurrentCR() - pick.cr);

        float dp = halvedDp;

        int numFragments = pick.fragments;
        float radiusMult = 1f;
        float collisionMult = 2f;
        float hpMult = 1f;
        float travelTime = 3f;

        if (variant.getHullSize() == HullSize.DESTROYER) {
            radiusMult = 2f;
            collisionMult = 4f;
            hpMult = radiusMult;
            travelTime = 4f;
        } else if (variant.getHullSize() == HullSize.CRUISER) {
            radiusMult = 3.5f;
            collisionMult = 6f;
            hpMult = radiusMult;
            travelTime = 5f;
        } else if (variant.getHullSize() == HullSize.CAPITAL_SHIP) {
            radiusMult = 4;
            collisionMult = 8f;
            hpMult = radiusMult;
            travelTime = 6f;
        }

        for (SegmentAPI s : fighter.getExactBounds().getOrigSegments()) {
            s.getP1().scale(collisionMult);
            s.getP2().scale(collisionMult);
            s.set(s.getP1().x, s.getP1().y, s.getP2().x, s.getP2().y);
        }
        fighter.setCollisionRadius(fighter.getCollisionRadius() * collisionMult);

        fighter.setMaxHitpoints(fighter.getMaxHitpoints() * hpMult);
        fighter.setHitpoints(fighter.getHitpoints() * hpMult);

        swarm.getParams().maxOffset *= radiusMult;
        swarm.getParams().initialMembers = numFragments;
        swarm.getParams().baseMembersToMaintain = numFragments;

        boolean overseer = variant.getHullSpec().hasTag(Tags.THREAT_OVERSEER);

        SwarmConstructionData data = new SwarmConstructionData();
        data.variantId = variantId;
        data.constructionTime = BASE_CONSTRUCTION_TIME + dp * CONSTRUCTION_TIME_DP_MULT;
        if (overseer) {
            data.constructionTime += CONSTRUCTION_TIME_OVERSEER_EXTRA;
        }
        data.preConstructionTravelTime = travelTime;

        if (fastConstructionLeft > 0) {
            if (pick.size == HullSize.FRIGATE) {
                fastConstructionLeft--;
                data.constructionTime = 2f;
            } else {
                fastConstructionLeft = 0;
            }
        }

        swarm.custom1 = data;

        int transfer = Math.min(numFragments, sourceSwarm.getNumActiveMembers());
        if (transfer > 0) {
            loc = new Vector2f(takeoffVel);
            loc.scale(0.5f);
            Vector2f.add(loc, fighter.getLocation(), loc);
            sourceSwarm.transferMembersTo(swarm, transfer, loc, 100f);
        }

        int add = numFragments - transfer;
        if (add > 0) {
            swarm.addMembers(add);
        }
    }

    protected boolean isInPlayerFleet(ShipAPI ship) {
        if (ship == null) return false;
        return ship.getOwner() == 0;
    }

    protected int getPlayerConstructionCount(CombatEngineAPI engine) {
        int count = 0;
        int frigateCount = 0;
        int destroyerCount = 0;

        CombatFleetManagerAPI playerManager = engine.getFleetManager(0);
        if (playerManager == null) return 0;

        for (DeployedFleetMemberAPI dfm : playerManager.getDeployedCopyDFM()) {
            ShipAPI ship = dfm.getShip();
            if (ship == null) continue;

            if (ship.hasTag(ThreatShipConstructionScript.SWARM_CONSTRUCTING_SHIP)) {
                count++;
                if (ship.getHullSize() == HullSize.FRIGATE) {
                    frigateCount++;
                }
                if (ship.getHullSize() == HullSize.DESTROYER) {
                    destroyerCount++;
                }
            }

            if (constructionSwarmWillBuild(ship, Tags.THREAT_COMBAT, null)) {
                count++;
                if (ship.getHullSize() == HullSize.FRIGATE) {
                    frigateCount++;
                }
                if (ship.getHullSize() == HullSize.DESTROYER) {
                    destroyerCount++;
                }
            }
        }

        engine.getCustomData().put("nsp_construction_count", count);
        engine.getCustomData().put("nsp_frigate_count", frigateCount);
        engine.getCustomData().put("nsp_destroyer_count", destroyerCount);

        return count;
    }

    protected boolean hasReachedConstructionLimit(ShipAPI ship) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return false;

        if (!isInPlayerFleet(ship)) return false;

        int currentCount = getPlayerConstructionCount(engine);
        return currentCount >= MAX_PLAYER_CONSTRUCTIONS;
    }

    protected boolean isVariantAllowedForPlayer(SwarmConstructableVariant variant) {
        if (variant == null) return false;

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return false;

        if (variant.size == HullSize.FRIGATE) {
            Object frigateCount = engine.getCustomData().get("nsp_frigate_count");
            if (frigateCount instanceof Integer && (Integer) frigateCount >= MAX_PLAYER_FRIGATES) {
                return false;
            }
            return true;
        }

        if (variant.size == HullSize.DESTROYER) {
            Object destroyerCount = engine.getCustomData().get("nsp_destroyer_count");
            if (destroyerCount instanceof Integer && (Integer) destroyerCount >= MAX_PLAYER_DESTROYERS) {
                return false;
            }
            return true;
        }

        return false;
    }

    protected SwarmConstructableVariant pickVariantWithRestrictions(ShipAPI ship) {
        init();

        CombatEngineAPI engine = Global.getCombatEngine();
        CombatFleetManagerAPI manager = engine.getFleetManager(ship.getOwner());
        if (manager == null) return null;

        RoilingSwarmEffect swarm = RoilingSwarmEffect.getSwarmFor(ship);
        int fragments = swarm == null ? 0 : swarm.getNumActiveMembers();

        int dpLeft = manager.getMaxStrength() - manager.getCurrStrength();
        float cr = ship.getCurrentCR();

        int overseers = getNumOverseersDeployed(manager);
        int hives = getNumHivesDeployed(manager);
        int fabricators = getNumFabricatorsDeployed(manager);
        float combatWeight = getCombatWeightDeployed(manager);

        boolean isPlayer = isInPlayerFleet(ship);
        if (isPlayer) {
            Object currentCount = engine.getCustomData().get("nsp_construction_count");
            if (currentCount instanceof Integer && (Integer) currentCount >= MAX_PLAYER_CONSTRUCTIONS) {
                return null;
            }
        }

        int wantOverseers = (int) Math.round(combatWeight / OTHER_SHIP_WEIGHT_PER_OVERSEER);
        if (wantOverseers < 1) wantOverseers = 1;

        combatWeight += Math.max(0, fabricators - 1f) * 16f;
        int wantHives = (int) (combatWeight / 16f);

        if (wantHives < 1) wantHives = 1;
        if (wantHives > 2) wantHives = 2;

        wantOverseers -= overseers;
        wantHives -= hives;

        float frigates = getCombatDeployed(manager, HullSize.FRIGATE);
        float destroyers = getCombatDeployed(manager, HullSize.DESTROYER);
        float cruisers = getCombatDeployed(manager, HullSize.CRUISER);
        float capitals = getCombatDeployed(manager, HullSize.CAPITAL_SHIP);
        float large = cruisers + capitals;

        if (frigates >= 2) {
            fastConstructionLeft = 0;
        }

        CountingMap<HullSize> numCombatVariants = new CountingMap<>();
        for (SwarmConstructableVariant curr : CONSTRUCTABLE) {
            if (curr.type == SwarmConstructableType.COMBAT_UNIT) {
                numCombatVariants.add(curr.size);
            }
        }

        WeightedRandomPicker<SwarmConstructableVariant> hivePicker = new WeightedRandomPicker<>();
        WeightedRandomPicker<SwarmConstructableVariant> overseerPicker = new WeightedRandomPicker<>();
        WeightedRandomPicker<SwarmConstructableVariant> smallPicker = new WeightedRandomPicker<>();
        WeightedRandomPicker<SwarmConstructableVariant> mediumPicker = new WeightedRandomPicker<>();
        WeightedRandomPicker<SwarmConstructableVariant> largePicker = new WeightedRandomPicker<>();

        for (SwarmConstructableVariant curr : CONSTRUCTABLE) {
            // Use original DP for checking against dpLeft (not halved)
            float originalDp = getOriginalDP(curr.variantId);
            if (originalDp > dpLeft) continue;
            if (curr.cr > cr) continue;
            if (curr.fragments > fragments) continue;

            if (isPlayer && !isVariantAllowedForPlayer(curr)) {
                continue;
            }

            if (curr.type == SwarmConstructableType.HIVE) {
                hivePicker.add(curr, 1f / originalDp);
            } else if (curr.type == SwarmConstructableType.OVERSEER) {
                overseerPicker.add(curr, 1f / originalDp);
            } else {
                float wMult = 1f / Math.max(1f, numCombatVariants.getCount(curr.size));
                if (curr.size == HullSize.FRIGATE) {
                    smallPicker.add(curr, 1f / originalDp * wMult);
                } else if (curr.size == HullSize.DESTROYER) {
                    mediumPicker.add(curr, 1f / originalDp * wMult);
                } else {
                    largePicker.add(curr, 1f / originalDp * wMult);
                }
            }
        }

        if (isPlayer) {
            WeightedRandomPicker<SwarmConstructableVariant> combinedPicker = new WeightedRandomPicker<>();

            for (SwarmConstructableVariant variant : smallPicker.getItems()) {
                combinedPicker.add(variant, smallPicker.getWeight(variant));
            }
            for (SwarmConstructableVariant variant : mediumPicker.getItems()) {
                combinedPicker.add(variant, mediumPicker.getWeight(variant));
            }

            if (!combinedPicker.isEmpty()) {
                return combinedPicker.pick();
            }
            return null;
        }

        if (frigates <= 1 && !smallPicker.isEmpty()) {
            return smallPicker.pick();
        }

        if (wantOverseers > 0 || wantHives > 0) {
            if (wantOverseers >= wantHives && !overseerPicker.isEmpty()) {
                return overseerPicker.pick();
            } else if (!hivePicker.isEmpty()) {
                return hivePicker.pick();
            }
        }

        if (large <= destroyers * NUM_LARGE_AS_FRACTION_OF_DESTROYERS && !largePicker.isEmpty()) {
            return largePicker.pick();
        }

        if (destroyers <= frigates * NUM_DESTROYERS_AS_FRACTION_OF_FRIGATES && !mediumPicker.isEmpty()) {
            return mediumPicker.pick();
        }

        return smallPicker.pick();
    }

    private float getOriginalDP(String variantId) {
        if (ORIGINAL_DP_VALUES.containsKey(variantId)) {
            return ORIGINAL_DP_VALUES.get(variantId);
        }

        ShipVariantAPI variant = Global.getSettings().getVariant(variantId);
        float originalDp = variant.getHullSpec().getSuppliesToRecover();
        ORIGINAL_DP_VALUES.put(variantId, originalDp);
        return originalDp;
    }
}