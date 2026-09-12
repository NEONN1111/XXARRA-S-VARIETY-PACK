package neon.nsp.data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.combat.AIUtils;
import org.magiclib.subsystems.MagicSubsystem;

import java.awt.*;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

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
    protected Object STATUSKEY4 = new Object();

    protected Object STATUSKEY5 = new Object();
    protected Object STATUSKEY6 = new Object();
    protected Object STATUSKEY7 = new Object();
    protected Object STATUSKEY8 = new Object();

    String DPO_EngineIcon = Global.getSettings().getSpriteName("subsystems", "DPO_engines");
    String DPO_WeaponsIcon = Global.getSettings().getSpriteName("subsystems", "DPO_weapons");

    float engine_effect = 0.0F;
    float weapon_effect = 0.0F;

    //AI stuff
    private HashMap<ShipAPI.HullSize, Float> mults = new HashMap();
    private int desiredMode;
    float AI_UPDATE_INTERVAL = 0.2f;

    //0 - Drive mode
    //1 - Weapons mode
    int ACTIVE_MODE = 0;


    private Color color = new Color(255,100,255,255);

    @Override
    public float getBaseActiveDuration() {
        return 0.5f;
    }

    @Override
    public float getBaseCooldownDuration() {
        return 0.5f;
    }


    public void init() {
        super.init();

        desiredMode = 1;
        mults.put(ShipAPI.HullSize.CAPITAL_SHIP, 1.5F);
        mults.put(ShipAPI.HullSize.CRUISER, 1.25F);
        mults.put(ShipAPI.HullSize.DESTROYER, 1.0F);
        mults.put(ShipAPI.HullSize.FRIGATE, 0.75F);
        mults.put(ShipAPI.HullSize.FIGHTER, 0.0F);
    }

    @Override
    public boolean shouldActivateAI(float amount) {
        if (ship != null && ship.isAlive() && !Global.getCombatEngine().isPaused()) {

            float analysisRange = getLongestRange(ship);
            float threat = getThreatWeight(analysisRange, ship);
            if (threat < 10.0F && ship.getFluxLevel() < 0.05F) {
                desiredMode = 0; //2 is subsystem off
            } else if (!(threat > 75.0F) && !(ship.getFluxLevel() > 0.5F)) {
                desiredMode = 1; //1 is subsystem off
            } else {
                desiredMode = 0; //2 is subsystem off
            }

            //To prevent off/on looping
            if(ACTIVE_MODE == 0){
                //Swap mode
                if(desiredMode == 1){
                    return true;
                }

                //Don't touch anything in other case
                return false;
            }
            else if(ACTIVE_MODE == 1){
                //Swap mode
                if(desiredMode == 0){
                    return true;
                }

                //Don't touch anything in other case
                return false;
            }
        }
        return false;
    }

    @Override
    public void advance(float amount, boolean isPaused) {

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

        //0 - Drive mode
        if (ACTIVE_MODE == 0) {
            engine_direction += amount;
            weapons_direction -= amount * 5.0F;

            //Speed buff if subsystem is off
            if (engine_direction > 0.0F) {
                engine_effect = Math.max(1.0F, 1.0F + ENGINE_BONUS_MAX * (active_effect / ENGINE_BONUS_TIME_MAX));
                ship.getMutableStats().getMaxSpeed().modifyMult(MOD_KEY, engine_effect);
                ship.getMutableStats().getMaxTurnRate().modifyMult(MOD_KEY, engine_effect);
                ship.getMutableStats().getAcceleration().modifyMult(MOD_KEY, engine_effect);
                ship.getMutableStats().getTurnAcceleration().modifyMult(MOD_KEY, engine_effect);
            }

            if (engine_direction > 7f) {
                ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
                Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY4,
                        DPO_EngineIcon,
                        "Dynamic Power Overrides",
                        "Main shipsystem disabled.", true);
            }

            if (weapons_direction <= 0.0F) {
                weapon_effect = 1.0F - WEAPONS_NERF_MAX * (engine_direction / ENGINE_BONUS_TIME_MAX);
                ship.getMutableStats().getFluxDissipation().modifyMult(MOD_KEY, weapon_effect);
                ship.getMutableStats().getBallisticRoFMult().modifyMult(MOD_KEY, weapon_effect);
                ship.getMutableStats().getEnergyRoFMult().modifyMult(MOD_KEY, weapon_effect);
            }

            //For Off case
            if (weapons_direction <= 0.0F) {
                Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY1,
                        DPO_EngineIcon,
                        "Dynamic Power Overrides - Weapons",
                        "Weapon RoF: -" + String.format("%.0f", (1 - weapon_effect) * 100) + "%", true);
            } else {
                Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY1,
                        DPO_EngineIcon,
                        "Dynamic Power Overrides - Weapons",
                        "Weapon RoF: -0%", true);
            }

            Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY2,
                    DPO_EngineIcon,
                    "Dynamic Power Overrides - Engines",
                    "Max Speed: +" + String.format("%.0f", (engine_effect - 1) * 100) + "%", false);

            Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY3,
                    DPO_EngineIcon,
                    "Dynamic Power Overrides",
                    "active_effect: " + String.format("%.2f", active_effect) + " | EE: " + String.format("%.2f", engine_effect) + " | WE: " + String.format("%.2f", weapon_effect), false);
        }
        //1 - Weapons mode
        else if (ACTIVE_MODE == 1) {
            engine_direction -= amount * 5.0F;
            weapons_direction += amount;

            if (weapons_direction > 0.0F) {
                weapon_effect = Math.max(1.0F, 1.0F + WEAPONS_BONUS_MAX * (active_effect / WEAPONS_BONUS_TIME_MAX));
                ship.getMutableStats().getFluxDissipation().modifyMult(MOD_KEY, weapon_effect);
                ship.getMutableStats().getBallisticRoFMult().modifyMult(MOD_KEY, weapon_effect);
                ship.getMutableStats().getEnergyRoFMult().modifyMult(MOD_KEY, weapon_effect);
            }

            if (weapons_direction > 7f) {
                ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
                Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY8,
                        DPO_WeaponsIcon,
                        "Dynamic Power Overrides",
                        "Main shipsystem disabled.", true);
            }

            if (engine_direction <= 0.0F) {
                engine_effect = 1.0F - ENGINE_NERF_MAX * (weapons_direction / WEAPONS_BONUS_TIME_MAX);
                ship.getMutableStats().getMaxSpeed().modifyMult(MOD_KEY, engine_effect);
                ship.getMutableStats().getMaxTurnRate().modifyMult(MOD_KEY, engine_effect);
                ship.getMutableStats().getAcceleration().modifyMult(MOD_KEY, engine_effect);
                ship.getMutableStats().getTurnAcceleration().modifyMult(MOD_KEY, engine_effect);
            }

            Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY5,
                    DPO_WeaponsIcon,
                    "Dynamic Power Overrides - Weapons",
                    "Weapon RoF: +" + String.format("%.0f", (weapon_effect - 1) * 100) + "%", false);
            if (engine_direction <= 0.0F) {
                Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY6,
                        DPO_WeaponsIcon,
                        "Dynamic Power Overrides - Engines",
                        "Max Speed: -" + String.format("%.0f", (1 - engine_effect) * 100) + "%", true);
            } else {
                Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY6,
                        DPO_WeaponsIcon,
                        "Dynamic Power Overrides - Engines",
                        "Max Speed: -0%", true);
            }

            Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY7,
                    DPO_WeaponsIcon,
                    "Dynamic Power Overrides",
                    "active_effect: " + String.format("%.2f", active_effect) + " | EE: " + String.format("%.2f", engine_effect) + " | WE: " + String.format("%.2f", weapon_effect), false);

        }

        engines.extendFlame(MOD_KEY, 0.8F * (engine_effect - 1.1F), 0.0F, 0.25F * (engine_effect - 1.0F));
        if (weapon_effect > 1.0F) {
            Color weapon_hot_color = new Color(255, 165, 132, 170);
            ship.setWeaponGlow(weapon_effect / 2.0F, weapon_hot_color, EnumSet.of(WeaponAPI.WeaponType.BALLISTIC));
        } else {
            ship.setWeaponGlow(0, null, EnumSet.of(WeaponAPI.WeaponType.BALLISTIC));
        }

        ship.getEngineController().fadeToOtherColor(this, color, null, 1f, 0.4f);
        //ship.getEngineController().extendFlame(this, 0.25f, 0.25f, 0.25f);
    }

    @Override
    public void onActivate() {
        Global.getSoundPlayer().playSound(DPO_ACTIVATE, 1f, 1f, ship.getLocation(), ship.getVelocity());

        if(ACTIVE_MODE == 0){
            ACTIVE_MODE = 1;
        }
        else if(ACTIVE_MODE == 1){
            ACTIVE_MODE = 0;
        }
    }

    @Override
    public void onFinished() {
    }

    @Override
    public String getDisplayText() {
        if(ACTIVE_MODE == 0){
            return "Dynamic Power Overrides - Engine Overdrive";
        }
        else if(ACTIVE_MODE == 1){
            return "Dynamic Power Overrides - Weapons Overdrive";
        }
        return "Dynamic Power Overrides";
    }

    private float getThreatWeight(float range, ShipAPI ship) {
        float threatWeightTotal = 0.0F;

        for(ShipAPI enemy : AIUtils.getNearbyEnemies(ship, range)) {
            if (enemy != null && enemy.getFleetMember() != null) {
                float weight = enemy.getFleetMember().getDeploymentCostSupplies();
                weight *= (Float)this.mults.get(enemy.getHullSize());
                if (enemy.getFluxTracker().isOverloadedOrVenting()) {
                    weight *= 1.5F;
                }

                if (enemy.getHullLevel() < 0.4F) {
                    weight *= 1.5F;
                }

                if (enemy.getFluxLevel() > 0.5F) {
                    weight *= 1.25F;
                }

                threatWeightTotal += weight;
            }
        }

        return threatWeightTotal;
    }

    private float getLongestRange(ShipAPI ship) {
        float longestRange = 0.0F;
        List<WeaponAPI> weapons = ship.getAllWeapons();
        WeaponAPI.WeaponSize largestWeaponSize = getWeaponSize(weapons);

        for(WeaponAPI weapon : weapons) {
            if (weapon.getType() != WeaponAPI.WeaponType.MISSILE && weapon.getSize() == largestWeaponSize && !weapon.hasAIHint(WeaponAPI.AIHints.PD)) {
                float range = weapon.getRange();
                if (range > longestRange) {
                    longestRange = range;
                }
            }
        }

        if (longestRange < 100.0F) {
            longestRange = 600.0F * ship.getMutableStats().getEnergyWeaponRangeBonus().computeEffective(1.0F);
        }
        return longestRange;
    }

    private static WeaponAPI.WeaponSize getWeaponSize(List<WeaponAPI> weapons) {
        WeaponAPI.WeaponSize largestWeaponSize = WeaponAPI.WeaponSize.SMALL;

        for(WeaponAPI weapon : weapons) {
            WeaponAPI.WeaponSize size = weapon.getSize();
            if (largestWeaponSize == WeaponAPI.WeaponSize.SMALL && weapon.getSize() != largestWeaponSize) {
                largestWeaponSize = weapon.getSize();
            }
            if (largestWeaponSize == WeaponAPI.WeaponSize.MEDIUM && weapon.getSize() == WeaponAPI.WeaponSize.LARGE) {
                largestWeaponSize = WeaponAPI.WeaponSize.LARGE;
            }
        }
        return largestWeaponSize;
    }
}
