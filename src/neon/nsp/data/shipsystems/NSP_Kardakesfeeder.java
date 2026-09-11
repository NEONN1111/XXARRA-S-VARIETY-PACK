package neon.nsp.data.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class NSP_Kardakesfeeder extends BaseShipSystemScript {
    public static final float FLUX_REDUCTION_MULT = 0.5f;
    public static float BASE_BONUS = 100f; //Flat buff at activation
    public static float BONUS_SCALING_CAP = 500f; //Cap to bonus reached when system swithes to ACTIVE state
    public static float RECOIL_BONUS = 0.25f;
    public static float PROJECTILE_SPEED_BONUS = 50f;

    float scaling_effect = 0f;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        final ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }

        scaling_effect = BASE_BONUS + effectLevel * BONUS_SCALING_CAP;
        //Ballistic stuff
        stats.getBallisticWeaponRangeBonus().modifyPercent(id, scaling_effect);
        stats.getBallisticRoFMult().modifyPercent(id, scaling_effect);
        stats.getBallisticAmmoRegenMult().modifyPercent(id, scaling_effect);
        stats.getBallisticProjectileSpeedMult().modifyPercent(id, PROJECTILE_SPEED_BONUS);
        stats.getBallisticWeaponFluxCostMod().modifyMult(id, FLUX_REDUCTION_MULT);

        //Energy stuff
        stats.getEnergyWeaponRangeBonus().modifyPercent(id, scaling_effect);
        stats.getEnergyRoFMult().modifyPercent(id, scaling_effect);
        stats.getEnergyAmmoRegenMult().modifyPercent(id, scaling_effect);
        stats.getEnergyProjectileSpeedMult().modifyPercent(id, PROJECTILE_SPEED_BONUS);
        stats.getEnergyWeaponFluxCostMod().modifyMult(id, FLUX_REDUCTION_MULT);

        //stats.getCombatWeaponRepairTimeMult().modifyMult(id, 2 * mult); //feels bit extra? also not sure why it was scaling
        stats.getMaxRecoilMult().modifyMult(id, RECOIL_BONUS);
        stats.getRecoilPerShotMult().modifyMult(id, RECOIL_BONUS);
        stats.getRecoilDecayMult().modifyMult(id, RECOIL_BONUS); //Debuffs decay to compensate for recoil reduction buff
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        //Ballistic stuff
        stats.getBallisticWeaponRangeBonus().unmodifyPercent(id);
        stats.getBallisticRoFMult().unmodifyPercent(id);
        stats.getBallisticAmmoRegenMult().unmodifyPercent(id);
        stats.getBallisticProjectileSpeedMult().unmodifyPercent(id);
        stats.getBallisticWeaponFluxCostMod().unmodifyMult(id);

        //Energy stuff
        stats.getEnergyWeaponRangeBonus().unmodifyPercent(id);
        stats.getEnergyRoFMult().unmodifyPercent(id);
        stats.getEnergyAmmoRegenMult().unmodifyPercent(id);
        stats.getEnergyProjectileSpeedMult().unmodifyPercent(id);
        stats.getEnergyWeaponFluxCostMod().modifyMult(id, FLUX_REDUCTION_MULT);

        stats.getMaxRecoilMult().modifyMult(id, RECOIL_BONUS);
        stats.getRecoilPerShotMult().modifyMult(id, RECOIL_BONUS);
        stats.getRecoilDecayMult().modifyMult(id, RECOIL_BONUS); //Debuffs decay to compensate for recoil reduction buff
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        scaling_effect = BASE_BONUS + effectLevel * BONUS_SCALING_CAP;

        if (index == 3) {
            return new StatusData("weapon range and rate of fire: +" + (int) scaling_effect + "%", false);
        }
        if (index == 2) {
            return new StatusData("weapon flux cost: -" + (int) ((1f-FLUX_REDUCTION_MULT)*100) + "%", false);
        }
        if (index == 1) {
            return new StatusData("weapon recoil: -" + (int) ((1f-RECOIL_BONUS) * 100f) + "%", false);
        }
        if (index == 0 && PROJECTILE_SPEED_BONUS > 0) {
            return new StatusData("projectile speed: +" + (int) PROJECTILE_SPEED_BONUS + "%", false);
        }
        return null;
    }
}
