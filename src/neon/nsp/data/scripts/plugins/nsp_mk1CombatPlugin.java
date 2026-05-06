//package neon.nsp.data.scripts.plugins;
//
//import com.fs.starfarer.api.Global;
//import com.fs.starfarer.api.campaign.BattleAPI;
//import com.fs.starfarer.api.campaign.CampaignFleetAPI;
//import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
//import com.fs.starfarer.api.combat.ShipAPI;
//import com.fs.starfarer.api.combat.ViewportAPI;
//import com.fs.starfarer.api.impl.campaign.ids.Factions;
//import com.fs.starfarer.api.input.InputEventAPI;
//import com.fs.starfarer.api.util.IntervalUtil;
//import org.lazywizard.lazylib.MathUtils;
//import org.magiclib.util.MagicUI;
//import org.lwjgl.util.vector.Vector2f;
//
//import java.awt.*;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class nsp_mk1CombatPlugin extends BaseEveryFrameCombatPlugin {
//
//    String fullString = "T H R E A T  D E T E C T E D";
//    //String fullOtherString = "!!WARNING!!";
//    String soundID = "nsp_threat_detected";
//    Float soundPitch = 1f;
//    Float soundVolume = 0.7f;
//    Color textColor = Color.RED;
//    Float minTimer = 5f;
//    Float maxTimer = 10f;
//    Float maxIntensity = 1.5f;
//    boolean isThreat = false;
//    boolean init = false;
//    IntervalUtil timer = new IntervalUtil(minTimer, maxTimer);
//    Float jitterTimer = 0f;
//    Float midJitter = 0f;
//    Float intensity = 1f;
//    Float fadeJitterOutAt = 3f;
//
//    // Text crawl variables - matching cs_suppressionUnit exactly
//    Map<ShipAPI, TextCrawlData> shipTextCrawls = new HashMap<>();
//
//    private static class TextCrawlData {
//        int currDex = 0;
//        String currString = "";
//        String currOtherString = "";
//        IntervalUtil nextCharacterInterval = new IntervalUtil(0.10f, 0.10f);
//        IntervalUtil fadeInterval = new IntervalUtil(3f, 3f);
//        boolean drawText = false;
//
//        TextCrawlData() {}
//    }
//
//    public static final List<String> HullIDList = new ArrayList<>(Arrays.asList(
//            "nsp_dominatormk1", "nsp_legionmk1", "onslaught_mk1"
//    ));
//    private final List<ShipAPI> foundShipsList = new ArrayList<>();
//
//    @Override
//    public void advance(float amount, List<InputEventAPI> events) {
//        if (Global.getCombatEngine().isPaused()) return;
//
//        if (!init) {
//            init = true;
//            if (Global.getSector().getPlayerFleet() != null && Global.getSector().getPlayerFleet().getBattle() != null) {
//                CampaignFleetAPI enemyFleet = getFleetFromBattle(Global.getSector().getPlayerFleet().getBattle());
//                if (enemyFleet != null && enemyFleet.getFaction() != null && enemyFleet.getFaction().getId().equals(Factions.THREAT)) {
//                    isThreat = true;
//                    for (ShipAPI ship : Global.getCombatEngine().getShips()) {
//                        if (HullIDList.contains(ship.getHullSpec().getBaseHullId())) {
//                            foundShipsList.add(ship);
//                            foundShipsList.addAll(ship.getChildModulesCopy());
//                        }
//                    }
//                    jitterTimer = timer.getIntervalDuration() * 1.8f;
//                    midJitter = jitterTimer / 2f;
//                }
//            }
//        }
//
//        if (init && !isThreat) {
//            Global.getCombatEngine().removePlugin(this);
//            return;
//        }
//
//        timer.advance(amount);
//        jitterTimer -= amount;
//
//        if (jitterTimer <= 0f) {
//            Global.getCombatEngine().removePlugin(this);
//            return;
//        }
//
//        // Fade jitter intensity
//        float toadd;
//        if (jitterTimer > fadeJitterOutAt) {
//            toadd = amount / midJitter;
//        } else {
//            toadd = -amount / fadeJitterOutAt;
//        }
//        intensity = Math.min(maxIntensity, Math.max(0.01f, intensity + toadd));
//
//        ViewportAPI viewport = Global.getCombatEngine().getViewport();
//
//        // Process each ship
//        for (ShipAPI ship : foundShipsList) {
//            ship.setJitterUnder(ship, textColor, intensity, 5, 8f);
//
//            TextCrawlData data = shipTextCrawls.get(ship);
//
//            // Start new text crawl on timer interval
//            if (data == null && timer.intervalElapsed() && ship.getParentStation() == null) {
//                Global.getSoundPlayer().playSound(soundID, soundPitch, soundVolume, ship.getLocation(), ship.getVelocity());
//                data = new TextCrawlData();
//                data.drawText = true;
//                shipTextCrawls.put(ship, data);
//            }
//
//            // Handle text drawing - EXACT pattern from cs_suppressionUnit
//            if (data != null && data.drawText) {
//                data.nextCharacterInterval.advance(amount);
//
//                Character next = getCharAt(fullString, data.currDex);
//
//                if (data.nextCharacterInterval.intervalElapsed() && next != null) {
//                //    Character other = getCharAt(fullOtherString, data.currDex);
//                  //  if (other != null) {
//                   //     data.currOtherString += other;
//                   // }
//                    data.currString += next;
//                    data.currDex++;
//                } else if (next == null) {
//                    data.fadeInterval.advance(amount);
//                    if (data.fadeInterval.intervalElapsed()) {
//                        data.drawText = false;
//                        shipTextCrawls.remove(ship);
//                        continue;
//                    }
//                }
//
//                // Check if ship is near viewport before drawing
//                if (viewport != null && viewport.isNearViewport(ship.getLocation(), ship.getCollisionRadius())) {
//                    // Draw glitch text at random positions (matching cs_suppressionUnit)
//                    for (int i = 0; i < MathUtils.getRandomNumberInRange(4, 8); i++) {
//                        Vector2f randomWorldPos = MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius());
//                        float screenX = viewport.convertWorldXtoScreenX(randomWorldPos.x);
//                        float screenY = viewport.convertWorldYtoScreenY(randomWorldPos.y);
//                        // Correct MagicUI.addText signature: (ShipAPI, String, Color, Vector2f, boolean)
//                        MagicUI.addText(null, data.currOtherString, textColor, new Vector2f(screenX, screenY), true);
//                    }
//
//                    // Draw main text above ship
//                    Vector2f textWorldPos = new Vector2f(ship.getLocation().x, ship.getLocation().y + ship.getCollisionRadius());
//                    float screenX = viewport.convertWorldXtoScreenX(textWorldPos.x);
//                    float screenY = viewport.convertWorldYtoScreenY(textWorldPos.y);
//                    MagicUI.addText(null, data.currString, textColor, new Vector2f(screenX, screenY), true);
//                }
//            }
//        }
//    }
//
//    private Character getCharAt(String str, int index) {
//        if (index >= 0 && index < str.length()) {
//            return str.charAt(index);
//        }
//        return null;
//    }
//
//    CampaignFleetAPI getFleetFromBattle(BattleAPI battle) {
//        CampaignFleetAPI bestFallback = null;
//        for (CampaignFleetAPI fleet : battle.getNonPlayerSide()) {
//            bestFallback = fleet;
//        }
//        return bestFallback;
//    }
//}