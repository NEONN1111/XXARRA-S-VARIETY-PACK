package data.missions.XVP_Mod_Showcase;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import com.fs.starfarer.api.util.ListMap;
import com.fs.starfarer.api.util.Pair;
import org.apache.log4j.Logger;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import com.fs.starfarer.api.util.Pair;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.*;
import java.util.ArrayList;
import java.util.TreeSet;

import static javax.swing.UIManager.getString;

//General mission to add all ships
//Class needs to be in matching not source data/mission package and named MissionDefinition
public class MissionDefinition implements MissionDefinitionPlugin {
    private static final Logger log = Global.getLogger(MissionDefinition.class);

    private static String MOD_ID = "NSP";

    public static Set<String> allModules = new HashSet<>();
    public static ListMap<String> modToHull = new ListMap<>();

    private static boolean firstLoad = true;
    private static int currentSelection;

    public void defineMission(MissionDefinitionAPI api) {
        allModules = getAllModuleIds();
        modToHull = getModToHullListMap(allModules);

        // Set up the fleets
        api.initFleet(FleetSide.PLAYER, "XVP", FleetGoal.ATTACK, false, 10);
        api.initFleet(FleetSide.ENEMY, "IWD", FleetGoal.ATTACK, true, 10);

        // Set a blurb for each fleet
        api.setFleetTagline(FleetSide.PLAYER, "Your Boyz!!!");
        api.setFleetTagline(FleetSide.ENEMY, "Targets!!!");

        // These show up as items in the bulleted list under
        // "Tactical Objectives" on the mission detail screen

        //Maybe redo for hulls?
        ArrayList<String> Variants = new ArrayList<>(Global.getSettings().getAllVariantIds());

        List<String> shipList = new ArrayList<>(modToHull.getList(MOD_ID)); // make new ship list so removing doesn't affect the original list

        // don't use api.addFleetMember() because then the ships start at 0 CR
        if (shipList.isEmpty()) {
            api.addToFleet(FleetSide.PLAYER, Global.getSettings().getString("errorShipVariant"), FleetMemberType.SHIP, getString("modNoShips"), true);
        } else {
            boolean flagship = true;
            for (FleetMemberAPI member : getModFleetMembers(shipList)) {
                String variantId = member.getVariant().getHullVariantId();
                FleetMemberAPI ship = api.addToFleet(FleetSide.PLAYER, variantId, FleetMemberType.SHIP, "XVP" + " " + member.getHullId(), flagship);
                if (flagship) {
                    flagship = false;
                }
            }
        }

        api.addBriefingItem("Showing " + shipList.size() + " ships (that's excluding modules and wing only ships.");
        api.addBriefingItem("Test what you like");
        api.addBriefingItem("Try not to have too much fun");

        // Set up the enemy fleet
        api.addToFleet(FleetSide.ENEMY, "atlas_Standard", FleetMemberType.SHIP, true);
        api.addToFleet(FleetSide.ENEMY, "atlas_Standard", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "atlas_Standard", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "atlas_Standard", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "atlas_Standard", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "atlas_Standard", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "atlas_Standard", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "atlas_Standard", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "atlas_Standard", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "atlas_Standard", FleetMemberType.SHIP, false);

        // Set up the map.
        float width = 24000f;
        float height = 18000f;
        api.initMap((float) -width / 2f, (float) width / 2f, (float) -height / 2f, (float) height / 2f);

        float minX = -width / 2;
        float minY = -height / 2;

        for (int i = 0; i < 15; i++) {
            float x = (float) Math.random() * width - width / 2;
            float y = (float) Math.random() * height - height / 2;
            float radius = 100f + (float) Math.random() * 900f;
            api.addNebula(x, y, radius);
        }

        // Add an asteroid field
        api.addAsteroidField(minX + width * 0.3f, minY, 90, 3000f,
                20f, 70f, 50);

        // Add some planets.  These are defined in data/config/planets.json.
        api.addPlanet(300, 100, 200f, "terran", 350f, true);
    }

