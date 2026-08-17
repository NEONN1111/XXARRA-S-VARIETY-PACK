package neon.nsp.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NSP_SafetyOverridesLP extends BaseHullMod {

    private static final Set<String> BLOCKED_HULLMODS = new HashSet<>();

    static {
        // These hullmods will automatically be removed
        // This prevents unexplained hullmod blocking
        BLOCKED_HULLMODS.add("safetyoverrides");

    }

    //Provides bonus to ship max speed and acceleration for short time after engines are not active
    private static Map speed = new HashMap();
    static {
        speed.put(ShipAPI.HullSize.FRIGATE, 50f);
        speed.put(ShipAPI.HullSize.DESTROYER, 30f);
        speed.put(ShipAPI.HullSize.CRUISER, 20f);
        speed.put(ShipAPI.HullSize.CAPITAL_SHIP, 10f);
    }
    private static final float PEAK_MULT = 0.33f;
    private static final float FLUX_DISSIPATION_MULT = 2f;

    private static final float RANGE_THRESHOLD = 450f;
    private static final float RANGE_MULT = 0.25f;

    float boost_accumulator = 0;
    float AfterBurnerBonusMax = 0.75f;

    String MOD_KEY = "NSP_SafetyOverridesLP";

    protected Object STATUSKEY1 = new Object();
    protected Object STATUSKEY2 = new Object();

    Color AfterImageColor = new Color(100, 100, 100, 100);

    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getMaxSpeed().modifyFlat(id, (Float) speed.get(hullSize));
        stats.getAcceleration().modifyFlat(id, (Float) speed.get(hullSize) * 2f);
        stats.getDeceleration().modifyFlat(id, (Float) speed.get(hullSize) * 2f);
        stats.getZeroFluxMinimumFluxLevel().modifyFlat(id, 2f); // set to two, meaning boost is always on

        stats.getFluxDissipation().modifyMult(id, FLUX_DISSIPATION_MULT);

        stats.getPeakCRDuration().modifyMult(id, PEAK_MULT);

        stats.getVentRateMult().modifyMult(id, 0f);

        stats.getWeaponRangeThreshold().modifyFlat(id, RANGE_THRESHOLD);
        stats.getWeaponRangeMultPastThreshold().modifyMult(id, RANGE_MULT);

    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {

        tooltip.addPara("Disabling safety protocols increases the ship's top speed in combat by %s/%s/%s/%s (depending on ship size, with a corresponding increase in acceleration) and allows the zero-flux engine boost to take effect regardless of flux level. The flux dissipation rate, including that of additional vents, is increased by a factor of %s.", 5f, Color.ORANGE, "20", "10", "30", "50", "2");

        tooltip.addPara("Reduces the peak performance time by a factor of %s, prevents the use of active venting, and drastically reduces weapon ranges past %s units.", 5f, Color.ORANGE, "3", "450");

        tooltip.addPara("Can not be installed on civilian ships.", 5f);
        tooltip.addSectionHeading("Afterburners", Alignment.MID, 5f);
        tooltip.addPara("A Speed Bonus is built up passively, which can be expended when accelerating the ship.", 5f);

        //tooltip.addSectionHeading("Incompatibilites", Alignment.MID, 5f);
       // tooltip.addPara("Due to the nature of this armor type, it is incompatible with %s.", 5f, Misc.getNegativeHighlightColor(),
          //      getHullmodName("heavyarmor"));
    //    tooltip.addSectionHeading("Effects On Other Hullmods", Alignment.MID, 5f);
      //  tooltip.addPara("%s hullmods which add armor will have no effect on the hull's armor.", 5f, Color.ORANGE, "Most");
    }

    public void applyEffectsAfterShipCreation(ShipAPI ship, String id, MutableShipStatsAPI stats) {
        for (String tmp : BLOCKED_HULLMODS) {
            if (ship.getVariant().getHullMods().contains(tmp)) {
                ship.getVariant().removeMod(tmp);
                String ERROR = "nsp_incompatible";
                ship.getVariant().addMod(ERROR);
            }
        }

    }



    public String getHullmodName(String id) {
        return Global.getSettings().getHullModSpec(id).getDisplayName();
    }

    private Color color = new Color(255,100,255,255);
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);


        ShipEngineControllerAPI engines = ship.getEngineController();

        if(!engines.isAccelerating() & !engines.isDecelerating() & !engines.isAcceleratingBackwards()) {
            boost_accumulator += amount;
        }

        Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY2,
                ship.getSystem().getSpecAPI().getIconSpriteName(),
                "Speed Bonus",
                "boost_accumulator = " + boost_accumulator,
                true);

        if (boost_accumulator >= 6f) {
            boost_accumulator = 6f;
        }

        if (engines.isAccelerating() || engines.isDecelerating() || engines.isAcceleratingBackwards()) {
            if (boost_accumulator > 0) {
                float effect = Math.max(1 + AfterBurnerBonusMax * (boost_accumulator / 5f), 1f);
                ship.getMutableStats().getMaxSpeed().modifyMult(MOD_KEY, effect);
                ship.getMutableStats().getAcceleration().modifyMult(MOD_KEY, 2*effect);
                boost_accumulator -= amount;

                engines.getExtendGlowFraction().shift(ship, 1f, 0, 0.1f, 3f*(effect-0.9f));
               // engines.getExtendLengthFraction().shift(ship, 1f, 0, 0.1f, 3f*(effect-0.9f));
               // engines.getExtendWidthFraction().shift(ship, 1f, 0, 0.1f, 3f*(effect-0.9f));

                for (int i = 0; i < 5; i++){
                    //ship.addAfterimage(AfterImageColor, (-ship.getVelocity().x/10f)*i, (-ship.getVelocity().y/10f)*i,
                    //        0, 0, 0, 0, 0.1f, 0, false, true, false);
                }

                Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY1,
                        ship.getSystem().getSpecAPI().getIconSpriteName(),
                        "effect strength",
                        "boost_accumulator = " + effect,
                        true);
            }
            ship.getEngineController().fadeToOtherColor(this, color, null, 1f, 0.4f);
            ship.getEngineController().extendFlame(this, 0.25f, 0.25f, 0.25f);
        }
    }


}
