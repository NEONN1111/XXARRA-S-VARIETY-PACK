package neon.nsp.data.scripts.skills;

import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.util.HashMap;
import java.util.Map;

public class WeirdslayerNSP {
	public static Map<HullSize, Float> BONUS = new HashMap<HullSize, Float>();
	public static float DAMAGE_BONUS_PERCENT = 25f;


	public static class Level1 implements ShipSkillEffect {
		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
			ShipAPI ship = (ShipAPI) stats.getEntity();
			if (ship != null && !ship.hasListenerOfClass(WeirdslayerListenerNSP.class)) {
				ship.addListener(new WeirdslayerListenerNSP());
			}
		}

		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
			ShipAPI ship = (ShipAPI) stats.getEntity();
			if (ship != null && ship.hasListenerOfClass(WeirdslayerListenerNSP.class)) {
				ship.removeListenerOfClass(WeirdslayerListenerNSP.class);
			}
		}

		public String getEffectDescription(float level) {
			int bonus = (int) DAMAGE_BONUS_PERCENT;
			return "+" + bonus + "% damage dealt to existential threats.";		}

		public String getEffectPerLevelDescription() {
			return null;
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
}