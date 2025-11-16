package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.DefenderDataOverride;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import org.lwjgl.util.vector.Vector2f;
import data.scripts.util.TLL_factions;

import java.awt.*;

public class tll_system implements SectorGeneratorPlugin {
	public static String NOT_RANDOM_MISSION_TARGET = "$not_random_mission_target";
	@Override
	public void generate(SectorAPI sector) {
		Vector2f location  = new Vector2f(-29697,13959);

		StarSystemAPI system = Global.getSector().createStarSystem("Alphard");
		system.setName("Alphard");
		system.getLocation().set(location);
		system.initNonStarCenter();
		system.generateAnchorIfNeeded();
	    system.addTag(Tags.THEME_REMNANT);
		system.addTag(Tags.THEME_HIDDEN);
		system.addTag(Tags.THEME_SPECIAL);


		system.setBackgroundTextureFilename("graphics/backgrounds/background5.jpg");

		//In the Abyss


		HyperspaceTerrainPlugin hyperTerrain = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
		NebulaEditor editor = new NebulaEditor(hyperTerrain);
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, 200, 0, 360f);

		//Vigil Star
		PlanetAPI alphard = system.initStar("tll_alphard", // unique id for this star
				"star_blue_supergiant", // id in planets.json
				1500f, 		  // radius (in pixels at default zoom)
				700, // corona
				10f, // solar wind burn level
				0.7f, // flare probability
				3.0f); // CR loss multiplier, good values are in the range of 1-5

		system.setLightColor(new Color(20, 190, 200));
		system.getLocation().set(location);
		//Maddie Fractal World

		system.addAsteroidBelt(alphard, 300, 9000, 1000, 160, 220); // Ring system located between inner and outer planets
		system.addRingBand(alphard, "misc", "rings_asteroids0", 256f, 1, Color.white, 256f, 8900, 200f, null, null);
		system.addRingBand(alphard, "misc", "rings_dust0", 256f, 1, Color.white, 256f, 9000, 200f, null, null);
		PlanetAPI pyyhrus = system.addPlanet("tll_pyyrhus", alphard, "Pyyrhus", "gas_giant", 50, 300, 9000, 400);
		pyyhrus.setCustomDescriptionId("tll_pyyhrus");
		pyyhrus.getMarket().addCondition(Conditions.HIGH_GRAVITY);
		pyyhrus.getMarket().addCondition(Conditions.VOLATILES_PLENTIFUL);
		pyyhrus.getMarket().addCondition(Conditions.DENSE_ATMOSPHERE);
		pyyhrus.getMarket().addCondition(Conditions.EXTREME_WEATHER);
		pyyhrus.getMarket().addCondition(Conditions.HIGH_GRAVITY);
		pyyhrus.getMarket().addCondition(Conditions.POPULATION_5);
		pyyhrus.getMarket().getMemoryWithoutUpdate().set(NOT_RANDOM_MISSION_TARGET, true);
		pyyhrus.getMemoryWithoutUpdate().set("$tll_pyyrhustag", true);
		pyyhrus.getMarket().setFactionId("tll");
		SectorEntityToken field = system.addTerrain(Terrain.MAGNETIC_FIELD,
				new MagneticFieldTerrainPlugin.MagneticFieldParams(150f, // terrain effect band width
						320, // terrain effect middle radius
						pyyhrus, // entity that it's around
						309f, // visual band start
						710f, // visual band end
						new Color(130, 60, 150, 130), // base color
						4f, // probability to spawn aurora sequence, checked once/day when no aurora in progress
						new Color(130, 60, 150, 130),
						new Color(150, 30, 120, 150),
						new Color(10, 190, 200, 190),
						new Color(100, 200, 150, 240),
						new Color(100, 200, 130, 255),
						new Color(75, 90, 160, 255),
						new Color(100, 200, 200, 240)));
		field.setCircularOrbit(pyyhrus, 0, 0, 75);


