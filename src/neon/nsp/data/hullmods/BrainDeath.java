package neon.nsp.data.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

//V: why it in hullmods??? p.s. looks to be used by ShieldStrengthSpeed hullmod code (another fascinating hullmod name)
//Some of there remind me of Iron Shell code naming scheme, lol, imagine DPO be called "fuckthatafterburnergotoutofcontrol"
public class BrainDeath implements ShipAIPlugin {

	ShipwideAIFlags flags = new ShipwideAIFlags();

	
	public String getDescriptionParam(int index, HullSize hullSize) {
		return null;
	}


	@Override
	public void setDoNotFireDelay(float amount) {

	}

	@Override
	public void forceCircumstanceEvaluation() {

	}

	@Override
	public void advance(float amount) {

	}

	@Override
	public boolean needsRefit() {
		return false;
	}

	@Override
	public ShipwideAIFlags getAIFlags() {
		return flags;
	}

	@Override
	public void cancelCurrentManeuver() {

	}

	@Override
	public ShipAIConfig getConfig() {
		return null;
	}
}
