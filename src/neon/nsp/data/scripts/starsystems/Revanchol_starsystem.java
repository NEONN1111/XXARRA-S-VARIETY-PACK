package neon.nsp.data.scripts.starsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.CoreLifecyclePluginImpl;
import com.fs.starfarer.api.impl.campaign.econ.FreeMarket;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Revanchol_starsystem {

    public void generate(SectorAPI sector) {
        // Create system
        StarSystemAPI system = sector.createStarSystem("Insulinde");
        system.addTag(Tags.THEME_CORE_POPULATED);

        system.getLocation().set(1700, 8000); //position of the system on the map
        system.setBackgroundTextureFilename("graphics/backgrounds/background3.jpg");

        // create the star
        PlanetAPI star = system.initStar("ISotD_WIP_star", // unique id for this star
                "star_yellow", // id in planets.json
                480f, // radius (in pixels at default zoom)
                400); // corona radius, from star edge
        system.setLightColor(new Color(195, 187, 137)); // light color in entire system, affects all entities
        star.setDiscoverable(false);

        //Tri Tach Sensor array
        SectorEntityToken loc1 = system.addCustomEntity(null,null, "sensor_array", Factions.TRITACHYON);
        loc1.getMemoryWithoutUpdate().set("$nsp_InsulindeArray", true);
        loc1.setCircularOrbitPointingDown(star, 65, 1430, 130);

        //Inner Asteroid belt
        system.addAsteroidBelt(star, 90, 1600, 150, 100, 110, Terrain.ASTEROID_BELT, "Inner Asteroid belt");
        system.addRingBand(star, "misc", "rings_dust0", 256f, 0, Color.white, 256f, 1600, 110, Terrain.RING, "Inner Asteroid belt");

        system.addAsteroidBelt(star, 120, 1900, 150, 100, 110, Terrain.ASTEROID_BELT, "Inner Asteroid belt");
        system.addRingBand(star, "misc", "rings_asteroids0", 256f, 3, Color.white, 256f, 1900, 110, Terrain.RING, "Inner Asteroid belt");

        system.addRingBand(star, "misc", "rings_ice0", 256f, 2, Color.white, 256f, 2000, 110, Terrain.RING, "Inner Asteroid belt");

        //Small planet inside inner asteroid belt
        PlanetAPI planet1 = system.addPlanet("nsp_insulinde0", star, "Insulinde I", Planets.IRRADIATED,
                230, //angle
                40f, //radius
                1730, //distance from star
                95f); //how many days to orbit
        //planet1.setCustomDescriptionId("nsp_revanchol");
        planet1.setDiscoverable(false);


        //Tri Tach Center of power in the system. Bombarded and Twice lost in early conflict with Revanchol
        //It mostly hosts entrenched military divisions of Tri-Tachyon.
        //Name for planet taken from wiki, supposedly city on other island in Insulinde isola
        PlanetAPI Deora = system.addPlanet("nsp_deora", star, "Deora-of-the-Seven-Seas", Planets.BARREN_BOMBARDED,
                180, //angle
                80f, //radius
                2550, //distance from star
                145f); //how many days to orbit
        Deora.setCustomDescriptionId("nsp_deora");
        Deora.setDiscoverable(false);

        MarketAPI Deora_Marketplace = NSP_addMarketplace.NSP_addMarketplace(
                Factions.TRITACHYON,
                Deora, //the PlanetAPI variable that this market will be assigned to
                null, //some mods and vanilla will have additional floating space stations or other entities, that when accessed, will open this marketplace. We don't have any associated entities for this method to add, so we leave null
                "Deora-of-the-Seven-Seas", //Display name of market
                4, //population size
                new ArrayList<>(Arrays.asList( //List of conditions for this method to iterate through and add to the market
                        Conditions.POPULATION_4,
                        Conditions.DECIVILIZED_SUBPOP,
                        Conditions.NO_ATMOSPHERE,
                        Conditions.HOT,
                        Conditions.IRRADIATED,
                        Conditions.ORE_SPARSE,
                        Conditions.RARE_ORE_SPARSE,
                        Conditions.SOLAR_ARRAY
                )),
                new ArrayList<>(Arrays.asList( //list of submarkets for this method to iterate through and add to the market. if a military base industry was added to this market, it would be consistent to add a military submarket too
                        Submarkets.SUBMARKET_OPEN, //add a default open market
                        Submarkets.SUBMARKET_STORAGE, //add a player storage market
                        Submarkets.SUBMARKET_BLACK, //add a black market
                        Submarkets.GENERIC_MILITARY
                )),
                new ArrayList<>(Arrays.asList( //list of industries for this method to iterate through and add to the market
                        Industries.POPULATION, //population industry is required for weirdness to not happen
                        Industries.SPACEPORT, //same with spaceport
                        Industries.WAYSTATION,
                        Industries.HEAVYBATTERIES,
                        Industries.BATTLESTATION_HIGH,
                        Industries.MILITARYBASE
                )),
                true, //if true, the planet will have visual junk orbiting and will play an ambient chatter audio track when the player is nearby
                false //used by the method to make a market hidden like a pirate base, not recommended for generating markets in a core world
        );

        planet1.setMarket(Deora_Marketplace);
        Deora_Marketplace.reapplyIndustries();

        ////////////////////////////////////////////////////////

        // Actual Revanchol - from wiki descriptions quite unremarkable outside recent communist revolution and reactionary take over.
        PlanetAPI Revanchol = system.addPlanet("nsp_revanchol", star, "Revanchol", Planets.PLANET_TERRAN_ECCENTRIC,
                0, //angle
                130f, //radius
                4500f, //distance from star
                600f); //how many days to orbit
        Revanchol.setCustomDescriptionId("nsp_revanchol");
        Revanchol.setDiscoverable(false);

        MarketAPI Revanchol_Marketplace = NSP_addMarketplace.NSP_addMarketplace(
                Factions.INDEPENDENT,
                Revanchol, //the PlanetAPI variable that this market will be assigned to
                null,
                "Revanchol", //Display name of market
                6, //population size
                new ArrayList<>(Arrays.asList( //List of conditions for this method to iterate through and add to the market
                        Conditions.MILD_CLIMATE,
                        Conditions.POPULATION_6,
                        Conditions.FARMLAND_ADEQUATE,
                        Conditions.FREE_PORT,
                        Conditions.HABITABLE,
                        Conditions.POLLUTION
                )),
                new ArrayList<>(Arrays.asList(
                        Submarkets.SUBMARKET_OPEN, //add a default open market
                        Submarkets.SUBMARKET_STORAGE, //add a player storage market
                        Submarkets.SUBMARKET_BLACK, //add a black market
                        Submarkets.GENERIC_MILITARY
                )),
                new ArrayList<>(Arrays.asList( //list of industries for this method to iterate through and add to the market
                        Industries.POPULATION, //population industry is required for weirdness to not happen
                        Industries.SPACEPORT, //same with spaceport
                        Industries.COMMERCE,
                        Industries.WAYSTATION,
                        Industries.FARMING,
                        Industries.LIGHTINDUSTRY,
                        Industries.GROUNDDEFENSES,
                        Industries.MILITARYBASE
                )),
                true, //if true, the planet will have visual junk orbiting and will play an ambient chatter audio track when the player is nearby
                false //used by the method to make a market hidden like a pirate base, not recommended for generating markets in a core world
        );
        Revanchol_Marketplace.setFreePort(true);
        FreeMarket.get(Revanchol_Marketplace).setDaysActive(368);


        //Revanchol makeshift Relay
        SectorEntityToken loc2 = system.addCustomEntity(null,null, "comm_relay_makeshift", Factions.INDEPENDENT);
        loc2.getMemoryWithoutUpdate().set("$nsp_InsulindeRelay", true);
        loc2.setCircularOrbitPointingDown(star, 65, 8100, 170);

        SectorEntityToken miningstation = system.addCustomEntity("nsp_revanchol_miningstation",
                "Revanchol's Mining Station", "station_mining00", Factions.INDEPENDENT, 75, 75f, 75f);

        miningstation.setCircularOrbitPointingDown(star, 45, 7200, 350);
        miningstation.setCustomDescriptionId("nsp_revanchol_miningstation");

        miningstation.getDetectedRangeMod().modifyFlat("gen", 5000f);

        miningstation.setDiscoverable(false);
        miningstation.addTag(Tags.STATION);

        MarketAPI miningstation_Marketplace = NSP_addMarketplace.NSP_addMarketplace(
                Factions.INDEPENDENT, //Factions.INDEPENDENT references the id String of the Independent faction, so it is the same as writing "independent", but neater. This determines the Faction associated with this market
                miningstation, //the PlanetAPI variable that this market will be assigned to
                null, //some mods and vanilla will have additional floating space stations or other entities, that when accessed, will open this marketplace. We don't have any associated entities for this method to add, so we leave null
                "Revanchol's Mining Station", //Display name of market
                4, //population size
                new ArrayList<>(Arrays.asList( //List of conditions for this method to iterate through and add to the market
                        Conditions.POPULATION_3,
                        Conditions.HABITABLE,
                        Conditions.RARE_ORE_MODERATE, // little space mining
                        Conditions.ORE_ABUNDANT,
                        Conditions.VOLATILES_TRACE
                )),
                new ArrayList<>(Arrays.asList( //list of submarkets for this method to iterate through and add to the market.
                        Submarkets.SUBMARKET_OPEN, //add a default open market
                        Submarkets.SUBMARKET_STORAGE, //add a player storage market
                        Submarkets.SUBMARKET_BLACK
                )),
                new ArrayList<>(Arrays.asList( //list of industries for this method to iterate through and add to the market
                        Industries.POPULATION, //population industry is required for weirdness to not happen
                        Industries.SPACEPORT, //same with spaceport
                        Industries.MINING,
                        Industries.REFINING,
                        Industries.PATROLHQ,
                        Industries.ORBITALSTATION
                )),
                false, //if true, the planet will have visual junk orbiting and will play an ambient chatter audio track when the player is nearby
                false
        );

        miningstation.setMarket(miningstation_Marketplace);
        miningstation_Marketplace.reapplyIndustries();


        //Outer Asteroid Belt
        //Ringband texture is referenced by png name in starsector-core\graphics\planets
        system.addRingBand(star, "misc", "rings_asteroids0", 256f, 1, Color.white, 256f, 6350, 300, Terrain.RING, "Outer Asteroid belt");
        system.addRingBand(star, "misc", "rings_ice0", 256f, 2, Color.white, 256f, 6500, 300, Terrain.RING, "Outer Asteroid belt");
        system.addAsteroidBelt(star, 120, 6600, 150, 280, 320, Terrain.ASTEROID_BELT, "Outer Asteroid belt");
        system.addRingBand(star, "misc", "rings_asteroids0", 256f, 3, Color.white, 256f, 6750, 300, Terrain.RING, "Outer Asteroid belt");

        system.addRingBand(star, "misc", "rings_asteroids0", 256f, 0, Color.white, 256f, 6900, 300, Terrain.RING, "Outer Asteroid belt");
        system.addAsteroidBelt(star, 90, 6950, 150, 100, 110, Terrain.ASTEROID_BELT, "Outer Asteroid belt");
        system.addRingBand(star, "misc", "rings_dust0", 256f, 3, Color.white, 256f, 7000, 300, Terrain.RING, "Outer Asteroid belt");

        system.addRingBand(star, "misc", "rings_asteroids0", 256f, 1, Color.white, 256f, 7450, 300, Terrain.RING, "Outer Asteroid belt");
        system.addAsteroidBelt(star, 120, 7520, 150, 280, 320, Terrain.ASTEROID_BELT, "Outer Asteroid belt");
        system.addRingBand(star, "misc", "rings_ice0", 256f, 0, Color.white, 256f, 7550, 300, Terrain.RING, "Outer Asteroid belt");



        //Outer Ice giant
        PlanetAPI planet2 = system.addPlanet("nsp_insulinde1", star, "Insulinde IV", Planets.ICE_GIANT,
                230, //angle
                215f, //radius
                9500, //distance from star
                255f); //how many days to orbit
        //planet1.setCustomDescriptionId("nsp_revanchol");
        planet2.setDiscoverable(false);



        system.addRingBand(planet2, "misc", "rings_special0", 256f, 0, Color.white, 256f, 480, 300, Terrain.RING, "Ice rings of Insulinde IV");
        system.addAsteroidBelt(planet2, 6, 500, 190, 240, 320, Terrain.ASTEROID_BELT, "Outer Asteroid belt");
        system.addRingBand(planet2, "misc", "rings_dust0", 256f, 0, Color.white, 256f, 550, 300, Terrain.RING, "Ice rings of Insulinde IV");

        //Moon around the Ice giant
        PlanetAPI planet2a = system.addPlanet("nsp_insulinde1a", planet2, "Insulinde IVa", Planets.FROZEN,
                230, //angle
                55f, //radius
                695, //distance from star
                78f); //how many days to orbit
        //planet1.setCustomDescriptionId("nsp_revanchol");
        planet2a.setDiscoverable(false);

        Misc.setAllPlanetsSurveyed(system, true);

        // autogenerate jump points
        system.autogenerateHyperspaceJumpPoints(true, true);

        // delete surrounding hyperspace
        HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin(); //these lines clear the hyperspace clouds around the system
        NebulaEditor editor = new NebulaEditor(plugin);
        float minRadius = plugin.getTileSize() * 2f;

        float radius = system.getMaxRadiusInHyperspace();
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f);
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0, 360f, 0.25f);

        ////////////////////////////////////////////////////////



        //Creates administrators, quartermasters and stuff for system created after initial economy gen
        CoreLifecyclePluginImpl.createInitialPeople(Deora_Marketplace, new Random());
        CoreLifecyclePluginImpl.createInitialPeople(Revanchol_Marketplace, new Random());
        CoreLifecyclePluginImpl.createInitialPeople(miningstation_Marketplace, new Random());
        CoreLifecyclePluginImpl.addMissingPeople();
    }
}

