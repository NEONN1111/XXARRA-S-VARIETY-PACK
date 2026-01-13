package neon.nsp.data.scripts.everyframe;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import com.fs.starfarer.api.util.IntervalUtil;
import neon.nsp.data.plugins.NSPModPlugin;
import neon.nsp.data.scripts.util.NSP_Util;
import java.awt.Color;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicTrailPlugin;

public class NSP_Trails extends BaseEveryFrameCombatPlugin {

    private static final String CANISTER_PROJECTILE_ID = "nsp_restored_sporeship_canistercannon_shot";
    private static final String CANISTER_SUBMUNITION_PROJECTILE_ID = "nsp_sporeship_canister_sub_shot";

    private static final Color CANISTER_TRAIL_COLOR_START = new Color(255, 150, 100);
    private static final Color CANISTER_TRAIL_COLOR_END = new Color(255, 125, 75);
    private static final Color CANISTER_TRAIL2_COLOR = new Color(100, 90, 80);
    private static final Color CANISTER_SUBMUNITION_TRAIL_COLOR_START = new Color(150, 110, 110);
    private static final Color CANISTER_SUBMUNITION_TRAIL_COLOR_END = new Color(150, 90, 70);

    private static final float SIXTY_FPS = 1f / 60f;
    private static final String DATA_KEY = "NSP_Trails";

