package neon.nsp.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

public class ShieldStrengthSpeed extends BaseHullMod {

	public String getUnapplicableReason(ShipAPI ship) {
		return null;
		//return "Incompatible with Dedicated Targeting Core";
	}
	public static float vectorToSpeed(Vector2f vector){
		return (float) Math.sqrt(Math.pow(vector.getX(),2)+ Math.pow(vector.getY(),2));
	}
	public static final float MAGIC_NUMBER=0.005f;

	@Override
	public void    advanceInCombat(ShipAPI ship, float amount){
		//bonus based on average spread of the weapons
		float magnitude = 1 + vectorToSpeed(ship.getVelocity())*MAGIC_NUMBER;
		ship.getMutableStats().getShieldDamageTakenMult().modifyMult("nsp_shieldspeed",1/magnitude);
		//draw UI bonus
		if(ship.equals(Global.getCombatEngine().getPlayerShip())) {
			Global.getCombatEngine().maintainStatusForPlayerShip(this.getClass(), Global.getSettings().getSpriteName("ui", "icon_op"), "Shield Efficiency", Misc.getRoundedValue(1/magnitude), false);
		}
		if(ship.getStationSlot()!=null&&ship.getParentStation()!=null){
			ShipAPI ParentShip=ship.getParentStation();
			ShieldAPI ParentShield= ParentShip.getShield();
			if (ParentShield != null) {
				ship.getShield().forceFacing(ParentShield.getFacing());
				if (ParentShield.isOn()) {
					ship.getShield().toggleOn();
				} else if (ParentShield.isOff()) {
					ship.getShield().toggleOff();
				}
			}
				if (!(ship.getAI() instanceof BrainDeath)) {
					ship.setShipAI(new BrainDeath());
				}
				shareflux(ParentShip, ship);

		}
	}

	private void shareflux(ShipAPI ship, ShipAPI othership){
		if (!ship.isAlive() || ship.isHulk()) return;
        FluxTrackerAPI shipflux = ship.getFluxTracker();
		FluxTrackerAPI othershipflux = othership.getFluxTracker();
		shipflux.increaseFlux(othershipflux.getCurrFlux() - othershipflux.getHardFlux(), false);
		shipflux.increaseFlux(othershipflux.getHardFlux(), true);
		othershipflux.setCurrFlux(0);
		othershipflux.setHardFlux(0);
		if (shipflux.isOverloaded() && !othershipflux.isOverloaded()) {
			othershipflux.beginOverloadWithTotalBaseDuration(shipflux.getOverloadTimeRemaining());
		}
	}



	public String getDescriptionParam(int index, HullSize hullSize) {

		if (index == 0) return Misc.getRoundedValue(MAGIC_NUMBER * 100f) + "%";
		return null;
	}


}
