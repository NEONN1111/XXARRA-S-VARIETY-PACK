package neon.nsp.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.EnumSet;

public class NSP_DynamicPowerOverrides extends BaseHullMod {
    String MOD_KEY = "NSP_DynamicPowerOverrides";

    private static final float RANGE_THRESHOLD = 450.0F;
    private static final float RANGE_MULT = 0.25F;

    float engine_direction = 0.0F;
    float weapons_direction = 0.0F;

    float ENGINE_BONUS_TIME_MAX = 10.0F;
    float ENGINE_BONUS_MAX = 1.0F;
    float ENGINE_NERF_MAX = 0.5F;

    float WEAPONS_BONUS_TIME_MAX = 10.0F;
    float WEAPONS_BONUS_MAX = 1.0F;
    float WEAPONS_NERF_MAX = 0.5F;

    protected Object STATUSKEY1 = new Object();
    protected Object STATUSKEY2 = new Object();
    protected Object STATUSKEY3 = new Object();

    float engine_effect = 0.0F;
    float weapon_effect = 0.0F;

    public NSP_DynamicPowerOverrides() {
    }

    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
       // stats.getZeroFluxSpeedBoost().modifyMult(id, 0.0F);
        stats.getVentRateMult().modifyMult(id, 0.0F);

        stats.getWeaponRangeThreshold().modifyFlat(id, RANGE_THRESHOLD);
        stats.getWeaponRangeMultPastThreshold().modifyMult(id, RANGE_MULT);