    private CombatEngineAPI engine;

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null) {
            return;
        }
        if (engine.isPaused()) {
            return;
        }

        final LocalData localData = (LocalData) engine.getCustomData().get(DATA_KEY);
        final Map<DamagingProjectileAPI, TrailData> trailMap = localData.trailMap;

        List<DamagingProjectileAPI> projectiles = engine.getProjectiles();
        int size = projectiles.size();
        double trailCount = 0f;
        for (int i = 0; i < size; i++) {
            DamagingProjectileAPI projectile = projectiles.get(i);
            if (projectile.getProjectileSpecId() == null) {
                continue;
            }

            //switch (projectile.getProjectileSpecId()) {
            // case CONTENDER_PROJECTILE_ID -> {
            // if (SWP_Util.isOnscreen(projectile.getLocation(), projectile.getVelocity().length() * 0.3f)) {
            //trailCount += 1f;
            //}
            // }
            // default -> {
            // }
            // }
        }

        float trailFPSRatio = Math.min(3f, (float) Math.max(1f, (trailCount / 30f)));

        for (int i = 0; i < size; i++) {
            DamagingProjectileAPI proj = projectiles.get(i);
            String spec = proj.getProjectileSpecId();
            TrailData data;
            if (spec == null) {
                continue;
            }

            // Enable angle fade for all projectiles (true by default for canister types)
            boolean enableAngleFade = true;

            switch (spec) {
                case CANISTER_PROJECTILE_ID, CANISTER_SUBMUNITION_PROJECTILE_ID -> {
                    data = trailMap.get(proj);
                    if (data == null) {
                        data = new TrailData();
                        data.id = MagicTrailPlugin.getUniqueID();

                        switch (spec) {
                            case CANISTER_PROJECTILE_ID:
                                data.id2 = MagicTrailPlugin.getUniqueID();
                                break;
                            default:
                                break;
                        }
                        trailMap.put(proj, data);
                    }
                }
                default -> {
                    continue;
                }
            }

            if (!data.enabled) {
                continue;
            }

            float fade = 1f;
            if (proj.getBaseDamageAmount() > 0f) {
                fade = proj.getDamageAmount() / proj.getBaseDamageAmount();
            }

            if (enableAngleFade) {
                float velFacing = VectorUtils.getFacing(proj.getVelocity());
                float angleError = Math.abs(MathUtils.getShortestRotation(proj.getFacing(), velFacing));

                float angleFade = 1f - Math.min(Math.max(angleError - 45f, 0f) / 45f, 1f);
                fade *= angleFade;

                if (angleFade <= 0f) {
                    if (!data.cut) {
                        MagicTrailPlugin.cutTrailsOnEntity(proj);
                        data.cut = true;
                    }
                } else {
                    data.cut = false;
                }
            }

            if (fade <= 0f) {
                continue;
            }

            fade = Math.max(0f, Math.min(1f, fade));

            Vector2f projVel = new Vector2f(proj.getVelocity());
            Vector2f projBodyVel = VectorUtils.rotate(new Vector2f(projVel), -proj.getFacing());
            Vector2f projLateralBodyVel = new Vector2f(0f, projBodyVel.getY());
            Vector2f sidewaysVel = VectorUtils.rotate(new Vector2f(projLateralBodyVel), proj.getFacing());

            Vector2f spawnPosition = new Vector2f(proj.getLocation());
            if (proj.getSpawnType() != ProjectileSpawnType.BALLISTIC_AS_BEAM) {
                spawnPosition.x += sidewaysVel.x * amount * -1.05f;
                spawnPosition.y += sidewaysVel.y * amount * -1.05f;
            }

            switch (spec) {
                case CANISTER_PROJECTILE_ID -> {
                    if (data.interval == null) {
                        data.interval = new IntervalUtil(SIXTY_FPS, SIXTY_FPS);
                    }
                    data.interval.advance(amount);
                    if (data.interval.intervalElapsed()) {
                        float offset = 30f;
                        Vector2f offsetPoint = new Vector2f((float) Math.cos(Math.toRadians(proj.getFacing())) * offset, (float) Math.sin(Math.toRadians(proj.getFacing())) * offset);
                        spawnPosition.x += offsetPoint.x;
                        spawnPosition.y += offsetPoint.y;

                        MagicTrailPlugin.addTrailMemberAdvanced(
                                proj, /* linkedEntity */
                                data.id, /* ID */
                                Global.getSettings().getSprite("swp_trails", "swp_fuzzytrail"), /* sprite */
                                spawnPosition, /* position */
                                0f, /* startSpeed */
                                0f, /* endSpeed */
                                proj.getFacing() - 180f, /* angle */
                                0f, /* startAngularVelocity */
                                0f, /* endAngularVelocity */
                                15f, /* startSize */
                                10f, /* endSize */
                                CANISTER_TRAIL_COLOR_START, /* startColor */
                                CANISTER_TRAIL_COLOR_END, /* endColor */
                                fade, /* opacity */
                                0f, /* inDuration */
                                0f, /* mainDuration */
                                0.8f, /* outDuration */
                                GL11.GL_SRC_ALPHA, /* blendModeSRC */
                                GL11.GL_ONE, /* blendModeDEST */
                                500f, /* textureLoopLength */
                                400f, /* textureScrollSpeed */
                                -1, /* textureOffset */
                                sidewaysVel, /* offsetVelocity */
                                null, /* advancedOptions */
                                CombatEngineLayers.CONTRAILS_LAYER, /* layerToRenderOn */
                                1f /* frameOffsetMult */
                        );
                        MagicTrailPlugin.addTrailMemberAdvanced(
                                proj, /* linkedEntity */
                                data.id2, /* ID */
                                Global.getSettings().getSprite("swp_trails", "swp_dirtytrail"), /* sprite */
                                spawnPosition, /* position */
                                0f, /* startSpeed */
                                0f, /* endSpeed */
                                proj.getFacing() - 180f, /* angle */
                                0f, /* startAngularVelocity */
                                0f, /* endAngularVelocity */
                                30f, /* startSize */
                                60f, /* endSize */
                                CANISTER_TRAIL2_COLOR, /* startColor */
                                CANISTER_TRAIL2_COLOR, /* endColor */
                                fade * 0.7f, /* opacity */
                                0f, /* inDuration */
                                0f, /* mainDuration */
                                2f, /* outDuration */
                                GL11.GL_SRC_ALPHA, /* blendModeSRC */
                                GL11.GL_ONE, /* blendModeDEST */
                                500f, /* textureLoopLength */
                                600f, /* textureScrollSpeed */
                                -1, /* textureOffset */
                                sidewaysVel, /* offsetVelocity */
                                null, /* advancedOptions */
                                CombatEngineLayers.CONTRAILS_LAYER, /* layerToRenderOn */
                                1f /* frameOffsetMult */
                        );
                    }
                }

                case CANISTER_SUBMUNITION_PROJECTILE_ID -> {
                    if (data.interval == null) {
                        data.interval = new IntervalUtil(SIXTY_FPS * 2f, SIXTY_FPS * 2f);
                    }
                    data.interval.advance(amount);
                    if (data.interval.intervalElapsed()) {
                        float offset = 5f;
                        Vector2f offsetPoint = new Vector2f((float) Math.cos(Math.toRadians(proj.getFacing())) * offset, (float) Math.sin(Math.toRadians(proj.getFacing())) * offset);
                        spawnPosition.x += offsetPoint.x;
                        spawnPosition.y += offsetPoint.y;

                        MagicTrailPlugin.addTrailMemberAdvanced(
                                proj, /* linkedEntity */
                                data.id, /* ID */
                                Global.getSettings().getSprite("swp_trails", "swp_dirtytrail"), /* sprite */
                                spawnPosition, /* position */
                                0f, /* startSpeed */
                                0f, /* endSpeed */
                                proj.getFacing() - 180f, /* angle */
                                0f, /* startAngularVelocity */
                                0f, /* endAngularVelocity */
                                10f, /* startSize */
                                20f, /* endSize */
                                CANISTER_SUBMUNITION_TRAIL_COLOR_START, /* startColor */
                                CANISTER_SUBMUNITION_TRAIL_COLOR_END, /* endColor */
                                fade * 0.4f, /* opacity */
                                0f, /* inDuration */
                                0f, /* mainDuration */
                                0.8f, /* outDuration */
                                GL11.GL_SRC_ALPHA, /* blendModeSRC */
                                GL11.GL_ONE, /* blendModeDEST */
                                400f, /* textureLoopLength */
                                400f, /* textureScrollSpeed */
                                -1, /* textureOffset */
                                sidewaysVel, /* offsetVelocity */
                                null, /* advancedOptions */
                                CombatEngineLayers.CONTRAILS_LAYER, /* layerToRenderOn */
                                1f /* frameOffsetMult */
                        );
                    }
                }

                default -> {
                }
            }
        }

        /* Clean up */
        Iterator<DamagingProjectileAPI> iter = trailMap.keySet().iterator();
        while (iter.hasNext()) {
            DamagingProjectileAPI proj = iter.next();
            if (!engine.isEntityInPlay(proj)) {
                iter.remove();
            }
        }
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }

    public static void createIfNeeded() {
        if (!NSPModPlugin.hasMagicLib) {
            return;
        }

        if (Global.getCombatEngine() != null) {
            if (!Global.getCombatEngine().getCustomData().containsKey(DATA_KEY)) {
                Global.getCombatEngine().getCustomData().put(DATA_KEY, new LocalData());
                Global.getCombatEngine().addPlugin(new NSP_Trails()); // Changed from SWP_Trails to NSP_Trails
            }
        }
    }

    private static final class LocalData {
        final Map<DamagingProjectileAPI, TrailData> trailMap = new LinkedHashMap<>(100);
    }

    private static final class TrailData {
        Float id = null;
        Float id2 = null;
        Float id3 = null;
        Float id4 = null;
        Float id5 = null;
        IntervalUtil interval = null;
        boolean cut = false;
        boolean enabled = true;
    }
}