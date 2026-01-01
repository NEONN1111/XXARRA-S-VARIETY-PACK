package neon.nsp.data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.DefenderDataOverride;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class nsp_gate implements SectorGeneratorPlugin {
	public static String NOT_RANDOM_MISSION_TARGET = "$not_random_mission_target";

	@Override
	public void generate(SectorAPI sector) {
		Vector2f location  = new Vector2f(-41517, -22061);

		StarSystemAPI system = Global.getSector().createStarSystem("Vigil");
		system.setName("Vigil");
		system.getLocation().set(location);
		system.initNonStarCenter();
		system.generateAnchorIfNeeded();
	    system.addTag(Tags.THEME_DERELICT);
		system.addTag(Tags.THEME_HIDDEN);
		system.addTag(Tags.THEME_UNSAFE);
		system.addTag(Tags.THEME_DERELICT_PROBES);

		system.setBackgroundTextureFilename("graphics/backgrounds/background4.jpg");

		//In the Abyss


		HyperspaceTerrainPlugin hyperTerrain = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
		NebulaEditor editor = new NebulaEditor(hyperTerrain);
		editor.clearArc(system.getLocation().x, system.getLocation().y, 0, 200, 0, 360f);

		//Vigil Star
		PlanetAPI gate = system.initStar("nsp_gate", // unique id for this star
				"star_red_supergiant", // id in planets.json
				1500f, 		  // radius (in pixels at default zoom)
				700, // corona
				10f, // solar wind burn level
				0.7f, // flare probability
				3.0f); // CR loss multiplier, good values are in the range of 1-5

		system.setLightColor(new Color(255, 80, 0));
		system.getLocation().set(location);
		//Maddie Fractal World

		system.addAsteroidBelt(gate, 300, 9000, 1000, 160, 220); // Ring system located between inner and outer planets
		system.addRingBand(gate, "misc", "rings_asteroids0", 256f, 1, Color.white, 256f, 8900, 200f, null, null);
		system.addRingBand(gate, "misc", "rings_dust0", 256f, 1, Color.white, 256f, 9000, 200f, null, null);
		PlanetAPI fueldepot = system.addPlanet("nsp_fueldepot", gate, "DESIGNATION FFL-217-PS-RN", "gas_giant", 50, 300, 6000, 400);
		fueldepot.setCustomDescriptionId("nsp_fueldepot");
		fueldepot.getMarket().addCondition(Conditions.HIGH_GRAVITY);
		fueldepot.getMarket().addCondition(Conditions.VOLATILES_PLENTIFUL);
		fueldepot.getMarket().addCondition(Conditions.DENSE_ATMOSPHERE);
		fueldepot.getMarket().addCondition(Conditions.EXTREME_WEATHER);
		fueldepot.getMarket().getMemoryWithoutUpdate().set(NOT_RANDOM_MISSION_TARGET, true);
		fueldepot.getMemoryWithoutUpdate().set("$nsp_fueldepottag", true);

		SectorEntityToken field = system.addTerrain(Terrain.MAGNETIC_FIELD,
				new MagneticFieldTerrainPlugin.MagneticFieldParams(150f, // terrain effect band width
						320, // terrain effect middle radius
						fueldepot, // entity that it's around
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
		field.setCircularOrbit(fueldepot, 0, 0, 75);
		Misc.setDefenderOverride(
				fueldepot,//Code name foir the entity
				new DefenderDataOverride("derelict", //code name for the faction doing the defender
						1f, //Probibility there will be defenders
						150, // Minimum fleet points for the defenders
						200 // Maxmimum fleet points for the defenders
				));


		PlanetAPI manufacturingcenter2 = system.addPlanet("nsp_manufacturingcenter2", fueldepot, "DESIGNATION MNG-5541-PS-RN", "barren_castiron", 50, 60, 720, 90);
		manufacturingcenter2.setCustomDescriptionId("nsp_manufacturingcenter2");
		manufacturingcenter2.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
		manufacturingcenter2.getMarket().addCondition(Conditions.COLD);
		manufacturingcenter2.getMarket().addCondition(Conditions.DARK);
		manufacturingcenter2.getMarket().addCondition(Conditions.RUINS_SCATTERED);
		manufacturingcenter2.getMarket().addCondition(Conditions.RARE_ORE_SPARSE);
		manufacturingcenter2.getMarket().addCondition(Conditions.ORE_MODERATE);
		manufacturingcenter2.getMarket().getMemoryWithoutUpdate().set(NOT_RANDOM_MISSION_TARGET, true);
		manufacturingcenter2.getMemoryWithoutUpdate().set("$nsp_manufacturingcenter2tag", true);
		manufacturingcenter2.getMarket().setFactionId("derelict");
		SectorEntityToken field2 = system.addTerrain(Terrain.MAGNETIC_FIELD,
				new MagneticFieldTerrainPlugin.MagneticFieldParams(150f, // terrain effect band width
						320, // terrain effect middle radius
						manufacturingcenter2, // entity that it's around
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
		field2.setCircularOrbit(fueldepot, 0, 0, 75);
		Misc.setDefenderOverride(
				manufacturingcenter2,//Code name foir the entity
				new DefenderDataOverride("derelict", //code name for the faction doing the defender
						1f, //Probibility there will be defenders
						190, // Minimum fleet points for the defenders
						300 // Maxmimum fleet points for the defenders
				));


		SectorEntityToken anchor = system.getHyperspaceAnchor();
		CustomCampaignEntityAPI beacon = Global.getSector().getHyperspace().addCustomEntity("warning_beacon_nsp", "Warning Beacon", "warning_beacon", Factions.DERELICT);
		beacon.setCircularOrbitPointingDown(anchor, 100, 300, 65f);
		Color glowColor = new Color(250,55,0,255);
		Color pingColor = new Color(250,55,0,255);
		Misc.setWarningBeaconColors(beacon, glowColor, pingColor);
		beacon.getMemoryWithoutUpdate().set("$nsp_beacontag", true);
		beacon.setCustomDescriptionId("nsp_warningbeacon");
		beacon.getMemoryWithoutUpdate().set("$nsp_warningbeacontag", true);
		Misc.setDefenderOverride(
				beacon,//Code name foir the entity
				new DefenderDataOverride("derelict", //code name for the faction doing the defender
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
		
		

		SectorEntityToken VigilRelay = system.addCustomEntity(null, "Comm Relay", "comm_relay", "derelict");
		VigilRelay.setCircularOrbit(gate, 190, 7000, 280);

		SectorEntityToken VigilBuoy = system.addCustomEntity(null, "Nav Buoy", "nav_buoy", "derelict"); // Makeshift nav buoy at L5 of Orguk
		VigilBuoy.setCircularOrbit(gate, 310, 7000, 280);

		PlanetAPI manufacturingcenter = system.addPlanet("nsp_manufacturingcenter", gate, "DESIGNATION MNG-5540-PS-RN", "barren_castiron", 50, 80, 3800, 90);
		manufacturingcenter.setCustomDescriptionId("nsp_manufacturingcenter");
		manufacturingcenter.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
		manufacturingcenter.getMarket().addCondition(Conditions.LOW_GRAVITY);
		manufacturingcenter.getMarket().addCondition(Conditions.COLD);
		manufacturingcenter.getMarket().addCondition(Conditions.RUINS_WIDESPREAD);
		manufacturingcenter.getMarket().addCondition(Conditions.RARE_ORE_RICH);
		manufacturingcenter.getMarket().addCondition(Conditions.ORE_RICH);
		manufacturingcenter.getMarket().getMemoryWithoutUpdate().set(NOT_RANDOM_MISSION_TARGET, true);
		manufacturingcenter.getMemoryWithoutUpdate().set("$nsp_manufacturingcentertag", true);
		Misc.setDefenderOverride(
				manufacturingcenter,//Code name foir the entity
				new DefenderDataOverride("derelict", //code name for the faction doing the defender
						1f, //Probibility there will be defenders
						150, // Minimum fleet points for the defenders
						300 // Maxmimum fleet points for the defenders
				));


		PlanetAPI habplanet = system.addPlanet("nsp_habplanet", gate, "DESIGNATION HBT-1030789-PS-RN", "terran-eccentric", 50, 75, 5900, 90);
		habplanet.setCustomDescriptionId("nsp_habplanet");
		habplanet.getMarket().addCondition(Conditions.HABITABLE);
		habplanet.getMarket().addCondition(Conditions.RUINS_VAST);
		habplanet.getMarket().addCondition(Conditions.FARMLAND_ADEQUATE);
		habplanet.getMarket().addCondition(Conditions.ORE_SPARSE);
		habplanet.getMarket().addCondition(Conditions.MILD_CLIMATE);
		habplanet.getMarket().getMemoryWithoutUpdate().set(NOT_RANDOM_MISSION_TARGET, true);
		habplanet.getMemoryWithoutUpdate().set("$nsp_habplanettag", true);
		Misc.setDefenderOverride(
				habplanet,//Code name foir the entity
				new DefenderDataOverride("derelict", //code name for the faction doing the defender
						1f, //Probibility there will be defenders
						150, // Minimum fleet points for the defenders
						300 // Maxmimum fleet points for the defenders
				));


		PlanetAPI fringeworld = system.addPlanet("nsp_fringeworld", gate, "DESIGNATION HBT-1030799-PS-RN-FL", "cryovolcanic", 50, 50, 12000, 450);
		fringeworld.setCustomDescriptionId("nsp_fringeworld");
		fringeworld.getMarket().addCondition(Conditions.EXTREME_WEATHER);
		fringeworld.getMarket().addCondition(Conditions.VERY_COLD);
		fringeworld.getMarket().addCondition(Conditions.RUINS_SCATTERED);
		fueldepot.getMarket().addCondition(Conditions.TECTONIC_ACTIVITY);
		habplanet.getMarket().addCondition(Conditions.FARMLAND_ADEQUATE);
		habplanet.getMarket().addCondition(Conditions.ORE_SPARSE);
		habplanet.getMarket().getMemoryWithoutUpdate().set(NOT_RANDOM_MISSION_TARGET, true);
		habplanet.getMemoryWithoutUpdate().set("$nsp_fringeworldtag", true);
		Misc.setDefenderOverride(
				fringeworld,//Code name foir the entity
				new DefenderDataOverride("derelict", //code name for the faction doing the defender
						1f, //Probibility there will be defenders
						150, // Minimum fleet points for the defenders
						300 // Maxmimum fleet points for the defenders
				));


		SectorEntityToken deadgate = system.addCustomEntity(
				"nsp_deadgate",
				"Inactive Gate",
				"inactive_gate",
				"derelict");
		deadgate.setCircularOrbitPointingDown(gate, 270, 4900, 175);
		deadgate.setCustomDescriptionId("nsp_deadgate");
		deadgate.setInteractionImage("illustrations", "dead_gate");
		deadgate.getMemoryWithoutUpdate().set("$nsp_deadgatetag", true);


		SectorEntityToken miningstation = system.addCustomEntity(
				"nsp_miningstation",
				"Explorarium Mining Station",
				"station_mining_remnant",
				"derelict");
		miningstation.setCircularOrbitPointingDown(manufacturingcenter, 270, 200, 175);
		miningstation.setCustomDescriptionId("nsp_miningstation");
		miningstation.setInteractionImage("illustrations", "abandoned_station3");
		miningstation.addTag("nsp_miningstationtag");
		miningstation.getMemoryWithoutUpdate().set("$nsp_miningstationtag", true);
		Misc.setDefenderOverride(
				miningstation,//Code name foir the entity
				new DefenderDataOverride("derelict", //code name for the faction doing the defender
						1f, //Probibility there will be defenders
						150, // Minimum fleet points for the defenders
						300 // Maxmimum fleet points for the defenders
				));

		SectorEntityToken mothership = system.addCustomEntity(
				"nsp_mothership",
				"Explorarium Mothership",
				"derelict_mothership",
				"derelict");
		mothership.setCircularOrbitPointingDown(fueldepot, 270, 500, 400);
		mothership.setCustomDescriptionId("nsp_mothership");
		mothership.addTag("nsp_mothershiptag");
		mothership.getMemoryWithoutUpdate().set("$nsp_mothershiptag", true);

		SectorEntityToken surveyship = system.addCustomEntity(
				"nsp_surveyship",
				"Explorarium Survey Ship",
				"derelict_survey_ship",
				"derelict");
		surveyship.setCircularOrbitPointingDown(habplanet, 270, 225, 175);
		surveyship.setCustomDescriptionId("nsp_miningstation");
		surveyship.addTag("nsp_surveyshiptag");
		surveyship.getMemoryWithoutUpdate().set("$nsp_surveyshiptag", true);
		Misc.setDefenderOverride(
				surveyship,//Code name foir the entity
				new DefenderDataOverride("derelict", //code name for the faction doing the defender
						1f, //Probibility there will be defenders
						150, // Minimum fleet points for the defenders
						300 // Maxmimum fleet points for the defenders
				));

		//Jump points
		JumpPointAPI jumpPointAlpha = Global.getFactory().createJumpPoint("vigil_jump_point_alpha", "Vigil, Inner Jump-Point");
		OrbitAPI orbitVigil = Global.getFactory().createCircularOrbit(gate, 4000, 4300, 300);
		jumpPointAlpha.setOrbit(orbitVigil);
		jumpPointAlpha.setStandardWormholeToHyperspaceVisual();
		system.addEntity(jumpPointAlpha);

		JumpPointAPI jumpPointBeta = Global.getFactory().createJumpPoint("vigil_jump_point_beta", "Vigil, Jump-Point");
		OrbitAPI orbitVigil2 = Global.getFactory().createCircularOrbit(gate, 4000, 12000, 300);
		jumpPointBeta.setOrbit(orbitVigil2);
		jumpPointBeta.setStandardWormholeToHyperspaceVisual();
		system.addEntity(jumpPointBeta);
		system.autogenerateHyperspaceJumpPoints(true, true);

	}
}