		PlanetAPI vokha = system.addPlanet("tll_vokha", pyyhrus, "Vokha", "barren-desert", 50, 60, 720, 90);
		vokha.setCustomDescriptionId("tll_vokha");
		vokha.getMarket().addCondition(Conditions.THIN_ATMOSPHERE);
		vokha.getMarket().addCondition(Conditions.HOT);
		vokha.getMarket().addCondition(Conditions.DARK);
		vokha.getMarket().addCondition(Conditions.RUINS_SCATTERED);
		vokha.getMarket().addCondition(Conditions.RARE_ORE_SPARSE);
		vokha.getMarket().addCondition(Conditions.ORE_MODERATE);
		vokha.getMarket().addCondition(Conditions.POPULATION_5);
		vokha.getMarket().getMemoryWithoutUpdate().set(NOT_RANDOM_MISSION_TARGET, true);
		vokha.getMemoryWithoutUpdate().set("$tll_vokhatag", true);
		vokha.getMarket().setFactionId("tll");
		SectorEntityToken field2 = system.addTerrain(Terrain.MAGNETIC_FIELD,
				new MagneticFieldTerrainPlugin.MagneticFieldParams(150f, // terrain effect band width
						320, // terrain effect middle radius
						vokha, // entity that it's around
						61f, // visual band start
						200f, // visual band end
						new Color(130, 60, 150, 130), // base color
						2f, // probability to spawn aurora sequence, checked once/day when no aurora in progress
						new Color(130, 60, 150, 130),
						new Color(150, 30, 120, 150),
						new Color(10, 190, 200, 190),
						new Color(100, 200, 150, 240),
						new Color(100, 200, 130, 255),
						new Color(75, 90, 160, 255),
						new Color(100, 200, 200, 240)));
		field2.setCircularOrbit(pyyhrus, 0, 0, 75);


		SectorEntityToken anchor = system.getHyperspaceAnchor();
		CustomCampaignEntityAPI beacon = Global.getSector().getHyperspace().addCustomEntity("tll_warning_beacon", "TLL Warning Beacon", "warning_beacon", TLL_factions.TELLASIAN_LEAGUE);
		beacon.setCircularOrbitPointingDown(anchor, 100, 300, 65f);
		Color glowColor = new Color(250,55,0,255);
		Color pingColor = new Color(250,55,0,255);
		Misc.setWarningBeaconColors(beacon, glowColor, pingColor);
		beacon.getMemoryWithoutUpdate().set("$tll_beacontag", true);
		beacon.setCustomDescriptionId("tll_warningbeacon");
		beacon.getMemoryWithoutUpdate().set("$tll_warningbeacontag", true);
		Misc.setDefenderOverride(
				beacon,//Code name foir the entity
				new DefenderDataOverride("tll", //code name for the faction doing the defender
						0.5f, //Probibility there will be defenders
						40, // Minimum fleet points for the defenders
						60 // Maxmimum fleet points for the defenders
				));

		//SuperDerelictSeededFleetManager(
		//		SectorEntityToken manufacturingcenter, // Where the thing is spawning from
		//float thresholdLY, //How far away your fleet is from this thing before it starts spawning things (to save on CPU)
		//int minFleets, //(The minimum number o ffleets to spawn)
		//int maxFleets 10,//(The maximum number o ffleets to spawn)
		//float respawnDelay, //(How long it takes for a fleet to spawn in days up to max fleets)
		//int minPts, //(How small the fleet can be)
		//int maxPts) //(How large the fleet can be)
		
		//Or, written in this form:
		//system.addScript(new SuperDerelictSeededFleetManager(entity, thresholdLY, minFleets, maxFleets, respawnDelay, minPts, maxPts));
		//For example:
		
		

		SectorEntityToken VigilRelay = system.addCustomEntity(null, "Comm Relay", "comm_relay", "tll");
		VigilRelay.setCircularOrbit(alphard, 190, 7000, 280);

		SectorEntityToken VigilBuoy = system.addCustomEntity(null, "Nav Buoy", "nav_buoy", "tll"); // Makeshift nav buoy at L5 of Orguk
		VigilBuoy.setCircularOrbit(alphard, 310, 7000, 280);

