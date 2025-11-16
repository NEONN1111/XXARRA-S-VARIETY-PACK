package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class NSP_Kardakesfeeder extends BaseShipSystemScript {

    public static float EFFECT = 0f;
    public static final float FLUX_REDUCTION = 50f;
    public static float RANGE_BONUS = 100f;
    public static float RECOIL_BONUS = 75f;
    public static float PROJECTILE_SPEED_BONUS = 50f;


    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        final ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }

        if (state == State.ACTIVE) {
            EFFECT = EFFECT + 0.02f;
        }

        float mult = 1f + EFFECT * effectLevel;
        stats.getBallisticRoFMult().modifyMult(id, mult);
        stats.getBallisticAmmoRegenMult().modifyMult(id, mult);
        stats.getBallisticWeaponFluxCostMod().modifyMult(id, 0.5f);
        stats.getBallisticWeaponRangeBonus().modifyMult(id, mult);
        stats.getBallisticWeaponRangeBonus().modifyPercent(id, RANGE_BONUS);
        stats.getEnergyWeaponRangeBonus().modifyPercent(id, RANGE_BONUS);
        stats.getMaxRecoilMult().modifyMult(id, 1f - (0.01f * RECOIL_BONUS));
        stats.getRecoilPerShotMult().modifyMult(id, 1f - (0.01f * RECOIL_BONUS));
        stats.getRecoilDecayMult().modifyMult(id, 1f - (0.01f * RECOIL_BONUS));

        stats.getBallisticProjectileSpeedMult().modifyPercent(id, PROJECTILE_SPEED_BONUS);
        stats.getEnergyProjectileSpeedMult().modifyPercent(id, PROJECTILE_SPEED_BONUS);


        stats.getCombatWeaponRepairTimeMult().modifyMult(id, 2 * mult);

    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getBallisticRoFMult().unmodify(id);
        stats.getCombatWeaponRepairTimeMult().unmodify(id);

        EFFECT = 0;
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        float mult = (EFFECT * effectLevel) * 100;

        if (index == 0) {
            return new StatusData("ballistic ammo regeneration and rate of fire +" + (int) mult + "%", false);
        }
        return null;
    }
}
