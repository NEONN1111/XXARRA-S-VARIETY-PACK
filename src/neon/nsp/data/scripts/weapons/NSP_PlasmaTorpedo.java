package neon.nsp.data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import neon.nsp.data.scripts.plugins.EnergyTorpedoPlugin;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class NSP_PlasmaTorpedo implements OnFireEffectPlugin, OnHitEffectPlugin, EveryFrameWeaponEffectPlugin {


    protected CombatEntityAPI torpedoEffectEntity;
    protected EnergyTorpedoPlugin torpedoEffectPlugin;

    protected Color COLOR_INNER = new Color(255,100,100,255);
    protected Color COLOR_OUTER = new Color(0,0,255,255);

    public NSP_PlasmaTorpedo() {
    }

    //protected IntervalUtil interval = new IntervalUtil(0.1f, 0.2f);
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        //interval.advance(amount);

        boolean charging = weapon.getChargeLevel() > 0 && weapon.getCooldownRemaining() <= 0;
        if (charging && torpedoEffectEntity == null) {
            torpedoEffectPlugin = new EnergyTorpedoPlugin(weapon, COLOR_INNER, COLOR_OUTER);
            torpedoEffectEntity = Global.getCombatEngine().addLayeredRenderingPlugin(torpedoEffectPlugin);
        } else if (!charging && torpedoEffectEntity != null) {
            torpedoEffectEntity = null;
            torpedoEffectPlugin = null;
        }
    }


    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

    }

    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (torpedoEffectPlugin != null) {
            torpedoEffectPlugin.attachToProjectile(projectile);
            torpedoEffectPlugin = null;
            torpedoEffectEntity = null;
        }
    }
}