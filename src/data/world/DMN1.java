package data.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.SectorProcGen;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.world.TTBlackSite;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class DMN1 {
    public static Logger log = Global.getLogger(DMN1.class);
    public static void main(String[] args) {
    }
    public static void addDMN(SectorAPI sector){
        Iterator<StarSystemAPI> stariter = sector.getStarSystems().iterator();
        ArrayList<StarSystemAPI> validstars = new ArrayList<StarSystemAPI>();
        while (stariter.hasNext()){
            StarSystemAPI star = stariter.next();
            if (star.isProcgen()){
                validstars.add(star);
            }
        }
        Collections.shuffle(validstars);
        StarSystemAPI targetstar = validstars.get(0);
        SectorEntityToken entity = targetstar.getPlanets().get(0);
        TTBlackSite.addDerelict(targetstar, entity, "nsp_absolution_domain_standard", "DMN Into The Flames", "nsp_absolution_domain", ShipRecoverySpecial.ShipCondition.PRISTINE, entity.getRadius() +300, true);
        log.info("The DMN Into The Flames is in" + targetstar.getName());
    }
}