		PlanetAPI brigid = system.addPlanet("tll_brigid", alphard, "Brigid", "barren-bombarded", 50, 80, 3800, 90);
		brigid.setCustomDescriptionId("tll_brigid");
		brigid.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
		brigid.getMarket().addCondition(Conditions.HIGH_GRAVITY);
		brigid.getMarket().addCondition(Conditions.COLD);
		brigid.getMarket().addCondition(Conditions.RARE_ORE_RICH);
		brigid.getMarket().addCondition(Conditions.ORE_RICH);
		brigid.getMarket().addCondition(Conditions.POPULATION_4);
		brigid.getMarket().getMemoryWithoutUpdate().set(NOT_RANDOM_MISSION_TARGET, true);
		brigid.getMemoryWithoutUpdate().set("$tll_brigidtag", true);
		Misc.setDefenderOverride(
				brigid,//Code name foir the entity
				new DefenderDataOverride("tll", //code name for the faction doing the defender
						1f, //Probibility there will be defenders
						150, // Minimum fleet points for the defenders
						300 // Maxmimum fleet points for the defenders
				));


		PlanetAPI habplanet = system.addPlanet("tll_tellas", alphard, "Tellas", "terran", 50, 75, 5900, 90);
		habplanet.setCustomDescriptionId("tll_tellas");
		habplanet.getMarket().addCondition(Conditions.HABITABLE);
		habplanet.getMarket().addCondition(Conditions.FARMLAND_BOUNTIFUL);
		habplanet.getMarket().addCondition(Conditions.ORE_SPARSE);
		habplanet.getMarket().addCondition(Conditions.MILD_CLIMATE);
		habplanet.getMarket().addCondition(Conditions.SOLAR_ARRAY);
		habplanet.getMarket().addCondition(Conditions.POPULATION_7);
		habplanet.getMarket().getMemoryWithoutUpdate().set(NOT_RANDOM_MISSION_TARGET, true);
		habplanet.getMemoryWithoutUpdate().set("$tll_tellastag", true);
		Misc.setDefenderOverride(
				habplanet,//Code name foir the entity
				new DefenderDataOverride("tll", //code name for the faction doing the defender
						1f, //Probibility there will be defenders
						150, // Minimum fleet points for the defenders
						300 // Maxmimum fleet points for the defenders
				));


		PlanetAPI nisse = system.addPlanet("tll_nisse", alphard, "Nisse", "cryovolcanic", 50, 50, 12000, 450);
		nisse.setCustomDescriptionId("tll_nisse");
		nisse.getMarket().addCondition(Conditions.EXTREME_WEATHER);
		nisse.getMarket().addCondition(Conditions.VERY_COLD);
		nisse.getMarket().addCondition(Conditions.RUINS_SCATTERED);
		nisse.getMarket().addCondition(Conditions.RARE_ORE_SPARSE);
		nisse.getMarket().addCondition(Conditions.ORE_SPARSE);
		nisse.getMarket().addCondition(Conditions.TECTONIC_ACTIVITY);
		nisse.getMarket().addCondition(Conditions.POPULATION_4);
		nisse.getMarket().getMemoryWithoutUpdate().set(NOT_RANDOM_MISSION_TARGET, true);
		nisse.getMemoryWithoutUpdate().set("$tll_nissetag", true);
		Misc.setDefenderOverride(
				nisse,//Code name foir the entity
				new DefenderDataOverride("tll", //code name for the faction doing the defender
						1f, //Probibility there will be defenders
						150, // Minimum fleet points for the defenders
						300 // Maxmimum fleet points for the defenders
				));


		SectorEntityToken deadgate = system.addCustomEntity(
				"tll_deadgate",
				"Inactive Gate",
				"inactive_gate",
				"neutral");
		deadgate.setCircularOrbitPointingDown(alphard, 270, 4900, 175);
		deadgate.setCustomDescriptionId("tll_deadgate");
		deadgate.setInteractionImage("illustrations", "dead_gate");
		deadgate.getMemoryWithoutUpdate().set("$tll_deadgatetag", true);

		//Jump points
		system.autogenerateHyperspaceJumpPoints(true, true, true);

	}
}













