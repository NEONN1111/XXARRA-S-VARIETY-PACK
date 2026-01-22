package neon.nsp.data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import org.apache.log4j.Logger;

public class NSP_SymbiantArms implements EveryFrameWeaponEffectPlugin {

    private static final Logger log = Logger.getLogger(NSP_SymbiantArms.class);

    private float delay = 0f;
    private boolean runOnce = false;
    private ShipAPI nsp_symbiant_l, nsp_symbiant_r;
    private WeaponAPI nsp_symbiant_left_arm, nsp_symbiant_right_arm;

    private boolean wpn_found = false, prt_found = false;

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || !weapon.getShip().isAlive()) return;
        ShipAPI ship = weapon.getShip();

        if (!runOnce) {
            runOnce = true;
            log.info("NSP_SymbiantArms: Script initialized for ship: " + ship.getName());
            if (ship.getOriginalOwner() == -1 || ship.getOwner() == -1) {
                return;
            }
            if (ship.getOriginalOwner() != 0) {
                delay = 2.1f;
            }
        }

        if (delay >= 0) {
            delay -= amount;
            if (delay <= 0) {
                log.info("NSP_SymbiantArms: Delay finished, looking for modules");
                if (!ship.getChildModulesCopy().isEmpty()) {
                    for (ShipAPI module : ship.getChildModulesCopy()) {
                        String slotId = module.getStationSlot().getId();
                        log.info("NSP_SymbiantArms: Found module with slot ID: " + slotId);
                        switch (slotId) {
                            case "SMB_LEFT_ARM":
                                nsp_symbiant_l = module;
                                log.info("NSP_SymbiantArms: Assigned to nsp_symbiant_l");
                                break;
                            case "SMB_RIGHT_ARM":
                                nsp_symbiant_r = module;
                                log.info("NSP_SymbiantArms: Assigned to nsp_symbiant_r");
                                break;
                        }
                    }
                } else {
                    log.info("NSP_SymbiantArms: No child modules found!");
                }
            }
            return;
        }

        if (nsp_symbiant_l != null && nsp_symbiant_r != null) {
            prt_found = true;
            log.info("NSP_SymbiantArms: Both modules found");
        }

        if(!wpn_found){
            log.info("NSP_SymbiantArms: Looking for weapons");
            for (WeaponAPI w : ship.getAllWeapons()) {
                String slotId = w.getSlot().getId();
                String cleanSlotId = slotId.replaceAll("[^\\x20-\\x7E]", "");
                log.info("NSP_SymbiantArms: Weapon slot ID: " + slotId + " (clean: " + cleanSlotId + ")");

                switch (cleanSlotId) {
                    case "nsp_symbiant_left_arm":
                        nsp_symbiant_left_arm = w;
                        log.info("NSP_SymbiantArms: Assigned to nsp_symbiant_left_arm");
                        break;
                    case "nsp_symbiant_right_arm":
                        nsp_symbiant_right_arm = w;
                        log.info("NSP_SymbiantArms: Assigned to nsp_symbiant_right_arm");
                        break;
                }
            }

            if (nsp_symbiant_left_arm != null && nsp_symbiant_right_arm != null) {
                wpn_found = true;
                log.info("NSP_SymbiantArms: Both weapons found");
            } else {
                log.info("NSP_SymbiantArms: Weapons missing - left: " + (nsp_symbiant_left_arm != null) +
                        ", right: " + (nsp_symbiant_right_arm != null));
            }
        }

        if(!wpn_found || !prt_found) {
            log.info("NSP_SymbiantArms: Waiting for components - wpn_found: " + wpn_found + ", prt_found: " + prt_found);
            return;
        }

        if(!nsp_symbiant_l.isAlive()){
            nsp_symbiant_left_arm.disable(true);
            nsp_symbiant_left_arm.getSprite().setSize(0,0);
        }
        if(!nsp_symbiant_r.isAlive()){
            nsp_symbiant_right_arm.disable(true);
            nsp_symbiant_right_arm.getSprite().setSize(0,0);
        }

        if (nsp_symbiant_l != null && nsp_symbiant_left_arm != null) {
            nsp_symbiant_l.setFacing(nsp_symbiant_left_arm.getCurrAngle());
            log.info("NSP_SymbiantArms: Setting left arm facing to: " + nsp_symbiant_left_arm.getCurrAngle());
        }
        if (nsp_symbiant_r != null && nsp_symbiant_right_arm != null) {
            nsp_symbiant_r.setFacing(nsp_symbiant_right_arm.getCurrAngle());
            log.info("NSP_SymbiantArms: Setting right arm facing to: " + nsp_symbiant_right_arm.getCurrAngle());
        }
    }
}