package data.hullmods;

import data.scripts.util.NSP_Util;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.NSPSandevistan;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicIncompatibleHullmods;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.awt.*;
import java.util.*;
import java.util.List;

public class NSPSandyEffect extends BaseHullMod {
    public static float SHIELD_BONUS_TURN = 120f;
    public static float SHIELD_BONUS_UNFOLD = 120f;
    public static float MISSILE_ROF_MULT = 0.3f;
    public static float ARMOR_DAMAGE_MULT = 5f;
    public static float HULL_DAMAGE_MULT = 5f;
    public static float EMP_DAMAGE_MULT = 0.5f;
    public static float ENERGY_WEAPON_FLUX_INCREASE = 50f;
    public static final float RESISTANCE = 0.35f;
    public static final float RESIST_TIME = 0.1f;
    private static final Set<String> BLOCKED_HULLMODS = new HashSet<>();

    static {
        // These hullmods will automatically be removed
        // This prevents unexplained hullmod blocking
        BLOCKED_HULLMODS.add("safetyoverrides");
        BLOCKED_HULLMODS.add("targetingunit");
        BLOCKED_HULLMODS.add("dedicated_targeting_core");
        BLOCKED_HULLMODS.add("shield_shunt");
        BLOCKED_HULLMODS.add("heavyarmor");
        BLOCKED_HULLMODS.add("advancedshieldemitter");
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("Due to extensive weapon pattern modification and standard system limit overrides required for operation, this vessel's systems are incompatible with %s, %s, %s, %s and %s", 3f, Misc.getNegativeHighlightColor(),
                getHullmodName("safetyoverrides"), getHullmodName("targetingunit"), getHullmodName("dedicated_targeting_core"), getHullmodName("heavyarmor"), getHullmodName("shield_shunt"), getHullmodName("advancedshieldemitter"));
        tooltip.addPara("Timeflow is passivley increased by %s and is compounded by a temporary %s boost for each enemy vessel destroyed, " +
                "capable of stacking up to %s times.", 5f, Color.ORANGE, "20%", "30%", "5");
        tooltip.addPara("Additionally, autorepair systems mend armor damage by up to %s across the duration of the timeflow surge.  " +
                "The maximum armor regeneration is %s.", 5f, Color.ORANGE, "10%", "2000");
        tooltip.addPara("Hullfoam systems are also activated during the timeflow surge, repairing hull damage through it's duration.", 5f, Color.ORANGE);
        tooltip.addPara("As a result of the instabilities within this vessel's resonant field dampener, it's effects are reversed when the system is inactive, resulting in the vessel taking %s more damage to it's armor, and %s more to it's hull. Energy weapon flux generation is increased by %s. EMP damage is reduced by %s. Shield raise and turn rate is increased by %s.", 5f, Color.ORANGE, "50%", "50%", "50%", "50%", "120%");
    }

    public String getHullmodName(String id) {
        return Global.getSettings().getHullModSpec(id).getDisplayName();
    }

    public void applyEffectsAfterShipCreated(ShipAPI ship, String id) {
        super.applyEffectsAfterShipCreation(ship, id);
        ship.addListener(new SandyListener(ship));
    }

