package neon.nsp.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.subsystems.MagicSubsystem;

import java.awt.*;
import java.util.EnumSet;

public class NSP_DynamicPowerOverrides_Subsystem extends MagicSubsystem {
    public NSP_DynamicPowerOverrides_Subsystem(ShipAPI ship) {
        super(ship);
    }

    String MOD_KEY = "NSP_DynamicPowerOverrides";

    public static String DPO_ACTIVATE = "ui_transponder_on";

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

    private Color color = new Color(255,100,255,255);

    @Override
    public boolean isToggle() {
        return true;
    }

    @Override
    public float getBaseActiveDuration() {
        return 0.5f;
    }

    @Override
    public float getBaseCooldownDuration() {
        return 0.5f;
    }

    @Override
    public boolean shouldActivateAI(float amount) {
        ShipAPI target = ship.getShipTarget();
        if (target != null) {
            float score = 0f;

            if (target.getFluxTracker().isOverloadedOrVenting()) {
                score += 9f;
            } else {
                score += target.getFluxLevel() * 6f;
            }

            float dist = Misc.getDistance(ship.getLocation(), target.getLocation());
            if (dist > aiData.getEngagementRange()) {
                score -= 3f;
            } else {
                score += 3f;
            }

            float avgRange = aiData.getAverageWeaponRange(false);
            score += Math.min(avgRange / dist, 8f);

            return score > 5f;
        }

        return false;
    }

    @Override
    public void advance(float amount, boolean isPaused) {
        super.advance(amount, isPaused);

        if(ship.getOriginalOwner() == -1){ //in refit
            return;
        }

        float active_effect = Math.max(engine_direction, weapons_direction);

        ShipEngineControllerAPI engines = ship.getEngineController();

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

        if(this.isOn()) {
            engine_direction -= amount * 5.0F;
            weapons_direction += amount;

            //Speed buff if subsystem is on
            if (weapons_direction > 0.0F) {
                weapon_effect = Math.max(1.0F, 1.0F + WEAPONS_BONUS_MAX * (active_effect / WEAPONS_BONUS_TIME_MAX));
                ship.getMutableStats().getFluxDissipation().modifyMult(MOD_KEY, weapon_effect);
                ship.getMutableStats().getBallisticRoFMult().modifyMult(MOD_KEY, weapon_effect);
            }

            if (engine_direction <= 0.0F) {
                engine_effect = 1.0F - ENGINE_NERF_MAX * (weapons_direction / WEAPONS_BONUS_TIME_MAX);
                ship.getMutableStats().getMaxSpeed().modifyMult(MOD_KEY, engine_effect);
                ship.getMutableStats().getMaxTurnRate().modifyMult(MOD_KEY, engine_effect);
                ship.getMutableStats().getAcceleration().modifyMult(MOD_KEY, engine_effect);
                ship.getMutableStats().getTurnAcceleration().modifyMult(MOD_KEY, engine_effect);
            }
            
            Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY1,
                    ship.getSystem().getSpecAPI().getIconSpriteName(),
                    "Dynamic Power Overrides - Weapons",
                    "Ballistic weapons RoF: +" + String.format("%.0f", (weapon_effect - 1) * 100) + "%", false);
            Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY2,
                    ship.getSystem().getSpecAPI().getIconSpriteName(),
                    "Dynamic Power Overrides - Engines",
                    "Max Speed: -" + String.format("%.0f", (1 - engine_effect) * 100) + "%", true);
        }
        else {
            engine_direction += amount;
            weapons_direction -= amount * 5.0F;

            //Speed buff if subsystem is off
            if (engine_direction > 0.0F) {
                engine_effect = Math.max(1.0F, 1.0F + ENGINE_BONUS_MAX * (active_effect / ENGINE_BONUS_TIME_MAX));
                ship.getMutableStats().getMaxSpeed().modifyMult(MOD_KEY, engine_effect);
                ship.getMutableStats().getMaxTurnRate().modifyMult(MOD_KEY, engine_effect);
                ship.getMutableStats().getAcceleration().modifyMult(MOD_KEY,  engine_effect);
                ship.getMutableStats().getTurnAcceleration().modifyMult(MOD_KEY, engine_effect);
            }

            if (weapons_direction <= 0.0F) {
                weapon_effect = 1.0F - WEAPONS_NERF_MAX * (engine_direction / ENGINE_BONUS_TIME_MAX);
                ship.getMutableStats().getFluxDissipation().modifyMult(MOD_KEY, weapon_effect);
                ship.getMutableStats().getBallisticRoFMult().modifyMult(MOD_KEY, weapon_effect);
            }

            //For Off case
            Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY1,
                    ship.getSystem().getSpecAPI().getIconSpriteName(),
                    "Dynamic Power Overrides - Weapons",
                    "Ballistic weapons RoF: -" + String.format("%.0f", (1 - weapon_effect) * 100) + "%", true);
            Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY2,
                    ship.getSystem().getSpecAPI().getIconSpriteName(),
                    "Dynamic Power Overrides - Engines",
                    "Max Speed: +" + String.format("%.0f", (engine_effect - 1) * 100) + "%", false);
        }

        Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY3,
                ship.getSystem().getSpecAPI().getIconSpriteName(),
                "Dynamic Power Overrides",
                "active_effect: " + String.format("%.2f", active_effect) + " | EE: " + String.format("%.2f", engine_effect) + " | WE: " + String.format("%.2f", weapon_effect), false);

        engines.extendFlame(MOD_KEY, 0.8F * (engine_effect - 1.1F), 0.0F, 0.25F * (engine_effect - 1.0F));
        if (weapon_effect > 1.0F) {
            Color weapon_hot_color = new Color(255, 165, 132, 170);
            ship.setWeaponGlow(weapon_effect / 2.0F, weapon_hot_color, EnumSet.of(WeaponAPI.WeaponType.BALLISTIC));
        }
        else {
            ship.setWeaponGlow(0, null, EnumSet.of(WeaponAPI.WeaponType.BALLISTIC));
        }

        ship.getEngineController().fadeToOtherColor(this, color, null, 1f, 0.4f);
        //ship.getEngineController().extendFlame(this, 0.25f, 0.25f, 0.25f);
    }

    @Override
    public void onActivate() {
        Global.getSoundPlayer().playSound(DPO_ACTIVATE, 1f, 1f, ship.getLocation(), ship.getVelocity());
    }

    @Override
    public void onFinished() {
    }

    @Override
    public String getDisplayText() {
        return "Dynamic Power Overrides";
    }
}