        stats.getZeroFluxMinimumFluxLevel().modifyFlat(id, 2f); // set to two, meaning boost is always on
        stats.getVentRateMult().modifyMult(id, 0f);
    }

    private Color color = new Color(255,100,255,255);
    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);
        ShipEngineControllerAPI engines = ship.getEngineController();
        if (!engines.isAccelerating() & !engines.isDecelerating() & !engines.isAcceleratingBackwards()) {
            engine_direction -= amount * 5.0F;
            weapons_direction += amount;
        } else {
            engine_direction += amount;
            weapons_direction -= amount * 5.0F;
        }

        if (engine_direction > ENGINE_BONUS_TIME_MAX) {
            engine_direction = ENGINE_BONUS_TIME_MAX;
        } else if (engine_direction < 0.0F) {
            engine_direction = 0.0F;
        }

        if (weapons_direction > WEAPONS_BONUS_TIME_MAX) {
            weapons_direction = WEAPONS_BONUS_TIME_MAX;
        } else if (weapons_direction < 0.0F) {
            weapons_direction = 0.0F;
        }

        float active_effect = Math.max(engine_direction, weapons_direction);

        Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY1,
                ship.getSystem().getSpecAPI().getIconSpriteName(),
                "Dynamic Power Overrides",
                "engine_direction: " + String.format("%.2f", engine_direction), false);
        Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY2,
                ship.getSystem().getSpecAPI().getIconSpriteName(),
                "Dynamic Power Overrides",
                "weapons_direction: " + String.format("%.2f", weapons_direction), false);

        Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY3,
                ship.getSystem().getSpecAPI().getIconSpriteName(),
                "Dynamic Power Overrides",
                "active_effect: " + String.format("%.2f", active_effect) + " | EE: " + String.format("%.2f", engine_effect) + " | WE: " + String.format("%.2f", weapon_effect), false);

        if (engine_direction > 0.0F) {
            engine_effect = Math.max(1.0F, 1.0F + ENGINE_BONUS_MAX * (active_effect / ENGINE_BONUS_TIME_MAX));
            ship.getMutableStats().getMaxSpeed().modifyMult(MOD_KEY, engine_effect);
            ship.getMutableStats().getMaxTurnRate().modifyMult(MOD_KEY, engine_effect);
            ship.getMutableStats().getAcceleration().modifyMult(MOD_KEY, 2.0F * engine_effect);
            ship.getMutableStats().getTurnAcceleration().modifyMult(MOD_KEY, 2.0F * engine_effect);
        }

        if (weapons_direction == 0.0F) {
            weapon_effect = 1.0F - WEAPONS_NERF_MAX * (engine_direction / ENGINE_BONUS_TIME_MAX);
            ship.getMutableStats().getFluxDissipation().modifyMult(MOD_KEY, weapon_effect);
            ship.getMutableStats().getBallisticRoFMult().modifyMult(MOD_KEY, weapon_effect);
        }

        if (weapons_direction > 0.0F) {
            weapon_effect = Math.max(1.0F, 1.0F + WEAPONS_BONUS_MAX * (active_effect / WEAPONS_BONUS_TIME_MAX));
            ship.getMutableStats().getFluxDissipation().modifyMult(MOD_KEY, weapon_effect);
            ship.getMutableStats().getBallisticRoFMult().modifyMult(MOD_KEY, weapon_effect);
        }

        if (engine_direction == 0.0F) {
            engine_effect = 1.0F - ENGINE_NERF_MAX * (weapons_direction / WEAPONS_BONUS_TIME_MAX);
            ship.getMutableStats().getMaxSpeed().modifyMult(MOD_KEY, engine_effect);
            ship.getMutableStats().getMaxTurnRate().modifyMult(MOD_KEY, engine_effect);
            ship.getMutableStats().getAcceleration().modifyMult(MOD_KEY, engine_effect);
            ship.getMutableStats().getTurnAcceleration().modifyMult(MOD_KEY, engine_effect);
        }

        engines.extendFlame(MOD_KEY, 0.8F * (engine_effect - 1.0F), 0.0F, 0.25F * (engine_effect - 1.0F));
        if (weapon_effect > 1.0F) {
            Color weapon_hot_color = new Color(255, 165, 132, 170);
            ship.setWeaponGlow(weapon_effect / 2.0F, weapon_hot_color, EnumSet.of(WeaponType.BALLISTIC));
        }
        ship.getEngineController().fadeToOtherColor(this, color, null, 1f, 0.4f);
        ship.getEngineController().extendFlame(this, 0.25f, 0.25f, 0.25f);

    }

    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        Color hColor = Misc.getHighlightColor();
        Color pColor = Misc.getPositiveHighlightColor();
        Color nColor = Misc.getNegativeHighlightColor();
        Color dColor = Misc.getDarkHighlightColor();
        new Color(110, 110, 110, 255);
        float pad = 10.0F;
        float pad2 = 0.0F;
        float height = 50.0F;
        float padList = 6.0F;
        float padSig = 1.0F;

        tooltip.addSectionHeading("Effects", Alignment.MID, 10.0F);

        tooltip.addPara("Prevents the use of active venting, drastically reduces weapon ranges past %s units and disables the zero-flux engine boost.",
                padList, Color.ORANGE, "" + RANGE_THRESHOLD);

        tooltip.addPara("In combat ship actively diverts reactor power to either weapons or engines, depending on what currently used.", padList);

        tooltip.addPara("In combat, when ship %s, reactor power diverted to weapons providing up to %s firerate and ship's flux dissipation, but reduces maneuverability and max speed by as much as %s.",
                padList, hColor, "stops using engines", (int)(100.0F + WEAPONS_BONUS_MAX * 100.0F) + "%", (int)(ENGINE_NERF_MAX * 100.0F) + "%");

        tooltip.addPara("In combat, when ship %s, reactor power diverted to engines providing up to %s maneuverability and max speed, but reduces weapons firerate and ship's flux dissipation by as much as %s.",
                padList, hColor, "actively using its engines", (int)(100.0F + ENGINE_BONUS_MAX * 100.0F) + "%", (int)(WEAPONS_NERF_MAX * 100.0F) + "%");

        tooltip.addPara("When ship %s, short power surge boost currently active focus.",
                padList, hColor, "switches from one focus to another");
    }
}
