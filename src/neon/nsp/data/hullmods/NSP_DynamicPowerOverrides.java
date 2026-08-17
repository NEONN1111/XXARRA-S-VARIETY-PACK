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
    protected Object STATUSKEY_ENGINE1 = new Object();
    protected Object STATUSKEY_ENGINE2 = new Object();
    protected Object STATUSKEY_WEAPONS1 = new Object();
    protected Object STATUSKEY_WEAPONS2 = new Object();
    float engine_effect = 0.0F;
    float weapon_effect = 0.0F;

    public NSP_DynamicPowerOverrides() {
    }

    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getZeroFluxSpeedBoost().modifyMult(id, 0.0F);
        stats.getVentRateMult().modifyMult(id, 0.0F);
        stats.getWeaponRangeThreshold().modifyFlat(id, 450.0F);
        stats.getWeaponRangeMultPastThreshold().modifyMult(id, 0.25F);
    }

    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);
        ShipEngineControllerAPI engines = ship.getEngineController();
        if (!engines.isAccelerating() & !engines.isDecelerating() & !engines.isAcceleratingBackwards()) {
            this.engine_direction -= amount * 5.0F;
            this.weapons_direction += amount;
        } else {
            this.engine_direction += amount;
            this.weapons_direction -= amount * 5.0F;
        }

        if (this.engine_direction > this.ENGINE_BONUS_TIME_MAX) {
            this.engine_direction = this.ENGINE_BONUS_TIME_MAX;
        } else if (this.engine_direction < 0.0F) {
            this.engine_direction = 0.0F;
        }

        if (this.weapons_direction > this.WEAPONS_BONUS_TIME_MAX) {
            this.weapons_direction = this.WEAPONS_BONUS_TIME_MAX;
        } else if (this.weapons_direction < 0.0F) {
            this.weapons_direction = 0.0F;
        }

        Global.getCombatEngine().maintainStatusForPlayerShip(this.STATUSKEY1, ship.getSystem().getSpecAPI().getIconSpriteName(), "Dynamic Power Overrides", "engine_direction: " + String.format("%.2f", this.engine_direction), false);
        Global.getCombatEngine().maintainStatusForPlayerShip(this.STATUSKEY2, ship.getSystem().getSpecAPI().getIconSpriteName(), "Dynamic Power Overrides", "weapons_direction: " + String.format("%.2f", this.weapons_direction), false);
        float active_effect = Math.max(this.engine_direction, this.weapons_direction);
        CombatEngineAPI var10000 = Global.getCombatEngine();
        Object var10001 = this.STATUSKEY_WEAPONS2;
        String var10002 = ship.getSystem().getSpecAPI().getIconSpriteName();
        String var10004 = String.format("%.2f", active_effect);
        var10000.maintainStatusForPlayerShip(var10001, var10002, "Dynamic Power Overrides", "active_effect: " + var10004 + " | EE: " + String.format("%.2f", this.engine_effect) + " | WE: " + String.format("%.2f", this.weapon_effect), false);
        if (this.engine_direction > 0.0F) {
            this.engine_effect = Math.max(1.0F, 1.0F + this.ENGINE_BONUS_MAX * (active_effect / this.ENGINE_BONUS_TIME_MAX));
            ship.getMutableStats().getMaxSpeed().modifyMult(this.MOD_KEY, this.engine_effect);
            ship.getMutableStats().getMaxTurnRate().modifyMult(this.MOD_KEY, this.engine_effect);
            ship.getMutableStats().getAcceleration().modifyMult(this.MOD_KEY, 2.0F * this.engine_effect);
            ship.getMutableStats().getTurnAcceleration().modifyMult(this.MOD_KEY, 2.0F * this.engine_effect);
        }

        if (this.weapons_direction == 0.0F) {
            this.weapon_effect = 1.0F - this.WEAPONS_NERF_MAX * (this.engine_direction / this.ENGINE_BONUS_TIME_MAX);
            ship.getMutableStats().getFluxDissipation().modifyMult(this.MOD_KEY, this.weapon_effect);
            ship.getMutableStats().getBallisticRoFMult().modifyMult(this.MOD_KEY, this.weapon_effect);
        }

        if (this.weapons_direction > 0.0F) {
            this.weapon_effect = Math.max(1.0F, 1.0F + this.WEAPONS_BONUS_MAX * (active_effect / this.WEAPONS_BONUS_TIME_MAX));
            ship.getMutableStats().getFluxDissipation().modifyMult(this.MOD_KEY, this.weapon_effect);
            ship.getMutableStats().getBallisticRoFMult().modifyMult(this.MOD_KEY, this.weapon_effect);
        }

        if (this.engine_direction == 0.0F) {
            this.engine_effect = 1.0F - this.ENGINE_NERF_MAX * (this.weapons_direction / this.WEAPONS_BONUS_TIME_MAX);
            ship.getMutableStats().getMaxSpeed().modifyMult(this.MOD_KEY, this.engine_effect);
            ship.getMutableStats().getMaxTurnRate().modifyMult(this.MOD_KEY, this.engine_effect);
            ship.getMutableStats().getAcceleration().modifyMult(this.MOD_KEY, this.engine_effect);
            ship.getMutableStats().getTurnAcceleration().modifyMult(this.MOD_KEY, this.engine_effect);
        }

        engines.extendFlame(this, 0.8F * (this.engine_effect - 1.0F), 0.0F, 0.25F * (this.engine_effect - 1.0F));
        if (this.weapon_effect > 1.0F) {
            Color weapon_hot_color = new Color(255, 165, 132, 170);
            ship.setWeaponGlow(this.weapon_effect / 2.0F, weapon_hot_color, EnumSet.of(WeaponType.BALLISTIC));
        }

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
        tooltip.addPara("Prevents the use of active venting, drastically reduces weapon ranges past %s units and disables the zero-flux engine boost.", padList, Color.ORANGE, new String[]{"450"});
        tooltip.addPara("In combat ship actively diverts reactor power to either weapons or engines, depending on what currently used.", padList);
        tooltip.addPara("In combat, when ship %s, reactor power diverted to weapons providing up to %s firerate and ship's flux dissipation, but reduces maneuverability and max speed by as much as %s.", padList, hColor, new String[]{"stops using engines", (int)(100.0F + this.WEAPONS_BONUS_MAX * 100.0F) + "%", (int)(this.ENGINE_NERF_MAX * 100.0F) + "%"});
        tooltip.addPara("In combat, when ship %s, reactor power diverted to engines providing up to %s maneuverability and max speed, but reduces weapons firerate and ship's flux dissipation by as much as %s.", padList, hColor, new String[]{"actively using its engines", (int)(100.0F + this.ENGINE_BONUS_MAX * 100.0F) + "%", (int)(this.WEAPONS_NERF_MAX * 100.0F) + "%"});
        tooltip.addPara("When ship %s, short power surge boost currently active focus.", padList, hColor, new String[]{"switches from one focus to another"});
    }
}
