package neon.nsp.data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class nsp_mk1CombatPlugin extends BaseEveryFrameCombatPlugin {

    String flavorText = "T H R E A T  D E T E C T E D";
    String soundID = "cr_playership_warning";
    Float soundPitch = 1f;
    Float soundVolume = 0.7f;
    Color textColor = Color.RED;
    Float textDuration = 5f;
    Float textFlashTimer = 1.5f;
    Float textSize = 64f;
    Float minTimer = 5f;
    Float maxTimer = 10f;
    Float maxIntensity = 1.5f;
    boolean isThreat = false;
   boolean init = false; // init function is kinda ass here, so just do this
    IntervalUtil timer = new IntervalUtil(minTimer, maxTimer);
    Float jitterTimer = timer.getIntervalDuration()*1.8f;
    Float midJitter = jitterTimer/2f;
    Float intensity = 1f;
    Float fadeJitterOutAt = 3f;
    public static final List<String> HullIDList = new ArrayList<>(Arrays.asList(
            "nsp_dominatormk1", "nsp_legionmk1", "onslaught_mk1"
    ));
    private final List<ShipAPI> foundShipsList = new ArrayList<>();


    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (Global.getCombatEngine().isPaused()) return;
        if (!init) {
            init = true;
            // a bunch of nullchecks. SURELY most of these can't be null under any situation, but i'm writing this code for someone else so I have a standard to keep up
            // unfortunately. otherwise i'd rawdog this
            if (Global.getSector().getPlayerFleet() != null && Global.getSector().getPlayerFleet().getBattle() != null) {
                CampaignFleetAPI enemyFleet = getFleetFromBattle(Global.getSector().getPlayerFleet().getBattle());
                if (enemyFleet != null && enemyFleet.getFaction() != null && enemyFleet.getFaction().getId().equals(Factions.THREAT)) {
                    isThreat = true;
                    for (ShipAPI ship : Global.getCombatEngine().getShips()) {
                        if (HullIDList.contains(ship.getHullSpec().getBaseHullId())) {
                            foundShipsList.add(ship);
                            foundShipsList.addAll(ship.getChildModulesCopy());
                        }
                    }
                }
            }
        }
        if (init && !isThreat) {
            Global.getCombatEngine().removePlugin(this);
            return;
        }
        timer.advance(amount);
        jitterTimer -= amount;
        if (jitterTimer <= 0f){
            Global.getCombatEngine().removePlugin(this);
            return;
        }
        // fade jitter in over half midJitter at start of combat, hold intensity and then fade out at x sec left
        Float toadd; // TRUE dogshit code
        if (jitterTimer > fadeJitterOutAt) { // look it's been 6 months since i've modded this game, give me a break
             toadd = amount / midJitter;
        } else {
            toadd = -amount/fadeJitterOutAt;
        }
        intensity = Math.min(maxIntensity, (intensity+toadd));
        if (intensity < 0.01f) intensity = 0.01f; // don't crash the game
        for (ShipAPI ship : foundShipsList) {
            ship.setJitterUnder(ship, textColor, intensity,5,8f);
            if (timer.intervalElapsed() && ship.getParentStation() == null) {
                Global.getSoundPlayer().playSound(soundID, soundPitch, soundVolume, ship.getLocation(), ship.getVelocity());
                Global.getCombatEngine().addFloatingText(ship.getLocation(), flavorText, textSize, textColor, ship, textFlashTimer, textDuration);
            }
        }
    }
     CampaignFleetAPI getFleetFromBattle(BattleAPI battle){ // not fantastic, but i don't remember how to convert this function back from kotlin to java
         // it'll get the job done...
         CampaignFleetAPI bestfallback = null;
         for (CampaignFleetAPI fleet : battle.getNonPlayerSide()){
             bestfallback = fleet;
         }

         return bestfallback;


     }

}
