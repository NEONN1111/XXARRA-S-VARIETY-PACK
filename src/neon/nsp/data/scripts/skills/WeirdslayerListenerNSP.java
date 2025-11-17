package neon.nsp.data.scripts.skills;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;

class WeirdslayerListenerNSP implements DamageDealtModifier {

	public String modifyDamageDealt(Object param,
									CombatEntityAPI target, DamageAPI damage,
									Vector2f point, boolean shieldHit) {

		ShipAPI enemy = null;


		if (target instanceof ShipAPI) {
			enemy = (ShipAPI) target;
		}
		if (enemy == null || enemy.getVariant() == null) return null;
		if (enemy.getVariant() == null) return null;
		if ((enemy.getVariant().hasHullMod("threat_hullmod"))
				|| (enemy.getVariant().hasHullMod("dweller_hullmod"))
				|| (enemy.getVariant().hasHullMod("shard_spawner"))){
			String id = "templar_damagemod_NSP";
			damage.getModifier().modifyPercent(id, 25f); //
			return id;
		}
		else return null;
	}
}