package neon.nsp.data.scripts.plugins;

import java.util.ArrayList;
import java.util.List;
import java.awt.Color;
import org.lwjgl.util.vector.Vector2f;

import com.fs.graphics.Sprite;
import com.fs.starfarer.api.impl.combat.CombatEntityPluginWithParticles;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

//"Energy Missile" VFX plugin
//By Cap'n MacHaddish
//Free to use - seriously, please steal my code.

public class EnergyTorpedoPlugin extends CombatEntityPluginWithParticles {

    public Color MAIN_COLOR = new Color(255,255,255,255);
    public Color FRINGE_COLOR = new Color(255, 255, 255,255);
    public float CLOUD_ALPHA_MULT = 0.3f;

    protected WeaponAPI weapon;
    protected DamagingProjectileAPI proj;
    protected IntervalUtil interval = new IntervalUtil(0.1f, 0.2f);

    private MissileSpecAPI missileSpec;
    private float minSize;
    private float actualSize;
    private float maxHealth;
    private float brightness = 0.0f;

    protected Sprite raySprite = new Sprite("graphics/fx/torpedoray32.png");

    private class Ray {
        private float angle;
        private float spin;
        private float size;
    }

    private List<Ray> rays = new ArrayList<>();

    public EnergyTorpedoPlugin(WeaponAPI weapon) {
        super();
        this.weapon = weapon;

        setSpriteSheetKey("fx_particles2");

        minSize = 0.033333335F * this.weapon.getProjectileSpeed();

        missileSpec = (MissileSpecAPI)this.weapon.getSpec().getProjectileSpec();

        actualSize = missileSpec.getHullSpec().getCollisionRadius();

        raySprite.setAdditiveBlend();

        MAIN_COLOR = missileSpec.getGlowColor();
        FRINGE_COLOR = Misc.setAlpha(missileSpec.getGlowColor(),100);

        for(int i = 0; i < 6; ++i) {
            Ray r = new Ray();
            r.angle = (float)i * 360.0F / 6.0F;
            r.spin = 160.0F;
            r.size = (float)Math.random() * 0.25F + 0.75F;
            this.rays.add(r);
        }

        for(int i = 0; i < 5; ++i) {
            Ray r = new Ray();
            r.angle = (float)i * 360.0F / 5.0F;
            r.spin = -160.0F;
            r.size = (float)Math.random() * 0.25F + 0.75F;
            this.rays.add(r);
        }

    }

    public EnergyTorpedoPlugin(WeaponAPI weapon, Color mainCol, Color fringeCol) {
        super();
        this.weapon = weapon;

        setSpriteSheetKey("fx_particles2");

        minSize = 0.033333335F * this.weapon.getProjectileSpeed();

        missileSpec = (MissileSpecAPI)this.weapon.getSpec().getProjectileSpec();

        actualSize = missileSpec.getHullSpec().getCollisionRadius();

        raySprite.setAdditiveBlend();

        MAIN_COLOR = mainCol;
        FRINGE_COLOR = fringeCol;

        for(int i = 0; i < 6; ++i) {
            Ray r = new Ray();
            r.angle = (float)i * 360.0F / 6.0F;
            r.spin = 160.0F;
            r.size = (float)Math.random() * 0.25F + 0.75F;
            this.rays.add(r);
        }

        for(int i = 0; i < 5; ++i) {
            Ray r = new Ray();
            r.angle = (float)i * 360.0F / 5.0F;
            r.spin = -160.0F;
            r.size = (float)Math.random() * 0.25F + 0.75F;
            this.rays.add(r);
        }

    }


    public void attachToProjectile(DamagingProjectileAPI proj) {
        this.proj = proj;

        maxHealth = this.proj.getMaxHitpoints();
    }


    private void updateSpriteSize(float in) {
        float newSize = actualSize * in * Math.min(this.brightness * 5.0F, 1.0F);
        raySprite.setSize(newSize / 4.0F, newSize);
        raySprite.setCenter(newSize / 8.0F, newSize * 0.2F);
    }

    public void setColors(Color main, Color fringe) {
        this.MAIN_COLOR = main;
        this.FRINGE_COLOR = fringe;
    }

    public void setColors(Color main, Color fringe, float alpha) {
        this.MAIN_COLOR = main;
        this.FRINGE_COLOR = fringe;
        this.CLOUD_ALPHA_MULT = alpha;
    }

    public void advance(float amount) {
        if (Global.getCombatEngine().isPaused()) return;
        if (proj != null) {
            entity.getLocation().set(proj.getLocation());
            if(isProjectileExpired(proj)) {
                this.rays.clear();
            }
        }
        else {
            entity.getLocation().set(weapon.getFirePoint(0));
        }
        super.advance(amount);

        boolean keepSpawningParticles = isWeaponCharging(weapon) ||
                (proj != null && !isProjectileExpired(proj) && !proj.isFading());
        if (keepSpawningParticles) {
            interval.advance(amount);
            if (interval.intervalElapsed()) {
                addChargingParticles(weapon);
            }
        }

        if (proj != null || isWeaponCharging(weapon)) {
            //Stolen from vanilla PlasmaShot
            if (isWeaponCharging(weapon)) {

                this.brightness += amount / (this.minSize / this.actualSize * 2.0F);
            }
            else {
                this.brightness += amount * 5.0F;
            }

            if (this.brightness > 1.0F) {
                this.brightness = 1.0F;
            }

            for (Ray r : this.rays) {
                r.angle += r.spin * amount * 0.5F;
            }
        }
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (viewport.isNearViewport(entity.getLocation(), actualSize + this.getRenderRadius()) && !this.isExpired()) {
            int alpha = 255;
            if (this.proj != null) {alpha = (int) (255.0F * this.proj.getHitpoints() / this.maxHealth * this.brightness);}

            this.raySprite.setColorLL(Misc.setAlpha(FRINGE_COLOR, alpha));
            this.raySprite.setColorLR(Misc.setAlpha(FRINGE_COLOR, alpha));
            this.raySprite.setColorUL(Misc.setAlpha(MAIN_COLOR, alpha));
            this.raySprite.setColorUR(Misc.setAlpha(MAIN_COLOR, alpha));

            for (Ray r : this.rays) {
                this.updateSpriteSize(r.size);
                this.raySprite.setAngle(r.angle);
                //this.raySprite.setAlphaMult(alphaMult);
                this.raySprite.renderAtCenterWithCornerColors(entity.getLocation().x, entity.getLocation().y);
            }
        }
        // pass in proj as last argument to have particles rotate
        super.render(layer, viewport, null);
    }