    @Override
    public boolean affectsOPCosts() {
        return true;
    }
    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getMissileRoFMult().modifyMult(id, MISSILE_ROF_MULT);
        stats.getEnergyWeaponFluxCostMod().modifyPercent(id, ENERGY_WEAPON_FLUX_INCREASE);
        stats.getShieldTurnRateMult().modifyPercent(id, SHIELD_BONUS_TURN);
        stats.getShieldUnfoldRateMult().modifyPercent(id, SHIELD_BONUS_UNFOLD);
    }

    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        for (String tmp : BLOCKED_HULLMODS) {
            if (ship.getVariant().getHullMods().contains(tmp)) {
                ship.getVariant().removeMod(tmp);
                String ERROR = "nsp_incompatible";
                ship.getVariant().addMod(ERROR);
            }
        }
    }

    @Override
    public void applyEffectsAfterShipAddedToCombatEngine(ShipAPI ship, String id) {
        ship.addListener(new SandyListener(ship));
        ship.getMutableStats().getTimeMult().modifyMult(id, SandyListener.PASSIVE_TIMEFLOW);
        ship.getMutableStats().getArmorDamageTakenMult().modifyMult(id, ARMOR_DAMAGE_MULT);
        ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, HULL_DAMAGE_MULT);
        ship.getMutableStats().getEmpDamageTakenMult().modifyMult(id, EMP_DAMAGE_MULT);


    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "" + (int) Math.round((1f - SandyListener.ARMOR_REGEN) * 100f) + "%";
        if (index == 1) return "" + (int) Math.round((1f - SandyListener.DISRUPTION_TIME) * 100f) + "%";
        return null;
    }

    private static final Object STATUS_KEY = new Object();

    class SandyListener implements AdvanceableListener, DamageDealtModifier {
        String id = "example_ID";
        ShipAPI ship;
        float timer = 0.05f;
        float duration = 0.05f;
        Color color = Color.GRAY;

        // Kill tracking
        int kills = 0;
        int maxKillsBonus = 7;
        float bonusPerKill = 0.5f; // flat bonus per kill
        float killBonusDuration = 30f; // how long each kill's bonus lasts in seconds

        // Time multiplier system
        float currentBonus = 1f; // current active bonus
        float maxBonus = 5f + (maxKillsBonus * bonusPerKill); // Maximum possible bonus
        float minBonus = 1f; // minimum bonus (normal time flow)
        float decayRate = 0.0001f; // how fast the bonus decays per second (now decays entire kill bonuses)
        private static final float ARMOR_REGEN = 15f;
        private static final float ARMOR_CAP = 2000f;
        private static final float DISRUPTION_TIME = 2f;
        private static final float PASSIVE_TIMEFLOW = 1.2f;
        private static final float DAMAGE_REDUCTION = 0.12f;
        private static final Color AFTERIMAGE_COLOR = new Color(255, 63, 0, 100);

        // Hullfoam parameters
        final Map<HullSize, Float> repairSpeed = new HashMap<>();
        final float maxFoam = 60f;
        final float maxRepairReductionPerUsed = 0.7f;
        {
            repairSpeed.put(HullSize.CRUISER, 1.5f);
            repairSpeed.put(HullSize.DESTROYER, 3f);
        }

        // track individual kill bonuses and their timers
        float[] killBonuses = new float[maxKillsBonus];
        float[] killTimers = new float[maxKillsBonus];

        IntervalUtil interval = new IntervalUtil(timer, timer);

        public SandyListener(ShipAPI attachedShip) {
            ship = attachedShip;
        }

        @Override
        public void advance(float amount) {
            if (Global.getCombatEngine().isPaused() || ship.isHulk()) return;

            // update and decay kill bonuses
            float totalActiveBonus = 0f;
            boolean hasActiveBonuses = false;

            for (int i = 0; i < kills; i++) {
                if (killBonuses[i] > 0) {
                    killTimers[i] -= amount;
                    if (killTimers[i] <= 0) {
                        // This kill's bonus has expired
                        killBonuses[i] = 0;
                    } else {
                        // decay the bonus over time
                        killBonuses[i] = Math.max(0, killBonuses[i] - (decayRate * amount));
                        totalActiveBonus += killBonuses[i];
                        hasActiveBonuses = true;
                    }
                }
            }

            // calculate current bonus (base + active kill bonuses)
            currentBonus = minBonus + totalActiveBonus;

            // apply the time mult
            ship.getMutableStats().getTimeMult().modifyMult(id, currentBonus);

            // activate armor regeneration and hullfoam when kill bonuses are active
            if (hasActiveBonuses && !ship.isHulk() && !ship.getFluxTracker().isVenting() && !ship.isPhased()) {
                repairArmor(amount);
                applyHullfoam(amount);
            }

            if (ship == Global.getCombatEngine().getPlayerShip()) {
                Global.getCombatEngine().maintainStatusForPlayerShip(
                        STATUS_KEY,
                        Global.getSettings().getSpriteName("ui", "icon_op"),
                        "KILL THEM ALL",
                        Misc.getRoundedValue(currentBonus) + "x Timeflow",
                        false
                );
            }

            // Create afterimages
            interval.advance(amount);
            if (interval.intervalElapsed()) {
                NSPSandevistan.afterimage(ship, color, duration, duration, duration);
            }
        }

        private void repairArmor(float amount) {
            if (ship.getFluxTracker().isOverloaded() || ship.getFluxTracker().getTimeToVent() < DISRUPTION_TIME) {
                return;
            }

            ArmorGridAPI armorGrid = ship.getArmorGrid();
            float[][] grid = armorGrid.getGrid();
            float max = armorGrid.getMaxArmorInCell();
            float statusMult = ship.getFluxTracker().isOverloaded() ? 0.5f : 1f;
            float baseCell = armorGrid.getMaxArmorInCell() * Math.min(ship.getHullSpec().getArmorRating(), ARMOR_CAP) / armorGrid.getArmorRating();
            float repairAmount = baseCell * (ARMOR_REGEN / 100f) * statusMult * amount;

            // Iterate through all armor cells and find any that aren't at max
            for (int x = 0; x < grid.length; x++) {
                for (int y = 0; y < grid[0].length; y++) {
                    if (grid[x][y] < max) {
                        float regen = Math.min(grid[x][y] + repairAmount, max);
                        armorGrid.setArmorValue(x, y, regen);
                    }
                }
            }
            ship.syncWithArmorGridState();
        }

        private void applyHullfoam(float amount) {
            if (!ship.isAlive() || ship.getFluxTracker().isOverloaded() || ship.getFluxTracker().getTimeToVent() < DISRUPTION_TIME) {
                return;
            }

            Map<String, Object> customCombatData = Global.getCombatEngine().getCustomData();
            String id = ship.getId();
            float foamLeft = maxFoam;

            if (customCombatData.get("MHMods_hullfoam" + id) != null && customCombatData.get("MHMods_hullfoam" + id) instanceof Float)
                foamLeft = (float) customCombatData.get("MHMods_hullfoam" + id);

            float currentHP = ship.getHitpoints();
            float repairReduction = maxRepairReductionPerUsed * (1 - foamLeft);
            float missingHP = Math.max(0f, 1f - repairReduction - ship.getHullLevel());

            if (missingHP > 0) {
                float repairThatFrame = repairSpeed.get(ship.getHullSize()) * 0.01f * amount;
                if (missingHP < repairThatFrame) repairThatFrame = missingHP;

                float hullToRepair = ship.getMaxHitpoints() * repairThatFrame;
                float percentRepaired = ship.getMaxHitpoints() * repairThatFrame / ship.getHullSpec().getHitpoints();
                if (percentRepaired > foamLeft) {
                    percentRepaired = foamLeft;
                    hullToRepair = ship.getHullSpec().getHitpoints() * percentRepaired;
                }
                ship.setHitpoints(currentHP + hullToRepair);

                foamLeft -= percentRepaired;
            }

            if (ship == Global.getCombatEngine().getPlayerShip()) {
                if (foamLeft != 0) {
                    Global.getCombatEngine().maintainStatusForPlayerShip("MHMods_hullfoam", "graphics/icons/hullsys/mhmods_hullfoam.png", "Hullfoam Left", (float) Math.round(foamLeft * 1000) / 10 + "%", false);
                } else {
                    Global.getCombatEngine().maintainStatusForPlayerShip("MHMods_hullfoam", "graphics/icons/hullsys/mhmods_hullfoam.png", "Hullfoam Left", "OUT OF FOAM", true);
                }
            }

            customCombatData.put("MHMods_hullfoam" + id, foamLeft);
        }

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (target instanceof ShipAPI) {
                ShipAPI targetShip = (ShipAPI) target;
                if (targetShip.getOwner() != ship.getOwner()
                        && !targetShip.isHulk()
                        && targetShip.getHullSize() != ShipAPI.HullSize.FIGHTER
                        && !targetShip.isStationModule()
                        && !targetShip.hasListener(new KillTracker(ship))) {
                    targetShip.addListener(new KillTracker(ship));
                    return "added_kill_tracker";
                }
            }
            return null;
        }

        public void addKillBonus() {
            if (kills < maxKillsBonus) {
                killBonuses[kills] = bonusPerKill;
                killTimers[kills] = killBonusDuration;
                kills++;
            } else {
                // Find the oldest bonus to replace
                int oldestIndex = 0;
                for (int i = 1; i < maxKillsBonus; i++) {
                    if (killTimers[i] < killTimers[oldestIndex]) {
                        oldestIndex = i;
                    }
                }
                killBonuses[oldestIndex] = bonusPerKill;
                killTimers[oldestIndex] = killBonusDuration;
            }
        }
    }

    private Set<ShipAPI> nearbyShips = new HashSet<>();
    private float resisting = 0f;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount){

        if (Global.getCombatEngine() == null)
            return;


        List<ShipAPI> shipPlusModules = ship.getChildModulesCopy();
        shipPlusModules.add(ship);

        resisting -= amount;
        for (ShipAPI s : Global.getCombatEngine().getShips()){
            if (s != ship && s.getHullSize() != HullSize.FIGHTER && s.isAlive()){
                float distance = Vector2f.sub(ship.getLocation(),s.getLocation(),new Vector2f()).length() - ship.getCollisionRadius();
                for (ShipAPI child : ship.getChildModulesCopy()){
                    float newDistance = Vector2f.sub(child.getLocation(),s.getLocation(),new Vector2f()).length() - child.getCollisionRadius();
                    distance = Math.min(distance,newDistance);
                }
                float mult = s.getMutableStats().getDynamic().getValue(Stats.EXPLOSION_RADIUS_MULT);
                float radius = s.getCollisionRadius() + Math.min(200f, s.getCollisionRadius()) * mult;
                if (distance <= radius){
                    nearbyShips.add(s);
                } else {
                    nearbyShips.remove(s);
                }
            }
        }
        Iterator<ShipAPI> iter = nearbyShips.iterator();
        while (iter.hasNext()){
            ShipAPI t = iter.next();
            if (t == null || !t.isAlive()){
                iter.remove();
                for (ShipAPI s : shipPlusModules) {
                    s.getMutableStats().getHighExplosiveDamageTakenMult().modifyMult("KT_blastdampeners", RESISTANCE);
                    s.getMutableStats().getHighExplosiveShieldDamageTakenMult().modifyMult("KT_blastdampeners", RESISTANCE);
                }
                resisting = RESIST_TIME;
            }
        }
        if (resisting <= 0f){
            for (ShipAPI s : shipPlusModules) {
                s.getMutableStats().getHighExplosiveDamageTakenMult().unmodify("KT_blastdampeners");
                s.getMutableStats().getHighExplosiveShieldDamageTakenMult().unmodify("KT_blastdampeners");
            }
        }

    }

static class KillTracker implements HullDamageAboutToBeTakenListener {
        String key = "$nsp_kill_tracker_key";
        ShipAPI dealer;
        private Object spec;

        public KillTracker(ShipAPI ship) {
            dealer = ship;
        }

        @Override
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
            if (damageAmount >= ship.getHitpoints()) {
                if (param instanceof ShipAPI) {
                    if (param != dealer) return false;
                    if (ship.getCustomData().containsKey(key)) return false;

                    ship.setCustomData(key, true);

                    if (dealer.getListenerManager() != null) {
                        Optional<SandyListener> maybelistener = dealer.getListenerManager()
                                .getListeners(SandyListener.class).stream().findFirst();

                        if (maybelistener.isPresent()) {
                            SandyListener listener = maybelistener.get();
                            listener.addKillBonus();
                        }
                    }
                }
            }
            return false;
        }
    }
}