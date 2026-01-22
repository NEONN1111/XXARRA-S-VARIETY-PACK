package neon.nsp.data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.MagicRender;
import java.awt.Color;
import java.util.List;
import org.lazywizard.lazylib.FastTrig;
import org.lwjgl.util.vector.Vector2f;

public class NSP_SymbiantSystem extends BaseShipSystemScript {
    public static final float DAMAGE_BONUS_PERCENT = 1.5F;

    public static final float DISSIPATION_MULT = 1.5F;

    public static final float MAX_TIME_MULT = 3.0F;

    private static final Color AFTERIMAGE_COLOR = new Color(255, 63, 0, 100);

    // Mild red jitter colors
    private static final Color MILD_JITTER_COLOR = new Color(255, 80, 60, 30);
    private static final Color MILD_JITTER_UNDER_COLOR = new Color(255, 50, 50, 60);

    private IntervalUtil interval = new IntervalUtil(0.2F, 0.2F);

    public void apply(MutableShipStatsAPI stats, String id, ShipSystemStatsScript.State state, float effectLevel) {
        ShipAPI ship = null;
        boolean player = false;
        CombatEngineAPI engine = Global.getCombatEngine();
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI)stats.getEntity();
            player = (ship == Global.getCombatEngine().getPlayerShip());
        } else {
            return;
        }
        float bonusPercent = 1.0F + 0.5F * effectLevel;
        stats.getEnergyWeaponDamageMult().modifyMult(id, bonusPercent);
        stats.getBallisticWeaponDamageMult().modifyMult(id, bonusPercent);
        stats.getBeamWeaponDamageMult().modifyMult(id, bonusPercent);
        stats.getFluxDissipation().modifyMult(id, 1.5F);
        float TimeMult = 1.0F + 2.0F * effectLevel;
        stats.getTimeMult().modifyMult(id, TimeMult);
        if (player) {
            Global.getCombatEngine().getTimeMult().modifyMult(id, 1.0F / TimeMult);
        } else {
            Global.getCombatEngine().getTimeMult().unmodify(id);
        }
        if (state == ShipSystemStatsScript.State.OUT) {
            stats.getMaxSpeed().unmodify(id);
        } else {
            stats.getMaxSpeed().modifyFlat(id, 200.0F);
            stats.getAcceleration().modifyFlat(id, 400.0F);
            stats.getDeceleration().modifyFlat(id, 400.0F);
            stats.getTurnAcceleration().modifyMult(id, 2.0F);
            stats.getMaxTurnRate().modifyMult(id, 2.0F);

            // Apply mild red jitter to ship and modules
            float jitterLevel = effectLevel;
            if (jitterLevel > 0) {
                // Apply to main ship
                ship.setJitter(this, MILD_JITTER_COLOR, jitterLevel * 0.8f, 3, 0f, jitterLevel * 5f);
                ship.setJitterUnder(this, MILD_JITTER_UNDER_COLOR, jitterLevel * 0.8f, 12, 0f, jitterLevel * 8f);

                // Apply to modules
                applyJitterToModules(ship, jitterLevel);
            }
        }
        if (!Global.getCombatEngine().isPaused()) {
            this.interval.advance(Global.getCombatEngine().getElapsedInLastFrame());
            if (this.interval.intervalElapsed()) {
                // Create after-image for main ship
                createAfterimage(ship);

                // Create after-images for all child modules
                if (ship.hasLaunchBays() || !ship.getChildModulesCopy().isEmpty()) {
                    createAfterimagesForModules(ship);
                }
            }
        }
    }

    private void applyJitterToModules(ShipAPI ship, float jitterLevel) {
        if (ship == null) return;

        List<ShipAPI> modules = ship.getChildModulesCopy();
        for (ShipAPI module : modules) {
            if (module != null && module.isAlive()) {
                // Apply mild red jitter to module
                module.setJitter(this,
                        new Color(255, 70, 50, 25), // Even more subtle for modules
                        jitterLevel * 0.6f, // Lower intensity for modules
                        2, // Fewer cells
                        0f,
                        jitterLevel * 3f);

                module.setJitterUnder(this,
                        new Color(255, 40, 30, 40),
                        jitterLevel * 0.6f,
                        8, // Fewer cells for under jitter
                        0f,
                        jitterLevel * 5f);

                // Recursively apply to sub-modules if any
                if (module.hasLaunchBays() || !module.getChildModulesCopy().isEmpty()) {
                    applyJitterToModules(module, jitterLevel * 0.8f);
                }
            }
        }
    }

    private void createAfterimage(ShipAPI ship) {
        SpriteAPI sprite = ship.getSpriteAPI();
        float offsetX = sprite.getWidth() / 2.0F - sprite.getCenterX();
        float offsetY = sprite.getHeight() / 2.0F - sprite.getCenterY();
        float trueOffsetX = (float)FastTrig.cos(Math.toRadians((ship.getFacing() - 90.0F))) * offsetX - (float)FastTrig.sin(Math.toRadians((ship.getFacing() - 90.0F))) * offsetY;
        float trueOffsetY = (float)FastTrig.sin(Math.toRadians((ship.getFacing() - 90.0F))) * offsetX + (float)FastTrig.cos(Math.toRadians((ship.getFacing() - 90.0F))) * offsetY;

        try {
            MagicRender.battlespace(
                    Global.getSettings().getSprite(ship.getHullSpec().getSpriteName()),
                    new Vector2f(ship.getLocation().getX() + trueOffsetX, ship.getLocation().getY() + trueOffsetY),
                    new Vector2f(0.0F, 0.0F),
                    new Vector2f(ship.getSpriteAPI().getWidth(), ship.getSpriteAPI().getHeight()),
                    new Vector2f(0.0F, 0.0F),
                    ship.getFacing() - 90.0F,
                    0.0F,
                    AFTERIMAGE_COLOR,
                    true,
                    0.0F,
                    0.0F,
                    0.0F,
                    0.0F,
                    0.0F,
                    0.1F,
                    0.1F,
                    1.0F,
                    CombatEngineLayers.BELOW_SHIPS_LAYER
            );
        } catch (Exception e) {
            // Fallback to simple particle if MagicRender fails
            Global.getCombatEngine().addSmoothParticle(
                    ship.getLocation(),
                    new Vector2f(),
                    15f + (float)Math.random() * 10f,
                    0.8f,
                    0.5f,
                    AFTERIMAGE_COLOR
            );
        }
    }

    private void createAfterimagesForModules(ShipAPI ship) {
        List<ShipAPI> modules = ship.getChildModulesCopy();

        for (ShipAPI module : modules) {
            if (module != null && module.isAlive()) {
                // Create after-image for module
                createModuleAfterimage(module);

                // Recursively create after-images for any sub-modules
                if (module.hasLaunchBays() || !module.getChildModulesCopy().isEmpty()) {
                    createAfterimagesForModules(module);
                }
            }
        }
    }

    private void createModuleAfterimage(ShipAPI module) {
        if (module == null || !module.isAlive()) return;

        SpriteAPI sprite = module.getSpriteAPI();
        if (sprite == null) {
            // Fallback to simple particle for modules without sprites
            Global.getCombatEngine().addSmoothParticle(
                    module.getLocation(),
                    new Vector2f(),
                    10f + (float)Math.random() * 5f,
                    0.6f,
                    0.4f,
                    new Color(255, 63, 0, 80)
            );
            return;
        }

        float offsetX = sprite.getWidth() / 2.0F - sprite.getCenterX();
        float offsetY = sprite.getHeight() / 2.0F - sprite.getCenterY();
        float trueOffsetX = (float)FastTrig.cos(Math.toRadians((module.getFacing() - 90.0F))) * offsetX - (float)FastTrig.sin(Math.toRadians((module.getFacing() - 90.0F))) * offsetY;
        float trueOffsetY = (float)FastTrig.sin(Math.toRadians((module.getFacing() - 90.0F))) * offsetX + (float)FastTrig.cos(Math.toRadians((module.getFacing() - 90.0F))) * offsetY;

        try {
            // Use the module's hull sprite
            String spriteName = module.getHullSpec().getSpriteName();
            if (spriteName != null && !spriteName.isEmpty()) {
                MagicRender.battlespace(
                        Global.getSettings().getSprite(spriteName),
                        new Vector2f(module.getLocation().getX() + trueOffsetX, module.getLocation().getY() + trueOffsetY),
                        new Vector2f(0.0F, 0.0F),
                        new Vector2f(module.getSpriteAPI().getWidth(), module.getSpriteAPI().getHeight()),
                        new Vector2f(0.0F, 0.0F),
                        module.getFacing() - 90.0F,
                        0.0F,
                        new Color(255, 63, 0, 80), // Slightly more transparent for modules
                        true,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        0.08F,
                        0.08F,
                        0.8F, // Shorter duration for modules
                        CombatEngineLayers.BELOW_SHIPS_LAYER
                );
            }
        } catch (Exception e) {
            // Fallback to simple particle if MagicRender fails
            Global.getCombatEngine().addSmoothParticle(
                    module.getLocation(),
                    new Vector2f(),
                    10f + (float)Math.random() * 5f,
                    0.6f,
                    0.4f,
                    new Color(255, 63, 0, 80)
            );
        }
    }

    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();

            // Clear jitter from ship and modules
            if (ship != null) {
                ship.setJitter(this, MILD_JITTER_COLOR, 0f, 0, 0f, 0f);
                ship.setJitterUnder(this, MILD_JITTER_UNDER_COLOR, 0f, 0, 0f, 0f);
                clearJitterFromModules(ship);
            }
        }

        stats.getEnergyWeaponDamageMult().unmodify(id);
        stats.getBallisticWeaponDamageMult().unmodify(id);
        stats.getBeamWeaponDamageMult().unmodify(id);
        stats.getFluxDissipation().unmodify(id);
        Global.getCombatEngine().getTimeMult().unmodify(id);
        stats.getTimeMult().unmodify(id);
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
    }

    private void clearJitterFromModules(ShipAPI ship) {
        if (ship == null) return;

        List<ShipAPI> modules = ship.getChildModulesCopy();
        for (ShipAPI module : modules) {
            if (module != null) {
                module.setJitter(this, MILD_JITTER_COLOR, 0f, 0, 0f, 0f);
                module.setJitterUnder(this, MILD_JITTER_UNDER_COLOR, 0f, 0, 0f, 0f);

                // Recursively clear from sub-modules
                if (!module.getChildModulesCopy().isEmpty()) {
                    clearJitterFromModules(module);
                }
            }
        }
    }

    public ShipSystemStatsScript.StatusData getStatusData(int index, ShipSystemStatsScript.State state, float effectLevel) {
        float bonusPercent = 1.5F * effectLevel;
        if (index == 0)
            return new ShipSystemStatsScript.StatusData("+50.0% weapon damage", false);
        if (index == 1)
            return new ShipSystemStatsScript.StatusData("engines and dissipation boosted", false);
        if (index == 2)
            return new ShipSystemStatsScript.StatusData("Timeflow accelerated by " + Math.round(200.0F) + "%", false);
        if (index == 3)
            return null;
        return null;
    }
}