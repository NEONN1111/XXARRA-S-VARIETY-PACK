package neon.nsp.data.scripts.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.*;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.Map;

public class WeirdslayerNSP {
	public static Map<ShipAPI.HullSize, Float> BONUS = new HashMap<ShipAPI.HullSize, Float>();
	public static float DAMAGE_BONUS_PERCENT = 25f;

	// Static initializer for BONUS map
	static {
		BONUS.put(ShipAPI.HullSize.FIGHTER, 25f);
		BONUS.put(ShipAPI.HullSize.FRIGATE, 25f);
		BONUS.put(ShipAPI.HullSize.DESTROYER, 25f);
		BONUS.put(ShipAPI.HullSize.CRUISER, 25f);
		BONUS.put(ShipAPI.HullSize.CAPITAL_SHIP, 25f);
	}

	// Level1 class for Weirdslayer skill
	public static class Level1 implements ShipSkillEffect {
		public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
			ShipAPI ship = (ShipAPI) stats.getEntity();
			if (ship != null && !ship.hasListenerOfClass(WeirdslayerListenerNSP.class)) {
				ship.addListener(new WeirdslayerListenerNSP());
			}
		}

		public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
			ShipAPI ship = (ShipAPI) stats.getEntity();
			if (ship != null && ship.hasListenerOfClass(WeirdslayerListenerNSP.class)) {
				ship.removeListenerOfClass(WeirdslayerListenerNSP.class);
			}
		}

		public String getEffectDescription(float level) {
			int bonus = (int) DAMAGE_BONUS_PERCENT;
			return "+" + bonus + "% damage dealt to existential threats.";
		}

		public String getEffectPerLevelDescription() {
			return null;
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}

	// Storm Queller Level1 class
	public static class StormQuellerLevel1 implements AfterShipCreationSkillEffect {
		@Override
		public String getEffectDescription(float level) {
			return "-20% damage taken from Shroud\nShroud will attempt to avoid this ship in combat if possible";
		}

		@Override
		public String getEffectPerLevelDescription() {
			return null;
		}

		@Override
		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}

		@Override
		public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
			// Empty implementation - effects are handled by listener
		}

		@Override
		public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
			// Empty implementation
		}

		@Override
		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship != null && !ship.hasListenerOfClass(StormQuellerListener.class)) {
				ship.addListener(new StormQuellerListener(ship));
			}
		}

		@Override
		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (ship != null && ship.hasListenerOfClass(StormQuellerListener.class)) {
				ship.removeListenerOfClass(StormQuellerListener.class);
			}
		}
	}

	// Storm Queller Listener
	public static class StormQuellerListener implements AdvanceableListener, DamageTakenModifier {
		private final ShipAPI ship;
		private final float mult = 0.80f;
		private final String mod = "dweller_hullmod";
		private final IntervalUtil checker = new IntervalUtil(3f, 3f);

		public StormQuellerListener(ShipAPI ship) {
			this.ship = ship;
		}

		@Override
		public void advance(float amount) {
			CombatEngineAPI engine = Global.getCombatEngine();
			if (engine == null || engine.isPaused()) return;

			checker.advance(amount);
			if (checker.intervalElapsed()) {
				for (ShipAPI enemy : AIUtils.getNearbyEnemies(ship, 1500f)) {
					if (enemy.getVariant() != null && enemy.getVariant().hasHullMod(mod)) {
						enemy.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.BACK_OFF, 6f);
						enemy.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.RUN_QUICKLY, 6f);
						enemy.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.NEEDS_HELP, 6f);
					}
				}
			}
		}




		@Override
		public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
			if (damage == null) return "oops";

			if (param instanceof DamagingProjectileAPI) {
				DamagingProjectileAPI projectile = (DamagingProjectileAPI) param;
				ShipAPI source = projectile.getSource();
				if (source != null && source.getVariant() != null && source.getVariant().hasHullMod(mod)) {
					damage.getModifier().modifyMult(this.getClass().getName(), mult);
					return this.getClass().getName();
				}
			}

			if (param instanceof BeamAPI) {
				BeamAPI beam = (BeamAPI) param;
				ShipAPI source = beam.getSource();
				if (source != null && source.getVariant() != null && source.getVariant().hasHullMod(mod)) {
					damage.getModifier().modifyMult(this.getClass().getName(), mult);
					return this.getClass().getName();
				}
			}

			return "gaming";
		}
	}

	// Weirdslayer Listener
	public static class WeirdslayerListenerNSP implements AdvanceableListener {
		// You'll need to implement the listener logic for Weirdslayer here
		// This would handle the +25% damage to existential threats

		@Override
		public void advance(float amount) {
			// Implementation for Weirdslayer advancement logic
		}
	}
}