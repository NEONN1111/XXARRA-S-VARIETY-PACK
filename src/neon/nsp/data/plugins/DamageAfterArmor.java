package neon.nsp.data.plugins;

import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class DamageAfterArmor {

    public static Pair<Float, Float> calculate(
            DamageType damageType,
            float damage,
            float hitStrength,
            float armorValue,
            ShipAPI ship) {

        MutableShipStatsAPI stats = ship.getMutableStats();

        float armorMultiplier = stats.getArmorDamageTakenMult().getModifiedValue();
        float effectiveArmorMult = stats.getEffectiveArmorBonus().getBonusMult();
        float hullMultiplier = stats.getHullDamageTakenMult().getModifiedValue();
        float minArmor = stats.getMinArmorFraction().getModifiedValue();
        float maxDR = stats.getMaxArmorDamageReduction().getModifiedValue();

        switch (damageType) {
            case FRAGMENTATION:
                armorMultiplier *= (0.25f * stats.getFragmentationDamageTakenMult().getModifiedValue());
                hullMultiplier *= stats.getFragmentationDamageTakenMult().getModifiedValue();
                break;
            case KINETIC:
                armorMultiplier *= (0.5f * stats.getKineticDamageTakenMult().getModifiedValue());
                hullMultiplier *= stats.getKineticDamageTakenMult().getModifiedValue();
                break;
            case HIGH_EXPLOSIVE:
                armorMultiplier *= (2f * stats.getHighExplosiveDamageTakenMult().getModifiedValue());
                hullMultiplier *= stats.getHighExplosiveDamageTakenMult().getModifiedValue();
                break;
            case ENERGY:
                armorMultiplier *= stats.getEnergyDamageTakenMult().getModifiedValue();
                hullMultiplier *= stats.getEnergyDamageTakenMult().getModifiedValue();
                break;
            default:
                break;
        }

        float effectiveArmor = Math.max(minArmor * ship.getArmorGrid().getArmorRating(), armorValue) * effectiveArmorMult;
        float damageReductionFactor = (hitStrength * armorMultiplier) / (effectiveArmor + hitStrength * armorMultiplier);
        float armorDR = Math.max(1f - maxDR, damageReductionFactor);

        float effectiveDamage = damage * armorDR;
        float armorDamage = effectiveDamage * armorMultiplier;

        float hullDamage = 0f;
        if (armorDamage > armorValue) {
            hullDamage = ((armorDamage - armorValue) / armorDamage) * effectiveDamage * hullMultiplier;
        }

        return new Pair<>(armorDamage, hullDamage);
    }

    public static class Pair<A, B> {
        private final A first;
        private final B second;

        public Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }

        public A getFirst() { return first; }
        public B getSecond() { return second; }
    }
}