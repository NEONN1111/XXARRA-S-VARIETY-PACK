package neon.nsp.data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;


public class NSP_SV_DamperFieldStats extends BaseShipSystemScript {

	protected Object STATUSKEY1 = new Object();
	protected Object STATUSKEY2 = new Object();

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

		stats.getHullDamageTakenMult().modifyMult(id, 0.2f * effectLevel);
		stats.getArmorDamageTakenMult().modifyMult(id, 0.2f  * effectLevel);
		stats.getEmpDamageTakenMult().modifyMult(id, 0.2f * effectLevel);
		
		
		ShipAPI ship = null;
		boolean player = false;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
			player = ship == Global.getCombatEngine().getPlayerShip();
		}
		if (player) {
			ShipSystemAPI system = ship.getSystem();
			if (system != null) {
				float percent = 0.75f * effectLevel * 100;
				Global.getCombatEngine().maintainStatusForPlayerShip(
						STATUSKEY1,
						"graphics/icons/hullsys/damper_field.png",
						"Damper Field",
					(int) Math.round(percent) + "% less damage taken", false);
				Global.getCombatEngine().maintainStatusForPlayerShip(
						STATUSKEY2,
						"graphics/icons/hullsys/damper_field.png",
						"Damper Field",
						"Passive Venting Reduced", false);
			}
		}
	}
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getHullDamageTakenMult().unmodify(id);
		stats.getArmorDamageTakenMult().unmodify(id);
		stats.getEmpDamageTakenMult().unmodify(id);
	}
}
