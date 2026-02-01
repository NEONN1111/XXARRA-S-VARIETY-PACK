package neon.nsp.data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class TLL_Refit extends BaseHullMod {

	public static float RANGE_MULT = 0.8f;
	public static float FLUX_MULT = 1.1f;
	public static float ENERGY_WEAPON_FLUX_DECREASE = 0.1f;
	public static float ENERGY_RANGE_MULT = 1.1f;
	public static float COST_REDUCTION1 = 5f;
	public static float COST_REDUCTION2 = 7f;
	public static float HULL_DECREASE = 10f;
	public static float ARMOR_DECREASE = 10f;
	private static final float PEAK_MULT = 240;
	public static final float DEGRADE_INCREASE_PERCENT = 25f;
	
	public static boolean AlLOW_CONVERTED_HANGAR = true;

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		tooltip.addPara("This vessel has been refit and overhauled by the %s.", 5f, Color.ORANGE, "Tellasian League");

		tooltip.addPara("Energy weapon flux usage is decreased by %s, and range increased by %s.", 5f, Color.ORANGE, "10%", "10%");
		
		tooltip.addPara("The Ordinance Point cost of Medium and Large energy weapons are reduced by %s and %s, respectively.", 5f, Color.ORANGE, "5", "7");

		tooltip.addPara("As a result of the modifications performed, armor and hull integrity are reduced by %s, and the vessel's base speed is increased by %s.", 5f, Color.ORANGE, "5%", "10");

		tooltip.addPara("Peak performance time is reduced, with the least reduction found in frigates, and the highest in capital class ships. Performance degrades %s faster when peak performance runs out.", 5f, Color.ORANGE, "25%");



	}
	private static Map mag = new HashMap();
	static {
		mag.put(HullSize.FRIGATE, 30f);
		mag.put(HullSize.DESTROYER, 25f);
		mag.put(HullSize.CRUISER, 20f);
		mag.put(HullSize.CAPITAL_SHIP, 15f);
	}
	private static Map peakmult = new HashMap();
	static {
		peakmult.put(HullSize.FRIGATE, 0.8f);
		peakmult.put(HullSize.DESTROYER, 0.7f);
		peakmult.put(HullSize.CRUISER, 0.6f);
		peakmult.put(HullSize.CAPITAL_SHIP, 0.5f);
	}
	
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getEnergyWeaponRangeBonus().modifyMult(id, ENERGY_RANGE_MULT);
		stats.getEnergyWeaponFluxCostMod().modifyPercent(id, ENERGY_WEAPON_FLUX_DECREASE);
		stats.getDynamic().getMod(Stats.MEDIUM_ENERGY_MOD).modifyFlat(id, -COST_REDUCTION1);
		stats.getDynamic().getMod(Stats.LARGE_ENERGY_MOD).modifyFlat(id, -COST_REDUCTION2);
		stats.getHullBonus().modifyPercent(id, -HULL_DECREASE);
		stats.getArmorBonus().modifyPercent(id, -ARMOR_DECREASE);
		stats.getPeakCRDuration().modifyFlat(id, (Float) peakmult.get(hullSize));
		stats.getMaxSpeed().modifyFlat(id, (Float) mag.get(hullSize));
		stats.getCRLossPerSecondPercent().modifyPercent(id, DEGRADE_INCREASE_PERCENT);
		
		stats.getFluxDissipation().modifyMult(id, FLUX_MULT);
		stats.getFluxCapacity().modifyMult(id, FLUX_MULT);
		stats.getSystemFluxCostBonus().modifyMult(id, FLUX_MULT);

	}

	public boolean isApplicableToShip(ShipAPI ship) {
		return ship != null && (ship.getHullSpec().getNoCRLossTime() < 10000 || ship.getHullSpec().getCRLossPerSecond(ship.getMutableStats()) > 0);
	}
@Override
	public boolean affectsOPCosts() {
		return true;
	}
	
}