    public static Set<FleetMemberAPI> getModFleetMembers(List<String> modShipIds) {
        Set<FleetMemberAPI> fleetMemberSet = new TreeSet<>(memberComparator);
        for (String hullId : modShipIds) {
            String hullVariantId = hullId + HULL_SUFFIX;
            FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, hullVariantId);
            fleetMemberSet.add(member);
        }
        return fleetMemberSet;
    }

    public static final String
            HULL_SUFFIX = "_Hull",
            SHIP_DATA_CSV = "data/hulls/ship_data.csv";

    public static String getString(String id) {
        return Global.getSettings().getString(MOD_ID, id);
    }

    public static Set<String> getAllModuleIds() {
        Set<String> modulesSet = new HashSet<>();
        for (ShipHullSpecAPI shipHullSpec : Global.getSettings().getAllShipHullSpecs()) {
            if (shipHullSpec.isDefaultDHull()) continue;
            String hullVariantId = shipHullSpec.getHullId() + HULL_SUFFIX;
            ShipVariantAPI variant = Global.getSettings().getVariant(hullVariantId);
            for (String moduleId : variant.getStationModules().values()) {
                modulesSet.add(Global.getSettings().getVariant(moduleId).getHullSpec().getHullId());
            }
        }
        // add skins of modules because idk this is a thing with Diable
        for (ShipHullSpecAPI shipHullSpec : Global.getSettings().getAllShipHullSpecs()) {
            if (!shipHullSpec.isBaseHull()
                    && modulesSet.contains(shipHullSpec.getBaseHullId())) {
                modulesSet.add(shipHullSpec.getHullId());
            }
        }
        return modulesSet;
    }


    public static ListMap<String> getModToHullListMap(Set<String> blacklist) {
        try {
            // create ListMap of sources (file paths) to base hulls, mods only
            ListMap<String> sourceToHullListMap = new ListMap<>();
            JSONArray array = Global.getSettings().getMergedSpreadsheetDataForMod("id", SHIP_DATA_CSV, "starsector-core");
            for (int i = 0; i < array.length(); i++) {
                JSONObject row = array.getJSONObject(i);
                String id = row.getString("id");
                String source = row.getString("fs_rowSource");
                if (id.isEmpty() || source.startsWith("null")) { // block vanilla hulls
                    continue;
                }
                ShipHullSpecAPI shipHullSpec = Global.getSettings().getHullSpec(id);
                // double check that the csv entry isn't just an edited vanilla hull
                if (shipHullSpec.getShipFilePath().startsWith("data") && validateHullSpec(shipHullSpec, blacklist)) {
                    sourceToHullListMap.add(source, id);
                }
            }
            // convert ListMap keys from sources to mod IDs
            ListMap<String> modToHullListMap = new ListMap<>();
            for (String source : sourceToHullListMap.keySet()) {
                for (ModSpecAPI modSpec : Global.getSettings().getModManager().getEnabledModsCopy()) {
                    if (modSpec.isUtility() || !source.contains(modSpec.getPath())) continue;
                    modToHullListMap.put(modSpec.getId(), sourceToHullListMap.getList(source));
                }
            }
            // add skins to ListMap
            for (ShipHullSpecAPI shipHullSpec : Global.getSettings().getAllShipHullSpecs()) {
                if (!shipHullSpec.getShipFilePath().startsWith("data") // skip vanilla hulls
                        || shipHullSpec.isBaseHull() // skip non-skins
                        || !(validateHullSpec(shipHullSpec, blacklist))) { // skip modules/stations
                    continue;
                }
                String hullId = shipHullSpec.getHullId();
                // non-vanilla skins
                boolean foundBaseHull = false;
                for (String key : modToHullListMap.keySet()) {
                    // if we see the base hull in the mod, add the skin to the mod's list
                    if (modToHullListMap.getList(key).contains(shipHullSpec.getBaseHullId())) {
                        modToHullListMap.add(key, hullId);
                        foundBaseHull = true;
                        break;
                    }
                }
                // vanilla skins
                if (!foundBaseHull) {
                    // we can't only include the mods that are already in the list map because some mods don't have any ship_data but do have skins
                    for (ModSpecAPI modSpec : Global.getSettings().getModManager().getEnabledModsCopy()) {
                        if (modSpec.isUtility()) continue;
                        // check if the .skin file is within that mod
                        try {
                            String shipFilePath = shipHullSpec.getShipFilePath().replace("\\", "/");
                            Global.getSettings().loadJSON(shipFilePath, modSpec.getId());
                            modToHullListMap.add(modSpec.getId(), hullId);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            //log.info("modToHullListMap: " + modToHullListMap);
            return modToHullListMap;
        } catch (IOException | JSONException e) {
            log.error("Could not load " + SHIP_DATA_CSV, e);
        }
        return null;
    }

    public static boolean validateHullSpecExcludingFighters(ShipHullSpecAPI shipHullSpec, Set<String> blacklist) {
        if (shipHullSpec.isDefaultDHull()) {
            return false;
        } else return !(shipHullSpec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.STATION)
                || blacklist.contains(shipHullSpec.getHullId())
                || Global.getSettings().getVariant(shipHullSpec.getHullId() + "_Hull").isStation()
                || (shipHullSpec.getManufacturer().equals("Common") && (!shipHullSpec.hasHullName() || shipHullSpec.getDesignation().isEmpty()))
                || shipHullSpec.getHullId().equals("shuttlepod") // frick it has the same format as SWP arcade ships
                || shipHullSpec.getHullId().startsWith("TAR_")); // literally can't find anything to block Practice Target hulls from the custom mission
    }

    public static boolean validateHullSpec(ShipHullSpecAPI shipHullSpec, Set<String> blacklist) {
        if (shipHullSpec.isDefaultDHull()) {
            return false;
        } else return !(shipHullSpec.getHullSize() == ShipAPI.HullSize.FIGHTER
                || shipHullSpec.getHints().contains(ShipHullSpecAPI.ShipTypeHints.STATION)
                || blacklist.contains(shipHullSpec.getHullId())
                || Global.getSettings().getVariant(shipHullSpec.getHullId() + "_Hull").isStation()
                || (shipHullSpec.getManufacturer().equals("Common") && (!shipHullSpec.hasHullName() || shipHullSpec.getDesignation().isEmpty()))
                || shipHullSpec.getHullId().equals("shuttlepod") // frick it has the same format as SWP arcade ships
                || shipHullSpec.getHullId().startsWith("TAR_") // literally can't find anything to block Practice Target hulls from the custom mission
                || shipHullSpec.getHullId().startsWith("loa_arscapitol") // breaks stuff in simulator
        );

    }

    public static final List<String> vanillaManufacturers = new ArrayList<>(Arrays.asList(
            "Low Tech", "Midline", "High Tech", "Pirate", "Luddic Path",
            "Hegemony", "XIV Battlegroup", "Lion's Guard", "Luddic Church", "Tri-Tachyon",
            "Explorarium", "Remnant", "Unknown"
    ));

    public static boolean isDamagedVersion(ShipHullSpecAPI hullSpec) {
        // isDHull() only checks if it has d-mods, thus including LP ships
        // but some mod ships can end in _d without being d-hulls, so include both
        return (hullSpec.isDHull() && hullSpec.getHullId().endsWith("_d"));
    }

    //non-vanilla tech types go last using this method
    public static int manufacturerToInt(String manufacturer) {
        return (vanillaManufacturers.contains(manufacturer)) ? vanillaManufacturers.indexOf(manufacturer) : 999;
    }

    public static int hullSizeToInt(ShipAPI.HullSize hullSize) {
        switch (hullSize) {
            case FRIGATE:
                return 1;
            case DESTROYER:
                return 2;
            case CRUISER:
                return 3;
            case CAPITAL_SHIP:
                return 4;
            default:
                return -1;
        }
    }

    public static final Comparator<FleetMemberAPI> memberComparator = new Comparator<FleetMemberAPI>() {
        // sort by:
        // 1. non-d-hulls > d-hulls
        // 2. manufacturer/tech type
        // 3. hull size (ascending)
        // 4. DP (ascending)
        // 5. variant name
        // 6. variant id
        @Override
        public int compare(FleetMemberAPI m1, FleetMemberAPI m2) {
            ShipVariantAPI var1 = m1.getVariant();
            ShipVariantAPI var2 = m2.getVariant();
            boolean isRestricted1 = m1.getHullSpec().hasTag(Tags.RESTRICTED);
            boolean isRestricted2 = m2.getHullSpec().hasTag(Tags.RESTRICTED);
            boolean isHideFromCodex1 = m1.getHullSpec().getHints().contains(ShipHullSpecAPI.ShipTypeHints.HIDE_IN_CODEX);
            boolean isHideFromCodex2 = m2.getHullSpec().getHints().contains(ShipHullSpecAPI.ShipTypeHints.HIDE_IN_CODEX);
            boolean isGoalVariant1 = var1.isGoalVariant();
            boolean isGoalVariant2 = var2.isGoalVariant();
            boolean isDHull1 = isDamagedVersion(var1.getHullSpec());
            boolean isDHull2 = isDamagedVersion(var2.getHullSpec());
            String manufacturer1 = var1.getHullSpec().getManufacturer();
            String manufacturer2 = var2.getHullSpec().getManufacturer();
            int manufacturerScore1 = manufacturerToInt(manufacturer1);
            int manufacturerScore2 = manufacturerToInt(manufacturer2);
            int sizeScore1 = hullSizeToInt(var1.getHullSize());
            int sizeScore2 = hullSizeToInt(var2.getHullSize());
            float DP1 = m1.getStats().getSuppliesToRecover().getBaseValue();
            float DP2 = m2.getStats().getSuppliesToRecover().getBaseValue();
            String name1 = var1.getDisplayName();
            String name2 = var2.getDisplayName();
            String id1 = m1.getHullId();
            String id2 = m2.getHullId();
            // put goalVariants at the top. shouldn't matter for stripped hulls, but using this for other stuff
            if (isGoalVariant1 && !isGoalVariant2) return -1;
            if (!isGoalVariant1 && isGoalVariant2) return 1;
            if (isRestricted1 && !isRestricted2) return 1;
            if (!isRestricted1 && isRestricted2) return -1;
            if (isHideFromCodex1 && !isHideFromCodex2) return 1;
            if (!isHideFromCodex1 && isHideFromCodex2) return -1;
            if (isDHull1 && !isDHull2) return 1;
            if (!isDHull1 && isDHull2) return -1;
            if (manufacturerScore1 != manufacturerScore2) return Integer.compare(manufacturerScore1, manufacturerScore2);
            if (!manufacturer1.equalsIgnoreCase(manufacturer2)) return manufacturer1.compareToIgnoreCase(manufacturer2);
            if (sizeScore1 != sizeScore2) return Integer.compare(sizeScore1, sizeScore2);
            if (DP1 != DP2) return Float.compare(DP1, DP2);
            if (!name1.equalsIgnoreCase(name2)) return name1.compareToIgnoreCase(name2);
            return id1.compareToIgnoreCase(id2);
        }
    };

}