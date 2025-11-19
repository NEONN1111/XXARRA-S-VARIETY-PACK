package neon.nsp.data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.DefenderDataOverride;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import org.lwjgl.util.vector.Vector2f;
import neon.nsp.data.scripts.util.TLL_factions;

import java.awt.*;

public class tll_system2 implements SectorGeneratorPlugin {
	public static String NOT_RANDOM_MISSION_TARGET = "$not_random_mission_target";
	@Override
	public void generate(SectorAPI sector) {
		Vector2f location  = new Vector2f(-27697,11959);

		StarSystemAPI system = Global.getSector().createStarSystem("Alaugon");
		system.setName("Alaugon");
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
		PlanetAPI alaugon = system.initStar("tll_alaugon", // unique id for this star
				"star_yellow", // id in planets.json
				900f, 		  // radius (in pixels at default zoom)
				500, // corona
				6f, // solar wind burn level
				0.3f, // flare probability
				2.0f); // CR loss multiplier, good values are in the range of 1-5

		system.setLightColor(new Color(230,230,190));
		system.getLocation().set(location);

		system.addAsteroidBelt(alaugon, 300, 2600, 1000, 160, 220); // Ring system located between inner and outer planets
		system.addRingBand(alaugon, "misc", "rings_asteroids0", 256f, 1, Color.white, 256f, 14000, 200f, null, null);
		system.addRingBand(alaugon, "misc", "rings_dust0", 256f, 1, Color.white, 256f, 14500, 200f, null, null);



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
		
		

		SectorEntityToken AlaugonRelay = system.addCustomEntity(null, "Comm Relay", "comm_relay", "tll");
		AlaugonRelay.setCircularOrbit(alaugon, 190, 7000, 280);

		SectorEntityToken AlaugonBuoy = system.addCustomEntity(null, "Nav Buoy", "nav_buoy", "tll"); // Makeshift nav buoy at L5 of Orguk
		AlaugonBuoy.setCircularOrbit(alaugon, 310, 7000, 280);


		PlanetAPI vosta = system.addPlanet("tll_vosta", alaugon, "Vosta", "TLL_alpine", 50, 100, 9200, 330);
		vosta.setCustomDescriptionId("tll_vosta");
		vosta.getMarket().addCondition(Conditions.HABITABLE);
		vosta.getMarket().addCondition(Conditions.COLD);
		vosta.getMarket().addCondition(Conditions.FARMLAND_ADEQUATE);
		vosta.getMarket().addCondition(Conditions.ORE_ABUNDANT);
		vosta.getMarket().addCondition(Conditions.RARE_ORE_SPARSE);
		vosta.getMarket().addCondition(Conditions.HABITABLE);
		vosta.getMarket().addCondition(Conditions.POPULATION_6);
		vosta.getMarket().getMemoryWithoutUpdate().set(NOT_RANDOM_MISSION_TARGET, true);
		vosta.getMemoryWithoutUpdate().set("$tll_vostatag", true);

		PlanetAPI hades = system.addPlanet("tll_hades", alaugon, "Hades", "ice_giant", 50, 340, 6000, 450);
		hades.setCustomDescriptionId("tll_hades");
		hades.getMarket().addCondition(Conditions.VOLATILES_TRACE);
		hades.getMarket().addCondition(Conditions.VERY_COLD);
		hades.getMarket().addCondition(Conditions.HIGH_GRAVITY);
		hades.getMarket().getMemoryWithoutUpdate().set(NOT_RANDOM_MISSION_TARGET, true);
		hades.getMemoryWithoutUpdate().set("$tll_hades", true);

		system.addAsteroidBelt(hades, 300, 750, 300, 160, 220); // Ring system located between inner and outer planets
		system.addRingBand(hades, "misc", "rings_asteroids0", 256f, 1, Color.white, 256f, 14000, 200f, null, null);
		system.addRingBand(hades, "misc", "rings_dust0", 256f, 1, Color.white, 256f, 14500, 200f, null, null);

		PlanetAPI amphitrite = system.addPlanet("tll_amphitrite", hades, "Amphitrite", "water", 50, 75, 1200, 230);
		amphitrite.setCustomDescriptionId("tll_amphitrite");
		amphitrite.getMarket().addCondition(Conditions.WATER_SURFACE);
		amphitrite.getMarket().addCondition(Conditions.HABITABLE);
		amphitrite.getMarket().addCondition(Conditions.RUINS_SCATTERED);
		amphitrite.getMarket().addCondition(Conditions.MILD_CLIMATE);
		amphitrite.getMarket().addCondition(Conditions.POPULATION_4);
		amphitrite.getMarket().getMemoryWithoutUpdate().set(NOT_RANDOM_MISSION_TARGET, true);
		amphitrite.getMemoryWithoutUpdate().set("$tll_amphitrite", true);

		PlanetAPI vbarren1 = system.addPlanet("tll_vbarren1", hades, "Amoxyl", "barren", 50, 60, 1600, 520);
		vbarren1.setCustomDescriptionId("tll_vbarren1");
		vbarren1.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
		vbarren1.getMarket().getMemoryWithoutUpdate().set(NOT_RANDOM_MISSION_TARGET, true);
		vbarren1.getMemoryWithoutUpdate().set("$tll_vbarren1", true);

		PlanetAPI vbarren2 = system.addPlanet("tll_vbarren2", vbarren1, "Pyrite", "barren", 50, 45, 300, 100);
		vbarren2.setCustomDescriptionId("tll_vbarren2");
		vbarren2.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
		vbarren2.getMarket().addCondition(Conditions.LOW_GRAVITY);
		vbarren2.getMarket().getMemoryWithoutUpdate().set(NOT_RANDOM_MISSION_TARGET, true);
		vbarren2.getMemoryWithoutUpdate().set("$tll_vbarren2", true);

		PlanetAPI vbarren3 = system.addPlanet("tll_vbarren3", alaugon, "Uside", "barren-bombarded", 50, 40, 2600, 200);
		vbarren3.setCustomDescriptionId("tll_vbarren3");
		vbarren3.getMarket().addCondition(Conditions.NO_ATMOSPHERE);
		vbarren3.getMarket().addCondition(Conditions.LOW_GRAVITY);
		vbarren3.getMarket().addCondition(Conditions.IRRADIATED);
		vbarren3.getMarket().addCondition(Conditions.VERY_HOT);
		vbarren3.getMarket().getMemoryWithoutUpdate().set(NOT_RANDOM_MISSION_TARGET, true);
		vbarren3.getMemoryWithoutUpdate().set("$tll_vbarren3", true);


		SectorEntityToken deadgate2 = system.addCustomEntity(
				"tll_deadgate",
				"Inactive Gate",
				"inactive_gate",
				"neutral");
		deadgate2.setCircularOrbitPointingDown(alaugon, 270, 4900, 175);
		deadgate2.setCustomDescriptionId("tll_deadgate2");
		deadgate2.setInteractionImage("illustrations", "dead_gate");
		deadgate2.getMemoryWithoutUpdate().set("$tll_deadgatetag2", true);



		DebrisFieldTerrainPlugin.DebrisFieldParams params_alaugon_main = new DebrisFieldTerrainPlugin.DebrisFieldParams(
				350f, // field radius - should not go above 1000 for performance reasons
				1.5f, // density, visual - affects number of debris pieces
				10000000f, // duration in days
				0f); // days the field will keep generating glowing pieces
		params_alaugon_main.source = DebrisFieldTerrainPlugin.DebrisFieldSource.MIXED;
		params_alaugon_main.baseSalvageXP = 500; // base XP for scavenging in field
		SectorEntityToken debrisalaugon_main1 = Misc.addDebrisField(system, params_alaugon_main, StarSystemGenerator.random);
		debrisalaugon_main1.setSensorProfile(1000f);
		debrisalaugon_main1.setDiscoverable(true);
		debrisalaugon_main1.setCircularOrbit(alaugon, 120f, 9800, 365f);
		debrisalaugon_main1.setId("alaugon_main_debrisBelt");
		addDerelict(system, debrisalaugon_main1, "onslaught_xiv_Elite", ShipRecoverySpecial.ShipCondition.WRECKED, 100f,   false);
		addDerelict(system, debrisalaugon_main1, "tll_eagle_standard", ShipRecoverySpecial.ShipCondition.WRECKED, 130f,  true);
		addDerelict(system, debrisalaugon_main1, "cerberus_d_pirates_Standard", ShipRecoverySpecial.ShipCondition.WRECKED, 300f,  false);
		addDerelict(system, debrisalaugon_main1, "kite_pirates_Raider", ShipRecoverySpecial.ShipCondition.WRECKED, 270f,  false);
		addDerelict(system, debrisalaugon_main1, "dominator_XIV_Elite", ShipRecoverySpecial.ShipCondition.WRECKED, 380f, false);

		DebrisFieldTerrainPlugin.DebrisFieldParams params_alaugon_main2 = new DebrisFieldTerrainPlugin.DebrisFieldParams(
				350f, // field radius - should not go above 1000 for performance reasons
				1.5f, // density, visual - affects number of debris pieces
				10000000f, // duration in days
				0f); // days the field will keep generating glowing pieces
		params_alaugon_main2.source = DebrisFieldTerrainPlugin.DebrisFieldSource.MIXED;
		params_alaugon_main2.baseSalvageXP = 500; // base XP for scavenging in field
		SectorEntityToken debrisalaugon_main2 = Misc.addDebrisField(system, params_alaugon_main2, StarSystemGenerator.random);
		debrisalaugon_main2.setSensorProfile(1000f);
		debrisalaugon_main2.setDiscoverable(true);
		debrisalaugon_main2.setCircularOrbit(alaugon, 120f, 2700, 365f);
		debrisalaugon_main2.setId("alaugon_main_debrisBelt");
		addDerelict(system, debrisalaugon_main2, "hound_Starting", ShipRecoverySpecial.ShipCondition.WRECKED, 100f,   true);

		//Jump points
		system.autogenerateHyperspaceJumpPoints(true, true, true);

	}

	private void addDerelict(StarSystemAPI alaugon,
							 SectorEntityToken focus,
							 String variantId,
							 ShipRecoverySpecial.ShipCondition condition,
							 float orbitRadius,
							 boolean recoverable) {
		DerelictShipEntityPlugin.DerelictShipData params = new DerelictShipEntityPlugin.DerelictShipData(new ShipRecoverySpecial.PerShipData(variantId, condition), false);
		SectorEntityToken ship = BaseThemeGenerator.addSalvageEntity(alaugon, Entities.WRECK, Factions.NEUTRAL, params);
		ship.setDiscoverable(true);

		float orbitDays = orbitRadius / (10f + (float) Math.random() * 5f);
		ship.setCircularOrbit(focus, (float) Math.random() * 360f, orbitRadius, orbitDays);

		if (recoverable) {
			SalvageSpecialAssigner.ShipRecoverySpecialCreator creator = new SalvageSpecialAssigner.ShipRecoverySpecialCreator(null, 0, 0, false, null, null);
			Misc.setSalvageSpecial(ship, creator.createSpecial(ship, null));
		}
	}

}













