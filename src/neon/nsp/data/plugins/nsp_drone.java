package neon.nsp.data.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import neon.nsp.data.hullmods.BrainDeath;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints.UNBOARDABLE;


public class nsp_drone implements EveryFrameCombatPlugin {
    public static float vectorToSpeed(Vector2f vector){
        return (float) Math.sqrt(Math.pow(vector.getX(),2)+ Math.pow(vector.getY(),2));
    }
    public static final float MAGIC_NUMBER=0.005f;
    private float slotX;
    private float slotY;

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

    }

    @Override
    public void init(CombatEngineAPI engine) {
        droneWeaponPerShip = new HashMap<>();
    }

    private Map<ShipAPI, WeaponAPI> droneWeaponPerShip = new HashMap<>();

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isPaused()) {
            return;
        }

        for (ShipAPI ship : engine.getShips()) {

                if (ship != null && ship.isAlive()) {

                    boolean canEndCombat = true; //default to assuming combat will end
                    for (ShipAPI other : engine.getShips()) {
                        if (other.getOwner() != ship.getOwner()) continue; //only check allies
                        if (other.getHullSpec().getHullId().equals("omm_octocorallia") || other.getHullSpec().getHullId().equals("omm_algaepod")
                                || other.getHullSpec().getHullId().equals("omm_nanodrone") || other.getHullSpec().getHullId().equals("FreitagCorporation_ShieldDrone")
                                || other.getHullSpec().getHullId().equals("omm_pddrone") || other.getHullSpec().getHullId().equals("omm_weaponpod")
                                || other.getHullSpec().getHullId().equals("omm_synergypod") || other.getHullSpec().getHullId().equals("omm_missilepod")
                                || other.getHullSpec().getHullId().equals("omm_medmissilepod") || other.getHullSpec().getHullId().equals("omm_compositepod")
                                || other.getHullSpec().getHullId().equals("omm_smallpod") || other.getHullSpec().getHullId().equals("omm_ballisticpod")
                                || other.getHullSpec().getHullId().equals("omm_largepod"))
                            continue; //if it's the same hull id, ignore it
                        canEndCombat = false; //else, combat shouldn't end
                    }

                    if (canEndCombat) {
                        engine.endCombat(1);
                    } //end combat
                }

            for (ShipAPI moduleGroup : ship.getChildModulesCopy()){
          //  for (ShipAPI WingMembers : fighterWing.getWingMembers()) {
                {
//                if (ship.getHullLevel() >= 10) {
//                    engine.applyDamage(WingMembers, WingMembers.getLocation(), 1000, DamageType.ENERGY, 0, true, false, ship);
//                }
                }}
            if (!ship.isAlive() || !ship.getHullSpec().getHullId().startsWith("nsp_")) {
                continue;
            }
            if (ship.getOriginalOwner() == 1) {
                ship.getFleetMember().getVariant().getHints().add(UNBOARDABLE);
            } else {
                ship.getFleetMember().getVariant().getHints().remove(UNBOARDABLE);
            }

            for (WeaponAPI weap : ship.getAllWeapons()) {
                if (ship.isFighter()) {
                    break;
                }
                if (!droneWeaponPerShip.containsKey(ship)) {
                    droneWeaponPerShip.put(ship, weap);
                }                    //WEAPON interface for drones
                if (weap.getId().equals("nsp_weaponpoddeco") //decorative looks like drone
                        || weap.getId().equals("nsp_pddronedeco")
                        || weap.getId().equals("nsp_smallpoddeco")
                        || weap.getId().equals("nsp_synergypoddeco")
                        || weap.getId().equals("nsp_missilepoddeco")
                        || weap.getId().equals("nsp_compositepoddeco")
                        || weap.getId().equals("nsp_largepoddeco")
                        || weap.getId().equals("nsp_ballisticpoddeco")) {

                    weap.getSprite().setSize(0, 0);

                    if (weap.getUnderSpriteAPI() != null) {
                        weap.getUnderSpriteAPI().setSize(0, 0);
                    }
                    if (weap.getBarrelSpriteAPI() != null) {
                        weap.getBarrelSpriteAPI().setSize(0, 0);
                    }
                }
               // if ((weap.getSlot().getId().equals("droneslot")//slot has same name as on drone !important!
              //          || weap.getSlot().getId().equals("pdslot")
              //          || weap.getSlot().getId().equals("smlhybridslot")
               //         || weap.getSlot().getId().equals("synergyslot")
               //         || weap.getSlot().getId().equals("smlmissileslot")
                //        || weap.getSlot().getId().equals("compositeslot")
                  //      || weap.getSlot().getId().equals("largeslot")
                   //     || weap.getSlot().getId().equals("ballisticslot"))) {


                 //   weap.getSprite().setSize(0, 0);
//                    slotX = weap.getSlot().getLocation().getX();
//                    slotY = weap.getSlot().getLocation().getY();
//                    if (ship.getOriginalOwner() != -1) {
//                        weap.getSlot().getLocation().set(99999, 99999);
//                    } else if (ship.getOriginalOwner() != -1){
//                        weap.getSlot().getLocation().set(slotX, slotY);
//                    }

                  //  weap.setMaxAmmo(0);
                  //  weap.setAmmo(0);
                    //weap.setForceNoFireOneFrame(true);
//                    weap.getSpec().setMaxRange(0f);
//                   weap.disable(true);
                    //ship.removeWeaponFromGroups(weap);                           //removes the weapons swap "interface" from weapon groups

                  //  if (weap.getUnderSpriteAPI() != null) {
                    //    weap.getUnderSpriteAPI().setSize(0, 0);
                    //}
                   // if (weap.getBarrelSpriteAPI() != null) {
                   //     weap.getBarrelSpriteAPI().setSize(0, 0);
                   // }
                   // if (weap.getSlot().getId().equals("pdslot")) {ship.removeWeaponFromGroups(weap); }

                //Weapon Stuff, swapping and such
                for (ShipAPI moduleGroup : ship.getChildModulesCopy()) {
                    if (!moduleGroup.getChildModulesCopy().equals("nsp_pddrone_wing")
                           // || !moduleGroup.getChildModulesCopy().equals("nsp_weaponpod_wing")
                          //  || !moduleGroup.getChildModulesCopy().equals("nsp_weaponpod_wing")
                            || !moduleGroup.getChildModulesCopy().equals("nsp_invictus_lp_right")
                            || !moduleGroup.getChildModulesCopy().equals("nsp_invictus_lp_left")
                         ) {
                       // ShipAPI ship = moduleGroup.getLeader();
                        ship.getMutableStats().getBallisticWeaponRangeBonus().modifyMult(this.getClass().toString(), ship.getMutableStats().getBallisticWeaponRangeBonus().computeEffective(1f));
                        ship.getMutableStats().getEnergyWeaponRangeBonus().modifyMult(this.getClass().toString(), ship.getMutableStats().getEnergyWeaponRangeBonus().computeEffective(1f));
                        ship.getMutableStats().getBeamWeaponRangeBonus().modifyFlat(this.getClass().toString(), ship.getMutableStats().getBeamWeaponRangeBonus().computeEffective(1f));
                        continue;
                    }

                  //  ShipAPI fighter = moduleGroup.getLeader();
                    for (WeaponAPI modulewep : ship.getAllWeapons()) {
                        if (!modulewep.getSlot().getId().equals("pdslot")
                                || !modulewep.getSlot().getId().equals("droneslot")
                                || !modulewep.getSlot().getId().equals("smlhybridslot")
                                || !modulewep.getSlot().getId().equals("synergyslot")
                                || !modulewep.getSlot().getId().equals("smlmissileslot")
                                || !modulewep.getSlot().getId().equals("compositeslot")
                                || !modulewep.getSlot().getId().equals("largeslot")
                                || !modulewep.getSlot().getId().equals("ballisticslot")
                                || !modulewep.getSlot().getId().equals("MODULE_WEP1")
                                    || !modulewep.getSlot().getId().equals("MODULE_WEP2")
                                    || !modulewep.getSlot().getId().equals("MODULE_WEP3")
                                    || !modulewep.getSlot().getId().equals("MODULE_WEP4")){
                            continue;
                        }


                        WeaponAPI weapon = droneWeaponPerShip.get(ship);
                        if (weapon != null) {
                            weapon.getLocation().set(modulewep.getLocation());

                            //         weapon.repair();
                            weapon.getSprite().setColor(new Color(1f, 1f, 1f, 1f));
                            if (weapon.getBarrelSpriteAPI() != null) {
                                weapon.getBarrelSpriteAPI().setColor(new Color(255, 255, 255, 255));
                            }

                            WeaponGroupAPI group = ship.getWeaponGroupFor(weapon);
                            if (group != null) {
                                group.removeWeapon(0);

                                ship.getWeaponGroupFor(modulewep).addWeaponAPI(weapon);
                               // fighter.getVariant().assignUnassignedWeapons();
                            }
                        }

                        //aims the drone weapon
                    }

                    //get enemies in range
                    List<ShipAPI> enemies = AIUtils.getNearbyEnemies(ship, 2000f);

                    //REFIT AI
                //    if (ship.getHullLevel() < 0.9f && enemies.isEmpty() && !moduleGroup.isReturning(fighter)) {
                     //   moduleGroup.orderReturn(fighter);
                 //   }
                }
            }




           // if (!ship.isAlive() || ship.isHulk()) {

           //     return;
         //   }
            //if (ship.getFluxTracker().isOverloadedOrVenting()) {
            //    return;
           // }
          //  FluxTrackerAPI carrierFlux = ship.getFluxTracker();
          //  for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
           //     if (bay.getWing() == null) {
             //       continue;
            //    }
             //   for (ShipAPI fighter : bay.getWing().getWingMembers()) {
               //     if (!bay.getWing().getWingId().equals("FreitagCorporation_ShieldDrone_wing") && !bay.getWing().getWingId().equals("nsp_nanodrone_wing")) {
               //         transferFlux(fighter, carrierFlux);
                //    }

              //  }
              //  for (FighterWingAPI.ReturningFighter returning : bay.getWing().getReturning()) {
               //     transferFlux(returning.fighter, carrierFlux);
               // }
            }
        }


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

    @Override
    public void renderInWorldCoords(ViewportAPI viewport
    ) {

    }

    @Override
    public void renderInUICoords(ViewportAPI viewport
    ) {

    }

}
