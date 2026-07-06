package neon.nsp.data.scripts.weapons;

import com.fs.starfarer.api.combat.*;

import java.util.Arrays;
import java.util.List;

public class NSP_VPPCOnFireEffect implements OnFireEffectPlugin {

    private static final List<DamageType> DAMAGE_TYPES = Arrays.asList(
            DamageType.KINETIC,
            DamageType.FRAGMENTATION,
            DamageType.HIGH_EXPLOSIVE,
            DamageType.ENERGY
    );

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if(Math.random() <= 0.5f) {
            int randIndex = (int) (Math.random() * DAMAGE_TYPES.size());
            projectile.getDamage().setType(DAMAGE_TYPES.get(randIndex));
        }
    }
}