    public boolean isExpired() {
        boolean keepSpawningParticles = isWeaponCharging(weapon) ||
                (proj != null && !isProjectileExpired(proj) && !proj.isFading());
        return super.isExpired() && (!keepSpawningParticles || (!weapon.getShip().isAlive() && proj == null));
    }


    public float getRenderRadius() {
        return 500f;
    }


    @Override
    protected float getGlobalAlphaMult() {
        if (proj != null && proj.isFading()) {
            return proj.getBrightness();
        }
        return super.getGlobalAlphaMult();
    }


    public void addChargingParticles(WeaponAPI weapon) {
        //CombatEngineAPI engine = Global.getCombatEngine();
        Color color = Misc.scaleAlpha(MAIN_COLOR,CLOUD_ALPHA_MULT);

//		float b = 1f;
//		color = Misc.scaleAlpha(color, b);
        //undercolor = Misc.scaleAlpha(undercolor, b);

        float size = 30.0f;
        float underSize = 40.0f;
        //underSize = 100f;

        float in = 0.25f;
        float out = 0.75f;

        out *= 3f;

        float velMult = 0.2f;

        if (isWeaponCharging(weapon)) {
            size *= 0.25f + weapon.getChargeLevel() * 0.75f;
        }

        addParticle(size, in, out, 1f, size * 0.5f * velMult, 0f, color);
        randomizePrevParticleLocation(size * 0.33f);

        if (proj != null) {
            Vector2f dir = Misc.getUnitVectorAtDegreeAngle(proj.getFacing() + 180f);
            //size = 40f;
            if (proj.getElapsed() > 0.2f) {
                addParticle(size, in, out, 1.5f, size * 0.5f * velMult, 0f, color);
                Vector2f offset = new Vector2f(dir);
                offset.scale(size * 0.6f + (float) Math.random() * 0.2f);
                Vector2f.add(prev.offset, offset, prev.offset);
            }
            if (proj.getElapsed() > 0.4f) {
                addParticle(size * 1f, in, out, 1.3f, size * 0.5f * velMult, 0f, color);
                Vector2f offset = new Vector2f(dir);
                offset.scale(size * 1.2f + (float) Math.random() * 0.2f);
                Vector2f.add(prev.offset, offset, prev.offset);
            }
            if (proj.getElapsed() > 0.6f) {
                addParticle(size * .8f, in, out, 1.1f, size * 0.5f * velMult, 0f, color);
                Vector2f offset = new Vector2f(dir);
                offset.scale(size * 1.6f + (float) Math.random() * 0.2f);
                Vector2f.add(prev.offset, offset, prev.offset);
            }

            if (proj.getElapsed() > 0.8f) {
                addParticle(size * .8f, in, out, 1.1f, size * 0.5f * velMult, 0f, color);
                Vector2f offset = new Vector2f(dir);
                offset.scale(size * 2.0f + (float) Math.random() * 0.2f);
                Vector2f.add(prev.offset, offset, prev.offset);
            }
//			int num = (int) Math.round(proj.getElapsed() / 0.5f * 10f);
//			if (num > 15) num = 15;
//			for (int i = 0; i < num; i++) {
//				addDarkParticle(size, in, out, 1f, size * 0.5f, 0f, color);
//				Vector2f offset = new Vector2f(dir);
//				offset.scale(size * 0.1f * i);
//				Vector2f.add(prev.offset, offset, prev.offset);
//			}
        }


        addParticle(underSize * 0.5f, in, out, 1.5f * 3f, 0f, 0f, Misc.scaleAlpha(FRINGE_COLOR,CLOUD_ALPHA_MULT));
        randomizePrevParticleLocation(underSize * 0.67f);
        addParticle(underSize * 0.5f, in, out, 1.5f * 3f, 0f, 0f, Misc.scaleAlpha(FRINGE_COLOR,CLOUD_ALPHA_MULT));
        randomizePrevParticleLocation(underSize * 0.67f);

//		float facing = weapon.getCurrAngle();
//		if (proj != null) facing = proj.getFacing();
//		Vector2f dir = Misc.getUnitVectorAtDegreeAngle(facing + 210f * ((float) Math.random() - 0.5f));
//		dir.scale(underSize * 0.25f * (float) Math.random());
//		Vector2f.add(prev.offset, dir, prev.offset);
    }

    public static boolean isProjectileExpired(DamagingProjectileAPI proj) {
        return proj.isExpired() || proj.didDamage() || !Global.getCombatEngine().isEntityInPlay(proj);
    }

    public static boolean isWeaponCharging(WeaponAPI weapon) {
        return weapon.getChargeLevel() > 0 && weapon.getCooldownRemaining() <= 0;
    }
}
