package neon.nsp.data.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.entities.DamagingExplosion;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

public class ExplosionOcclusionRaycast implements DamageTakenModifier {
    public static final String EXPLOSION_RAYCAST_MAPS = "explosion_raycast";
    public static final String OCCLUSION_MODIFIER = "occlusion_modifier";
    public static final String DELETE_TIME = "delete_time";
    private static final int NUM_RAYCASTS = 36;

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (!(param instanceof DamagingProjectileAPI)) return null;
        if (!(target instanceof ShipAPI)) return null;

        ShipAPI ship = (ShipAPI) target;
        ShipAPI parent = ship.getParentStation() == null ? ship : ship.getParentStation();

        if (parent.getCustomData().get(EXPLOSION_RAYCAST_MAPS) == null) {
            parent.setCustomData(EXPLOSION_RAYCAST_MAPS, new HashMap<DamagingProjectileAPI, Map<String, Float>>());
        }

        if (param instanceof DamagingExplosion || param instanceof MissileAPI) {
            @SuppressWarnings("unchecked")
            Map<DamagingProjectileAPI, Map<String, Float>> explosionMaps =
                    (Map<DamagingProjectileAPI, Map<String, Float>>) parent.getCustomData().get(EXPLOSION_RAYCAST_MAPS);

            DamagingProjectileAPI projectile = (DamagingProjectileAPI) param;
            Map<String, Float> explosionMap = null;

            for (Map.Entry<DamagingProjectileAPI, Map<String, Float>> entry : explosionMaps.entrySet()) {
                if (entry.getKey() == projectile) {
                    explosionMap = entry.getValue();
                    break;
                } else if (Math.abs(entry.getKey().getDamageAmount() - projectile.getDamageAmount()) < 1e-6f &&
                        Misc.getDistanceSq(entry.getKey().getLocation(), projectile.getLocation()) < 25f) {
                    explosionMap = entry.getValue();
                    break;
                }
            }

            if (explosionMap == null) {
                explosionMap = generateExplosionRayhitMap(projectile, damage, parent);
            }

            Float modifier = explosionMap.get(((ShipAPI) target).getId());
            if (modifier != null) {
                damage.getModifier().modifyMult(OCCLUSION_MODIFIER, modifier);
            }
            return OCCLUSION_MODIFIER;
        }
        return null;
    }

    private Map<String, Float> generateExplosionRayhitMap(DamagingProjectileAPI projectile, DamageAPI damage, ShipAPI parent) {
        if (!(projectile instanceof DamagingExplosion) && !(projectile instanceof MissileAPI)) {
            return new HashMap<>();
        }

        @SuppressWarnings("unchecked")
        Map<DamagingProjectileAPI, Map<String, Float>> explosionMaps =
                (Map<DamagingProjectileAPI, Map<String, Float>>) parent.getCustomData().get(EXPLOSION_RAYCAST_MAPS);

        if (explosionMaps.containsKey(projectile)) {
            return explosionMaps.get(projectile);
        }

        float currentTime = Global.getCombatEngine().getTotalElapsedTime(false);
        Iterator<Map.Entry<DamagingProjectileAPI, Map<String, Float>>> iterator = explosionMaps.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<DamagingProjectileAPI, Map<String, Float>> entry = iterator.next();
            Float deleteTime = entry.getValue().get(DELETE_TIME);
            if (deleteTime != null && deleteTime < currentTime) {
                iterator.remove();
            }
        }

        Map<String, Float> explosionMap = new HashMap<>();
        explosionMaps.put(projectile, explosionMap);
        explosionMap.put(DELETE_TIME, currentTime + 0.1f);

        float radius;
        if (projectile instanceof DamagingExplosion) {
            radius = ((DamagingExplosion) projectile).getExplosionSpecIfExplosion().getRadius();
        } else {
            radius = ((MissileAPI) projectile).getSpec().getExplosionRadius();
        }

        List<ShipAPI> potentialOcclusions = new ArrayList<>(parent.getChildModulesCopy());
        potentialOcclusions.add(parent);

        potentialOcclusions.removeIf(occlusion -> {
            float maxDistance = radius + Misc.getTargetingRadius(projectile.getLocation(), occlusion, false);
            return Misc.getDistanceSq(occlusion.getLocation(), projectile.getLocation()) >= maxDistance * maxDistance;
        });

        if (potentialOcclusions.isEmpty()) return explosionMap;
        if (potentialOcclusions.size() == 1) {
            explosionMap.put(potentialOcclusions.get(0).getId(), 1f);
            return explosionMap;
        }

        Map<ShipAPI, Integer> hitsMap = new HashMap<>();
        List<Vector2f> rayEndpoints = MathUtils.getPointsAlongCircumference(projectile.getLocation(), radius, NUM_RAYCASTS, 0f);

        int totalRayHits = 0;
        for (Vector2f endpoint : rayEndpoints) {
            ShipAPI closestTarget = null;
            float targetDistanceSq = Float.POSITIVE_INFINITY;

            for (ShipAPI potentialOcclusion : potentialOcclusions) {
                Vector2f pointOnBounds = CollisionUtils.getCollisionPoint(projectile.getLocation(), endpoint, potentialOcclusion);
                if (pointOnBounds != null) {
                    float occlusionDistance = Misc.getDistanceSq(projectile.getLocation(), pointOnBounds);
                    if (occlusionDistance < targetDistanceSq) {
                        closestTarget = potentialOcclusion;
                        targetDistanceSq = occlusionDistance;
                    }
                }
            }

            if (closestTarget != null) {
                totalRayHits++;
                hitsMap.put(closestTarget, hitsMap.getOrDefault(closestTarget, 0) + 1);
            }
        }

        if (hitsMap.isEmpty()) return explosionMap;
        if (hitsMap.size() == 1) {
            explosionMap.put(hitsMap.keySet().iterator().next().getId(), 1f);
            return explosionMap;
        }

        float overkillDamage = 0f;
        for (Map.Entry<ShipAPI, Integer> entry : hitsMap.entrySet()) {
            ShipAPI occlusion = entry.getKey();
            int rayHits = entry.getValue();

            if (occlusion == parent) continue;

            float damageMult = Math.min(1f, Math.max(rayHits / (float) totalRayHits, rayHits / (float) (NUM_RAYCASTS / 2)));
            explosionMap.put(occlusion.getId(), damageMult);

            float angle = Misc.getAngleInDegrees(occlusion.getLocation(), projectile.getLocation());
            float armor = occlusion.getAverageArmorInSlice(angle, 30f);

            DamageAfterArmor.Pair<Float, Float> damages = DamageAfterArmor.calculate(
                    projectile.getDamageType(),
                    projectile.getDamageAmount() * damageMult,
                    projectile.getDamageAmount(),
                    armor,
                    occlusion
            );

            float hullDamage = damages.getSecond();
            overkillDamage += Math.max(0f, hullDamage - occlusion.getHitpoints());
        }

        float damageMult = hitsMap.containsKey(parent) ?
                Math.min(1f, Math.max(hitsMap.get(parent) / (float) totalRayHits, hitsMap.get(parent) / (float) (NUM_RAYCASTS / 2))) : 0f;

        float finalDamageMult = Math.min(((projectile.getDamageAmount() * damageMult) + overkillDamage) / projectile.getDamageAmount(), 1f);
        explosionMap.put(parent.getId(), finalDamageMult);

        return explosionMap;
    }
}